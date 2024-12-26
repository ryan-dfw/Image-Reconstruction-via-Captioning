package com.tollview.airis;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private static final String CAMERA_ENDPOINT = "http://10.0.0.1:10000/sony/camera";
    private TextView logTextView;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private String url = null;
    private static String destinationURL = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        logTextView = findViewById(R.id.logTextView);

        new Thread(() -> {
            String photoUrl = pollForPhotoUrls();
            if (photoUrl != null) {
                String urlname = extractFilenameFromUrl(photoUrl);
                if (urlname != null) {
                    uiHandler.post(() -> appendToTextView("Extracted Filename: " + urlname));
                } else {
                    uiHandler.post(() -> appendToTextView("Failed to extract filename."));
                }
                appendToTextView("About to try");
                System.out.println("About to try");
                try {
                    InputStream inputStream = null;
                    ByteArrayOutputStream outputStream = null;
                    HttpURLConnection connection = null;
                    String base64Image = null;

                    URL url = new URL(photoUrl);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.connect();

                    if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        inputStream = connection.getInputStream();
                        outputStream = new ByteArrayOutputStream();
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }

                        byte[] imageBytes = outputStream.toByteArray();
                        base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP);
                        appendToTextView("ALLEGED B64 IMAGE: " + base64Image);
                        System.out.println("ALLEGED B64 IMAGE: " + base64Image);
                    } else {
                        appendToTextView("THERE'S A PROBLEM");
                        System.out.println("THERE'S A PROBLEM");
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    private String extractFilenameFromUrl(String url) {
        try {
            if (url != null && !url.isEmpty()) {
                String baseUrl = url.split("\\?")[0];
                String[] parts = baseUrl.split("/");
                return parts[parts.length - 1];
            }
        } catch (Exception e) {
            appendToTextView("Error extracting filename: " + e.getMessage());
        }
        return null;
    }


    private String pollForPhotoUrls() {
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
                HttpURLConnection connection = (HttpURLConnection) new URL(CAMERA_ENDPOINT).openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                try (OutputStream os = connection.getOutputStream()) {
                    os.write(payload.getBytes("utf-8"));
                }

                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    StringBuilder response = new StringBuilder();
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            response.append(line.trim());
                        }
                    }

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    JSONArray resultArray = jsonResponse.optJSONArray("result");
                    if (resultArray != null && resultArray.length() > 5) {
                        JSONArray takePictureArray = resultArray.optJSONArray(5);
                        if (takePictureArray != null && takePictureArray.length() > 0) {
                            JSONObject takePictureObject = takePictureArray.optJSONObject(0);
                            if (takePictureObject != null) {
                                JSONArray takePictureUrls = takePictureObject.optJSONArray("takePictureUrl");
                                if (takePictureUrls != null) {
                                    for (int i = 0; i < takePictureUrls.length(); i++) {
                                        String foundUrl = takePictureUrls.optString(i);
                                        if (foundUrl != null && !foundUrl.isEmpty()) {
                                            url = foundUrl;
                                            appendToTextView("Photo URL: " + url);
                                            return url;
                                        }
                                    }
                                } else {
                                    appendToTextView("nothing");
                                }
                            } else {
                                appendToTextView("nothing");
                            }
                        } else {
                            appendToTextView("nothing");
                        }
                    } else {
                        appendToTextView("nothing");
                    }
                }

                Thread.sleep(500);
            }
        } catch (Exception e) {
            appendToTextView("Error polling for photo: " + e.getMessage());
        }
        return null;
    }

    private void appendToTextView(String message) {
        uiHandler.post(() -> {
            String currentText = logTextView.getText().toString();
            logTextView.setText(currentText + "\n" + message);
        });
    }
}
