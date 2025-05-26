#include <DHT.h>
#include <Servo.h>

// Định nghĩa chân
#define DHTPIN 7
#define DHTTYPE DHT11
#define MOTOR_ENA 10
#define MOTOR_IN1 9
#define MOTOR_IN2 8
#define SERVO_PIN 6
#define RELAY_PIN 5 // Chân điều khiển relay cho đầu tạo ẩm

// Khởi tạo đối tượng
DHT dht(DHTPIN, DHTTYPE);
Servo servo;

// Biến
int fanSpeed = 0;
bool autoMode = false;
bool oscillation = false;
int oscillationAngle = 90;
float temperature = 0.0;
bool humidifierOn = false; // Trạng thái đầu tạo ẩm

// Điều khiển tốc độ quạt
void setFanSpeed(int speed) {
  speed = constrain(speed, 0, 3); // Giới hạn tốc độ 0-3
  digitalWrite(MOTOR_IN1, HIGH);
  digitalWrite(MOTOR_IN2, LOW);
  if (speed == 0) {
    analogWrite(MOTOR_ENA, 0);
  } else if (speed == 1) {
    analogWrite(MOTOR_ENA, 85);
  } else if (speed == 2) {
    analogWrite(MOTOR_ENA, 170);
  } else if (speed == 3) {
    analogWrite(MOTOR_ENA, 255);
  }
  fanSpeed = speed;
}

// Điều khiển đầu tạo ẩm
void setHumidifier(bool state) {
  digitalWrite(RELAY_PIN, state ? HIGH : LOW);
  humidifierOn = state;
}

// Xoay servo
void oscillateServo() {
  if (oscillation) {
    for (int pos = 90 - oscillationAngle / 2; pos <= 90 + oscillationAngle / 2; pos += 1) {
      servo.write(pos);
      delay(15);
    }
    for (int pos = 90 + oscillationAngle / 2; pos >= 90 - oscillationAngle / 2; pos -= 1) {
      servo.write(pos);
      delay(15);
    }
  } else {
    servo.write(90);
  }
}

void setup() {
  Serial.begin(115200); // Serial trên chân 0, 1 cho ESP32
  
  pinMode(MOTOR_ENA, OUTPUT);
  pinMode(MOTOR_IN1, OUTPUT);
  pinMode(MOTOR_IN2, OUTPUT);
  pinMode(RELAY_PIN, OUTPUT); // Khởi tạo chân relay
  digitalWrite(RELAY_PIN, LOW); // Tắt relay ban đầu
  
  servo.attach(SERVO_PIN);
  servo.write(90);
  
  dht.begin();
}

void loop() {
  // Đọc nhiệt độ
  temperature = dht.readTemperature();
  if (isnan(temperature)) {
    temperature = 0.0; // Giá trị mặc định nếu lỗi
  }
  
  // Chế độ tự động
  if (autoMode) {
    if (temperature > 30.0) {
      setFanSpeed(3);
    } else {
      setFanSpeed(0);
    }
  }
  
  // Xử lý lệnh từ ESP32
  if (Serial.available()) {
    String command = Serial.readStringUntil('\n');
    command.trim();
    if (command.startsWith("speed:")) {
      int newSpeed = command.substring(6).toInt();
      if (!autoMode) {
        setFanSpeed(newSpeed);
      }
    } else if (command == "auto:on") {
      autoMode = true;
    } else if (command == "auto:off") {
      autoMode = false;
      setFanSpeed(fanSpeed);
    } else if (command == "oscillation:on") {
      oscillation = true;
    } else if (command == "oscillation:off") {
      oscillation = false;
    } else if (command.startsWith("angle:")) {
      oscillationAngle = command.substring(6).toInt();
      oscillationAngle = constrain(oscillationAngle, 30, 180);
    } else if (command == "humidifier:on") {
      setHumidifier(true);
    } else if (command == "humidifier:off") {
      setHumidifier(false);
    }
  }
  
  // Gửi trạng thái đến ESP32 mỗi 500ms
  static unsigned long lastSend = 0;
  if (millis() - lastSend >= 500) {
    String status = "{\"speed\":" + String(fanSpeed) + 
                    ",\"auto\":" + String(autoMode ? "true" : "false") + 
                    ",\"oscillation\":" + String(oscillation ? "true" : "false") + 
                    ",\"angle\":" + String(oscillationAngle) + 
                    ",\"temp\":" + String(temperature) + 
                    ",\"humidifier\":" + String(humidifierOn ? "true" : "false") + "}";
    Serial.println(status);
    lastSend = millis();
  }
  
  oscillateServo();
  delay(100);
}