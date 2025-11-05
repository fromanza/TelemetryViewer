/*
 * ESP32 Feather Huzzah Oval Test Pattern Generator
 * 
 * Outputs an oval/circle pattern in CSV format via Serial
 * Format: "X,Y"
 * 
 * This generates a parametric ellipse that will draw an oval
 * when plotted in the XY Plot chart.
 * 
 * Board: ESP32 Feather Huzzah (Adafruit)
 * Connect to Serial Monitor at 115200 baud
 */

// Oval parameters
const float a = 5.0;  // Semi-major axis (X-axis radius)
const float b = 3.0;  // Semi-minor axis (Y-axis radius)
const int delayMs = 10;  // Time step between points (milliseconds)

// Animation parameters
float angle = 0.0;  // Current angle in radians
const float angleStep = 0.05;  // Angle increment per step (controls smoothness)
const float twoPi = 2.0 * PI;

void setup() {
  // Initialize serial communication for ESP32
  Serial.begin(115200);
  
  // ESP32-specific: Wait a bit longer for serial port to initialize
  delay(2000);
  
  // Optional: Print header (comment out if you want pure CSV)
  // Serial.println("X,Y");
}

void loop() {
  // Calculate X and Y using parametric equations for an ellipse
  // Add small random variation to X to make it interesting (not just a perfect oval)
  float baseX = a * cos(angle);
  float randomVariation = (random(-100, 100) / 100.0) * 0.5;  // Random variation: -0.5 to +0.5
  float x = baseX + randomVariation;
  
  float y = b * sin(angle);
  
  // Output in CSV format: "X,Y"
  Serial.print(x, 3);  // Print X with 3 decimal places
  Serial.print(",");
  Serial.println(y, 3);  // Print Y with 3 decimal places and newline
  
  // Increment angle
  angle += angleStep;
  
  // Wrap angle back to 0 when we complete a full circle
  if (angle >= twoPi) {
    angle = 0.0;
  }
  
  // Wait for the specified time step
  delay(delayMs);
}

