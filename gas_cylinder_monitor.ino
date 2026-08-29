/*
  Smart Gas Cylinder Monitoring System - ESP32 Firmware
  Team: SentineIX | Data Odyssey 2026

  Hardware:
    - ESP32 Dev Board
    - HX711 Load Cell Amplifier + Load Cell (mounted under cylinder)
    - MQ-6 Gas Sensor (mounted near valve/regulator)

  Function:
    Reads weight (kg) and gas concentration (ppm-equivalent) every
    READING_INTERVAL_MS milliseconds and posts a JSON payload to a
    Supabase REST endpoint (the "readings" table).

  Libraries required (install via Arduino Library Manager):
    - HX711 by Bogdan Necula (bogde/HX711)
    - ArduinoJson by Benoit Blanchon
  (WiFi.h and HTTPClient.h come bundled with the ESP32 board package)
*/

#include <WiFi.h>
#include <HTTPClient.h>
#include <HX711.h>
#include <ArduinoJson.h>

// ---------------------- USER CONFIGURATION ----------------------

// WiFi credentials
const char* WIFI_SSID     = "YOUR_WIFI_SSID";
const char* WIFI_PASSWORD = "YOUR_WIFI_PASSWORD";

// Supabase project details
// Found in Supabase Dashboard -> Project Settings -> API
const char* SUPABASE_URL      = "https://YOUR_PROJECT_REF.supabase.co/rest/v1/readings";
const char* SUPABASE_API_KEY  = "YOUR_SUPABASE_ANON_OR_SERVICE_KEY";

// Device identifier (unique per cylinder/household unit)
const char* DEVICE_ID = "cylinder_01";

// HX711 pins
const int HX711_DOUT_PIN = 4;
const int HX711_SCK_PIN  = 5;

// MQ-6 gas sensor analog pin (use an ADC1 pin on ESP32, e.g. GPIO34-39)
const int MQ6_PIN = 34;

// Load cell calibration factor (see calibrateLoadCell() below to determine this)
// weight_kg = raw_reading / CALIBRATION_FACTOR
float CALIBRATION_FACTOR = 2280.0;  // <-- replace with your calibrated value

// Reading interval (5 minutes, matching the report's 5-minute sampling)
const unsigned long READING_INTERVAL_MS = 5UL * 60UL * 1000UL;

// Leak detection threshold (raw MQ-6 ppm-equivalent reading)
const float LEAK_THRESHOLD_PPM = 700.0;

// ------------------------------------------------------------------

HX711 scale;
unsigned long lastReadingTime = 0;

void setup() {
  Serial.begin(115200);
  delay(1000);

  // Initialise load cell
  scale.begin(HX711_DOUT_PIN, HX711_SCK_PIN);
  scale.set_scale(CALIBRATION_FACTOR);
  scale.tare();  // Reset to 0 with no load - ensure cylinder platform is
                 // in its resting/empty-of-load state when this runs,
                 // or replace with a known offset if calibrating with load present.

  // Initialise gas sensor pin
  pinMode(MQ6_PIN, INPUT);

  connectToWiFi();
}

void loop() {
  unsigned long now = millis();

  if (now - lastReadingTime >= READING_INTERVAL_MS || lastReadingTime == 0) {
    lastReadingTime = now;

    float weightKg = readWeight();
    float gasPpm   = readGasPPM();
    bool leakFlag  = gasPpm >= LEAK_THRESHOLD_PPM;

    Serial.println("---- New Reading ----");
    Serial.print("Weight (kg): "); Serial.println(weightKg, 3);
    Serial.print("Gas (ppm-equiv): "); Serial.println(gasPpm, 1);
    Serial.print("Leak detected: "); Serial.println(leakFlag ? "YES" : "no");

    sendReadingToSupabase(weightKg, gasPpm, leakFlag);
  }

  delay(1000); // small idle delay; loop checks time each second
}

// ---------------------- WiFi ----------------------

void connectToWiFi() {
  Serial.print("Connecting to WiFi");
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

  unsigned long startAttempt = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - startAttempt < 20000) {
    delay(500);
    Serial.print(".");
  }

  if (WiFi.status() == WL_CONNECTED) {
    Serial.println();
    Serial.print("Connected. IP address: ");
    Serial.println(WiFi.localIP());
  } else {
    Serial.println();
    Serial.println("WiFi connection failed. Will retry on next send attempt.");
  }
}

void ensureWiFiConnected() {
  if (WiFi.status() != WL_CONNECTED) {
    connectToWiFi();
  }
}

// ---------------------- Sensor Reading ----------------------

float readWeight() {
  if (scale.is_ready()) {
    // Average of several samples for stability
    float weight = scale.get_units(10);
    if (weight < 0) weight = 0; // clamp small negative noise near empty/tare drift
    return weight;
  } else {
    Serial.println("HX711 not ready, returning last known value as 0.");
    return 0.0;
  }
}

// Converts the MQ-6 raw analog reading into an approximate ppm-equivalent
// value. This is a simplified linear mapping intended for RELATIVE leak
// detection (i.e., "is the reading elevated compared to baseline?"), not a
// certified quantitative ppm measurement. For lab-accurate ppm, calibrate
// against the MQ-6 datasheet's Rs/R0 curve using clean-air R0 calibration.
float readGasPPM() {
  int raw = analogRead(MQ6_PIN);       // ESP32 ADC: 0-4095 (12-bit)
  float ppmEquivalent = map(raw, 0, 4095, 0, 1000); // simple linear scaling
  return ppmEquivalent;
}

// ---------------------- Supabase Upload ----------------------

void sendReadingToSupabase(float weightKg, float gasPpm, bool leakFlag) {
  ensureWiFiConnected();

  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("No WiFi connection - skipping upload for this cycle.");
    return;
  }

  HTTPClient http;
  http.begin(SUPABASE_URL);
  http.addHeader("Content-Type", "application/json");
  http.addHeader("apikey", SUPABASE_API_KEY);
  http.addHeader("Authorization", String("Bearer ") + SUPABASE_API_KEY);
  http.addHeader("Prefer", "return=minimal");

  StaticJsonDocument<256> doc;
  doc["device_id"] = DEVICE_ID;
  doc["weight"] = weightKg;
  doc["gas_ppm"] = gasPpm;
  doc["leak_detected"] = leakFlag;
  // timestamp is left out here so Supabase's default (now()) fills it in;
  // add doc["timestamp"] = "<ISO8601 string>" if you want the device to set it.

  String payload;
  serializeJson(doc, payload);

  int httpResponseCode = http.POST(payload);

  if (httpResponseCode > 0) {
    Serial.print("Upload response code: ");
    Serial.println(httpResponseCode);
  } else {
    Serial.print("Upload failed, error: ");
    Serial.println(http.errorToString(httpResponseCode));
  }

  http.end();
}

/*
  ---------------------- CALIBRATION HELPER ----------------------
  Run this once (temporarily call it from setup() instead of tare-only
  init) to determine CALIBRATION_FACTOR for your specific load cell:

  void calibrateLoadCell() {
    scale.begin(HX711_DOUT_PIN, HX711_SCK_PIN);
    Serial.println("Remove all weight, then send any character.");
    while (!Serial.available());
    Serial.read();
    scale.set_scale();
    scale.tare();
    Serial.println("Place a known weight (e.g. 5.000 kg), then send any character.");
    while (!Serial.available());
    Serial.read();
    long reading = scale.get_units(10);
    float knownWeightKg = 5.0; // change to your reference weight
    float factor = reading / knownWeightKg;
    Serial.print("Calibration factor: ");
    Serial.println(factor);
    // Use this printed value as CALIBRATION_FACTOR above.
  }
*/
