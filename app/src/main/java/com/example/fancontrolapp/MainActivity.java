package com.example.fancontrolapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketFactory;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "FanControlApp";
    private WebSocket ws;
    private TextView statusText, angleText;
    private Button toggleFanButton, toggleHumidifierButton, speedLowButton, speedMediumButton, speedHighButton, autoModeButton, oscillationButton;
    private SeekBar angleSlider;
    private boolean fanOn = false;
    private int currentSpeed = 1;
    private boolean autoMode = false;
    private boolean oscillation = false;
    private int oscillationAngle = 90;
    private boolean humidifierOn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, 1);
        }

        statusText = findViewById(R.id.statusText);
        angleText = findViewById(R.id.angleText);
        toggleFanButton = findViewById(R.id.toggleFanButton);
        toggleHumidifierButton = findViewById(R.id.toggleHumidifierButton);
        speedLowButton = findViewById(R.id.speedLowButton);
        speedMediumButton = findViewById(R.id.speedMediumButton);
        speedHighButton = findViewById(R.id.speedHighButton);
        autoModeButton = findViewById(R.id.autoModeButton);
        oscillationButton = findViewById(R.id.oscillationButton);
        angleSlider = findViewById(R.id.angleSlider);

        connectWebSocket();

        toggleFanButton.setOnClickListener(v -> {
            fanOn = !fanOn;
            toggleFanButton.setText(fanOn ? "Tắt quạt" : "Bật quạt");
            if (ws != null && ws.isOpen()) {
                String command = fanOn ? "speed:" + currentSpeed : "speed:0";
                ws.sendText(command);
                Log.d(TAG, "Gửi lệnh: " + command);
                statusText.setText("Trạng thái quạt: " + (fanOn ? "Đang chạy (Tốc độ " + currentSpeed + ")" : "Tắt") + "\nNhiệt độ: --°C\nĐầu tạo ẩm: " + (humidifierOn ? "Bật" : "Tắt"));
            } else {
                Log.e(TAG, "WebSocket chưa kết nối");
                statusText.setText("Lỗi: WebSocket chưa kết nối");
            }
        });

        toggleHumidifierButton.setOnClickListener(v -> {
            if (ws != null && ws.isOpen()) {
                humidifierOn = !humidifierOn;
                toggleHumidifierButton.setText(humidifierOn ? "Tắt đầu tạo ẩm" : "Bật đầu tạo ẩm");
                ws.sendText(humidifierOn ? "humidifier:on" : "humidifier:off");
                Log.d(TAG, "Gửi lệnh: " + (humidifierOn ? "humidifier:on" : "humidifier:off"));
                statusText.setText("Trạng thái quạt: " + (fanOn ? "Đang chạy (Tốc độ " + currentSpeed + ")" : "Tắt") + "\nNhiệt độ: --°C\nĐầu tạo ẩm: " + (humidifierOn ? "Bật" : "Tắt"));
            } else {
                Log.e(TAG, "WebSocket chưa kết nối");
                statusText.setText("Lỗi: WebSocket chưa kết nối");
            }
        });

        speedLowButton.setOnClickListener(v -> {
            if (!autoMode && ws != null && ws.isOpen()) {
                currentSpeed = 1;
                fanOn = true;
                toggleFanButton.setText("Tắt quạt");
                ws.sendText("speed:1");
                Log.d(TAG, "Gửi lệnh: speed:1");
                statusText.setText("Trạng thái quạt: Đang chạy (Tốc độ 1)\nNhiệt độ: --°C\nĐầu tạo ẩm: " + (humidifierOn ? "Bật" : "Tắt"));
            } else {
                Log.e(TAG, autoMode ? "Không gửi speed:1 vì autoMode bật" : "WebSocket chưa kết nối");
            }
        });

        speedMediumButton.setOnClickListener(v -> {
            if (!autoMode && ws != null && ws.isOpen()) {
                currentSpeed = 2;
                fanOn = true;
                toggleFanButton.setText("Tắt quạt");
                ws.sendText("speed:2");
                Log.d(TAG, "Gửi lệnh: speed:2");
                statusText.setText("Trạng thái quạt: Đang chạy (Tốc độ 2)\nNhiệt độ: --°C\nĐầu tạo ẩm: " + (humidifierOn ? "Bật" : "Tắt"));
            } else {
                Log.e(TAG, autoMode ? "Không gửi speed:2 vì autoMode bật" : "WebSocket chưa kết nối");
            }
        });

        speedHighButton.setOnClickListener(v -> {
            if (!autoMode && ws != null && ws.isOpen()) {
                currentSpeed = 3;
                fanOn = true;
                toggleFanButton.setText("Tắt quạt");
                ws.sendText("speed:3");
                Log.d(TAG, "Gửi lệnh: speed:3");
                statusText.setText("Trạng thái quạt: Đang chạy (Tốc độ 3)\nNhiệt độ: --°C\nĐầu tạo ẩm: " + (humidifierOn ? "Bật" : "Tắt"));
            } else {
                Log.e(TAG, autoMode ? "Không gửi speed:3 vì autoMode bật" : "WebSocket chưa kết nối");
            }
        });

        autoModeButton.setOnClickListener(v -> {
            if (ws != null && ws.isOpen()) {
                autoMode = !autoMode;
                autoModeButton.setText("Chế độ tự động: " + (autoMode ? "Bật" : "Tắt"));
                ws.sendText(autoMode ? "auto:on" : "auto:off");
                Log.d(TAG, "Gửi lệnh: " + (autoMode ? "auto:on" : "auto:off"));
                speedLowButton.setEnabled(!autoMode);
                speedMediumButton.setEnabled(!autoMode);
                speedHighButton.setEnabled(!autoMode);
                statusText.setText("Trạng thái quạt: " + (fanOn ? "Đang chạy (Tốc độ " + currentSpeed + ")" : "Tắt") + "\nNhiệt độ: --°C\nĐầu tạo ẩm: " + (humidifierOn ? "Bật" : "Tắt"));
            } else {
                Log.e(TAG, "WebSocket chưa kết nối");
            }
        });

        oscillationButton.setOnClickListener(v -> {
            if (ws != null && ws.isOpen()) {
                oscillation = !oscillation;
                oscillationButton.setText("Xoay quạt: " + (oscillation ? "Bật" : "Tắt"));
                ws.sendText(oscillation ? "oscillation:on" : "oscillation:off");
                Log.d(TAG, "Gửi lệnh: " + (oscillation ? "oscillation:on" : "oscillation:off"));
                statusText.setText("Trạng thái quạt: " + (fanOn ? "Đang chạy (Tốc độ " + currentSpeed + ")" : "Tắt") + "\nNhiệt độ: --°C\nĐầu tạo ẩm: " + (humidifierOn ? "Bật" : "Tắt"));
            } else {
                Log.e(TAG, "WebSocket chưa kết nối");
            }
        });

        angleSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && ws != null && ws.isOpen()) {
                    oscillationAngle = progress + 30;
                    angleText.setText("Góc: " + oscillationAngle + "°");
                    ws.sendText("angle:" + oscillationAngle);
                    Log.d(TAG, "Gửi lệnh: angle:" + oscillationAngle);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void connectWebSocket() {
        try {
            ws = new WebSocketFactory().createSocket("ws://192.168.4.1:81");
            ws.addListener(new WebSocketAdapter() {
                @Override
                public void onTextMessage(WebSocket websocket, String message) {
                    runOnUiThread(() -> {
                        try {
                            Log.d(TAG, "Nhận JSON: " + message);
                            JSONObject json = new JSONObject(message);
                            currentSpeed = json.getInt("speed");
                            autoMode = json.getBoolean("auto");
                            oscillation = json.getBoolean("oscillation");
                            oscillationAngle = json.getInt("angle");
                            float temp = (float) json.getDouble("temp");
                            humidifierOn = json.getBoolean("humidifier");

                            fanOn = currentSpeed > 0;
                            toggleFanButton.setText(fanOn ? "Tắt quạt" : "Bật quạt");
                            toggleHumidifierButton.setText(humidifierOn ? "Tắt đầu tạo ẩm" : "Bật đầu tạo ẩm");
                            autoModeButton.setText("Chế độ tự động: " + (autoMode ? "Bật" : "Tắt"));
                            oscillationButton.setText("Xoay quạt: " + (oscillation ? "Bật" : "Tắt"));
                            angleSlider.setProgress(oscillationAngle - 30);
                            angleText.setText("Góc: " + oscillationAngle + "°");
                            statusText.setText("Trạng thái quạt: " + (fanOn ? "Đang chạy (Tốc độ " + currentSpeed + ")" : "Tắt") + "\nNhiệt độ: " + String.format("%.1f", temp) + "°C\nĐầu tạo ẩm: " + (humidifierOn ? "Bật" : "Tắt"));
                            speedLowButton.setEnabled(!autoMode);
                            speedMediumButton.setEnabled(!autoMode);
                            speedHighButton.setEnabled(!autoMode);
                        } catch (Exception e) {
                            Log.e(TAG, "Lỗi phân tích JSON: " + e.getMessage());
                        }
                    });
                }

                @Override
                public void onConnected(WebSocket websocket, java.util.Map<String, java.util.List<String>> headers) {
                    Log.d(TAG, "WebSocket đã kết nối");
                    runOnUiThread(() -> statusText.setText("Đã kết nối với ESP32\nNhiệt độ: --°C\nĐầu tạo ẩm: Tắt"));
                }

                @Override
                public void onConnectError(WebSocket websocket, com.neovisionaries.ws.client.WebSocketException exception) {
                    Log.e(TAG, "Lỗi kết nối WebSocket: " + exception.getMessage());
                    runOnUiThread(() -> statusText.setText("Lỗi: Không kết nối được với ESP32"));
                }

                @Override
                public void onDisconnected(WebSocket websocket, com.neovisionaries.ws.client.WebSocketFrame serverCloseFrame, com.neovisionaries.ws.client.WebSocketFrame clientCloseFrame, boolean closedByServer) {
                    Log.d(TAG, "WebSocket đã ngắt kết nối");
                    runOnUiThread(() -> statusText.setText("Lỗi: WebSocket đã ngắt kết nối"));
                }
            });
            ws.connectAsynchronously();
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khởi tạo WebSocket: " + e.getMessage());
            runOnUiThread(() -> statusText.setText("Lỗi: Không khởi tạo được WebSocket"));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ws != null) {
            ws.disconnect();
            Log.d(TAG, "WebSocket đã ngắt kết nối khi thoát");
        }
    }
}