#include <WiFi.h>
#include <WebSocketsServer.h>

// Thông tin Wi-Fi cho Access Point
const char* ssid = "ESP32_Fan_AP";
const char* password = "12345678";

// Khởi tạo WebSocket server trên cổng 81
WebSocketsServer webSocket = WebSocketsServer(81);

// Cấu hình chân UART2
#define RXD2 19
#define TXD2 18

void webSocketEvent(uint8_t num, WStype_t type, uint8_t * payload, size_t length) {
  switch (type) {
    case WStype_CONNECTED: {
      IPAddress ip = webSocket.remoteIP(num);
      Serial.print("Client đã kết nối: ");
      Serial.println(ip);
      webSocket.sendTXT(num, "Đã kết nối với ESP32 WebSocket thành công");
      break;
    }
    case WStype_TEXT: {
      String message = "";
      for (size_t i = 0; i < length; i++) {
        message += (char)payload[i];
      }
      message.trim();
      Serial.print("Nhận lệnh từ client: ");
      Serial.println(message);
      
      Serial2.println(message);
      Serial.print("Đã gửi lệnh đến Arduino: ");
      Serial.println(message);
      break;
    }
    case WStype_DISCONNECTED: {
      Serial.print("Client đã ngắt kết nối: ");
      Serial.println(webSocket.remoteIP(num));
      break;
    }
  }
}

void setup() {
  Serial.begin(115200);
  Serial2.begin(115200, SERIAL_8N1, RXD2, TXD2);

  WiFi.softAP(ssid, password);
  delay(100);

  Serial.println("Access Point đã khởi động");
  Serial.print("SSID: ");
  Serial.println(ssid);
  Serial.print("IP Address: ");
  Serial.println(WiFi.softAPIP());

  webSocket.begin();
  webSocket.onEvent(webSocketEvent);
}

void loop() {
  webSocket.loop();

  if (Serial2.available()) {
    String status = Serial2.readStringUntil('\n');
    status.trim();
    if (status.length() > 0) {
      Serial.print("Trạng thái từ Arduino: ");
      Serial.println(status);
      webSocket.broadcastTXT(status);
    } else {
      Serial.println("Dữ liệu trạng thái từ Arduino rỗng");
    }
  }

  delay(10);
}