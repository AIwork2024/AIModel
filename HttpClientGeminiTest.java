import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class HttpClientGeminiTest {
    private static final String API_KEY = "AIzaSyBMRsS1waZAPtdTxjtfO_T9xn0bpXAirmA";
    // Make sure this endpoint is correct as per the latest documentation.
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-pro-002:generateContent?key=" + API_KEY;

    public static void main(String[] args) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(API_URL);
            // Set required headers
            post.addHeader("Content-Type", "application/json");
            post.addHeader("Accept", "application/json");

            // Updated payload with a top-level "text" field
            String jsonPayload = "{\"text\": \"Hello, Gemini!\"}";
            StringEntity entity = new StringEntity(jsonPayload, ContentType.APPLICATION_JSON);
            post.setEntity(entity);

            try (CloseableHttpResponse response = httpClient.execute(post)) {
                int statusCode = response.getCode();
                if (statusCode == 200) {
                    System.out.println("API Key is valid. Status Code: " + statusCode);
                    BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8));
                    String inputLine;
                    StringBuilder responseContent = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        responseContent.append(inputLine);
                    }
                    in.close();
                    System.out.println("Response: " + responseContent.toString());
                } else {
                    System.out.println("Invalid API Key or endpoint issue. Response Code: " + statusCode);
                    BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8));
                    String inputLine;
                    StringBuilder errorContent = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        errorContent.append(inputLine);
                    }
                    in.close();
                    System.out.println("Error Response: " + errorContent.toString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}