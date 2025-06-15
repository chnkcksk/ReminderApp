package com.chnkcksk.reminderapp.view

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.chnkcksk.reminderapp.R
import com.chnkcksk.reminderapp.adapter.DrawerMenuAdapter
import com.chnkcksk.reminderapp.databinding.FragmentAddReminderBinding
import com.chnkcksk.reminderapp.databinding.FragmentHomeBinding
import com.chnkcksk.reminderapp.permissions.NotificationPermissionManager
import com.chnkcksk.reminderapp.util.LoadingManager
import com.chnkcksk.reminderapp.viewmodel.AddReminderViewModel
import com.chnkcksk.reminderapp.worker.WorkerNotification
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit


class AddReminderFragment : Fragment() {

    private var _binding: FragmentAddReminderBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth

    private val loadingManager = LoadingManager.getInstance()

    private val viewModel: AddReminderViewModel by viewModels()

    val permissionManager = NotificationPermissionManager.getInstance()

    private var checkCount = 0

    // SharedPreferences anahtarı
    private val PREF_NAME = "ReminderAppPrefs"
    private val KEY_GOING_TO_SETTINGS = "isGoingToSettings"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth

        permissionManager.registerPermissionLauncher(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAddReminderBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObserves()

        setupDateAndTimePicker()
        setupButtons()
        setupSpinner()
    }

    private fun setupObserves() {

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.uiEvent.collect{ event ->

                when(event){
                    is AddReminderViewModel.UiEvent.ShowLoading ->loadingManager.showLoading(requireContext())
                    is AddReminderViewModel.UiEvent.HideLoading -> loadingManager.dismissLoading()
                    is AddReminderViewModel.UiEvent.ShowToast ->
                        Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                    is AddReminderViewModel.UiEvent.NavigateHome -> goBack()
                    is AddReminderViewModel.UiEvent.SetNotification -> requestNotification()
                    is AddReminderViewModel.UiEvent.ReminderAdded -> {
                        loadingManager.dismissLoading {
                            Toast.makeText(requireContext(), "Reminder saved", Toast.LENGTH_LONG).show()
                            //Buraya delay eklemek istiyorum
                            goBack()
                        }
                    }

                }

            }
        }

    }



    override fun onResume() {
        super.onResume()

        // SharedPreferences'tan bayrağın durumunu oku
        val prefs = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val isComingBackFromSettings = prefs.getBoolean(KEY_GOING_TO_SETTINGS, false)

        if (isComingBackFromSettings) {
            // Bayrak true ise, yani ayarlar ekranından dönüldüyse
            val hasPermission = permissionManager.isNotificationPermissionGranted(requireContext())
            Snackbar.make(
                requireView(),
                "Permission status after settings: ${hasPermission}",
                Snackbar.LENGTH_LONG
            ).show()

            // Checkbox'ın durumunu güncelle
            binding.reminderCheckBox.isChecked = hasPermission

            if (hasPermission == false) {
                binding.reminderCheckBox.isEnabled = false
            }

            // Bayrağı sıfırla, böylece bir sonraki onResume'da tekrar çalışmaz
            prefs.edit().putBoolean(KEY_GOING_TO_SETTINGS, false).apply()
        }
    }

    private fun checkPermission() {

        permissionManager.checkNotificationPermission(
            context = requireContext(),
            callback = object : NotificationPermissionManager.NotificationPermissionCallback {
                override fun onPermissionGranted(notificationContent: NotificationPermissionManager.NotificationContent) {
                    // İzin verildiğinde yapılacak işlemler
                    //Toast.makeText(context, "Notification permisson granted", Toast.LENGTH_LONG).show()

                }

                override fun onPermissionDenied() {
                    // İzin reddedildiğinde yapılacak işlemler

                    if (checkCount > 1) {
                        showPermissionDeniedDialog()
                    }

                    binding.reminderCheckBox.isChecked = false
                    //Toast.makeText(context, "Notification permisson denied: Turn on notification permission in settings", Toast.LENGTH_LONG).show()
                }

                override fun onNotificationsDisabled() {
                    binding.reminderCheckBox.isChecked = false
                    // Bildirimler sistem ayarlarından kapatıldığında yapılacak işlemler

                }
            }
        )
    }

    private fun showPermissionDeniedDialog() {
        context?.let { ctx ->
            AlertDialog.Builder(ctx, R.style.MyDialogTheme)  // Özel temayı burada belirtiyoruz
                .setTitle("Notification Permission Required")
                .setMessage("You need to grant notification permission for the app to send notifications. Would you like to go to the settings?")
                .setPositiveButton("Settings") { _, _ ->
                    permissionManager.openAppSettings(ctx)
                    val prefs = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                    prefs.edit().putBoolean(KEY_GOING_TO_SETTINGS, true).apply()
                }
                .setNegativeButton("Cancel") { _, _ ->
                    Toast.makeText(
                        context,
                        "Notification permission could not be granted",
                        Toast.LENGTH_SHORT
                    ).show()
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

    private fun calculateDelayInSeconds(): Long {
        val dateStr = binding.addReminderDate.text.toString()
        val timeStr = binding.addReminderTime.text.toString()

        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val selectedDateTime = dateFormat.parse("$dateStr $timeStr")

        val currentTime = Calendar.getInstance().time

        return if (selectedDateTime != null && selectedDateTime.after(currentTime)) {
            (selectedDateTime.time - currentTime.time) / 1000
        } else {
            Toast.makeText(
                requireContext(),
                "Lütfen gecerli bir tarih ve saat seçin",
                Toast.LENGTH_SHORT
            ).show()
            -1
        }
    }

    private fun requestNotification() {
        val delaySeconds = calculateDelayInSeconds()
        if (delaySeconds < 0) return

        val notificationContent = NotificationPermissionManager.NotificationContent(
            title = "${binding.addTitleET.text}",
            message = "${binding.addDescriptionET.text}\n${binding.addReminderTime.text},${binding.addReminderDate.text}",
            channelId = "main_channel",
            channelName = "Home Page Notifications",
            channelDescription = "Home page custom notifications",
            iconResId = android.R.drawable.ic_dialog_info,
            autoCancel = true,
            priority = NotificationCompat.PRIORITY_HIGH,
            vibrate = true,
            sound = true,
            delaySeconds = delaySeconds.toInt()
        )

        permissionManager.checkNotificationPermission(
            context = requireContext(),
            callback = object : NotificationPermissionManager.NotificationPermissionCallback {
                override fun onPermissionGranted(notificationContent: NotificationPermissionManager.NotificationContent) {
                    startNotificationWorker(notificationContent)
                }

                override fun onPermissionDenied() {
                    Toast.makeText(
                        requireContext(),
                        "Ana sayfa bildirimi gönderilemedi",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onNotificationsDisabled() {
                    TODO("Not yet implemented")
                }
            },
            notificationContent = notificationContent
        )

    }

    private fun startNotificationWorker(content: NotificationPermissionManager.NotificationContent) {
        val inputData = androidx.work.Data.Builder()
            .putString("title", content.title)
            .putString("message", content.message)
            .putString("channelId", content.channelId)
            .putString("channelName", content.channelName)
            .putString("channelDescription", content.channelDescription)
            .putInt("iconResId", content.iconResId)
            .putBoolean("autoCancel", content.autoCancel)
            .putInt("priority", content.priority)
            .putBoolean("vibrate", content.vibrate)
            .putBoolean("sound", content.sound)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<WorkerNotification>()
            .setInitialDelay(content.delaySeconds.toLong(), TimeUnit.SECONDS)
            .setInputData(inputData)
            .build()

        // WorkManager'a kalıcı olarak kaydet
        WorkManager.getInstance(requireContext())
            .enqueueUniqueWork(
                "notification_${System.currentTimeMillis()}",
                androidx.work.ExistingWorkPolicy.REPLACE,
                workRequest
            )

        Toast.makeText(
            requireContext(),
            "Bildirim ${binding.addReminderDate.text} tarihinde ${binding.addReminderTime.text} saatinde ayarlandi!",
            Toast.LENGTH_SHORT
        ).show()

    }

    private fun setupDateAndTimePicker() {
        // Başlangıç olarak bugünün tarihi ve saat 09:00
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
        }

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        // Başlangıçta TextView'lara varsayılan tarih ve saat ata
        binding.addReminderDate.text = dateFormat.format(calendar.time)
        binding.addReminderTime.text = timeFormat.format(calendar.time)

        // Tarih seçici
        binding.addReminderDate.setOnClickListener {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(
                binding.root.context,
                R.style.MyDialogTheme,
                { _, selectedYear, selectedMonth, selectedDay ->
                    calendar.set(Calendar.YEAR, selectedYear)
                    calendar.set(Calendar.MONTH, selectedMonth)
                    calendar.set(Calendar.DAY_OF_MONTH, selectedDay)
                    binding.addReminderDate.text = dateFormat.format(calendar.time)
                },
                year, month, day
            )
            datePicker.show()
        }

        // Saat seçici
        binding.addReminderTime.setOnClickListener {
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            val timePicker = TimePickerDialog(
                binding.root.context,
                R.style.MyDialogTheme,
                { _, selectedHour, selectedMinute ->
                    calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
                    calendar.set(Calendar.MINUTE, selectedMinute)
                    binding.addReminderTime.text = timeFormat.format(calendar.time)
                },
                hour, minute, true // 24 saat formatı
            )
            timePicker.show()
        }
    }

    private fun setupButtons() {

        binding.reminderCheckBox.setOnClickListener {
            checkCount += 1
            checkPermission()
        }

        binding.addReminderButton.setOnClickListener {


            val title = binding.addTitleET.text.toString()
            val description = binding.addDescriptionET.text.toString()
            val priority = binding.prioritySpinner.selectedItem.toString()
            val selectedDate = binding.addReminderDate.text.toString()
            val selectedTime = binding.addReminderTime.text.toString()
            val isNotificationChecked = binding.reminderCheckBox.isChecked


            val hasPermission = permissionManager.isNotificationPermissionGranted(requireContext())

            if (hasPermission == false && isNotificationChecked == true) {
                binding.reminderCheckBox.isChecked = false
                Toast.makeText(
                    requireContext(),
                    "Notification permission not granted. Reminder notification will not be generated.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }


            // Validate date and time
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val selectedDateTime = dateFormat.parse("$selectedDate $selectedTime")
            val currentDateTime = Calendar.getInstance().time

            if (selectedDateTime != null && selectedDateTime.before(currentDateTime) && isNotificationChecked == true) {
                Toast.makeText(
                    requireContext(),
                    "Please select a future date and time if you want to set a reminder!",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            if (title.isEmpty() || description.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in the blanks!", Toast.LENGTH_LONG)
                    .show()
            } else {



                    viewModel.addReminder(
                        title,
                        description,
                        priority,
                        selectedDate,
                        selectedTime,
                        isNotificationChecked
                    )



            }

        }
        binding.addTitleET
        binding.backButton.setOnClickListener {
            goBack()
        }
        binding.addDescriptionET

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {


                override fun handleOnBackPressed() {
                    goBack()

                }
            })

    }

    private fun setupSpinner() {
        //listeyi tanimla
        val priorities = resources.getStringArray(R.array.priorities)
        val adapter =
            ArrayAdapter(requireContext(), R.layout.custom_spinner_item, priorities)
        adapter.setDropDownViewResource(R.layout.custom_spinner_item)
        binding.prioritySpinner.adapter = adapter

    }

    private fun goBack() {
        val action =
            AddReminderFragmentDirections.actionAddReminderFragmentToHomeFragment()
        Navigation.findNavController(requireView()).navigate(action)
    }

    override fun onDestroy() {
        super.onDestroy()
        val prefs = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_GOING_TO_SETTINGS, false).apply()
        _binding = null
    }

}