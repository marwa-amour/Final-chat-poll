package org.example.ui;

import org.example.api.SekerClient;
import org.example.chatgpt.ChatGptPollGenerator;
import org.example.chatgpt.ChatGptPollGenerator.GenResult;
import org.example.core.PollManager;
import org.example.core.model.Question;
import org.example.ui.ManualPollForm;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class PollBuilderFrame extends JFrame {

    private final PollManager polls;

    private SekerClient seker;
    private ChatGptPollGenerator generator;

    private JTextField idField;
    private JTextField topicField;
    private JTextArea jsonArea;

    public PollBuilderFrame(PollManager polls) {
        this.polls = polls;
        this.seker = new SekerClient();
        this.generator = new ChatGptPollGenerator(seker);
        initUi();
    }

    private void initUi() {
        setTitle("Survey Builder (Manual + ChatGPT)");
        setSize(900, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("ChatGPT Survey", createChatGptPanel());
        tabs.addTab("Manual Survey", createManualPanel());

        setContentPane(tabs);
    }

    private JPanel createChatGptPanel() {
        JPanel p = new JPanel(null);

        JLabel lblId = new JLabel("ID (תעודת זהות ל-API):");
        lblId.setBounds(20, 20, 300, 24);
        p.add(lblId);

        idField = new JTextField();
        idField.setBounds(20, 45, 200, 24);
        p.add(idField);

        JButton btnCheck = new JButton("Check Balance");
        btnCheck.setBounds(230, 45, 140, 24);
        p.add(btnCheck);

        JLabel lblTopic = new JLabel("Topic (נושא הסקר באנגלית):");
        lblTopic.setBounds(20, 80, 350, 24);
        p.add(lblTopic);

        topicField = new JTextField("");
        topicField.setBounds(20, 105, 840, 24);
        p.add(topicField);

        JButton btnGenerate = new JButton("Generate Survey via ChatGPT");
        btnGenerate.setBounds(20, 140, 260, 30);
        p.add(btnGenerate);

        JLabel lblJson = new JLabel("Raw JSON / פירוק השאלות:");
        lblJson.setBounds(20, 180, 300, 24);
        p.add(lblJson);

        jsonArea = new JTextArea();
        jsonArea.setEditable(false);
        JScrollPane scroll = new JScrollPane(jsonArea);
        scroll.setBounds(20, 205, 840, 330);
        p.add(scroll);

        btnCheck.addActionListener(e -> onCheckBalance());
        btnGenerate.addActionListener(e -> onGenerateChatGpt());

        return p;
    }


    private void onCheckBalance() {
        String id = idField.getText().trim();
        if (id.isEmpty()) {
            JOptionPane.showMessageDialog(this, "מלאי ID קודם.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            String resp = seker.checkBalance(id);
            JOptionPane.showMessageDialog(this, resp, "Balance", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "שגיאה בבקשה:\n" + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onGenerateChatGpt() {
        String id = idField.getText().trim();
        String topic = topicField.getText().trim();

        if (id.isEmpty()) {
            JOptionPane.showMessageDialog(this, "מלאי ID קודם.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (topic.isEmpty()) {
            JOptionPane.showMessageDialog(this, "מלאי Topic קודם.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            GenResult res = generator.generate(id, topic);
            List<Question> qs = res.getQuestions();

            StringBuilder sb = new StringBuilder();
            sb.append("JSON from API:\n");
            sb.append(res.getRawJson());
            sb.append("\n\nParsed Questions:\n\n");

            for (int i = 0; i < qs.size(); i++) {
                Question q = qs.get(i);
                sb.append((i + 1)).append(". ").append(q.getText()).append("\n");

                List<String> opts = q.getOptions();
                for (int k = 0; k < opts.size(); k++) {
                    sb.append("   ").append(k + 1).append(") ").append(opts.get(k)).append("\n");
                }
                sb.append("\n");
            }

            jsonArea.setText(sb.toString());
            System.out.println(sb.toString());

            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "נוצרו " + qs.size() + " שאלות.\nלשלוח את הסקר לקהילה?",
                    "Send Survey",
                    JOptionPane.YES_NO_OPTION);

            if (choice == JOptionPane.YES_OPTION) {
                long creatorId = 0L;
                polls.startPoll(creatorId, qs, 0);
                JOptionPane.showMessageDialog(this, "הסקר נשלח לבוט!");
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "שגיאה ב-ChatGPT / API:\n" + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }


    private JPanel createManualPanel() {
        ManualPollForm form = new ManualPollForm();

        form.setCreateListener((questions, delayMinutes) -> {
            try {
                long creatorId = 0L;
                polls.startPoll(creatorId, questions, delayMinutes);

                JOptionPane.showMessageDialog(
                        this,
                        "הסקר הידני נוצר ונשלח לקהילה.",
                        "Manual Survey",
                        JOptionPane.INFORMATION_MESSAGE
                );
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(
                        this,
                        "שגיאה ביצירת סקר:\n" + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(form, BorderLayout.CENTER);
        return wrapper;
    }

}
