package com.mwmapps.transportoffline

enum class UpdateStage {
    Downloading,
    Extracting,
    Comparing,
    Importing,
    DownloadError,
    ExtractionError,
    ComparisonError,
    ImportError,
    Error,
    Completed
}
