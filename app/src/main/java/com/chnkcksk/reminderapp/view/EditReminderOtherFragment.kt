package com.chnkcksk.reminderapp.view

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.chnkcksk.reminderapp.R
import com.chnkcksk.reminderapp.databinding.FragmentEditReminderBinding
import com.chnkcksk.reminderapp.databinding.FragmentEditReminderOtherBinding
import com.chnkcksk.reminderapp.util.LoadingManager
import com.chnkcksk.reminderapp.viewmodel.AddReminderViewModel
import com.chnkcksk.reminderapp.viewmodel.EditReminderOtherViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class EditReminderOtherFragment : Fragment() {

    private var _binding: FragmentEditReminderOtherBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private var workspaceId: String? = null
    private var reminderId: String? = null

    private val loadingManager = LoadingManager.getInstance()

    private val viewModel: EditReminderOtherViewModel by viewModels()

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth
        firestore = Firebase.firestore

        arguments?.let {
            workspaceId = EditReminderOtherFragmentArgs.fromBundle(it).workspaceId
            reminderId = EditReminderOtherFragmentArgs.fromBundle(it).reminderId
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentEditReminderOtherBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)




        viewModel.loadReminderData(workspaceId, reminderId)
        viewModel.loadWorkspaceData(workspaceId)


        setupObserves()
        setupSpinner()
        setupDateAndTimePicker()
        setupButtons()
    }

    private fun setupDateAndTimePicker() {
        // Başlangıç olarak bugünün tarihi ve saat 09:00
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        // Başlangıçta TextView'lara varsayılan tarih ve saat ata
        // Tarih seçici
        binding.editReminderODate.setOnClickListener {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(
                binding.root.context,
                { _, selectedYear, selectedMonth, selectedDay ->
                    calendar.set(Calendar.YEAR, selectedYear)
                    calendar.set(Calendar.MONTH, selectedMonth)
                    calendar.set(Calendar.DAY_OF_MONTH, selectedDay)
                    binding.editReminderODate.text = dateFormat.format(calendar.time)
                },
                year, month, day
            )
            datePicker.show()
        }

        // Saat seçici
        binding.editReminderOTime.setOnClickListener {
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            val timePicker = TimePickerDialog(
                binding.root.context,
                { _, selectedHour, selectedMinute ->
                    calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
                    calendar.set(Calendar.MINUTE, selectedMinute)
                    binding.editReminderOTime.text = timeFormat.format(calendar.time)
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
        binding.priorityOSpinner.adapter = adapter

    }

    private fun setupObserves() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.uiEvent.collect { event ->

                when (event) {

                    is EditReminderOtherViewModel.UiEvent.ShowLoading ->
                        loadingManager.showLoading(requireContext())

                    is EditReminderOtherViewModel.UiEvent.HideLoading ->
                        loadingManager.dismissLoading()

                    is EditReminderOtherViewModel.UiEvent.ShowToast ->
                        Toast.makeText(requireContext(), event.message, Toast.LENGTH_LONG).show()

                    is EditReminderOtherViewModel.UiEvent.NavigateHome ->
                        goBack()

                    is EditReminderOtherViewModel.UiEvent.ReminderDeleted -> {
                        loadingManager.dismissLoading {
                            Toast.makeText(
                                requireContext(),
                                "Reminder deleted successfully",
                                Toast.LENGTH_LONG
                            ).show()
                            goBack()
                        }
                    }

                    is EditReminderOtherViewModel.UiEvent.ReminderUpdated -> {
                        loadingManager.dismissLoading {
                            Toast.makeText(
                                requireContext(),
                                "Reminder updated successfully",
                                Toast.LENGTH_LONG
                            ).show()
                            goBack()
                        }
                    }

                    is EditReminderOtherViewModel.UiEvent.ReminderInformations -> {
                        //title
                        binding.editTitleOET.setText(event.title)
                        //description
                        binding.editDescriptionOET.setText(event.description)
                        //priority
                        val selectedIndex = when (event.priority) {
                            "None" -> 0
                            "Low" -> 1
                            "Medium" -> 2
                            "High" -> 3
                            else -> 0 // default
                        }
                        binding.priorityOSpinner.setSelection(selectedIndex)
                        //selected date
                        try {
                            val date = dateFormat.parse(event.selectedDate) // String -> Date
                            val calendar = Calendar.getInstance().apply {
                                time = date!!
                            }

                            val year = calendar.get(Calendar.YEAR)
                            val month = calendar.get(Calendar.MONTH)
                            val day = calendar.get(Calendar.DAY_OF_MONTH)

                            // TextView'a yaz
                            binding.editReminderODate.text = event.selectedDate

                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        //selected time
                        try {
                            var time = timeFormat.parse(event.selectedTime)
                            val calendar = Calendar.getInstance().apply {
                                time = time!!
                            }
                            val hour = calendar.get(Calendar.HOUR_OF_DAY)
                            val minute = calendar.get(Calendar.MINUTE)
                            // TextView'a yaz
                            binding.editReminderOTime.text = event.selectedTime
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    is EditReminderOtherViewModel.UiEvent.WorkspaceInformations -> {
                        binding.workspaceNameOET.text = event.workspaceName
                        binding.workspaceTypeOTV.text = event.workspaceType
                    }
                }

            }
        }
    }



    private fun cancelEditAndGoBack() {
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
    }

    private fun setupButtons() {

        binding.apply {
            backButton.setOnClickListener {
                cancelEditAndGoBack()
            }

            editReminderOButton.setOnClickListener {
                val title = binding.editTitleOET.text.toString()
                val desc = binding.editDescriptionOET.text.toString()
                val priority = binding.priorityOSpinner.selectedItem.toString()
                val date = binding.editReminderODate.text.toString()
                val time = binding.editReminderOTime.text.toString()


                viewModel.editReminderData(
                    workspaceId,
                    reminderId,
                    title,
                    desc,
                    priority,
                    date,
                    time
                )


            }

            requireActivity().onBackPressedDispatcher.addCallback(
                viewLifecycleOwner,
                object : OnBackPressedCallback(true) {


                    override fun handleOnBackPressed() {
                        cancelEditAndGoBack()

                    }
                })

            deleteReminderOButton.setOnClickListener {

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


            }

        }

    }


    private fun goBack() {
        val action =
            EditReminderOtherFragmentDirections.actionEditReminderOtherFragmentToOtherWorkspaceFragment(
                workspaceId!!
            )
        Navigation.findNavController(requireView()).navigate(action)
    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }


}