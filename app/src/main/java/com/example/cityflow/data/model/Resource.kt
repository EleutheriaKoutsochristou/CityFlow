package com.example.cityflow.data.model

/**
 * Μοντέλο κατάστασης (State) για ασύγχρονες λειτουργίες (MVVM Pattern).
 */
sealed class Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String, val cause: Throwable? = null) : Resource<Nothing>()
    object Loading : Resource<Nothing>()
    object Idle : Resource<Nothing>()
}
