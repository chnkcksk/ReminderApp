package com.chnkcksk.reminderapp.view

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.chnkcksk.reminderapp.MainNavGraphDirections
import com.chnkcksk.reminderapp.R
import com.chnkcksk.reminderapp.databinding.FragmentAppPreferencesBinding
import com.chnkcksk.reminderapp.permissions.NotificationPermissionManager
import com.chnkcksk.reminderapp.util.LoadingManager
import com.chnkcksk.reminderapp.util.NetworkHelper
import com.chnkcksk.reminderapp.viewmodel.HomeViewModel.UiEvent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class AppPreferencesFragment : Fragment() {

    private var _binding: FragmentAppPreferencesBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private val loadingManager = LoadingManager.getInstance()

    private lateinit var permissionManager: NotificationPermissionManager

    // Switch değişikliğini programatik olarak kontrol etmek için
    private var isUpdatingSwitch = false

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleReauthLauncher: ActivityResultLauncher<Intent>


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

        if (!NetworkHelper.isInternetAvailable(requireContext())) {
            NetworkHelper.showNoInternetDialog(requireContext(), requireView(), requireActivity())
        }

        setupButtons()
        updateSwitchState()
        reAuthLauncher()
    }

    private fun reAuthLauncher(){
        googleReauthLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    val idToken = account.idToken

                    val credential = GoogleAuthProvider.getCredential(idToken, null)
                    val currentUser = Firebase.auth.currentUser

                    if (currentUser != null) {
                        loadingManager.showLoading(requireContext())
                        currentUser.reauthenticate(credential)
                            .addOnSuccessListener {
                                lifecycleScope.launch {
                                    try {
                                        loadingManager.showLoading(requireContext())
                                        deleteUserDataAndOwnedWorkspaces(currentUser.uid)
                                        currentUser.delete().await()
                                        loadingManager.dismissLoading()
                                        Toast.makeText(requireContext(), "Account deleted!", Toast.LENGTH_LONG).show()
                                        Firebase.auth.signOut()
                                        goWelcome()
                                    } catch (e: Exception) {
                                        loadingManager.dismissLoading()
                                        Toast.makeText(requireContext(), "Delete failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                    }
                                }

                            }
                            .addOnFailureListener {
                                loadingManager.dismissLoading()
                                Toast.makeText(requireContext(), "Reauthentication failed!", Toast.LENGTH_LONG).show()
                            }
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Google Sign-in failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
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

        binding.deleteAccountButton.setOnClickListener {
            showDeleteDialog()
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

    private fun showDeleteDialog(){
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.alert_dialog_account_delete, null)

        val builder = AlertDialog.Builder(requireContext())
        builder.setView(dialogView)
        val alertDialog = builder.create()

// View'lara erişim
        val customET = dialogView.findViewById<EditText>(R.id.customET)
        val customB = dialogView.findViewById<AppCompatButton>(R.id.customB)
        val customB2 = dialogView.findViewById<AppCompatButton>(R.id.customB2)

        customET.isVisible = false
        customB.isEnabled = false

        customB.text = "3"



        Handler(Looper.getMainLooper()).postDelayed({
            customB.text = "2"
        }, 1000)

        Handler(Looper.getMainLooper()).postDelayed({
            customB.text = "1"
        }, 2000)

        Handler(Looper.getMainLooper()).postDelayed({
            customB.text = "Delete"
            customB.isEnabled = true
        }, 3000)



        val currentUser = auth.currentUser

        if (currentUser==null){
            Toast.makeText(requireContext(), "Error!", Toast.LENGTH_LONG).show()
            return
        }

        var isGoogleUser = false

        currentUser.providerData.forEach { profile ->
            if (profile.providerId == "google.com") {
                isGoogleUser = true
            }
        }

        customET.isVisible = !isGoogleUser

        customB2.setOnClickListener {
            alertDialog.dismiss()
        }

        customB.setOnClickListener {
            val currentUser = Firebase.auth.currentUser ?: return@setOnClickListener

            if (!isGoogleUser) {
                val passw = customET.text.toString()
                if (passw.isEmpty()) {
                    Toast.makeText(requireContext(), "Please fill password!", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                val email = currentUser.email
                if (email == null) {
                    Toast.makeText(requireContext(), "Error!", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                loadingManager.showLoading(requireContext())
                val credential = EmailAuthProvider.getCredential(email, passw)

                currentUser.reauthenticate(credential).addOnCompleteListener { reauthTask ->
                    if (reauthTask.isSuccessful) {
                        lifecycleScope.launch {
                            try {
                                loadingManager.showLoading(requireContext())
                                deleteUserDataAndOwnedWorkspaces(currentUser.uid)
                                currentUser.delete().await()
                                loadingManager.dismissLoading()
                                Toast.makeText(requireContext(), "Account deleted!", Toast.LENGTH_LONG).show()
                                Firebase.auth.signOut()
                                goWelcome()
                            } catch (e: Exception) {
                                loadingManager.dismissLoading()
                                Toast.makeText(requireContext(), "Delete failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }


                    } else {
                        loadingManager.dismissLoading()
                        Toast.makeText(requireContext(), "Reauthentication failed!", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()

                googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
                val signInIntent = googleSignInClient.signInIntent
                googleReauthLauncher.launch(signInIntent)
            }

            alertDialog.dismiss()
        }


        alertDialog.show()

    }

    suspend fun deleteUserDataAndOwnedWorkspaces(uid: String) {
        val firestore = Firebase.firestore

        try {
            // 1. reminders alt koleksiyonundaki belgeleri sil
            val remindersRef = firestore
                .collection("Users")
                .document(uid)
                .collection("workspaces")
                .document("personalWorkspace")
                .collection("reminders")

            val reminderDocs = remindersRef.get().await()
            for (doc in reminderDocs.documents) {
                remindersRef.document(doc.id).delete().await()
            }

            // 2. personalWorkspace dokümanını sil
            firestore.collection("Users")
                .document(uid)
                .collection("workspaces")
                .document("personalWorkspace")
                .delete()
                .await()

            // 3. Users/userId dokümanını sil
            firestore.collection("Users").document(uid).delete().await()

            // 4. workspaces koleksiyonundaki ownerId eşleşen belgeleri sil
            val ownedWorkspaces = firestore
                .collection("workspaces")
                .whereEqualTo("ownerId", uid)
                .get()
                .await()

            for (doc in ownedWorkspaces.documents) {
                firestore.collection("workspaces").document(doc.id).delete().await()
            }

        } catch (e: Exception) {
            // Eğer hata yönetmek istersen buraya log veya throw koyabilirsin
            Toast.makeText(requireContext(), "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            throw e
        }
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

    private fun goWelcome(){
        val action = MainNavGraphDirections.actionDeleteToWelcome()
        Navigation.findNavController(requireView()).navigate(action)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}