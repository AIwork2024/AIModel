import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class GeminiApiKeyValidator {
    public static void main(String[] args) {
        String apiKey = "AIzaSyCv6Ap6Jfg55ucjtZ2TATmVFFA7VL8LOjo"; // Replace with your actual API key
        boolean isValid = validateApiKey(apiKey);
        System.out.println("API Key Valid: " + isValid);
    }

    public static boolean validateApiKey(String apiKey) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS ) // ⏳ Increase connection timeout
                .readTimeout(30, TimeUnit.SECONDS)    // ⏳ Increase read timeout
                .writeTimeout(30, TimeUnit.SECONDS)   // ⏳ Increase write timeout
                .build();

        // ✅ Use an available model
        String url = "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-pro-002:generateContent?key=" + apiKey;

        //String jsonBody = "{ \"contents\": [{ \"parts\": [{ \"text\": \"Hello, Gemini!\" }] }] }";
        String elementDescription = "Trying to locate the 'login' button on the homepage.";
        String jsonBody = "{"
                + "\"contents\": [{"
                + "  \"parts\": [{"
                + "    \"text\": \"I'm trying to find an element on a webpage but it's missing. Here are the details: "
                + elementDescription
                + ". Suggest alternative XPath, CSS selectors, or IDs.\""
                + "  }]"
                + "}]"
                + "}";

        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json"));

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            System.out.println("Response Code: " + response.code());
            System.out.println("Response Body: " + response.body().string());
            return response.code() == 200;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}