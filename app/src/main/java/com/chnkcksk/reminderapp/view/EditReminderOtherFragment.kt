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
import com.chnkcksk.reminderapp.viewmodel.EditReminderOtherViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
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

    private fun setupLiveDatas() {

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        viewModel.title.observe(viewLifecycleOwner) { title ->
            binding.editTitleOET.setText(title)
        }
        viewModel.description.observe(viewLifecycleOwner) { desc ->
            binding.editDescriptionOET.setText(desc)
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

            binding.priorityOSpinner.setSelection(selectedIndex)
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
                binding.editReminderODate.text = selectedDateString

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
                binding.editReminderOTime.text = selectedTimeString

//                // İstersen burada TimePickerDialog açabilirsin:
//                TimePickerDialog(requireContext(), { _, h, m ->
//                    // kullanıcı yeni saat seçtiğinde
//                }, hour, minute, true).show()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        viewModel.workspaceName.observe(viewLifecycleOwner) { workspaceName ->
            binding.workspaceNameOET.text = workspaceName
        }
        viewModel.workspaceType.observe(viewLifecycleOwner) { workspaceType ->
            binding.workspaceTypeOTV.text = workspaceType
        }


    }

    private fun cancelEditAndGoBack(){
        AlertDialog.Builder(requireContext(), R.style.MyDialogTheme)
            .setTitle("Are you sure?")
            .setMessage("Are you sure you want to cancel the edit and leave?")
            .setPositiveButton("Yes"){_,_ ->
                goBack()
            }
            .setNegativeButton("No",null)
            .setCancelable(false)
            .create()
            .apply {
                setOnShowListener {
                    // Butonların metin rengini değiştir
                    getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_text_color))
                    getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(requireContext(), R.color.secondary_color))
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
                            getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_text_color))
                            getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(requireContext(), R.color.secondary_color))
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