#  Smart Gas Cylinder Monitoring System

**IoT-based weight monitoring with statistical and machine-learning prediction of gas depletion and leak detection**

Built for **Data Odyssey 2026** — *"Humanity x AI: The New Age of Innovation"*
General Sir John Kotelawala Defence University (KDU) | BSc (Hons) Applied Data Science & Communication

---

## 📌 Overview

Most households that rely on Liquefied Petroleum Gas (LPG) cylinders have no reliable way to know how much gas is left, or whether a cylinder is leaking, until it's too late — the gas runs out mid-cooking, or a leak goes undetected until it becomes dangerous.

**Smart Gas Cylinder Monitoring System** is an IoT prototype that continuously measures cylinder weight and gas concentration, applies statistical and machine-learning models to predict how many days of gas remain, and sends real-time alerts to a mobile app — including instant leak notifications.

This project was built by **Team SentineIX** and applies concepts from the *Fundamentals of Statistics* and *Applied Machine Learning* modules to a real, human-centred problem.

---

## ✨ Features

- 📡 **Real-time sensing** — cylinder weight and gas concentration captured every 5 minutes via an ESP32 microcontroller
- ☁️ **Cloud-connected** — sensor data streamed over Wi-Fi to a Supabase/PostgreSQL database
- 📊 **Statistical baseline** — STL decomposition and rolling linear regression to estimate daily consumption
- 🤖 **ML-based forecasting** — SARIMA model to predict time-to-depletion with much higher accuracy
- 🚨 **Independent leak detection** — threshold-based alerting using the MQ-6 gas sensor, decoupled from the forecasting models for fast, reliable response
- 📱 **Mobile app** — built with Kotlin & Jetpack Compose, showing live weight, gas levels, forecast comparisons, and push notifications via Firebase Cloud Messaging

---

## 🏗️ System Architecture

The system is organized into four layers:

| Layer | Description |
|---|---|
| **Sensing** | ESP32 + load cell + MQ-6 gas sensor capture weight and gas concentration |
| **Transmission & Storage** | Sensor data sent over Wi-Fi to a cloud-hosted Supabase/PostgreSQL database |
| **Analysis** | Statistical (STL, rolling regression) and SARIMA models are run on the stored readings to generate depletion forecasts |
| **Presentation** | A Kotlin/Jetpack Compose mobile app displays live data, forecasts, and push alerts |

---

## 🛠️ Tech Stack

**Hardware**
- ESP32 development board
- Load cell + HX711 amplifier
- MQ-6 gas sensor

**Backend / Data**
- Python (pandas, statsmodels, scikit-learn)
- Supabase (PostgreSQL)
- SARIMA (`statsmodels.tsa.statespace.sarimax`)

**Mobile App**
- Kotlin
- Jetpack Compose
- Firebase Cloud Messaging

---

## 📂 Repository Structure

├── firmware/ # ESP32 C++/Arduino code for sensor readings & data upload
├── backend/ # Python scripts for data processing, STL, and SARIMA modeling
├── mobile-app/ # Kotlin + Jetpack Compose Android application
├── data/ # Collected sensor readings (CSV/exports)
├── docs/ # Project report, diagrams, and supporting documents
└── README.md


> *Update the tree above to match your actual folder layout.*

---

## 📊 Dataset

- **Source:** Custom-collected via the ESP32 prototype (not a public dataset)
- **Collection period:** 25 July – 26 August 2026 (33 days)
- **Readings:** 9,360 total, sampled every 5 minutes
- **Refill cycles captured:** 3

**Fields:**
| Field | Description |
|---|---|
| `weight` | Cylinder weight (kg) — main depletion indicator |
| `gas_ppm` | Gas concentration — used for leak detection |
| `timestamp` | Time the reading was captured |
| `device_id` | Identifies the cylinder/household unit |

---

## 🔬 Methodology

**Statistical approach**
- STL decomposition to extract trend, seasonal usage patterns, and noise
- Change-point detection to flag refill events
- Rolling 7-day linear regression for a baseline daily consumption estimate

**Machine learning approach**
- SARIMA (1,1,1)(1,1,1,24) fitted on the hourly weight series to capture daily cooking-cycle seasonality
- A secondary regression model (using hour-of-day, day-of-week, and rolling averages) as a comparison benchmark
- Evaluation via MAE and RMSE over a 3-day held-out test period

**Leak detection**
- Independent, threshold-based rule on gas sensor readings (700 ppm-equivalent) — deliberately decoupled from the forecasting models to guarantee fast, reliable alerts regardless of forecast accuracy

---

## 📈 Results

| Model | MAE (kg) | RMSE (kg) |
|---|---|---|
| Rolling Linear Regression (baseline) | 0.050 | 0.065 |
| **SARIMA (1,1,1)(1,1,1,24)** | **0.004** | **0.005** |
| Secondary Regression Model | 0.029 | 0.036 |

- SARIMA reduced forecast error by roughly **10x** compared to the linear baseline, thanks to explicit modelling of the daily cooking cycle.
- The leak-detection system successfully flagged a controlled test leak within **20 minutes**, with **zero false positives** across the full 33-day trial.

---

## 🚀 Getting Started

### Prerequisites
- Python 3.9+
- Arduino IDE / PlatformIO (for ESP32 firmware)
- Android Studio (for the mobile app)
- A Supabase project (PostgreSQL database + REST API)
- A Firebase project (for Cloud Messaging)

### Setup

1. **Clone the repository**
```bash
   git clone https://github.com/seshanperera2004/SentineIX.git/<your-repo>.git
   cd <your-repo>
```

2. **ESP32 Firmware**
   - Open `firmware/gas_monitor.ino` in Arduino IDE
   - Install the required libraries via Library Manager: `WiFi.h`, `HX711.h`
   - Update your Wi-Fi credentials and Supabase URL/API key in the config section at the top of the sketch
   - Select **Board: ESP32 Dev Module** and the correct COM port
   - Upload the sketch to your ESP32

3. **Mobile App**
   - Open `mobile-app/` in Android Studio
   - Add your Supabase and Firebase configuration files
   - Build and run on an emulator or physical device
---

## ⚠️ Limitations

- Data was collected from a single prototype unit over 33 days (3 refill cycles), limiting generalisability to other households or cylinder sizes
- The MQ-6 sensor was used as a relative, threshold-based signal rather than a ppm-calibrated one
- Brief Wi-Fi dropouts caused minor gaps, filled via linear interpolation

## 🔭 Future Work

- Multi-cylinder monitoring support
- Calibrated gas sensor readings
- Longer-term, multi-household data collection for a more generalizable model

---

## 👥 Team SentineIX

- **Seshan Perera** 
- **Nisul Rankothge**
- **Dinithi Liyanage**
- **Nethma Medhavi**

---

## 📚 References

- Seabold, S. and Perktold, J. (2010) 'statsmodels: Econometric and statistical modeling with python', *Proceedings of the 9th Python in Science Conference*.
- Cleveland, R.B., Cleveland, W.S., McRae, J.E. and Terpenning, I. (1990) 'STL: A seasonal-trend decomposition procedure based on loess', *Journal of Official Statistics*, 6(1), pp. 3–73.
- Box, G.E.P., Jenkins, G.M., Reinsel, G.C. and Ljung, G.M. (2015) *Time Series Analysis: Forecasting and Control*. 5th edn. Hoboken, NJ: Wiley.
- Espressif Systems (2024) *ESP32 Series Datasheet*. Available at: https://www.espressif.com/en/products/socs/esp32
- SparkFun Electronics (n.d.) *HX711 Load Cell Amplifier Datasheet*. Available at: https://www.sparkfun.com/products/13879
- Winsen Electronics (n.d.) *MQ-6 Gas Sensor Datasheet*. Available at: https://www.winsen-sensor.com/sensors/gas-sensor/mq-6.html
- Supabase (2026) *Supabase Documentation*. Available at: https://supabase.com/docs
- Google Firebase (2026) *Firebase Cloud Messaging Documentation*. Available at: https://firebase.google.com/docs/cloud-messaging
- Pandas Development Team (2026) *pandas Documentation*. Available at: https://pandas.pydata.org/docs/

---

## 📄 License

This project was developed for academic purposes as part of Data Odyssey 2026 at KDU. 
