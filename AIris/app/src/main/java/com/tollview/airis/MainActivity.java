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
            String payload = """
            {
                "method": "getEvent",
                "params": [false],
                "id": 1,
                "version": "1.0"
            }
            """;
            appendToTextView("Ready for photography...");

            while (true) {
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

                    // Parse JSON and extract takePictureUrl
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    JSONArray resultArray = jsonResponse.optJSONArray("result");
                    if (resultArray != null && resultArray.length() > 5) {
                        JSONArray takePictureArray = resultArray.optJSONArray(5); // Access index [5]
                        if (takePictureArray != null && takePictureArray.length() > 0) {
                            JSONObject takePictureObject = takePictureArray.optJSONObject(0); // Access index [0]
                            if (takePictureObject != null) {
                                JSONArray takePictureUrls = takePictureObject.optJSONArray("takePictureUrl");
                                if (takePictureUrls != null) {
                                    for (int i = 0; i < takePictureUrls.length(); i++) {
                                        String url = takePictureUrls.optString(i);
                                        if (url != null && !url.isEmpty()) {
                                            appendToTextView("Photo URL: " + url);
                                        }
                                    }
                                } else {
                                    appendToTextView("nothing"); // takePictureUrl key missing
                                }
                            } else {
                                appendToTextView("nothing"); // Object at [5][0] missing
                            }
                        } else {
                            appendToTextView("nothing"); // Array at [5] missing or empty
                        }
                    } else {
                        appendToTextView("nothing"); // Index [5] not in result
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
}
