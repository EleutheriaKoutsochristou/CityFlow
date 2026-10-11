package com.example.cityflow.data.model

/**
 * Generic wrapper representing the state of asynchronous data operations (MVVM architecture).
 *
 * @param T The type of data payload wrapped by the resource.
 */
sealed class Resource<out T> {
    /** Operation completed successfully with payload [data]. */
    data class Success<out T>(val data: T) : Resource<T>()

    /** Operation failed with an explanatory [message] and optional [cause] throwable. */
    data class Error(val message: String, val cause: Throwable? = null) : Resource<Nothing>()

    /** Operation is currently executing asynchronously. */
    object Loading : Resource<Nothing>()

    /** Default uninitiated state prior to launching an operation. */
    object Idle : Resource<Nothing>()
}
