package org.example.ui;

import org.example.api.SekerClient;
import org.example.chatgpt.ChatGptPollGenerator;
import org.example.chatgpt.ChatGptPollGenerator.GenResult;
import org.example.core.PollManager;
import org.example.core.model.Question;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class PollBuilderFrame extends JFrame {

    private SekerClient seker;
    private ChatGptPollGenerator generator;
    private final PollManager polls;

    // ChatGPT tab
    private JTextField idField;
    private JTextField topicField;
    private JTextArea jsonArea;

    // Manual tab
    private JTextField manualQField;
    private JTextField manualOpt1;
    private JTextField manualOpt2;
    private JTextField manualOpt3;
    private JTextField manualOpt4;
    private JTextArea manualSummary;

    public PollBuilderFrame(PollManager polls) {
        seker = new SekerClient();
        generator = new ChatGptPollGenerator(seker);
        this.polls = polls;
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

        JLabel lblId = new JLabel("ID (תעודת זהות / מזהה ל-API):");
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

        topicField = new JTextField("coffee preferences among students");
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
        btnGenerate.addActionListener(e -> onGenerate());

        return p;
    }

    private JPanel createManualPanel() {
        JPanel p = new JPanel(null);

        JLabel lblQ = new JLabel("שאלה:");
        lblQ.setBounds(20, 20, 300, 24);
        p.add(lblQ);

        manualQField = new JTextField();
        manualQField.setBounds(20, 45, 840, 24);
        p.add(manualQField);

        JLabel lblOpt = new JLabel("אפשרויות (2-4):");
        lblOpt.setBounds(20, 80, 300, 24);
        p.add(lblOpt);

        manualOpt1 = new JTextField();
        manualOpt1.setBounds(20, 105, 400, 24);
        p.add(manualOpt1);

        manualOpt2 = new JTextField();
        manualOpt2.setBounds(20, 135, 400, 24);
        p.add(manualOpt2);

        manualOpt3 = new JTextField();
        manualOpt3.setBounds(20, 165, 400, 24);
        p.add(manualOpt3);

        manualOpt4 = new JTextField();
        manualOpt4.setBounds(20, 195, 400, 24);
        p.add(manualOpt4);

        JButton btnBuild = new JButton("Build Manual Survey");
        btnBuild.setBounds(20, 230, 220, 30);
        p.add(btnBuild);

        manualSummary = new JTextArea();
        manualSummary.setEditable(false);
        JScrollPane scroll = new JScrollPane(manualSummary);
        scroll.setBounds(20, 270, 840, 265);
        p.add(scroll);

        btnBuild.addActionListener(e -> onBuildManual());

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
            JOptionPane.showMessageDialog(this, "Response:\n" + resp, "Balance", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "שגיאה בבקשה:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onGenerate() {
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
                sb.append((i + 1));
                sb.append(". ");
                sb.append(q.getText());
                sb.append("\n");
                List<String> opts = q.getOptions();
                for (int k = 0; k < opts.size(); k++) {
                    sb.append("   ");
                    sb.append(k + 1);
                    sb.append(") ");
                    sb.append(opts.get(k));
                    sb.append("\n");
                }
                sb.append("\n");
            }
            System.out.println(sb.toString());

            jsonArea.setText(sb.toString());

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "שגיאה ב-ChatGPT / API:\n" + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onBuildManual() {
        String qText = manualQField.getText().trim();
        if (qText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "כתבי טקסט לשאלה.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<String> opts = new ArrayList<String>();
        if (!manualOpt1.getText().trim().isEmpty()) opts.add(manualOpt1.getText().trim());
        if (!manualOpt2.getText().trim().isEmpty()) opts.add(manualOpt2.getText().trim());
        if (!manualOpt3.getText().trim().isEmpty()) opts.add(manualOpt3.getText().trim());
        if (!manualOpt4.getText().trim().isEmpty()) opts.add(manualOpt4.getText().trim());

        if (opts.size() < 2) {
            JOptionPane.showMessageDialog(this, "צריך לפחות 2 אפשרויות.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Question q = new Question(qText, opts);

        StringBuilder sb = new StringBuilder();
        sb.append("Manual question built:\n");
        sb.append(q.getText());
        sb.append("\n");
        for (int i = 0; i < q.getOptions().size(); i++) {
            sb.append("   ");
            sb.append(i + 1);
            sb.append(") ");
            sb.append(q.getOptions().get(i));
            sb.append("\n");
        }

        manualSummary.setText(sb.toString());
    }
}
