package org.example.core;

import org.example.core.model.Poll;
import org.example.core.model.Question;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class PollManager {
    // אפשר לשנות ל-3 בהגשה; לשם בדיקות ביתיות השארתי 1
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

    public synchronized Poll startPoll(long creatorUserId, List<Question> qs, int delayMinutes) {
        if (activePoll != null && !activePoll.closed)
            throw new IllegalStateException("A poll is already active");

        if (members.size() < MIN_MEMBERS)
            throw new IllegalStateException("Need at least " + MIN_MEMBERS + " community members");

        if (qs == null || qs.isEmpty() || qs.size() > 3)
            throw new IllegalArgumentException("Poll must have 1..3 questions");

        for (Question q : qs) {
            if (q.options == null || q.options.size() < 2 || q.options.size() > 4)
                throw new IllegalArgumentException("Each question must have 2–4 options");
        }

        long closeAt = System.currentTimeMillis() + 5 * 60_000L;
        Poll p = new Poll(seq.getAndIncrement(), creatorUserId, closeAt, qs);
        activePoll = p;

        new Thread(() -> {
            try { Thread.sleep(Math.max(0, delayMinutes) * 60_000L); } catch (InterruptedException ignored) {}
            if (onPollReadyToSend != null) onPollReadyToSend.accept(p);
            scheduleClose(p);
        }).start();

        return p;
    }

    private void scheduleClose(Poll p) {
        new Thread(() -> {
            long sleep = Math.max(0, p.closesAtMs - System.currentTimeMillis());
            try { Thread.sleep(sleep); } catch (InterruptedException ignored) {}
            closeIfOpen(p);
        }).start();
    }

    public synchronized boolean submitAnswer(long userId, int questionIndex, int optionIndex) {
        Poll p = activePoll;
        if (p == null || p.closed) return false;
        if (!members.contains(userId)) return false;

        int qn = p.questions.size();
        if (questionIndex < 0 || questionIndex >= qn) return false;

        int optN = p.questions.get(questionIndex).options.size();
        if (optionIndex < 0 || optionIndex >= optN) return false;

        int[] arr = p.answersByUser.computeIfAbsent(userId, k -> new int[qn]);
        if (arr[questionIndex] != 0) return false; // כבר ענה על השאלה הזו
        arr[questionIndex] = optionIndex + 1;

        if (allUsersAnswered(p)) closeIfOpen(p);
        return true;
    }

    private boolean allUsersAnswered(Poll p) {
        if (p.answersByUser.size() < members.size()) return false;
        for (int[] arr : p.answersByUser.values()) {
            for (int v : arr) if (v == 0) return false;
        }
        return true;
    }

    private synchronized void closeIfOpen(Poll p) {
        if (p.closed) return;
        p.closed = true;
        if (onPollResults != null) onPollResults.accept(computeResults(p));
    }

    public static class PollResults {
        public final List<List<Integer>> counts;
        public final List<List<Double>>  percents;
        public final List<org.example.core.model.Question> questions;
        public PollResults(List<List<Integer>> c, List<List<Double>> p, List<org.example.core.model.Question> q) {
            this.counts = c; this.percents = p; this.questions = q;
        }
    }

    private PollResults computeResults(Poll p) {
        List<List<Integer>> counts = new ArrayList<>();
        List<List<Double>>  perc   = new ArrayList<>();

        for (int qi = 0; qi < p.questions.size(); qi++) {
            int optN = p.questions.get(qi).options.size();
            int[] c = new int[optN];

            for (int[] arr : p.answersByUser.values()) {
                int ans = arr[qi];
                if (ans > 0 && ans <= optN) c[ans-1]++;
            }

            int total = 0; for (int v : c) total += v;
            List<Integer> cc = new ArrayList<>();
            List<Double>  pp = new ArrayList<>();
            for (int v : c) { cc.add(v); pp.add(total==0?0.0:(100.0*v/total)); }
            counts.add(cc); perc.add(pp);
        }
        return new PollResults(counts, perc, p.questions);
    }
}
