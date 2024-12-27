package com.tollview.airis;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
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
                        String caption = sendToOpenAI(base64Image);
                        String outputURL = sendToDalle(caption);
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

    private String sendToOpenAI(String base64Image) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        Network cellularNetwork = null;

        try {
            String OPENAI_API_KEY = "KEYGOESHERE";

            // Identify the cellular network
            for (Network network : connectivityManager.getAllNetworks()) {
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
                if (capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    cellularNetwork = network;
                    break;
                }
            }

            if (cellularNetwork == null) {
                appendToTextView("No cellular network available.");
                return null;
            }

            // Construct the JSON payload
            JSONObject data = new JSONObject();
            data.put("model", "gpt-4o-mini");

            JSONArray messagesArray = new JSONArray();
            JSONObject textMessage = new JSONObject()
                    .put("type", "text")
                    .put("text", "Describe the image in detail. If there are people in the image, describe their appearance, facial expression, and pose (if visible). Make note of the image's composition. If the facial expression or pose is extreme, feel free to describe it as such.");

            JSONObject imageMessage = new JSONObject()
                    .put("type", "image_url")
                    .put("image_url", new JSONObject()
                            .put("url", "data:image/jpeg;base64," + base64Image));

            messagesArray.put(textMessage);
            messagesArray.put(imageMessage);

            data.put("messages", new JSONArray().put(new JSONObject()
                    .put("role", "user")
                    .put("content", messagesArray)));

            // Log the payload length (not the content)
            appendToTextView("Request payload length: " + data.toString().length());
            Log.d("OpenAI", "Request payload length: " + data.toString().length());

            // Force API call to use the cellular network
            URL openAIUrl = new URL("https://api.openai.com/v1/chat/completions");
            HttpURLConnection connection = (HttpURLConnection) cellularNetwork.openConnection(openAIUrl);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + OPENAI_API_KEY);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            // Send the payload
            try (OutputStream os = connection.getOutputStream()) {
                os.write(data.toString().getBytes("utf-8"));
            }

            // Read the response
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                StringBuilder response = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line.trim());
                    }
                }

                JSONObject responseJson = new JSONObject(response.toString());
                JSONArray choices = responseJson.optJSONArray("choices");
                if (choices != null && choices.length() > 0) {
                    JSONObject firstChoice = choices.optJSONObject(0);
                    if (firstChoice != null) {
                        JSONObject message = firstChoice.optJSONObject("message");
                        if (message != null) {
                            String caption = message.optString("content", "No caption provided.");
                            // Save to variable and append to TextView
                            appendToTextView("Caption: " + caption);
                            Log.d("OpenAI", "Caption: " + caption);
                            return caption;
                        }
                    }
                } else {
                    appendToTextView("API response missing valid choices or text content.");
                }
            } else {
                // Log error response details
                StringBuilder errorResponse = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getErrorStream(), "utf-8"))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        errorResponse.append(line.trim());
                    }
                }
                appendToTextView("Error: HTTP " + responseCode + " - " + errorResponse.toString());
                Log.e("OpenAI", "Error: HTTP " + responseCode + " - " + errorResponse.toString());
            }
        } catch (Exception e) {
            appendToTextView("Error sending to OpenAI: " + e.getMessage());
            Log.e("OpenAI", "Error sending to OpenAI: ", e);
        }
        return null;
    }

    private String sendToDalle(String caption) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        Network cellularNetwork = null;

        try {
            String OPENAI_API_KEY = "KEYGOESHERE";

            // Identify the cellular network
            for (Network network : connectivityManager.getAllNetworks()) {
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
                if (capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    cellularNetwork = network;
                    break;
                }
            }

            if (cellularNetwork == null) {
                appendToTextView("No cellular network available.");
                return null;
            }

            // Construct the JSON payload
            JSONObject data = new JSONObject();
            data.put("model", "dall-e-3");
            data.put("prompt", "In a photorealistic style: " + caption);
            data.put("n", 1);
            data.put("size", "1024x1024");

            // Log the payload length
            Log.d("DALL-E", "Request payload: " + data.toString());

            // Force API call to use the cellular network
            URL dalleUrl = new URL("https://api.openai.com/v1/images/generations");
            HttpURLConnection connection = (HttpURLConnection) cellularNetwork.openConnection(dalleUrl);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + OPENAI_API_KEY);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            // Send the payload
            try (OutputStream os = connection.getOutputStream()) {
                os.write(data.toString().getBytes("utf-8"));
            }

            // Read the response
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                StringBuilder response = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line.trim());
                    }
                }

                // Parse the response and extract the URL
                JSONObject responseJson = new JSONObject(response.toString());
                JSONArray dataArray = responseJson.optJSONArray("data");
                if (dataArray != null && dataArray.length() > 0) {
                    JSONObject firstImage = dataArray.optJSONObject(0);
                    if (firstImage != null) {
                        String imageUrl = firstImage.optString("url");
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            appendToTextView("Generated Image URL: " + imageUrl);
                            Log.d("DALL-E", "Generated Image URL: " + imageUrl);
                            return imageUrl;
                        }
                    }
                } else {
                    appendToTextView("DALL-E response missing image data.");
                    Log.e("DALL-E", "Response missing image data: " + response.toString());
                }
            } else {
                // Log error response details
                StringBuilder errorResponse = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getErrorStream(), "utf-8"))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        errorResponse.append(line.trim());
                    }
                }
                appendToTextView("Error: HTTP " + responseCode + " - " + errorResponse.toString());
                Log.e("DALL-E", "Error: HTTP " + responseCode + " - " + errorResponse.toString());
            }
        } catch (Exception e) {
            appendToTextView("Error sending to DALL-E: " + e.getMessage());
            Log.e("DALL-E", "Error sending to DALL-E: ", e);
        }
        return null;
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
                HttpURLConnection connection = (HttpURLConnection) new URL(CAMERA_ENDPOINT)
                        .openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                try (OutputStream os = connection.getOutputStream()) {
                    os.write(payload.getBytes("utf-8"));
                }

                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    StringBuilder response = new StringBuilder();
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(
                            connection.getInputStream(), "utf-8"
                    ))) {
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
                                JSONArray takePictureUrls = takePictureObject.optJSONArray(
                                        "takePictureUrl"
                                );
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
