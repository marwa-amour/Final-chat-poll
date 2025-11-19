package org.example.api;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class SekerClient {

    private static final String BASE = "https://app.seker.live/fm1";
    private HttpClient http;

    public SekerClient() {
        http = HttpClient.newHttpClient();
    }

    private String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private String get(String url) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> resp =
                    http.send(req, HttpResponse.BodyHandlers.ofString());

            return resp.body();
        } catch (Exception e) {
            throw new RuntimeException("HTTP error: " + e.getMessage(), e);
        }
    }

    public String checkBalance(String id) {
        String url = BASE + "/check-balance?id=" + encode(id);
        return get(url);
    }

    public String clearHistory(String id) {
        String url = BASE + "/clear-history?id=" + encode(id);
        return get(url);
    }

    public String sendMessage(String id, String text) {
        String url = BASE
                + "/send-message?id=" + encode(id)
                + "&text=" + encode(text);
        String body = get(url);
        return unwrapExtra(body);
    }

    private String unwrapExtra(String body) {
        if (body == null) {
            return null;
        }
        body = body.trim();
        if (body.startsWith("<TestResponseModel")) {
            int i = body.indexOf("<extra>");
            int j = body.indexOf("</extra>");
            if (i >= 0 && j > i) {
                String inner = body.substring(i + "<extra>".length(), j);
                return inner.trim();
            }
        }
        return body;
    }
}
