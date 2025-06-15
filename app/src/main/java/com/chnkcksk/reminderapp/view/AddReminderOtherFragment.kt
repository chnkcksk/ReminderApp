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
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.chnkcksk.reminderapp.R
import com.chnkcksk.reminderapp.databinding.FragmentAddReminderBinding
import com.chnkcksk.reminderapp.databinding.FragmentAddReminderOtherBinding
import com.chnkcksk.reminderapp.util.LoadingManager
import com.chnkcksk.reminderapp.viewmodel.AddReminderOtherViewModel
import com.chnkcksk.reminderapp.viewmodel.EditReminderViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class AddReminderOtherFragment : Fragment() {

    private var _binding: FragmentAddReminderOtherBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth

    private val loadingManager = LoadingManager.getInstance()

    private val viewModel: AddReminderOtherViewModel by viewModels()

    private lateinit var workspaceId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth

        arguments?.let {
            workspaceId = AddReminderOtherFragmentArgs.fromBundle(it).workspaceId
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAddReminderOtherBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        viewModel.getDatas(workspaceId)
        setupDateAndTimePicker()
        setupObserves()
        setupButtons()
        setupSpinner()
    }

    private fun setupObserves() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.uiEvent.collect { event ->


                when (event) {
                    is AddReminderOtherViewModel.UiEvent.ShowLoading -> loadingManager.showLoading(
                        requireContext()
                    )

                    is AddReminderOtherViewModel.UiEvent.HideLoading -> loadingManager.dismissLoading()
                    is AddReminderOtherViewModel.UiEvent.ShowToast -> Toast.makeText(
                        requireContext(),
                        event.message,
                        Toast.LENGTH_LONG
                    ).show()

                    is AddReminderOtherViewModel.UiEvent.WorkspaceInformations -> {
                        binding.workspaceNameOtherTV.text = event.workspaceName
                        binding.workspaceTypeOtherTV.text = event.workspaceType
                    }

                    is AddReminderOtherViewModel.UiEvent.NavigateWorkspace -> goBack()
                    is AddReminderOtherViewModel.UiEvent.ReminderAdded -> {
                        loadingManager.dismissLoading {
                            Toast.makeText(
                                requireContext(),
                                "Reminder added",
                                Toast.LENGTH_LONG
                            ).show()
                            goBack()
                        }
                    }
                }

            }
        }
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
        binding.addReminderDateOther.text = dateFormat.format(calendar.time)
        binding.addReminderTimeOther.text = timeFormat.format(calendar.time)

        // Tarih seçici
        binding.addReminderDateOther.setOnClickListener {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(
                binding.root.context,
                { _, selectedYear, selectedMonth, selectedDay ->
                    calendar.set(Calendar.YEAR, selectedYear)
                    calendar.set(Calendar.MONTH, selectedMonth)
                    calendar.set(Calendar.DAY_OF_MONTH, selectedDay)
                    binding.addReminderDateOther.text = dateFormat.format(calendar.time)
                },
                year, month, day
            )
            datePicker.show()
        }

        // Saat seçici
        binding.addReminderTimeOther.setOnClickListener {
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            val timePicker = TimePickerDialog(
                binding.root.context,
                { _, selectedHour, selectedMinute ->
                    calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
                    calendar.set(Calendar.MINUTE, selectedMinute)
                    binding.addReminderTimeOther.text = timeFormat.format(calendar.time)
                },
                hour, minute, true // 24 saat formatı
            )
            timePicker.show()
        }
    }


    private fun setupButtons() {

        binding.addReminderOtherButton.setOnClickListener {

            val title = binding.addTitleOtherET.text.toString()
            val description = binding.addDescriptionOtherET.text.toString()
            val priority = binding.prioritySpinnerOther.selectedItem.toString()
            val selectedDate = binding.addReminderDateOther.text.toString()
            val selectedTime = binding.addReminderTimeOther.text.toString()

            if (title.isEmpty() || description.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in the blanks!", Toast.LENGTH_LONG)
                    .show()
            } else {


                viewModel.addOtherReminder(
                    workspaceId,
                    title,
                    description,
                    priority,
                    selectedDate,
                    selectedTime
                )


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

    private fun setupSpinner() {
        //listeyi tanimla
        val priorities = resources.getStringArray(R.array.priorities)
        val adapter =
            ArrayAdapter(requireContext(), R.layout.custom_spinner_item, priorities)
        adapter.setDropDownViewResource(R.layout.custom_spinner_item)
        binding.prioritySpinnerOther.adapter = adapter

    }

    private fun goBack() {
        val action =
            AddReminderOtherFragmentDirections.actionAddReminderOtherFragmentToOtherWorkspaceFragment(
                workspaceId
            )
        Navigation.findNavController(requireView()).navigate(action)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

}