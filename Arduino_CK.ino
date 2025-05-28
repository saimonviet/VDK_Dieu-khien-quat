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
int servoAngle = 90; // Góc hiện tại của servo
float temperature = 0.0;
float humidity = 0.0;
bool humidifierOn = false;
float autoTempThreshold = 30.0; // Ngưỡng nhiệt độ tự động
float autoHumidityThreshold = 60.0; // Ngưỡng độ ẩm tự động

// Các góc cố định
const int fixedAngles[] = {0, 45, 90, 135, 180};
int currentAngleIndex = 2; // Bắt đầu ở 90 độ

// Điều khiển tốc độ quạt
void setFanSpeed(int speed) {
  speed = constrain(speed, 0, 3);
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
    for (int pos = 0; pos <= 180; pos += 1) {
      servo.write(pos);
      delay(30);
    }
    for (int pos = 180; pos >= 0; pos -= 1) {
      servo.write(pos);
      delay(30);
    }
  } else {
    servo.write(fixedAngles[currentAngleIndex]);
  }
}

void setup() {
  Serial.begin(115200);
  
  pinMode(MOTOR_ENA, OUTPUT);
  pinMode(MOTOR_IN1, OUTPUT);
  pinMode(MOTOR_IN2, OUTPUT);
  pinMode(RELAY_PIN, OUTPUT);
  digitalWrite(RELAY_PIN, LOW);
  
  servo.attach(SERVO_PIN);
  servo.write(fixedAngles[currentAngleIndex]);
  
  dht.begin();
}

void loop() {
  // Đọc nhiệt độ và độ ẩm
  temperature = dht.readTemperature();
  humidity = dht.readHumidity();
  if (isnan(temperature)) temperature = 0.0;
  if (isnan(humidity)) humidity = 0.0;
  
  // Chế độ tự động
  if (autoMode) {
    if (temperature > autoTempThreshold) {
      setFanSpeed(3); // Bật quạt tốc độ cao nếu nhiệt độ cao
    } else {
      setFanSpeed(0); // Tắt quạt nếu nhiệt độ thấp
    }
    setHumidifier(humidity < autoHumidityThreshold); // Bật phun sương nếu độ ẩm thấp
  }
  
  // Xử lý lệnh từ ESP32
  if (Serial.available()) {
    String command = Serial.readStringUntil('\n');
    command.trim();
    if (command.startsWith("speed:")) {
      if (!autoMode) {
        int newSpeed = command.substring(6).toInt();
        setFanSpeed(newSpeed);
      }
    } else if (command == "auto:on") {
      autoMode = true;
    } else if (command == "auto:off") {
      autoMode = false;
      setFanSpeed(fanSpeed); // Khôi phục tốc độ quạt thủ công
      setHumidifier(humidifierOn); // Khôi phục trạng thái phun sương thủ công
    } else if (command == "oscillation:on") {
      oscillation = true;
    } else if (command == "oscillation:off") {
      oscillation = false;
    } else if (command == "angle:next") {
      if (!oscillation) {
        currentAngleIndex = (currentAngleIndex + 1) % 5;
        servo.write(fixedAngles[currentAngleIndex]);
      }
    } else if (command == "humidifier:on") {
      if (!autoMode) {
        setHumidifier(true);
      }
    } else if (command == "humidifier:off") {
      if (!autoMode) {
        setHumidifier(false);
      }
    } else if (command.startsWith("temp_threshold:")) {
      autoTempThreshold = command.substring(15).toFloat();
    } else if (command.startsWith("humidity_threshold:")) {
      autoHumidityThreshold = command.substring(19).toFloat();
    }
  }
  
  // Gửi trạng thái đến ESP32
  static unsigned long lastSend = 0;
  if (millis() - lastSend >= 500) {
    String status = "{\"speed\":" + String(fanSpeed) + 
                    ",\"oscillation\":" + String(oscillation ? "true" : "false") + 
                    ",\"angle\":" + String(fixedAngles[currentAngleIndex]) + 
                    ",\"temp\":" + String(temperature) + 
                    ",\"humidity\":" + String(humidity) + 
                    ",\"humidifier\":" + String(humidifierOn ? "true" : "false") + 
                    ",\"auto\":" + String(autoMode ? "true" : "false") + 
                    ",\"temp_threshold\":" + String(autoTempThreshold) + 
                    ",\"humidity_threshold\":" + String(autoHumidityThreshold) + "}";
    Serial.println(status);
    lastSend = millis();
  }
  
  oscillateServo();
  delay(100);
}