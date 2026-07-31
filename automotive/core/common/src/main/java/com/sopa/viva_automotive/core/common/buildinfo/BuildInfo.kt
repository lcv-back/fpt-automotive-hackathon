package com.sopa.viva_automotive.core.common.buildinfo

data class BuildInfo(
    val versionName: String,
    val versionCode: Int,
    val applicationId: String,
        val buildType: String,
        val vehicleBackend: String,
    val isDebuggable: Boolean,
) {
    val versionLabel: String
        get() = "$versionName ($versionCode)"

        val purposeLabel: String
        get() = when {
            vehicleBackend == "mock" && isDebuggable -> "Development / testing"
            vehicleBackend == "mock" -> "Testing (mock vehicle)"
            isDebuggable -> "Product (debug)"
            else -> "Product"
        }
}

interface BuildInfoProvider {
    val buildInfo: BuildInfo
}
