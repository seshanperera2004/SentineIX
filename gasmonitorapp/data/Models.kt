package com.example.gasmonitorapp.data

data class Reading(
    val device_id: String,
    val weight: Double,
    val gas_ppm: Int,
    val timestamp: String
)

data class Prediction(
    val device_id: String,
    val days_remaining_regression: Double?,
    val days_remaining_sarima: Double?,
    val leak_detected: Boolean,
    val computed_at: String
)