package com.example.domain.model

sealed class AudioProcessingError {
    object UnsupportedFormat : AudioProcessingError()
    object FileTooLarge : AudioProcessingError()
    object InsufficientStorage : AudioProcessingError()
    object ModelLoadFailed : AudioProcessingError()
    object NnApiUnavailable : AudioProcessingError()
    data class Unknown(val cause: Throwable) : AudioProcessingError()
}
