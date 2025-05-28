package com.example.fancontrolapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketFactory;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "FanControlApp";
    private WebSocket ws;
    private TextView tempHumidityText, fanStatusLabel, autoModeStatusText;
    private Button speedOffButton, speedLowButton, speedMediumButton, speedHighButton,
            toggleHumidifierButton, oscillationButton, angleButton, autoModeButton;
    private boolean humidifierOn = false;
    private boolean oscillation = false;
    private int currentSpeed = 0;
    private boolean autoMode = false;
    private int currentAngleIndex = 2; // Start at 90 degrees
    private float tempThreshold = 30.0f;
    private float humidityThreshold = 60.0f;
    private float currentTemp = 0.0f;
    private float currentHumidity = 0.0f;
    private final int[] angles = {0, 45, 90, 135, 180};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI elements from XML
        tempHumidityText = findViewById(R.id.tempHumidityText);
        fanStatusLabel = findViewById(R.id.fanStatusLabel);
        autoModeStatusText = findViewById(R.id.autoModeStatusText);
        speedOffButton = findViewById(R.id.speedOffButton);
        speedLowButton = findViewById(R.id.speedLowButton);
        speedMediumButton = findViewById(R.id.speedMediumButton);
        speedHighButton = findViewById(R.id.speedHighButton);
        toggleHumidifierButton = findViewById(R.id.toggleHumidifierButton);
        oscillationButton = findViewById(R.id.oscillationButton);
        angleButton = findViewById(R.id.angleButton);
        autoModeButton = findViewById(R.id.autoModeButton);

        connectWebSocket();

        speedOffButton.setOnClickListener(v -> {
            if (!autoMode && ws != null && ws.isOpen()) {
                currentSpeed = 0;
                ws.sendText("speed:0");
                Log.d(TAG, "Sent command: speed:0");
                updateUI();
            }
        });

        speedLowButton.setOnClickListener(v -> {
            if (!autoMode && ws != null && ws.isOpen()) {
                currentSpeed = 1;
                ws.sendText("speed:1");
                Log.d(TAG, "Sent command: speed:1");
                updateUI();
            }
        });

        speedMediumButton.setOnClickListener(v -> {
            if (!autoMode && ws != null && ws.isOpen()) {
                currentSpeed = 2;
                ws.sendText("speed:2");
                Log.d(TAG, "Sent command: speed:2");
                updateUI();
            }
        });

        speedHighButton.setOnClickListener(v -> {
            if (!autoMode && ws != null && ws.isOpen()) {
                currentSpeed = 3;
                ws.sendText("speed:3");
                Log.d(TAG, "Sent command: speed:3");
                updateUI();
            }
        });

        toggleHumidifierButton.setOnClickListener(v -> {
            if (!autoMode && ws != null && ws.isOpen()) {
                humidifierOn = !humidifierOn;
                toggleHumidifierButton.setText(humidifierOn ? "Tắt đầu phun sương" : "Bật đầu phun sương");
                toggleHumidifierButton.setBackgroundTintList(ContextCompat.getColorStateList(
                        this, humidifierOn ? R.color.green : R.color.primary_blue));
                ws.sendText(humidifierOn ? "humidifier:on" : "humidifier:off");
                Log.d(TAG, "Sent command: " + (humidifierOn ? "humidifier:on" : "humidifier:off"));
                updateUI();
            }
        });

        oscillationButton.setOnClickListener(v -> {
            if (ws != null && ws.isOpen()) {
                oscillation = !oscillation;
                oscillationButton.setText("Xoay: " + (oscillation ? "Bật" : "Tắt"));
                oscillationButton.setBackgroundTintList(ContextCompat.getColorStateList(
                        this, oscillation ? R.color.green : R.color.primary_blue));
                ws.sendText(oscillation ? "oscillation:on" : "oscillation:off");
                Log.d(TAG, "Sent command: " + (oscillation ? "oscillation:on" : "oscillation:off"));
                updateUI();
            }
        });

        angleButton.setOnClickListener(v -> {
            if (!oscillation && ws != null && ws.isOpen()) {
                currentAngleIndex = (currentAngleIndex + 1) % angles.length;
                angleButton.setText("Hướng gió: " + angles[currentAngleIndex] + "°");
                ws.sendText("angle:next");
                Log.d(TAG, "Sent command: angle:next");
                updateUI();
            }
        });

        autoModeButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AutoModeActivity.class);
            startActivity(intent);
        });
    }

    private void updateUI() {
        // Update fan status
        fanStatusLabel.setText("Trạng thái quạt: " + (currentSpeed > 0 ? "Đang chạy (Tốc độ " + currentSpeed + ")" : "Tắt"));

        // Update temperature and humidity from sensor
        tempHumidityText.setText(String.format("Nhiệt độ: %.1f°C  |  Độ ẩm: %.1f%%",
                currentTemp, currentHumidity));

        // Update auto mode status with thresholds
        autoModeStatusText.setText(String.format("Chế độ tự động: %s\nNgưỡng nhiệt độ: %.1f°C\nNgưỡng độ ẩm: %.1f%%",
                autoMode ? "Bật" : "Tắt", tempThreshold, humidityThreshold));

        // Update button states and colors
        boolean enableManualControls = !autoMode;
        speedOffButton.setEnabled(enableManualControls);
        speedLowButton.setEnabled(enableManualControls);
        speedMediumButton.setEnabled(enableManualControls);
        speedHighButton.setEnabled(enableManualControls);
        toggleHumidifierButton.setEnabled(enableManualControls);
        oscillationButton.setEnabled(enableManualControls);
        angleButton.setEnabled(enableManualControls && !oscillation);

        // Set colors for enabled/disabled buttons
        int disabledColor = R.color.gray;
        int speedButtonColor = enableManualControls ? R.color.primary_blue : disabledColor;
        int speedOffButtonColor = enableManualControls ? R.color.red : disabledColor;
        speedOffButton.setBackgroundTintList(ContextCompat.getColorStateList(this, speedOffButtonColor));
        speedLowButton.setBackgroundTintList(ContextCompat.getColorStateList(this, speedButtonColor));
        speedMediumButton.setBackgroundTintList(ContextCompat.getColorStateList(this, speedButtonColor));
        speedHighButton.setBackgroundTintList(ContextCompat.getColorStateList(this, speedButtonColor));
        toggleHumidifierButton.setBackgroundTintList(ContextCompat.getColorStateList(
                this, enableManualControls ? (humidifierOn ? R.color.green : R.color.primary_blue) : disabledColor));
        oscillationButton.setBackgroundTintList(ContextCompat.getColorStateList(
                this, enableManualControls ? (oscillation ? R.color.green : R.color.primary_blue) : disabledColor));
        angleButton.setBackgroundTintList(ContextCompat.getColorStateList(
                this, (enableManualControls && !oscillation) ? R.color.primary_blue : disabledColor));
    }

    private void connectWebSocket() {
        try {
            ws = new WebSocketFactory().createSocket("ws://192.168.4.1:81");
            ws.addListener(new WebSocketAdapter() {
                @Override
                public void onTextMessage(WebSocket websocket, String message) {
                    runOnUiThread(() -> {
                        try {
                            Log.d(TAG, "Received JSON: " + message);
                            JSONObject json = new JSONObject(message);
                            currentSpeed = json.getInt("speed");
                            autoMode = json.getBoolean("auto");
                            oscillation = json.getBoolean("oscillation");
                            currentAngleIndex = java.util.Arrays.binarySearch(angles, json.getInt("angle"));
                            currentTemp = (float) json.getDouble("temp");
                            currentHumidity = (float) json.getDouble("humidity");
                            humidifierOn = json.getBoolean("humidifier");
                            tempThreshold = (float) json.getDouble("temp_threshold");
                            humidityThreshold = (float) json.getDouble("humidity_threshold");

                            // Update UI elements
                            oscillationButton.setText("Xoay: " + (oscillation ? "Bật" : "Tắt"));
                            angleButton.setText("Hướng gió: " + angles[currentAngleIndex] + "°");
                            toggleHumidifierButton.setText(humidifierOn ? "Tắt đầu phun sương" : "Bật đầu phun sương");
                            updateUI();
                        } catch (Exception e) {
                            Log.e(TAG, "JSON parsing error: " + e.getMessage());
                        }
                    });
                }

                @Override
                public void onConnected(WebSocket websocket, java.util.Map<String, java.util.List<String>> headers) {
                    Log.d(TAG, "WebSocket connected");
                    runOnUiThread(() -> {
                        tempHumidityText.setText("Nhiệt độ: --°C  |  Độ ẩm: --%");
                        fanStatusLabel.setText("Trạng thái quạt: Tắt");
                        autoModeStatusText.setText("Chế độ tự động: Tắt\nNgưỡng nhiệt độ: 30.0°C\nNgưỡng độ ẩm: 60.0%");
                        updateUI();
                    });
                }

                @Override
                public void onConnectError(WebSocket websocket, com.neovisionaries.ws.client.WebSocketException exception) {
                    Log.e(TAG, "WebSocket connection error: " + exception.getMessage());
                    runOnUiThread(() -> {
                        tempHumidityText.setText("Lỗi: Không kết nối được với ESP32");
                        fanStatusLabel.setText("Trạng thái quạt: Tắt");
                        updateUI();
                    });
                }

                @Override
                public void onDisconnected(WebSocket websocket, com.neovisionaries.ws.client.WebSocketFrame serverCloseFrame, com.neovisionaries.ws.client.WebSocketFrame clientCloseFrame, boolean closedByServer) {
                    Log.d(TAG, "WebSocket disconnected");
                    runOnUiThread(() -> {
                        tempHumidityText.setText("Lỗi: WebSocket đã ngắt kết nối");
                        fanStatusLabel.setText("Trạng thái quạt: Tắt");
                        updateUI();
                    });
                }
            });
            ws.connectAsynchronously();
        } catch (Exception e) {
            Log.e(TAG, "WebSocket initialization error: " + e.getMessage());
            runOnUiThread(() -> {
                tempHumidityText.setText("Lỗi: Không khởi tạo được WebSocket");
                fanStatusLabel.setText("Trạng thái quạt: Tắt");
                updateUI();
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ws != null) {
            ws.disconnect();
            Log.d(TAG, "WebSocket disconnected on destroy");
        }
    }
}