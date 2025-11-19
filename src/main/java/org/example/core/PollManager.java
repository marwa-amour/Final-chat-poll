package org.example.core;

import org.example.core.model.Poll;
import org.example.core.model.Question;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class PollManager {

    private static final int MIN_MEMBERS = 3;

    private final Set<Long> members = ConcurrentHashMap.newKeySet();
    public boolean addMember(long userId) { return members.add(userId); }
    public int communitySize() { return members.size(); }
    public Set<Long> members() { return members; }

    private final AtomicLong seq = new AtomicLong(1);

    private volatile Poll activePoll = null;

    public synchronized Poll getActivePoll() { return activePoll; }

    public Consumer<Poll> onPollReadyToSend = null;
    public Consumer<PollResults> onPollResults = null;

    // יצירת סקר חדש
    public synchronized Poll startPoll(long creatorUserId, List<Question> qs, int delayMinutes) {

        if (activePoll != null && !activePoll.closed)
            throw new IllegalStateException("A poll is already active");

        if (members.size() < MIN_MEMBERS)
            throw new IllegalStateException("Need at least " + MIN_MEMBERS + " community members");

        if (qs == null || qs.isEmpty() || qs.size() > 3)
            throw new IllegalArgumentException("Poll must have 1–3 questions");

        for (Question q : qs) {
            if (q.getOptions() == null || q.getOptions().size() < 2 || q.getOptions().size() > 4)
                throw new IllegalArgumentException("Each question must have 2–4 options");
        }

        long closeAt = System.currentTimeMillis() + 5 * 60_000L;

        Poll p = new Poll(seq.getAndIncrement(), creatorUserId, closeAt, qs);
        activePoll = p;

        // עיכוב לפני שליחה
        new Thread(() -> {
            try { Thread.sleep(Math.max(0, delayMinutes) * 60_000L); }
            catch (InterruptedException ignored) {}

            if (onPollReadyToSend != null)
                onPollReadyToSend.accept(p);

            scheduleClose(p);

        }).start();

        return p;
    }

    // תזמון סגירת הסקר
    private void scheduleClose(Poll p) {
        new Thread(() -> {
            long sleep = Math.max(0, p.closesAtMs - System.currentTimeMillis());

            try { Thread.sleep(sleep); }
            catch (InterruptedException ignored) {}

            closeIfOpen(p);
        }).start();
    }

    // קבלת תשובה ממשתמש
    public synchronized boolean submitAnswer(long userId, int questionIndex, int optionIndex) {
        Poll p = activePoll;

        if (p == null || p.closed) return false;
        if (!members.contains(userId)) return false;

        int qn = p.questions.size();
        if (questionIndex < 0 || questionIndex >= qn) return false;

        int optN = p.questions.get(questionIndex).getOptions().size();
        if (optionIndex < 0 || optionIndex >= optN) return false;

        int[] arr = p.answersByUser.computeIfAbsent(userId, k -> new int[qn]);

        if (arr[questionIndex] != 0) return false; // כבר ענה

        arr[questionIndex] = optionIndex + 1;

        if (allUsersAnswered(p))
            closeIfOpen(p);

        return true;
    }

    // בדיקת האם כל המשתמשים ענו על כל השאלות
    private boolean allUsersAnswered(Poll p) {
        if (p.answersByUser.size() < members.size()) return false;

        for (int[] arr : p.answersByUser.values()) {
            for (int v : arr)
                if (v == 0) return false;
        }

        return true;
    }

    // סגירת הסקר (מופעל רק פעם אחת)
    private synchronized void closeIfOpen(Poll p) {
        if (p.closed) return;

        p.closed = true;

        if (onPollResults != null)
            onPollResults.accept(computeResults(p));
    }

    // מבנה תוצאות
    public static class PollResults {
        public final List<List<Integer>> counts;
        public final List<List<Double>>  percents;
        public final List<Question> questions;

        public PollResults(List<List<Integer>> c, List<List<Double>> p, List<Question> q) {
            this.counts = c;
            this.percents = p;
            this.questions = q;
        }

        // מציג טקסט יפה לתוצאות
        public String formatAsText() {
            StringBuilder sb = new StringBuilder();

            sb.append("Survey results:\n\n");

            for (int qi = 0; qi < questions.size(); qi++) {
                Question q = questions.get(qi);
                sb.append("Q").append(qi + 1).append(": ").append(q.getText()).append("\n");

                List<Integer> cRow = counts.get(qi);
                List<Double>  pRow = percents.get(qi);

                int optN = q.getOptions().size();
                Integer[] idx = new Integer[optN];
                for (int i = 0; i < optN; i++) idx[i] = i;

                Arrays.sort(idx, (a, b) -> Double.compare(pRow.get(b), pRow.get(a)));

                for (int i : idx) {
                    sb.append("   - ")
                            .append(q.getOptions().get(i))
                            .append(": ")
                            .append(cRow.get(i))
                            .append(" votes (")
                            .append(String.format("%.1f", pRow.get(i)))
                            .append("%)\n");
                }
                sb.append("\n");
            }

            return sb.toString();
        }
    }

    // חישוב תוצאות הסקר
    private PollResults computeResults(Poll p) {

        List<List<Integer>> counts = new ArrayList<>();
        List<List<Double>>  perc   = new ArrayList<>();

        for (int qi = 0; qi < p.questions.size(); qi++) {

            int optN = p.questions.get(qi).getOptions().size();
            int[] c = new int[optN];

            for (int[] arr : p.answersByUser.values()) {
                int ans = arr[qi];
                if (ans > 0 && ans <= optN)
                    c[ans - 1]++;
            }

            int total = Arrays.stream(c).sum();

            List<Integer> cc = new ArrayList<>();
            List<Double> pp = new ArrayList<>();

            for (int v : c) {
                cc.add(v);
                pp.add(total == 0 ? 0.0 : (100.0 * v / total));
            }

            counts.add(cc);
            perc.add(pp);
        }

        return new PollResults(counts, perc, p.questions);
    }
}
