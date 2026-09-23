package com.example.cityflow.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Βοηθητική κλάση για τον προληπτικό έλεγχο συνδεσιμότητας στο διαδίκτυο (Network Connectivity Check).
 */
object NetworkUtils {

    /**
     * Ελέγχει αν η συσκευή είναι συνδεδεμένη σε ενεργό δίκτυο (Wi-Fi, Cellular, Ethernet)
     * με δυνατότητα πρόσβασης στο διαδίκτυο.
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false

        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
