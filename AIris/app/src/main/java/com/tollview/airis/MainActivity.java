package com.tollview.airis;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity {

    private static final String CAMERA_ENDPOINT = "http://10.0.0.1:10000/sony/camera";
    private TextView logTextView;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        logTextView = findViewById(R.id.logTextView);

        // Start polling for photo URLs
        new Thread(this::pollForPhotoUrls).start();
    }

    private void pollForPhotoUrls() {
        try {
            // Payload for getEvent API
            String payload = """
            {
                "method": "getEvent",
                "params": [false],
                "id": 1,
                "version": "1.0"
            }
            """;

            // Log that the app is ready for photography
            appendToTextView("Ready for photography...");

            while (true) {
                // Log polling attempt
                appendToTextView("...");

                // Connect to the camera API
                HttpURLConnection connection = (HttpURLConnection) new URL(CAMERA_ENDPOINT).openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                // Send request payload
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(payload.getBytes("utf-8"));
                }

                // Handle response
                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    StringBuilder response = new StringBuilder();
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            response.append(line.trim());
                        }
                    }

                    // Log the raw response
                    appendToTextView("Raw response: " + response.toString());

                    // Parse response to find photo URLs
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    JSONArray resultArray = jsonResponse.optJSONArray("result");
                    if (resultArray != null) {
                        for (int i = 0; i < resultArray.length(); i++) {
                            JSONObject event = resultArray.optJSONObject(i);
                            if (event != null) {
                                String url = extractPhotoUrl(event);
                                if (url != null) {
                                    // Log shutter pressed
                                    appendToTextView("Shutter pressed!");

                                    // Log photo URL
                                    appendToTextView("Photo URL: " + url);

                                    // Log capture finished
                                    appendToTextView("Capture finished.");

                                    return; // Exit after finding the first URL
                                }
                            }
                        }
                    }
                }

                // Wait before polling again
                Thread.sleep(500);
            }
        } catch (Exception e) {
            appendToTextView("Error polling for photo: " + e.getMessage());
        }
    }

    private void appendToTextView(String message) {
        uiHandler.post(() -> {
            String currentText = logTextView.getText().toString();
            logTextView.setText(currentText + "\n" + message);
        });
    }

    private String extractPhotoUrl(JSONObject jsonObject) {
        try {
            Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object value = jsonObject.get(key);

                if (value instanceof String) {
                    // Check if the value contains an HTTP URL
                    String stringValue = (String) value;
                    if (stringValue.contains("http")) {
                        return stringValue;
                    }
                } else if (value instanceof JSONObject) {
                    // Recursively search in nested JSONObject
                    String url = extractPhotoUrl((JSONObject) value);
                    if (url != null) {
                        return url;
                    }
                } else if (value instanceof JSONArray) {
                    // Recursively search in JSONArray
                    JSONArray jsonArray = (JSONArray) value;
                    for (int i = 0; i < jsonArray.length(); i++) {
                        Object arrayValue = jsonArray.get(i);
                        if (arrayValue instanceof JSONObject) {
                            String url = extractPhotoUrl((JSONObject) arrayValue);
                            if (url != null) {
                                return url;
                            }
                        } else if (arrayValue instanceof String) {
                            String stringValue = (String) arrayValue;
                            if (stringValue.contains("http")) {
                                return stringValue;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            appendToTextView("Error parsing JSON: " + e.getMessage());
        }
        return null;
    }
}
