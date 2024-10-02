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
