package org.example;

import org.example.core.PollManager;
import org.example.bot.MyBot;
import org.example.core.model.Poll;
import org.example.ui.PollBuilderFrame;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        PollManager polls = new PollManager();
        MyBot bot = new MyBot(polls);

        TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
        api.registerBot(bot);

// קישור שליחה
        polls.onPollReadyToSend = new Consumer<Poll>() {
            @Override
            public void accept(Poll p) {
                bot.broadcastPoll(p);
            }
        };

// קישור תוצאות
        polls.onPollResults = new Consumer<PollManager.PollResults>() {
            @Override
            public void accept(PollManager.PollResults r) {
                bot.sendResultsToCreator(r);
            }
        };

// פתיחת ה־Swing
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                PollBuilderFrame f = new PollBuilderFrame(polls);
                f.setVisible(true);
            }
        });

    }
}
