package sweetie.evaware.api.auth;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class HttpUtils {
    private static final String DEFAULT_AGENT = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.99 Safari/537.36";
    private static final Gson GSON = new GsonBuilder().create();

    public record Pair<T, E>(T first, E second) { }

    public record HttpResponse(int code, String text) {}

    private static HttpURLConnection makeConnection(String url, String method, String data, Map<String, String> headers, String agent) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();

        connection.setRequestMethod(method);
        connection.setConnectTimeout(2000);
        connection.setReadTimeout(10000);

        connection.setRequestProperty("User-Agent", agent);
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }

        connection.setInstanceFollowRedirects(true);
        connection.setDoOutput(true);

        if (data != null && !data.isEmpty()) {
            try (DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())) {
                outputStream.writeBytes(data);
            }
        }

        connection.connect();
        return connection;
    }

    public static HttpResponse request(String url, String method, String data, Map<String, String> headers, String agent) throws IOException {
        HttpURLConnection connection = makeConnection(url, method, data, headers, agent);

        int responseCode = connection.getResponseCode();
        InputStream stream = responseCode >= 200 && responseCode < 300 ?
                connection.getInputStream() : connection.getErrorStream();

        String responseText;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            responseText = response.toString();
        }

        return new HttpResponse(responseCode, responseText);
    }

    public static HttpResponse get(String url, Map<String, String> headers) throws IOException {
        return request(url, "GET", "", headers, DEFAULT_AGENT);
    }

    public static HttpResponse post(String url, String data, Map<String, String> headers) throws IOException {
        return request(url, "POST", data, headers, DEFAULT_AGENT);
    }

    public static <T> T post(String url, Object data, Class<T> responseType) throws IOException {
        HttpResponse response = post(url, GSON.toJson(data), Map.of("Content-Type", "application/json"));
        return GSON.fromJson(response.text, responseType);
    }

    public static <T, E> Pair<T, E> postWithFallback(String url, Object data, Class<T> successType, Class<E> errorType) throws IOException {
        HttpResponse response = post(url, GSON.toJson(data), Map.of("Content-Type", "application/json"));

        if (response.code == 200) {
            return new Pair<>(GSON.fromJson(response.text, successType), null);
        } else {
            return new Pair<>(null, GSON.fromJson(response.text, errorType));
        }
    }
}
