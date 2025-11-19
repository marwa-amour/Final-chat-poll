package org.example;

import org.example.bot.MyBot;
import org.example.core.PollManager;
import org.example.core.PollManager.PollResults;
import org.example.core.model.Poll;
import org.example.ui.PollBuilderFrame;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class Main {
    public static void main(String[] args) {
        try {
            PollManager polls = new PollManager();

            MyBot bot = new MyBot(polls);

            TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
            api.registerBot(bot);

            polls.onPollReadyToSend = new Consumer<Poll>() {
                @Override
                public void accept(Poll p) {
                    bot.broadcastPoll(p);
                }
            };

            polls.onPollResults = new Consumer<PollResults>() {
                @Override
                public void accept(PollResults r) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            String text = r.formatAsText();

                            JTextArea area = new JTextArea(text);
                            area.setEditable(false);
                            area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

                            JScrollPane scroll = new JScrollPane(area);
                            scroll.setPreferredSize(new Dimension(600, 400));

                            JOptionPane.showMessageDialog(
                                    null,
                                    scroll,
                                    "Survey Results",
                                    JOptionPane.INFORMATION_MESSAGE
                            );
                        }
                    });

                }
            };

            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    PollBuilderFrame f = new PollBuilderFrame(polls);
                    f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    f.setSize(900, 600);
                    f.setLocationRelativeTo(null);
                    f.setVisible(true);
                }
            });

        } catch (TelegramApiException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    null,
                    "Error initializing Telegram bot:\n" + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
