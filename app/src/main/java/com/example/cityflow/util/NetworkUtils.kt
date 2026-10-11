package com.example.cityflow.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Utility helper for verifying device network connectivity before executing remote data operations.
 */
object NetworkUtils {

    /**
     * Checks if the device has an active internet connection through Wi-Fi, Cellular, or Ethernet.
     *
     * @param context Application or Activity context.
     * @return True if connected with confirmed internet capability, false otherwise.
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false

        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

        // Ensure active network explicitly provides internet capability
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
