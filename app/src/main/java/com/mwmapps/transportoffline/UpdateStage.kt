// Section 1
// Comments with Section Numbers are Added, Removed, and Modified by the Human developer ONLY
// IMPORTANT: Do not change the location of section remarks. Keep them exactly as they are.
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
// Section 2