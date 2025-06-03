package com.chnkcksk.reminderapp.view

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
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

        setupLiveDatas()
        setupButtons()
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

        viewModel.navigateHome.observe(viewLifecycleOwner){ navigate ->
            if (navigate){
                goBack()
            }
        }

    }

    private fun setupButtons() {

        binding.addReminderButton.setOnClickListener {

            val title = binding.addTitleET.text.toString()
            val description = binding.addDescriptionET.text.toString()

            if (title.isEmpty() || description.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in the blanks!", Toast.LENGTH_LONG)
                    .show()
            }else{
                viewModel.addReminder(title, description)
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

    private fun goBack(){
        val action =
            AddReminderFragmentDirections.actionAddReminderFragmentToHomeFragment()
        Navigation.findNavController(requireView()).navigate(action)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

}