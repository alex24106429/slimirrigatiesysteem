#include <ArduinoJson.h>
#include <LiquidCrystal_I2C.h>
#include <EEPROM.h>

// LCD Configuration
LiquidCrystal_I2C lcd(0x27, 16, 2);

// Pin Definitions
const int moistureSensorPin = A1;
const int pumpPin = 8;
const int displayPowerPin = 7;
const int selectButtonPin = 6;

// Communication Constants
const int BUFFER_SIZE = 1024;
const unsigned long SERIAL_TIMEOUT = 1000;
const int MAX_RETRIES = 3;

// For JSON parsing, increase the capacity
StaticJsonDocument<800> doc;

// Variables
float delayTime = 1;
bool shouldUseDays = false;
bool needsWater = false;
unsigned long totalDelayMs = 0;
unsigned long targetTimestamp = 0;
unsigned long lastSaveTime = 0;  // Track when we last saved to EEPROM
int moistureLevel = 0;
bool setupComplete = false;
bool debug = false;
int outputML = 100;
const float flowRateMLPerSec = 26.67;

// Plant display details
char plantName[32] = "";
char plantType[32] = "";
bool displayingDetails = false;

// Communication buffer
char messageBuffer[BUFFER_SIZE];
int bufferIndex = 0;

// Debounce
unsigned long lastButtonPress = 0;
const int DEBOUNCE_DELAY = 200;

// EEPROM address definitions
#define EEPROM_INITIALIZED_ADDR 0    // 1 byte
#define UNIT_NAME_ADDR 1            // 32 bytes
#define PLANT_NAME_ADDR 33          // 32 bytes
#define PLANT_TYPE_ADDR 65          // 32 bytes
#define USE_DAYS_ADDR 97            // 1 byte
#define DELAY_TIME_ADDR 98          // 4 bytes (float)
#define OUTPUT_ML_ADDR 102          // 2 bytes
#define TARGET_TIMESTAMP_ADDR 104    // 4 bytes for target timestamp + 4 bytes for last known millis()
#define MOISTURE_LEVEL_ADDR 112      // 2 bytes

// Variables
char unitName[32] = "";  // Add unit name storage
bool isFactoryTest = false;
bool factoryResetChecked = false;  // Add flag to ensure we only check once

void setup() {
  Serial.begin(4800);
  Serial.setTimeout(5000);

  while (!Serial) {
    ; // Wait for serial port to connect
  }
  
  // Initialize pins
  pinMode(displayPowerPin, OUTPUT);
  pinMode(pumpPin, OUTPUT);
  pinMode(selectButtonPin, INPUT_PULLUP);
  
  digitalWrite(displayPowerPin, HIGH);
  
  // Initialize LCD
  lcd.init();
  lcd.backlight();
  updateLCD("Status:", "Initializing...");

  // Debug mode check
  checkDebugMode();
  
  clearSerialBuffer();

  if (!debug) {
    // Check if EEPROM is initialized
    if (EEPROM.read(EEPROM_INITIALIZED_ADDR) == 0xFF) {
      // First time running, initialize with defaults
      initializeEEPROM();
    } else {
      // Load saved values
      loadFromEEPROM();
    }
  }
}

void loop() {
  // Check for factory reset only once and only if not in debug mode
  if (!factoryResetChecked && !debug) {
    checkFactoryReset();
    factoryResetChecked = true;  // Mark as checked
    
    // If factory reset was triggered, perform it
    if (isFactoryTest) {
      performFactoryReset();
      return;  // Don't continue with normal operation this cycle
    }
  } else if (!factoryResetChecked && debug) {
    // Fake factory reset
    Serial.println("###DEBUG:Factory reset triggered###");
    factoryResetChecked = true;
  }

  // Make sure we always are able to handle Serial communication
  handleSerialCommunication();

  // Only if the setup is complete, handle display/watering
  if (setupComplete) {
    handleDisplayAndWatering();
  }
}

void handleSerialCommunication() {
  if (Serial.available()) {
    String message = readSerialMessage();
    clearSerialBuffer();

    if (message.length() > 0) {
      processMessage(message);
    }
  }
}

String readSerialMessage() {
  String message = "";
  unsigned long startTime = millis();

  // Read in chunks of data (1 byte at a time)
  while ((millis() - startTime) < SERIAL_TIMEOUT) {
    if (Serial.available()) {
      char c = Serial.read();
      if (c == '\n') {
        return message;
      }
      message += c;

      // Prevent the message from getting too large (buffer limit)
      if (message.length() >= BUFFER_SIZE - 1) {
        Serial.println("###ERROR:Message reached buffer limit###");
        return message;
      }
    }
  }

  if (message.length() > 0) {
    Serial.println("###ERROR:Timeout with partial message: " + message + "###");
  }

  return message;
}

void processMessage(String message) {
  message.trim();
  
  // Skip processing our own debug/protocol messages
  if (message.startsWith("###DEBUG:") || message.startsWith("###ERROR:") || message.startsWith("###PROTOCOL:")) {
    return;
  }
  
  if (message == "READY") {
    Serial.println("###PROTOCOL:READY_ACK###");
  }
  else if (message.startsWith("MSG:")) {
    handleMsgCommand(message);
  }
}

void handleMsgCommand(String message) {
  int firstColon = message.indexOf(':');
  int secondColon = message.indexOf(':', firstColon + 1);

  if (firstColon != -1 && secondColon != -1) {
    String checksumStr = message.substring(firstColon + 1, secondColon);
    String payload = message.substring(secondColon + 1);

    unsigned long calculatedChecksum = calculateChecksum(payload.c_str(), payload.length());
    unsigned long receivedChecksum = checksumStr.toInt();
    
    if (calculatedChecksum == receivedChecksum) {
      Serial.println("###PROTOCOL:CHECKSUM_OK###");

      // Handle different command types
      if (payload == "getUnitName") {
        // If unit name is empty, send empty response, otherwise send the name
        if (strlen(unitName) == 0) {
          Serial.println("###PROTOCOL:###");
        } else {
          Serial.println("###PROTOCOL:UNIT_NAME-" + String(unitName) + "###");
        }
      }
      else if (payload.startsWith("setname-")) {
        // Set the unit name and save to EEPROM
        String newName = payload.substring(8);
        strlcpy(unitName, newName.c_str(), sizeof(unitName));
        saveToEEPROM();
        Serial.println("###PROTOCOL:Done###");
      }
      else if (payload.startsWith("pn-")) {  // Plant Name
        strlcpy(plantName, payload.substring(3).c_str(), sizeof(plantName));
        Serial.println("###PROTOCOL:Done###");
      }
      else if (payload.startsWith("pt-")) {  // Plant Type
        strlcpy(plantType, payload.substring(3).c_str(), sizeof(plantType));
        Serial.println("###PROTOCOL:Done###");
      }
      else if (payload.startsWith("ud-")) {  // Use Days
        shouldUseDays = payload.substring(3).equals("true");
        Serial.println("###PROTOCOL:Done###");
      }
      else if (payload.startsWith("dt-")) {  // Delay Time
        delayTime = payload.substring(3).toFloat();
        Serial.println("###PROTOCOL:Done###");
      }
      else if (payload.startsWith("om-")) {  // Output ML
        outputML = payload.substring(3).toInt();
        Serial.println("###PROTOCOL:Done###");
      }
      else if (payload.startsWith("mm-")) {  // Min Moisture
        int minMoisture = payload.substring(3).toInt();
        // Store minMoisture if needed
        Serial.println("###PROTOCOL:Done###");
      }
      else if (payload == "apply") {  // Apply all settings
        totalDelayMs = delayTime * (shouldUseDays ? 24L * 60L * 60L * 1000L : 60L * 60L * 1000L);
        targetTimestamp = millis() + totalDelayMs;
        setupComplete = true;
        saveToEEPROM();  // Save settings after applying them
        updateLCD("Config Updated", plantName);
        Serial.println("###PROTOCOL:Done###");
      }
      else if (payload == "ping") {
        delay(50);
        Serial.println("###PROTOCOL:pong###");
      }
      else if (payload == "fetch") {
        // Read current moisture level
        moistureLevel = analogRead(moistureSensorPin);
        sendStatus("Data fetched successfully");
      }
      else {
        Serial.println("###PROTOCOL:UNKNOWN_COMMAND###");
      }
    } else {
      Serial.println("###PROTOCOL:CHECKSUM_ERROR###");
    }
  } else {
    if (!message.startsWith("###DEBUG:") && !message.startsWith("###ERROR:") && !message.startsWith("###PROTOCOL:")) {
      Serial.println("###PROTOCOL:FORMAT_ERROR###");
    }
  }
}

void handleDisplayAndWatering() {
  if (!setupComplete) return;

  // Read moisture and handle button press
  moistureLevel = analogRead(moistureSensorPin);
  needsWater = moistureLevel < 511;
  
  if (digitalRead(selectButtonPin) == LOW) {
    if (millis() - lastButtonPress > DEBOUNCE_DELAY) {
      displayingDetails = !displayingDetails;
      lastButtonPress = millis();
    }
  }

  // Save progress every 10 seconds
  if (millis() - lastSaveTime >= 10000) {
    saveCurrentProgress();
    lastSaveTime = millis();
  }

  // Update display once per second
  static unsigned long lastDisplayUpdate = 0;
  if (millis() - lastDisplayUpdate >= 1000) {
    updateDisplay();
    lastDisplayUpdate = millis();
  }

  // Check if watering is needed
  if (millis() >= targetTimestamp) {
    if (needsWater) {
      waterPlant();
    }
    targetTimestamp = millis() + totalDelayMs;
    saveCurrentProgress();  // Save after updating target timestamp
  }
}

void updateDisplay() {
  if (displayingDetails) {
    String topRow = String(plantName).substring(0, 16);
    String bottomRow = String(plantType) + " " + String(outputML) + "mL";
    updateLCD(topRow, bottomRow);
  } else {
    // Calculate and display time remaining
    unsigned long remainingTimeMs = (targetTimestamp > millis()) ? targetTimestamp - millis() : 0;
    displayTimeAndMoisture(remainingTimeMs);
  }
}

void displayTimeAndMoisture(unsigned long remainingTimeMs) {
  unsigned long totalSeconds = remainingTimeMs / 1000;
  unsigned int seconds = totalSeconds % 60;
  unsigned int minutes = (totalSeconds / 60) % 60;
  unsigned int hours = (totalSeconds / 3600) % 24;
  unsigned int days = totalSeconds / 86400;

  String timeStr = formatTimeString(days, hours, minutes, seconds);
  String moistureStr = "Moisture: " + String(moistureLevel);
  
  updateLCD(timeStr, moistureStr);
}

String formatTimeString(unsigned int days, unsigned int hours, 
                       unsigned int minutes, unsigned int seconds) {
  String result = "";
  
  if (shouldUseDays && days > 0) {
    result += (days < 10 ? "0" : "") + String(days) + ":";
  }
  if (shouldUseDays || hours > 0) {
    result += (hours < 10 ? "0" : "") + String(hours) + ":";
  }
  result += (minutes < 10 ? "0" : "") + String(minutes) + ":";
  result += (seconds < 10 ? "0" : "") + String(seconds);
  
  return result;
}

void waterPlant() {
  updateLCD("Watering plant", "Please wait...");
  
  unsigned long pumpDelayMs = (unsigned long)((outputML / flowRateMLPerSec) * 1000);
  
  digitalWrite(pumpPin, HIGH);
  delay(pumpDelayMs);
  digitalWrite(pumpPin, LOW);
  
  updateLCD("Status:", "Watering complete");
  delay(2000);
}

void checkDebugMode() {
  updateLCD("HOLD BUTTON TO", "START DEBUG MODE");
  int currentCheck = 0;

  while (currentCheck < 10) {
    if (digitalRead(selectButtonPin) == LOW) {
      debug = true;
      break;
    }
    delay(100);
    currentCheck++;
  }

  if (debug) {
    setupDebugMode();
  } else {
    // Only initialize/load EEPROM if not in debug mode
    if (EEPROM.read(EEPROM_INITIALIZED_ADDR) == 0xFF) {
      initializeEEPROM();
    } else {
      loadFromEEPROM();
    }
    updateLCD("Status:", "Awaiting setup..");
  }
}

void setupDebugMode() {
  // Set debug values without touching EEPROM
  strlcpy(plantName, "Debug Plant", sizeof(plantName));
  strlcpy(plantType, "Test Type", sizeof(plantType));
  delayTime = .005;
  shouldUseDays = false;
  totalDelayMs = delayTime * (shouldUseDays ? 24L * 60L * 60L * 1000L : 60L * 60L * 1000L);
  targetTimestamp = millis() + totalDelayMs;
  outputML = 100;
  setupComplete = true;
  
  updateLCD("Debug Mode", "Test Data Set");
  sendStatus("Debug mode active");
  delay(500);
}

unsigned long calculateChecksum(const char* data, size_t length) {
  Serial.println("###DEBUG:Calculating checksum###");

  unsigned long checksum = 0;
  
  // Iterate over each byte in the input data
  for (size_t i = 0; i < length; i++) {
    checksum += data[i];  // Add each byte value to the checksum
  }

  // Optional: Reduce the checksum size to fit into a 16-bit or 32-bit value
  checksum = checksum & 0xFFFFFFFF;  // Ensure it fits within a 32-bit value
  
  Serial.println("###DEBUG:Calculated checksum: " + String(checksum));
  return checksum;
}

void clearSerialBuffer() {
  Serial.flush();

  // Wait for the buffer to be empty
  while (Serial.available()) Serial.read();
}

// Improved sendStatus using ArduinoJson
void sendStatus(const char* status) {
  StaticJsonDocument<200> doc;
  
  doc["currentDelay"] = ((targetTimestamp - millis()) / 1000);
  doc["shouldUseDays"] = shouldUseDays;
  doc["moistureLevel"] = moistureLevel;
  doc["plantName"] = plantName;
  doc["status"] = status;
  
  Serial.print("###PROTOCOL:");
  serializeJson(doc, Serial);
  Serial.println("###");
}

void updateLCD(String topRow, String bottomRow) {
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print(topRow);
  lcd.setCursor(0, 1);
  lcd.print(bottomRow);
}

void initializeEEPROM() {
  EEPROM.write(EEPROM_INITIALIZED_ADDR, 0x01);
  
  // Initialize unit name as empty
  for (int i = 0; i < 32; i++) {
    EEPROM.write(UNIT_NAME_ADDR + i, 0);
  }
  
  // Initialize with empty values
  for (int i = 0; i < 32; i++) {
    EEPROM.write(PLANT_NAME_ADDR + i, 0);
    EEPROM.write(PLANT_TYPE_ADDR + i, 0);
  }
  
  EEPROM.write(USE_DAYS_ADDR, 0);
  EEPROM.put(DELAY_TIME_ADDR, 1.0f);
  EEPROM.put(OUTPUT_ML_ADDR, 100);
  EEPROM.put(TARGET_TIMESTAMP_ADDR, 0UL);
  EEPROM.put(TARGET_TIMESTAMP_ADDR + 4, 0UL);
  EEPROM.put(MOISTURE_LEVEL_ADDR, 0);
}

void loadFromEEPROM() {
  if (debug) return;  // Skip EEPROM operations in debug mode
  
  // Load unit name
  for (int i = 0; i < 32; i++) {
    unitName[i] = EEPROM.read(UNIT_NAME_ADDR + i);
    if (unitName[i] == 0) break;
  }
  
  // Load plant name
  for (int i = 0; i < 32; i++) {
    plantName[i] = EEPROM.read(PLANT_NAME_ADDR + i);
    if (plantName[i] == 0) break;
  }
  
  // Load plant type
  for (int i = 0; i < 32; i++) {
    plantType[i] = EEPROM.read(PLANT_TYPE_ADDR + i);
    if (plantType[i] == 0) break;
  }
  
  shouldUseDays = EEPROM.read(USE_DAYS_ADDR);
  EEPROM.get(DELAY_TIME_ADDR, delayTime);
  EEPROM.get(OUTPUT_ML_ADDR, outputML);
  
  // Load the saved target timestamp and last known millis
  unsigned long savedTargetTimestamp;
  unsigned long lastKnownMillis;
  EEPROM.get(TARGET_TIMESTAMP_ADDR, savedTargetTimestamp);
  EEPROM.get(TARGET_TIMESTAMP_ADDR + 4, lastKnownMillis);
  
  // Calculate how much time has passed and adjust the target timestamp
  totalDelayMs = delayTime * (shouldUseDays ? 24L * 60L * 60L * 1000L : 60L * 60L * 1000L);
  
  if (savedTargetTimestamp > lastKnownMillis) {
    // Calculate remaining time when Arduino was last running
    unsigned long remainingTime = savedTargetTimestamp - lastKnownMillis;
    // Set new target timestamp based on remaining time
    targetTimestamp = millis() + remainingTime;
  } else {
    // If timestamp was in the past or invalid, start a new countdown
    targetTimestamp = millis() + totalDelayMs;
  }
  
  EEPROM.get(MOISTURE_LEVEL_ADDR, moistureLevel);
  setupComplete = true;
  lastSaveTime = millis();  // Initialize last save time
}

void saveToEEPROM() {
  if (debug) return;  // Skip EEPROM operations in debug mode
  
  // Save unit name
  for (int i = 0; i < 32; i++) {
    EEPROM.update(UNIT_NAME_ADDR + i, unitName[i]);
  }
  
  // Save plant name
  for (int i = 0; i < 32; i++) {
    EEPROM.update(PLANT_NAME_ADDR + i, plantName[i]);
  }
  
  // Save plant type
  for (int i = 0; i < 32; i++) {
    EEPROM.update(PLANT_TYPE_ADDR + i, plantType[i]);
  }
  
  EEPROM.update(USE_DAYS_ADDR, shouldUseDays);
  EEPROM.put(DELAY_TIME_ADDR, delayTime);
  EEPROM.put(OUTPUT_ML_ADDR, outputML);
  EEPROM.put(TARGET_TIMESTAMP_ADDR, targetTimestamp);
  EEPROM.put(TARGET_TIMESTAMP_ADDR + 4, millis());
  EEPROM.put(MOISTURE_LEVEL_ADDR, moistureLevel);
}

void saveCurrentProgress() {
  if (debug) return;  // Skip EEPROM operations in debug mode
  
  // Calculate and save the current delay value
  unsigned long currentDelay = (targetTimestamp > millis()) ? (targetTimestamp - millis()) / 1000 : 0;
  EEPROM.put(TARGET_TIMESTAMP_ADDR, targetTimestamp);
  
  // Also save the last known millis() value for reference on reboot
  unsigned long currentMillis = millis();
  EEPROM.put(TARGET_TIMESTAMP_ADDR + 4, currentMillis);
}

void checkFactoryReset() {
  updateLCD("Hold 10s for", "Factory Reset");
  
  int holdTime = 0;
  const int checkInterval = 100;  // Check every 100ms
  const int requiredHoldTime = 10000;  // 10 seconds in milliseconds
  
  while (digitalRead(selectButtonPin) == LOW) {
    delay(checkInterval);
    holdTime += checkInterval;
    
    // Update progress on LCD
    int secondsHeld = holdTime / 1000;
    updateLCD("Hold " + String(10 - secondsHeld) + "s for", "Factory Reset");
    
    if (holdTime >= requiredHoldTime) {
      isFactoryTest = true;
      updateLCD("Factory Reset", "Activated!");
      delay(1000);  // Show confirmation message
      break;
    }
  }
}

void performFactoryReset() {
  updateLCD("Performing", "Factory Reset");
  
  // Iterate through each byte of the EEPROM storage using the pre-provided length function
  for (int i = 0; i < EEPROM.length(); i++) {
    EEPROM.write(i, 0);
    
    // Update progress every 64 bytes
    if (i % 64 == 0) {
      int progress = (i * 100) / EEPROM.length();
      updateLCD("Factory Reset", String(progress) + "%");
    }
  }
  
  // Show completion message
  updateLCD("Factory Reset", "Complete!");
  
  // Wait a moment before restarting
  delay(3000);
  
  // Force a restart by triggering the watchdog timer
  asm volatile ("jmp 0");  // Jump to reset vector
}