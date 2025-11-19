package org.example.ui;

import org.example.core.model.Question;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ManualPollForm extends JPanel {
    private final JTextField delayField;

    private final JTextField q1Field;
    private final JTextField q1Opts;

    private final JTextField q2Field;
    private final JTextField q2Opts;

    private final JTextField q3Field;
    private final JTextField q3Opts;

    public interface CreateListener {
        void onCreate(List<Question> questions, int delayMinutes);
    }

    private CreateListener createListener;

    public ManualPollForm() {
        setLayout(null);

        JLabel title = new JLabel("Manual Survey Builder");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        title.setBounds(20, 10, 300, 28);
        add(title);

        JLabel lDelay = new JLabel("Delay (min):");
        lDelay.setBounds(20, 50, 100, 24);
        add(lDelay);

        delayField = new JTextField("0");
        delayField.setBounds(120, 50, 80, 24);
        add(delayField);

        int y = 90;

        // Q1
        JLabel lq1 = new JLabel("Q1 text:");
        lq1.setBounds(20, y, 100, 24);
        add(lq1);
        q1Field = new JTextField();
        q1Field.setBounds(120, y, 700, 24);
        add(q1Field);

        JLabel lq1o = new JLabel("Q1 options (comma separated, 2-4):");
        lq1o.setBounds(20, y+34, 260, 24);
        add(lq1o);
        q1Opts = new JTextField();
        q1Opts.setBounds(280, y+34, 540, 24);
        add(q1Opts);

        // Q2
        y += 80;
        JLabel lq2 = new JLabel("Q2 text (optional):");
        lq2.setBounds(20, y, 160, 24);
        add(lq2);
        q2Field = new JTextField();
        q2Field.setBounds(180, y, 640, 24);
        add(q2Field);

        JLabel lq2o = new JLabel("Q2 options (2-4):");
        lq2o.setBounds(20, y+34, 160, 24);
        add(lq2o);
        q2Opts = new JTextField();
        q2Opts.setBounds(180, y+34, 640, 24);
        add(q2Opts);

        // Q3
        y += 80;
        JLabel lq3 = new JLabel("Q3 text (optional):");
        lq3.setBounds(20, y, 160, 24);
        add(lq3);
        q3Field = new JTextField();
        q3Field.setBounds(180, y, 640, 24);
        add(q3Field);

        JLabel lq3o = new JLabel("Q3 options (2-4):");
        lq3o.setBounds(20, y+34, 160, 24);
        add(lq3o);
        q3Opts = new JTextField();
        q3Opts.setBounds(180, y+34, 640, 24);
        add(q3Opts);

        JButton btnCreate = new JButton("Create & Send");
        btnCreate.setBounds(20, y+80, 150, 30);
        add(btnCreate);

        btnCreate.addActionListener(e -> onCreateClicked());
        setPreferredSize(new Dimension(860, y+130));
    }

    public void setCreateListener(CreateListener l) {
        this.createListener = l;
    }

    private void onCreateClicked() {
        try {
            int delay = Integer.parseInt(delayField.getText().trim());

            List<Question> qs = new ArrayList<>();

            // Q1 – חובה
            String t1 = q1Field.getText().trim();
            List<String> o1 = parseOptions(q1Opts.getText());
            if (t1.isBlank() || o1.size() < 2 || o1.size() > 4) {
                throw new IllegalArgumentException("Q1 חייבת טקסט ואופציות (2-4).");
            }
            qs.add(new Question(t1, o1));

            // Q2 – אופציונלי
            String t2 = q2Field.getText().trim();
            String o2s = q2Opts.getText().trim();
            if (!t2.isBlank() || !o2s.isBlank()) {
                List<String> o2 = parseOptions(o2s);
                if (t2.isBlank() || o2.size() < 2 || o2.size() > 4) {
                    throw new IllegalArgumentException("Q2: אם ממלאים, חובה 2-4 אופציות.");
                }
                qs.add(new Question(t2, o2));
            }

            // Q3 – אופציונלי
            String t3 = q3Field.getText().trim();
            String o3s = q3Opts.getText().trim();
            if (!t3.isBlank() || !o3s.isBlank()) {
                List<String> o3 = parseOptions(o3s);
                if (t3.isBlank() || o3.size() < 2 || o3.size() > 4) {
                    throw new IllegalArgumentException("Q3: אם ממלאים, חובה 2-4 אופציות.");
                }
                qs.add(new Question(t3, o3));
            }

            if (qs.isEmpty()) {
                throw new IllegalArgumentException("חייבת להיות לפחות שאלה אחת.");
            }
            if (qs.size() > 3) {
                throw new IllegalArgumentException("מותר עד 3 שאלות.");
            }

            if (createListener != null) {
                createListener.onCreate(qs, delay);
            }

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Delay חייב להיות מספר שלם.", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Unexpected error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private List<String> parseOptions(String text) {
        List<String> out = new ArrayList<>();
        for (String raw : text.split(",")) {
            String s = raw.trim();
            if (!s.isBlank()) out.add(s);
        }
        return out;
    }
}
