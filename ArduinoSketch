#include <ArduinoJson.h>         // Include ArduinoJson library for JSON parsing
#include <LiquidCrystal_I2C.h>   // Include LiquidCrystal_I2C library for the LCD

// LCD Configuration
LiquidCrystal_I2C lcd(0x27, 16, 2); // Set the LCD address to 0x27 for a 16x2 display

// Pin Definitions
const int moistureSensorPin = A1;  // Moisture sensor input pin (Analog)
const int pumpPin = 8;             // Pump control pin (Digital)

// Variables
int delayTime = 1;                // Delay time in hours (default 1)
bool shouldUseDays = false;       // Use days or hours for delay
bool needsWater = false;          // If the soil needs watering
long totalDelayMs = 0;            // Total delay time in milliseconds
long currentDelay = 0;            // Current delay time for countdown
int moistureLevel = 0;            // Moisture level reading
bool setupComplete = false;       // Tracks if setup is complete

void setup() {
  // Initialize serial communication
  Serial.begin(9600);   // Start serial communication at 9600 baud

  // Initialize LCD
  lcd.init();           // Initialize the LCD
  lcd.backlight();      // Turn on the backlight
  updateLCD("Status:", "Awaiting Input");

  // Set pump pin as output
  pinMode(pumpPin, OUTPUT);

  // Initially, respond to 'ping' and wait for setup JSON to complete setup
  buildResponse("Waiting for setup...");
}

void loop() {
  // Check if a message is available in the Serial buffer
  if (Serial.available()) {
    String receivedMessage = Serial.readStringUntil('\n');  // Read until newline character
    receivedMessage.trim();  // Remove any leading or trailing whitespace

    // Respond with "pong" if the message is "ping"
    if (receivedMessage == "ping") {
      Serial.println("pong");
    }

    // If received JSON contains "status": "setup", start the main logic
    else if (receivedMessage.startsWith("{")) {
      // Create a StaticJsonDocument to parse the incoming message
      StaticJsonDocument<200> doc;
      DeserializationError error = deserializeJson(doc, receivedMessage);

      if (!error) {
        // Check if status is "setup"
        const char* status = doc["status"];
        if (strcmp(status, "setup") == 0) {
          // Extract setup parameters from the received JSON and configure the system
          delayTime = doc["delayTime"] | 1;  // Default to 1 if not specified
          shouldUseDays = doc["shouldUseDays"] | false;
          totalDelayMs = delayTime * (shouldUseDays ? 24 * 60 * 60 * 1000 : 60 * 60 * 1000);
          currentDelay = totalDelayMs;
          setupComplete = true;
          updateLCD("Setup Complete", "Monitoring...");
          buildResponse("Setup complete. Starting plant monitoring...");
        }
      } else {
        buildResponse("Error parsing setup JSON.");
      }
    }
  }

  // If setup is complete, proceed with the plant monitoring logic
  if (setupComplete) {
    // Read moisture level
    moistureLevel = analogRead(moistureSensorPin);  // Read moisture level from the sensor
    needsWater = moistureLevel < 511;  // Check if the soil needs watering (threshold = 511)

    // Build and send the current response
    String status = "Moisture Level: " + String(moistureLevel) + " / Remaining Delay (ms): " + String(currentDelay);
    buildResponse(status);

    // Update the LCD display with the current status
    String topRow = String(currentDelay / 1000) + "s/" + String(totalDelayMs / 1000) + "s"; // Current and total delay in seconds
    String bottomRow = "Moisture: " + String(moistureLevel);
    updateLCD(topRow, bottomRow);

    // If delay has elapsed, check moisture level and water if necessary
    if (currentDelay <= 0) {
      if (needsWater) {
        waterPlant();
      }

      // Reset delay after watering
      currentDelay = totalDelayMs;
    } else {
      // Decrease delay by 200ms (the loop interval)
      currentDelay -= 200;
    }
  } else {
    // If setup isn't complete, just wait for a valid setup message or respond to ping
    delay(100);
  }

  // Pause for a short time (200ms) before the next loop iteration
  delay(200);
}

// Function to water the plant (turn pump on for 2 seconds)
void waterPlant() {
  buildResponse("Watering plant...");
  digitalWrite(pumpPin, HIGH);   // Turn pump ON
  delay(2000);                   // Water for 2 seconds
  digitalWrite(pumpPin, LOW);    // Turn pump OFF
}

// Function to build and send the response string
void buildResponse(String status) {
  String response = "{";
  response += "\"delayTime\":\"" + String(delayTime) + "\",";
  response += "\"shouldUseDays\":\"" + String(shouldUseDays ? "true" : "false") + "\",";
  response += "\"needsWater\":\"" + String(needsWater ? "true" : "false") + "\",";
  response += "\"totalDelayMs\":\"" + String(totalDelayMs) + "\",";
  response += "\"currentDelay\":\"" + String(currentDelay) + "\",";
  response += "\"moistureLevel\":\"" + String(moistureLevel) + "\",";
  response += "\"status\":\"" + status + "\"";
  response += "}";

  // Send the response to the Serial Monitor
  Serial.println(response);
}

// Function to update the LCD display
void updateLCD(String topRow, String bottomRow) {
  lcd.clear();
  lcd.setCursor(0, 0);  // Move to the top row
  lcd.print(topRow);
  lcd.setCursor(0, 1);  // Move to the bottom row
  lcd.print(bottomRow);
}
