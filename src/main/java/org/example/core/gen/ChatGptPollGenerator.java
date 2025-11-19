package org.example.chatgpt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.api.SekerClient;
import org.example.core.model.Question;

import java.util.ArrayList;
import java.util.List;

public class ChatGptPollGenerator {

    private SekerClient client;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public ChatGptPollGenerator(SekerClient client) {
        this.client = client;
    }

    public GenResult generate(String id, String topic) throws Exception {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("ID ריק – הזיני תעודת זהות / מזהה.");
        }
        if (topic == null || topic.trim().isEmpty()) {
            throw new IllegalArgumentException("Topic ריק – הזיני נושא לסקר.");
        }

        String balance = client.checkBalance(id);

        if (balance.contains("3001")) {
            throw new IllegalStateException("ID לא מוכר (3001) – בדקי שהכנסת את המספר שהמרצה נתן.");
        }
        if (balance.contains("3002")) {
            throw new IllegalStateException("נגמרה המכסה ל-ID הזה (3002).");
        }
        if (balance.contains("3000")) {
            throw new IllegalStateException("לא נשלח ID (3000) – בדקי שהשדה לא ריק.");
        }

        client.clearHistory(id);

        String prompt =
                "Return ONLY JSON (no markdown, no explanations).\n" +
                        "Output format:\n" +
                        "{\n" +
                        "  \"questions\": [\n" +
                        "    { \"text\": \"QUESTION_TEXT\", \"options\": [\"OPT1\",\"OPT2\"] }\n" +
                        "  ]\n" +
                        "}\n\n" +
                        "Topic: " + topic + "\n" +
                        "Create 1-3 multiple-choice survey questions.\n" +
                        "Each question must have 2-4 short options.\n";

        String response = client.sendMessage(id, prompt);
        if (response == null || response.trim().isEmpty()) {
            throw new RuntimeException("קיבלתי תשובה ריקה מה-API.");
        }

        JsonNode root;
        try {
            root = MAPPER.readTree(response);
        } catch (Exception e) {
            int i = response.indexOf('{');
            int j = response.lastIndexOf('}');
            if (i >= 0 && j > i) {
                String jsonOnly = response.substring(i, j + 1);
                root = MAPPER.readTree(jsonOnly);
            } else {
                throw new RuntimeException("התשובה מהשרת אינה JSON ואי אפשר לחלץ ממנה JSON.\nResponse:\n" + response);
            }
        }

        if (root.has("extra") && root.get("extra").isTextual()) {
            String innerJson = root.get("extra").asText();
            try {
                root = MAPPER.readTree(innerJson);
            } catch (Exception e) {
                throw new RuntimeException("לא הצלחתי לפרש את extra כ-JSON:\n" + innerJson, e);
            }
        }

        JsonNode arr = root.path("questions");
        if (!arr.isArray() || arr.size() == 0) {
            arr = root.path("items");
        }

        List<Question> questions = new ArrayList<Question>();

        if (arr.isArray()) {
            for (int i = 0; i < arr.size(); i++) {
                JsonNode qNode = arr.get(i);

                String text = null;
                if (qNode.hasNonNull("text")) {
                    text = qNode.get("text").asText();
                } else if (qNode.hasNonNull("question")) {
                    text = qNode.get("question").asText();
                }

                JsonNode optsNode = null;
                if (qNode.has("options")) {
                    optsNode = qNode.get("options");
                } else if (qNode.has("answers")) {
                    optsNode = qNode.get("answers");
                } else if (qNode.has("choices")) {
                    optsNode = qNode.get("choices");
                }

                List<String> opts = new ArrayList<String>();
                if (optsNode != null && optsNode.isArray()) {
                    for (int k = 0; k < optsNode.size(); k++) {
                        String s = optsNode.get(k).asText();
                        if (s != null && !s.trim().isEmpty()) {
                            opts.add(s.trim());
                        }
                    }
                }

                if (text != null && !text.trim().isEmpty()
                        && opts.size() >= 2 && opts.size() <= 4) {
                    questions.add(new Question(text, opts));
                }
            }
        }

        if (questions.isEmpty()) {
            throw new RuntimeException(
                    "No valid questions generated.\n" +
                            "בדקי: (1) שה-topic הגיוני, (2) שיש מכסה, (3) שה-API באמת מחזיר JSON.\n" +
                            "Raw response:\n" + root.toPrettyString()
            );
        }

        String prettyJson = root.toPrettyString();
        return new GenResult(questions, prettyJson);

    }

    public static class GenResult {
        private List<Question> questions;
        private String rawJson;

        public GenResult(List<Question> questions, String rawJson) {
            this.questions = questions;
            this.rawJson = rawJson;
        }

        public List<Question> getQuestions() {
            return questions;
        }

        public String getRawJson() {
            return rawJson;
        }
    }
}
