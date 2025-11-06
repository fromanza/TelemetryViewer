/*
 * ESP32 Feather Huzzah MPU-6050 Data Reader
 * 
 * Reads raw accelerometer and gyroscope data from MPU-6050
 * Outputs data in CSV format via Serial
 * Format: "AccelX,AccelY,AccelZ,GyroX,GyroY,GyroZ"
 * 
 * Board: ESP32 Feather Huzzah (Adafruit)
 * Sensor: SparkFun MPU-6050 (connected via I2C)
 * Connect to Serial Monitor at 115200 baud
 * 
 * MPU-6050 I2C connections:
 * - VCC -> 3.3V
 * - GND -> GND
 * - SCL -> GPIO 22 (default ESP32 I2C SCL)
 * - SDA -> GPIO 21 (default ESP32 I2C SDA)
 * 
 * Note: GPIO 34 and 39 are input-only and cannot be used for I2C.
 * If you need different pins, use any GPIO that supports output (not 34, 35, 36, 39).
 */

#include <Wire.h>

// I2C pin definitions for ESP32
// Default ESP32 I2C pins (can be changed to any output-capable GPIO)
const int SDA_PIN = 23;  // Default SDA
const int SCL_PIN = 22;  // Default SCL

// MPU-6050 I2C address (try 0x68 or 0x69 depending on AD0 pin)
int MPU6050_ADDR = 0x68;

// MPU-6050 register addresses
const int MPU6050_PWR_MGMT_1 = 0x6B;
const int MPU6050_ACCEL_XOUT_H = 0x3B;
const int MPU6050_GYRO_XOUT_H = 0x43;

void setup() {
  Serial.begin(115200);
  
  // ESP32-specific: Wait a bit longer for serial port to initialize
  delay(2000);
  
  Serial.println("Initializing I2C...");
  Serial.print("SDA pin: GPIO");
  Serial.println(SDA_PIN);
  Serial.print("SCL pin: GPIO");
  Serial.println(SCL_PIN);
  
  // Test pin states before I2C init
  pinMode(SDA_PIN, INPUT_PULLUP);
  pinMode(SCL_PIN, INPUT_PULLUP);
  delay(10);
  
  int sdaLevel = digitalRead(SDA_PIN);
  int sclLevel = digitalRead(SCL_PIN);
  
  Serial.print("SDA level (should be HIGH/1 with pullup): ");
  Serial.println(sdaLevel);
  Serial.print("SCL level (should be HIGH/1 with pullup): ");
  Serial.println(sclLevel);
  
  if(sdaLevel == LOW || sclLevel == LOW) {
    Serial.println("WARNING: SDA or SCL is stuck LOW!");
    Serial.println("This usually means:");
    Serial.println("  - Missing pullup resistors (add 4.7kΩ to 3.3V)");
    Serial.println("  - Short circuit to ground");
    Serial.println("  - Sensor not powered or damaged");
  }
  
  if(sdaLevel == LOW && sclLevel == LOW) {
    Serial.println("Both lines LOW - check power and pullups!");
  } else if(sdaLevel == LOW) {
    Serial.println("*** SDA is stuck LOW! ***");
    Serial.println("Possible causes:");
    Serial.println("  1. SDA wire shorted to ground");
    Serial.println("  2. MPU-6050 SDA pin damaged (pulling low)");
    Serial.println("  3. SDA not connected properly");
    Serial.println("");
    Serial.println("TEST: Disconnect MPU-6050 and check if SDA goes HIGH.");
    Serial.println("  - If SDA goes HIGH when disconnected = sensor is bad");
    Serial.println("  - If SDA stays LOW when disconnected = wiring issue");
  } else if(sclLevel == LOW) {
    Serial.println("SCL is stuck LOW - check SCL wiring!");
  }
  
  // Initialize I2C with custom pins and clock speed
  // CRITICAL: I2C requires pullup resistors (4.7kΩ) on SDA and SCL to 3.3V
  // ESP32 internal pullups are weak (45kΩ) and may not work reliably
  // Most breakout boards have pullups built-in, but bare modules need external pullups
  Wire.begin(SDA_PIN, SCL_PIN);
  Wire.setClock(100000); // 100kHz I2C speed (slower for reliability)
  
  delay(100);
  
  // Scan I2C bus for devices
  Serial.println("Scanning I2C bus...");
  int devicesFound = 0;
  for(byte address = 1; address < 127; address++) {
    Wire.beginTransmission(address);
    byte error = Wire.endTransmission();
    if(error == 0) {
      Serial.print("I2C device found at address 0x");
      if(address < 16) Serial.print("0");
      Serial.println(address, HEX);
      devicesFound++;
    }
  }
  if(devicesFound == 0) {
    Serial.println("No I2C devices found!");
    Serial.println("");
    Serial.println("TROUBLESHOOTING:");
    Serial.println("1. Check wiring:");
    Serial.println("   - SDA -> GPIO21");
    Serial.println("   - SCL -> GPIO22");
    Serial.println("   - VCC -> 3.3V (NOT 5V!)");
    Serial.println("   - GND -> GND");
    Serial.println("");
    Serial.println("2. I2C REQUIRES pullup resistors:");
    Serial.println("   - Add 4.7kΩ resistor from SDA to 3.3V");
    Serial.println("   - Add 4.7kΩ resistor from SCL to 3.3V");
    Serial.println("   - Most breakout boards have these built-in");
    Serial.println("   - Bare MPU-6050 modules need external pullups");
    Serial.println("");
    Serial.println("3. Verify power: MPU-6050 needs stable 3.3V");
    Serial.println("4. Check if SDA/SCL are swapped");
  }
  
  // Try to find MPU-6050 at address 0x68 or 0x69
  bool found = false;
  for(int addr = 0x68; addr <= 0x69; addr++) {
    Wire.beginTransmission(addr);
    Wire.write(MPU6050_PWR_MGMT_1);
    Wire.write(0);
    byte error = Wire.endTransmission();
    if(error == 0) {
      MPU6050_ADDR = addr;
      Serial.print("MPU-6050 found at address 0x");
      Serial.println(addr, HEX);
      found = true;
      break;
    }
  }
  
  if(!found) {
    Serial.println("MPU-6050 not found at 0x68 or 0x69!");
    Serial.println("Check wiring and power connections.");
  }
  
  delay(100);
}

void loop() {
  // Read accelerometer data
  Wire.beginTransmission(MPU6050_ADDR);
  Wire.write(MPU6050_ACCEL_XOUT_H);
  byte error = Wire.endTransmission(false);
  
  if(error != 0) {
    Serial.println("-1,-1,-1,-1,-1,-1");
    delay(100);
    return;
  }
  
  uint8_t bytesRead = Wire.requestFrom(MPU6050_ADDR, 6, true);
  if(bytesRead != 6) {
    Serial.println("-1,-1,-1,-1,-1,-1");
    delay(100);
    return;
  }
  
  int16_t accelX = (Wire.read() << 8) | Wire.read();
  int16_t accelY = (Wire.read() << 8) | Wire.read();
  int16_t accelZ = (Wire.read() << 8) | Wire.read();
  
  // Read gyroscope data
  Wire.beginTransmission(MPU6050_ADDR);
  Wire.write(MPU6050_GYRO_XOUT_H);
  error = Wire.endTransmission(false);
  
  if(error != 0) {
    Serial.println("-1,-1,-1,-1,-1,-1");
    delay(100);
    return;
  }
  
  bytesRead = Wire.requestFrom(MPU6050_ADDR, 6, true);
  if(bytesRead != 6) {
    Serial.println("-1,-1,-1,-1,-1,-1");
    delay(100);
    return;
  }
  
  int16_t gyroX = (Wire.read() << 8) | Wire.read();
  int16_t gyroY = (Wire.read() << 8) | Wire.read();
  int16_t gyroZ = (Wire.read() << 8) | Wire.read();
  
  // Output raw values in CSV format
  Serial.print(accelX);
  Serial.print(",");
  Serial.print(accelY);
  Serial.print(",");
  Serial.print(accelZ);
  Serial.print(",");
  Serial.print(gyroX);
  Serial.print(",");
  Serial.print(gyroY);
  Serial.print(",");
  Serial.println(gyroZ);
  
  delay(10);
}
