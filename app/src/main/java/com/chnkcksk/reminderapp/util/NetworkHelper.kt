package com.chnkcksk.reminderapp.util

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.view.View
import com.google.android.material.snackbar.Snackbar

object NetworkHelper {

    // İnternet bağlantısı kontrolü
    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo != null && networkInfo.isConnected
        }
    }

    // İnternet yoksa gösterilecek dialog
    fun showNoInternetDialog(
        context: Context,
        rootView: View,
        activity: Activity,
    ) {
        val builder = AlertDialog.Builder(context)
            .setTitle("No Internet Connection")
            .setMessage("Please check your internet connection to continue.")
            .setCancelable(false)
            // İlk olarak pozitif butonu mutlaka ekle, tıklama listenerı sonradan ekleyeceğiz:
            .setPositiveButton("Try Again", null)
            .setNegativeButton("Quit") { _, _ ->
                activity.finishAffinity()
            }

        val dialog = builder.create()
        dialog.show()

        // Try Again butonuna özel tıklama dinleyicisi ekle (böylece otomatik kapanmayı engelle)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (NetworkHelper.isInternetAvailable(context)) {
                dialog.dismiss()
            } else {
                Snackbar.make(rootView, "Still no internet connection!", Snackbar.LENGTH_LONG).show()
            }
        }
    }




}