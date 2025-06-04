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
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.navigation.Navigation
import com.chnkcksk.reminderapp.R
import com.chnkcksk.reminderapp.databinding.FragmentEditReminderBinding
import com.chnkcksk.reminderapp.databinding.FragmentHomeBinding
import com.chnkcksk.reminderapp.util.LoadingManager
import com.chnkcksk.reminderapp.viewmodel.EditReminderViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class EditReminderFragment : Fragment() {

    private var _binding: FragmentEditReminderBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private var workspaceId: String? = null
    private var reminderId: String? = null

    private val loadingManager = LoadingManager.getInstance()

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

        setupSpinner()
        setupDateAndTimePicker()
        setupLiveDatas()
        setupButtons()
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
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, priorities)
        adapter.setDropDownViewResource(R.layout.custom_spinner_item)
        binding.prioritySpinner.adapter = adapter

    }

    private fun setupLiveDatas() {

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        viewModel.title.observe(viewLifecycleOwner) { title ->
            binding.editTitleET.setText(title)
        }
        viewModel.description.observe(viewLifecycleOwner) { desc ->
            binding.editDescriptionET.setText(desc)
        }
        viewModel.isloading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading == true) {
                loadingManager.showLoading(requireContext())
            } else {
                loadingManager.dismissLoading()
            }
        }
        viewModel.toastMessage.observe(viewLifecycleOwner) { message ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
        viewModel.navigateHome.observe(viewLifecycleOwner) { navigate ->
            if (navigate == true) {
                goBack()
            }
        }
        viewModel.priority.observe(viewLifecycleOwner) {
            val selectedIndex = when (it) {
                "None" -> 0
                "Low" -> 1
                "Medium" -> 2
                "High" -> 3
                else -> 0 // default
            }

            binding.prioritySpinner.setSelection(selectedIndex)
        }
        viewModel.selectedDate.observe(viewLifecycleOwner) { selectedDateString ->
            try {
                val date = dateFormat.parse(selectedDateString) // String -> Date
                val calendar = Calendar.getInstance().apply {
                    time = date!!
                }

                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH)
                val day = calendar.get(Calendar.DAY_OF_MONTH)

                // TextView'a yaz
                binding.editReminderDate.text = selectedDateString

//                // İstersen burada DatePickerDialog açabilirsin:
//                DatePickerDialog(requireContext(), { _, y, m, d ->
//                    // kullanıcı yeni tarih seçtiğinde
//                }, year, month, day).show()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        viewModel.selectedTime.observe(viewLifecycleOwner) { selectedTimeString ->
            try {
                var time = timeFormat.parse(selectedTimeString)
                val calendar = Calendar.getInstance().apply {
                    time = time!!
                }

                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                val minute = calendar.get(Calendar.MINUTE)

                // TextView'a yaz
                binding.editReminderTime.text = selectedTimeString

//                // İstersen burada TimePickerDialog açabilirsin:
//                TimePickerDialog(requireContext(), { _, h, m ->
//                    // kullanıcı yeni saat seçtiğinde
//                }, hour, minute, true).show()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }



    }

    private fun setupButtons() {

        binding.apply {
            backButton.setOnClickListener {
                goBack()
            }

            editReminderButton.setOnClickListener {
                viewModel.editReminderData(workspaceId, reminderId)
            }

            deleteReminderButton.setOnClickListener {

                AlertDialog.Builder(requireContext())
                    .setTitle("Are You Sure?")
                    .setMessage("Are you sure you want to delete the reminder?")
                    .setPositiveButton("Yes") { _, _ ->
                        viewModel.deleteReminder(workspaceId, reminderId)
                    }.setNegativeButton("No", null)
                    .show()


            }

        }

    }


    private fun goBack() {
        val action = EditReminderFragmentDirections.actionEditReminderFragmentToHomeFragment()
        Navigation.findNavController(requireView()).navigate(action)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}