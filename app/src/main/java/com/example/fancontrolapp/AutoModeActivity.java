package com.example.fancontrolapp;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketFactory;

public class AutoModeActivity extends AppCompatActivity {
    private static final String TAG = "AutoModeActivity";
    private WebSocket ws;
    private EditText tempThresholdInput, humidityThresholdInput;
    private Button saveButton, disableAutoButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auto_mode);

        tempThresholdInput = findViewById(R.id.tempThresholdInput);
        humidityThresholdInput = findViewById(R.id.humidityThresholdInput);
        saveButton = findViewById(R.id.saveButton);
        disableAutoButton = findViewById(R.id.disableAutoButton);

        connectWebSocket();

        saveButton.setOnClickListener(v -> {
            if (ws != null && ws.isOpen()) {
                try {
                    float tempThreshold = Float.parseFloat(tempThresholdInput.getText().toString());
                    float humidityThreshold = Float.parseFloat(humidityThresholdInput.getText().toString());
                    ws.sendText("temp_threshold:" + tempThreshold);
                    ws.sendText("humidity_threshold:" + humidityThreshold);
                    ws.sendText("auto:on");
                    Log.d(TAG, "Gửi lệnh: temp_threshold:" + tempThreshold + ", humidity_threshold:" + humidityThreshold + ", auto:on");
                    finish();
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Lỗi định dạng số: " + e.getMessage());
                    tempThresholdInput.setError("Vui lòng nhập số hợp lệ");
                    humidityThresholdInput.setError("Vui lòng nhập số hợp lệ");
                }
            } else {
                Log.e(TAG, "WebSocket chưa kết nối");
            }
        });

        disableAutoButton.setOnClickListener(v -> {
            if (ws != null && ws.isOpen()) {
                ws.sendText("auto:off");
                Log.d(TAG, "Gửi lệnh: auto:off");
                finish();
            } else {
                Log.e(TAG, "WebSocket chưa kết nối");
            }
        });
    }

    private void connectWebSocket() {
        try {
            ws = new WebSocketFactory().createSocket("ws://192.168.4.1:81");
            ws.connectAsynchronously();
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khởi tạo WebSocket: " + e.getMessage());
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