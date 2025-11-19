package org.example.bot;

import org.example.core.PollManager;
import org.example.core.model.Poll;
import org.example.core.model.Question;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MyBot extends TelegramLongPollingBot {
    private final PollManager polls;

    public MyBot(PollManager polls) {
        this.polls = polls;
    }

    @Override
    public String getBotUsername() {
        return "Marwa312Bot";
    }

    @Override
    public String getBotToken() {
        String raw = "8090118633:AAGIzvG4yK0lfKMSgGfhr-fxMxsdxmsOe9I";
        return raw.replaceAll("\\s+", "");
    }

    @Override
    public void onUpdateReceived(Update u) {
        try {
            if (u.hasMessage() && u.getMessage().hasText()) {
                long uid = u.getMessage().getFrom().getId();
                String txt = u.getMessage().getText().trim();

                if (isJoinText(txt)) {
                    boolean isNew = polls.addMember(uid);
                    if (isNew) {
                        String name = u.getMessage().getFrom().getFirstName();
                        send(uid, "Welcome, " + (name != null ? name : "friend") + "! You have joined the community.");

                        broadcastJoin(uid, name);
                    } else {
                        send(uid, "You are already in the community.");
                    }
                    return;
                }

                send(uid, "Send /start or 'Hi' to join the community.");
            }

            if (u.hasCallbackQuery()) {
                var cq = u.getCallbackQuery();
                long uid = cq.getFrom().getId();
                String data = cq.getData();

                try {
                    String[] parts = data.split(":");
                    long pid = Long.parseLong(parts[1]);
                    int qi = Integer.parseInt(parts[3]);
                    int oi = Integer.parseInt(parts[5]);

                    Poll active = polls.getActivePoll();
                    if (active == null || active.closed || active.id != pid) {
                        answerCallback(cq.getId(), "Poll is closed.");
                        return;
                    }

                    boolean ok = polls.submitAnswer(uid, qi, oi);
                    answerCallback(cq.getId(), ok ? "Recorded ✔" : "Already answered.");

                    sendNextQuestionIfAny(uid, active, qi + 1);

                } catch (Exception ex) {
                    answerCallback(cq.getId(), "Invalid response.");
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private boolean isJoinText(String t) {
        String s = t.toLowerCase(Locale.ROOT);
        return s.equals("/start") || s.equals("hi") || s.equals("hey") || s.equals("היי");
    }

    private void broadcastJoin(long newUserId, String name) {
        int size = polls.communitySize();
        String msg = (name != null ? name : "Someone") + " joined. Community size: " + size;

        Set<Long> members = polls.members();
        for (long id : members) {
            if (id == newUserId) continue;
            send(id, msg);
        }
    }

    public void broadcastPoll(Poll p) {
        for (long id : polls.members()) {
            sendQuestion(id, p, 0);
        }
    }

    private void sendNextQuestionIfAny(long uid, Poll p, int nextQi) {
        if (p.closed) return;
        if (nextQi < p.questions.size()) {
            sendQuestion(uid, p, nextQi);
        } else {
            send(uid, "Thanks! You finished the survey.");
        }
    }

    private void sendQuestion(long uid, Poll p, int qi) {
        if (qi >= p.questions.size()) return;

        Question q = p.questions.get(qi);

        List<String> opts = q.getOptions();

        List<List<InlineKeyboardButton>> rows = new ArrayList<List<InlineKeyboardButton>>();
        for (int i = 0; i < opts.size(); i++) {
            InlineKeyboardButton b = new InlineKeyboardButton(opts.get(i));
            b.setCallbackData("poll:" + p.id + ":q:" + qi + ":opt:" + i);
            List<InlineKeyboardButton> row = new ArrayList<InlineKeyboardButton>();
            row.add(b);
            rows.add(row);
        }

        InlineKeyboardMarkup kb = new InlineKeyboardMarkup();
        kb.setKeyboard(rows);

        String questionText = "Q" + (qi + 1) + ": " + q.getText();
        SendMessage m = new SendMessage(String.valueOf(uid), questionText);
        m.setReplyMarkup(kb);

        try {
            execute(m);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void send(long chatId, String text) {
        SendMessage m = new SendMessage(String.valueOf(chatId), text);
        try {
            execute(m);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void answerCallback(String callbackId, String text) {
        try {
            AnswerCallbackQuery a = new AnswerCallbackQuery();
            a.setCallbackQueryId(callbackId);
            a.setText(text);
            execute(a);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

}
