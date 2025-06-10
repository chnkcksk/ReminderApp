package com.chnkcksk.reminderapp.view

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import com.chnkcksk.reminderapp.R
import com.chnkcksk.reminderapp.databinding.FragmentAppPreferencesBinding
import com.chnkcksk.reminderapp.permissions.NotificationPermissionManager
import com.chnkcksk.reminderapp.util.LoadingManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class AppPreferencesFragment : Fragment() {

    private var _binding: FragmentAppPreferencesBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private val loadingManager = LoadingManager.getInstance()

    private lateinit var permissionManager: NotificationPermissionManager

    // Switch değişikliğini programatik olarak kontrol etmek için
    private var isUpdatingSwitch = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth

        // Permission manager'ı başlat ve launcher'ı kaydet
        permissionManager = NotificationPermissionManager.getInstance()
            .registerPermissionLauncher(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAppPreferencesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupButtons()
        updateSwitchState()
    }

    override fun onResume() {
        super.onResume()
        // Kullanıcı ayarlardan geri döndüğünde switch durumunu güncelle
        // Kısa bir gecikme ile kontrol et (ayarlar değişikliğinin işlenmesi için)
        binding.root.postDelayed({
            updateSwitchState()
        }, 300)
    }

    private fun setupButtons() {
        binding.notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            // Programatik güncelleme sırasında işlem yapma
            if (isUpdatingSwitch) return@setOnCheckedChangeListener

            if (isChecked) {
                // Kullanıcı switch'i açmaya çalışıyor - izin kontrolü yap
                requestNotificationPermission()
            } else {
                // Kullanıcı switch'i kapatmaya çalışıyor - bildirimleri kapat
                disableNotifications()
            }
        }

        binding.backButton.setOnClickListener {
            goBack()
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {


                override fun handleOnBackPressed() {
                    goBack()

                }
            })
    }

    private fun updateSwitchState() {
        context?.let { ctx ->
            isUpdatingSwitch = true

            val hasPermission = permissionManager.isNotificationPermissionGranted(ctx)
            binding.notificationSwitch.isChecked = hasPermission

            updateSwitchAppearance(hasPermission)

            isUpdatingSwitch = false
        }
    }

    private fun updateSwitchAppearance(isEnabled: Boolean) {
        if (isEnabled) {
            val greenColor = ContextCompat.getColor(requireContext(), R.color.slider_color)
            binding.notificationSwitch.thumbTintList = ColorStateList.valueOf(greenColor)
            binding.notificationSwitch.trackTintList =
                ColorStateList.valueOf(greenColor)
        } else {
            val grayColor = ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
            binding.notificationSwitch.thumbTintList = ColorStateList.valueOf(grayColor)
            binding.notificationSwitch.trackTintList = ColorStateList.valueOf(grayColor)
        }
    }

    private fun requestNotificationPermission() {
        context?.let { ctx ->
            permissionManager.checkNotificationPermission(
                ctx,
                object : NotificationPermissionManager.NotificationPermissionCallback {
                    override fun onPermissionGranted(notificationContent: NotificationPermissionManager.NotificationContent) {
                        // İzin verildi - switch'i açık tut
                        Toast.makeText(context, "Notifications opened successfully", Toast.LENGTH_SHORT)
                            .show()
                        updateSwitchAppearance(true)
                    }

                    override fun onPermissionDenied() {
                        // İzin verilmedi - switch'i geri kapat ve dialog göster
                        isUpdatingSwitch = true
                        binding.notificationSwitch.isChecked = false
                        updateSwitchAppearance(false)
                        isUpdatingSwitch = false

                        showPermissionDeniedDialog()
                    }

                    override fun onNotificationsDisabled() {
                        // Bildirimler kapalı - switch'i geri kapat ve dialog göster
                        isUpdatingSwitch = true
                        binding.notificationSwitch.isChecked = false
                        updateSwitchAppearance(false)
                        isUpdatingSwitch = false

                        showNotificationDisabledDialog()
                    }

                    override fun onSettingsOpened() {
                        // Ayarlar açıldı - kullanıcı ayarlardan geri döndüğünde onResume'da kontrol edilecek
                        Toast.makeText(
                            context,
                            "You can switch on notification permission in the settings",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                // Özelleştirilmiş bildirim içeriği
                NotificationPermissionManager.NotificationContent(
                    title = "Reminder Notification",
                    message = "Example of notification from your reminder app",
                    channelId = "reminder_channel",
                    channelName = "Reminder Notifications",
                    channelDescription = "Reminder app notifications",
                    delaySeconds = 3
                )
            )
        }
    }

    /**
     * Bildirim izni reddedildiğinde gösterilen dialog
     */
    private fun showPermissionDeniedDialog() {
        context?.let { ctx ->
            AlertDialog.Builder(ctx, R.style.MyDialogTheme)
                .setTitle("Notification Permission Required")
                .setMessage("You need to grant notification permission for the app to send notifications. Would you like to go to the settings?")
                .setPositiveButton("Settings") { _, _ ->
                    permissionManager.openAppSettings(ctx)
                }
                .setNegativeButton("Cancel") { _, _ ->
                    Toast.makeText(context, "Notification permission could not be granted", Toast.LENGTH_SHORT)
                        .show()
                }
                .setCancelable(false)
                .create()
                .apply {
                    setOnShowListener {
                        // Butonların metin rengini değiştir
                        getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(ctx, R.color.primary_text_color))
                        getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(ctx, R.color.secondary_color))
                    }
                }
                .show()
        }
    }

    /**
     * Bildirimler kapalı olduğunda gösterilen dialog
     */
    private fun showNotificationDisabledDialog() {
        context?.let { ctx ->
            AlertDialog.Builder(ctx, R.style.MyDialogTheme)
                .setTitle("Notifications Off")
                .setMessage("Notifications are switched off for this application. To receive notifications, turn on notifications in settings.")
                .setPositiveButton("Go to Settings") { _, _ ->
                    permissionManager.openNotificationSettings(ctx)
                }
                .setNegativeButton("Cancel") { _, _ ->
                    Toast.makeText(context, "Notifications remain turned off", Toast.LENGTH_SHORT)
                        .show()
                }
                .setCancelable(false)
                .create()
                .apply {
                    setOnShowListener {
                        // Butonların metin rengini değiştir
                        getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(ctx, R.color.primary_text_color))
                        getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(ctx, R.color.secondary_color))
                    }
                }
                .show()
        }
    }

    private fun disableNotifications() {
        // Kullanıcı bildirimleri kapatmak istiyor
        // Android'de programatik olarak bildirimleri tamamen kapatamayız
        // Bu durumda kullanıcıyı ayarlara yönlendiriyoruz

        context?.let { ctx ->
            AlertDialog.Builder(ctx, R.style.MyDialogTheme)
                .setTitle("Close Notifications")
                .setMessage("To switch off notifications completely, you need to go to the phone settings. Would you like to go to the settings?")
                .setPositiveButton("Go to Settings") { _, _ ->
                    // Doğrudan ayarlara yönlendir
                    permissionManager.openNotificationSettings(ctx)
                    Toast.makeText(
                        context,
                        "You can switch off notifications in the settings",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .setNegativeButton("Cancel") { _, _ ->
                    // Switch'i geri aç
                    isUpdatingSwitch = true
                    binding.notificationSwitch.isChecked = true
                    updateSwitchAppearance(true)
                    isUpdatingSwitch = false
                }
                .setCancelable(false)
                .create()
                .apply {
                    setOnShowListener {
                        // Butonların metin rengini değiştir
                        getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(ctx, R.color.primary_text_color))
                        getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(ctx, R.color.secondary_color))
                    }
                }
                .show()
        }
    }

    private fun goBack(){
        val action=AppPreferencesFragmentDirections.actionAppPreferencesFragmentToHomeFragment()
        Navigation.findNavController(requireView()).navigate(action)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}