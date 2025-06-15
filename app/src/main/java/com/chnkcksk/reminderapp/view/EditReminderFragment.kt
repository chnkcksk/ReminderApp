package com.chnkcksk.reminderapp.view

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.chnkcksk.reminderapp.R
import com.chnkcksk.reminderapp.databinding.FragmentEditReminderBinding
import com.chnkcksk.reminderapp.databinding.FragmentHomeBinding
import com.chnkcksk.reminderapp.permissions.NotificationPermissionManager
import com.chnkcksk.reminderapp.util.LoadingManager
import com.chnkcksk.reminderapp.viewmodel.EditReminderViewModel
import com.chnkcksk.reminderapp.worker.WorkerNotification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit


class EditReminderFragment : Fragment() {

    private var _binding: FragmentEditReminderBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private var workspaceId: String? = null
    private var reminderId: String? = null

    private val loadingManager = LoadingManager.getInstance()
    val permissionManager = NotificationPermissionManager.getInstance()

    private val viewModel: EditReminderViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth
        firestore = Firebase.firestore

        arguments?.let {
            workspaceId = EditReminderFragmentArgs.fromBundle(it).workspaceId
            reminderId = EditReminderFragmentArgs.fromBundle(it).reminderId
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentEditReminderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        viewModel.loadReminderData(workspaceId, reminderId)



        setupObserves()

        setupSpinner()
        setupDateAndTimePicker()
        setupButtons()
    }

    private fun setupObserves() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                updateUI(state)
            }
        }
    }

    private fun updateUI(state: EditReminderViewModel.UiState) {

        viewLifecycleOwner.lifecycleScope.launch {
            // Loading
            if (state.isLoading) {
                loadingManager.showLoading(requireContext())
            } else {
                loadingManager.dismissLoading()
            }

            if (state.reminderDeleted == true) {
                loadingManager.dismissLoading {
                    Toast.makeText(requireContext(), "Reminder deleted", Toast.LENGTH_LONG).show()
                    goBack()
                }

            }

            if (state.reminderNotDeleted == true) {
                loadingManager.dismissLoading {
                    Toast.makeText(
                        requireContext(),
                        "Reminder could not be deleted: ${state.error}",
                        Toast.LENGTH_LONG
                    ).show()
                }


            }

            if (state.reminderEdited == true) {
                loadingManager.dismissLoading {
                    Toast.makeText(requireContext(), "Reminder updated!", Toast.LENGTH_LONG).show()
                    goBack()
                }
            }

            if (state.reminderNotEdited == true) {
                loadingManager.dismissLoading {
                    Toast.makeText(
                        requireContext(),
                        "Reminder could not be updated! Please try again: ${state.error}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }


            state.toastMessage?.let { message ->
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                viewModel.clearToastMessage()
            }

            // Toast gösterildikten sonra navigasyonu yap
            if (state.navigateHome) {
                goBack()
                viewModel.clearNavigateHome()
            }

            // Notification
            if (state.setNotification) {
                requestNotification()
                viewModel.clearSetNotification() // Event'i temizle
            }


            // Form data
            binding.editTitleET.setText(state.title)
            binding.editDescriptionET.setText(state.description)
            binding.editReminderDate.text = state.selectedDate
            binding.editReminderTime.text = state.selectedTime
            binding.editReminderCheckBox.isChecked = state.reminderState

            // Priority spinner
            val selectedIndex = when (state.priority) {
                "None" -> 0
                "Low" -> 1
                "Medium" -> 2
                "High" -> 3
                else -> 0
            }
            binding.prioritySpinner.setSelection(selectedIndex)
        }


    }

    private fun checkPermission() {

        permissionManager.checkNotificationPermission(
            context = requireContext(),
            callback = object : NotificationPermissionManager.NotificationPermissionCallback {
                override fun onPermissionGranted(notificationContent: NotificationPermissionManager.NotificationContent) {
                    // İzin verildiğinde yapılacak işlemler
                    //Toast.makeText(context, "Notification permisson granted", Toast.LENGTH_LONG).show()
                    binding.editReminderCheckBox.isChecked = true
                }

                override fun onPermissionDenied() {
                    // İzin reddedildiğinde yapılacak işlemler

                    binding.editReminderCheckBox.isChecked = false
                    //Toast.makeText(context, "Notification permisson denied: Turn on notification permission in settings", Toast.LENGTH_LONG).show()
                }

                override fun onNotificationsDisabled() {
                    binding.editReminderCheckBox.isChecked = false
                    // Bildirimler sistem ayarlarından kapatıldığında yapılacak işlemler

                }
            }
        )
    }


    private fun setupDateAndTimePicker() {
        // Başlangıç olarak bugünün tarihi ve saat 09:00
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        // Başlangıçta TextView'lara varsayılan tarih ve saat ata
        // Tarih seçici
        binding.editReminderDate.setOnClickListener {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(
                binding.root.context,
                { _, selectedYear, selectedMonth, selectedDay ->
                    calendar.set(Calendar.YEAR, selectedYear)
                    calendar.set(Calendar.MONTH, selectedMonth)
                    calendar.set(Calendar.DAY_OF_MONTH, selectedDay)
                    binding.editReminderDate.text = dateFormat.format(calendar.time)
                },
                year, month, day
            )
            datePicker.show()
        }

        // Saat seçici
        binding.editReminderTime.setOnClickListener {
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            val timePicker = TimePickerDialog(
                binding.root.context,
                { _, selectedHour, selectedMinute ->
                    calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
                    calendar.set(Calendar.MINUTE, selectedMinute)
                    binding.editReminderTime.text = timeFormat.format(calendar.time)
                },
                hour, minute, true // 24 saat formatı
            )
            timePicker.show()
        }
    }

    private fun setupSpinner() {
        //listeyi tanimla
        val priorities = resources.getStringArray(R.array.priorities)
        val adapter =
            ArrayAdapter(requireContext(), R.layout.custom_spinner_item, priorities)
        adapter.setDropDownViewResource(R.layout.custom_spinner_item)
        binding.prioritySpinner.adapter = adapter

    }


    private fun calculateDelayInSeconds(): Long {
        val dateStr = binding.editReminderDate.text.toString()
        val timeStr = binding.editReminderTime.text.toString()

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
            title = "${binding.editTitleET.text}",
            message = "${binding.editDescriptionET.text}\n${binding.editReminderTime.text},${binding.editReminderDate.text}",
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
            "Bildirim ${binding.editReminderDate.text} tarihinde ${binding.editReminderTime.text} saatinde ayarlandi!",
            Toast.LENGTH_SHORT
        ).show()

    }

    private fun cancelEditAndGoBack() {
        /*
        AlertDialog.Builder(requireContext(), R.style.MyDialogTheme)
            .setTitle("Are you sure?")
            .setMessage("Are you sure you want to cancel the edit and leave?")
            .setPositiveButton("Yes") { _, _ ->
                goBack()
            }
            .setNegativeButton("No", null)
            .setCancelable(false)
            .create()
            .apply {
                setOnShowListener {
                    // Butonların metin rengini değiştir
                    getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.primary_text_color
                        )
                    )
                    getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.secondary_color
                        )
                    )
                }
            }
            .show()

         */

        showAlertDialog(
            "Are you sure?",
            "Are you sure you want to cancel the edit and leave?",
            { goBack() }
        )

    }

    private fun setupButtons() {

        binding.apply {
            backButton.setOnClickListener {
                cancelEditAndGoBack()
            }

            editReminderCheckBox.setOnClickListener {
                checkPermission()
            }

            editReminderButton.setOnClickListener {
                val title = binding.editTitleET.text.toString()
                val desc = binding.editDescriptionET.text.toString()
                val priority = binding.prioritySpinner.selectedItem.toString()
                val date = binding.editReminderDate.text.toString()
                val time = binding.editReminderTime.text.toString()
                val reminderState = binding.editReminderCheckBox.isChecked

                val hasPermission =
                    permissionManager.isNotificationPermissionGranted(requireContext())

                if (hasPermission == false && reminderState == true) {
                    binding.editReminderCheckBox.isChecked = false
                    Toast.makeText(
                        requireContext(),
                        "Notification permission not granted. Reminder notification will not be generated.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@setOnClickListener
                }


                // Validate date and time
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val selectedDateTime = dateFormat.parse("$date $time")
                val currentDateTime = Calendar.getInstance().time

                if (selectedDateTime != null && selectedDateTime.before(currentDateTime) && reminderState == true) {
                    Toast.makeText(
                        requireContext(),
                        "Please select a future date and time if you want to set a reminder!",
                        Toast.LENGTH_LONG
                    ).show()
                    return@setOnClickListener
                }

                if (title.isEmpty() || desc.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        "Please fill in the blanks!",
                        Toast.LENGTH_LONG
                    )
                        .show()
                    return@setOnClickListener
                }


                viewModel.editReminderData(
                    workspaceId,
                    reminderId,
                    title,
                    desc,
                    priority,
                    date,
                    time,
                    reminderState
                )


            }

            deleteReminderButton.setOnClickListener {

                showAlertDialog(
                    "Are You Sure?",
                    "Are you sure you want to delete the reminder?",
                    {
                        viewModel.deleteReminder(workspaceId, reminderId)
                    }
                )

                /*
                AlertDialog.Builder(requireContext(), R.style.MyDialogTheme)
                    .setTitle("Are You Sure?")
                    .setMessage("Are you sure you want to delete the reminder?")
                    .setPositiveButton("Yes") { _, _ ->

                        viewModel.deleteReminder(workspaceId, reminderId)


                    }.setNegativeButton("No", null)
                    .setCancelable(false)
                    .create()
                    .apply {
                        setOnShowListener {
                            // Butonların metin rengini değiştir
                            getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.primary_text_color
                                )
                            )
                            getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.secondary_color
                                )
                            )
                        }
                    }
                    .show()

                 */


            }

            requireActivity().onBackPressedDispatcher.addCallback(
                viewLifecycleOwner,
                object : OnBackPressedCallback(true) {


                    override fun handleOnBackPressed() {
                        cancelEditAndGoBack()

                    }
                })

        }

    }

    fun showAlertDialog(
        title: String,
        message: String,
        positiveAction: () -> Unit
    ) {
        AlertDialog.Builder(requireContext(), R.style.MyDialogTheme)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Yes") { _, _ -> positiveAction() }
            .setNegativeButton("No", null)
            .setCancelable(false)
            .create()
            .apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(
                        ContextCompat.getColor(context, R.color.primary_text_color)
                    )
                    getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(
                        ContextCompat.getColor(context, R.color.secondary_color)
                    )
                }
            }
            .show()
    }

    private fun goBack() {
        val action = EditReminderFragmentDirections.actionEditReminderFragmentToHomeFragment()
        Navigation.findNavController(requireView()).navigate(action)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
        loadingManager.onDestroy()
    }
}