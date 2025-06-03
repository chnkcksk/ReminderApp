package com.chnkcksk.reminderapp.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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


        setupLiveDatas()
        setupButtons()
    }


    private fun setupLiveDatas() {

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