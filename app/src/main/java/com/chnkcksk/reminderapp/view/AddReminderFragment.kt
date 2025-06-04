package com.chnkcksk.reminderapp.view

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.fragment.app.viewModels
import androidx.navigation.Navigation
import com.chnkcksk.reminderapp.R
import com.chnkcksk.reminderapp.adapter.DrawerMenuAdapter
import com.chnkcksk.reminderapp.databinding.FragmentAddReminderBinding
import com.chnkcksk.reminderapp.databinding.FragmentHomeBinding
import com.chnkcksk.reminderapp.util.LoadingManager
import com.chnkcksk.reminderapp.viewmodel.AddReminderViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale


class AddReminderFragment : Fragment() {

    private var _binding: FragmentAddReminderBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth

    private val loadingManager = LoadingManager.getInstance()

    private val viewModel: AddReminderViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth
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

        setupDateAndTimePicker()
        setupLiveDatas()
        setupButtons()
        setupSpinner()
    }

    private fun setupLiveDatas() {
        viewModel.toastMessage.observe(viewLifecycleOwner) { message ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading == true) {
                loadingManager.showLoading(requireContext())
            } else {
                loadingManager.dismissLoading()
            }
        }

        viewModel.navigateHome.observe(viewLifecycleOwner) { navigate ->
            if (navigate) {
                goBack()
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
        binding.addReminderDate.text = dateFormat.format(calendar.time)
        binding.addReminderTime.text = timeFormat.format(calendar.time)

        // Tarih seçici
        binding.addReminderDate.setOnClickListener {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(
                binding.root.context,
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

        binding.addReminderButton.setOnClickListener {

            val title = binding.addTitleET.text.toString()
            val description = binding.addDescriptionET.text.toString()
            val priority = binding.prioritySpinner.selectedItem.toString()
            val selectedDate = binding.addReminderDate.text.toString()
            val selectedTime = binding.addReminderTime.text.toString()

            if (title.isEmpty() || description.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in the blanks!", Toast.LENGTH_LONG)
                    .show()
            } else {
                viewModel.addReminder(title, description, priority, selectedDate, selectedTime)
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
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, priorities)
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
        _binding = null
    }

}