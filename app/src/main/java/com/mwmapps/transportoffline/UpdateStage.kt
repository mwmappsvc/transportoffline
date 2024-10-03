// Begin UpdateStage.kt
// Enum class representing the different stages of the database update process.
// Externally Referenced Classes:
package com.mwmapps.transportoffline

enum class UpdateStage {
    Idle,
    Downloading,
    Extracting,
    Comparing,
    Importing,
    DownloadError,
    ExtractionError,
    ComparisonError,
    ImportError,
    Error,
    Completed,
    NoUpdateNeeded
}
// End UpdateStage.kt