package com.chnkcksk.reminderapp.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.chnkcksk.reminderapp.R
import com.chnkcksk.reminderapp.databinding.FragmentRegisterBinding
import com.chnkcksk.reminderapp.util.LoadingManager
import com.chnkcksk.reminderapp.util.SuccessDialog
import com.chnkcksk.reminderapp.viewmodel.RegisterViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch


class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RegisterViewModel by viewModels()

    private val loadingManager = LoadingManager.getInstance()
    private val successDialog = SuccessDialog()

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        setupButtons()
        setupObserves()
    }

    private fun setupObserves() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.uiEvent.collect { event ->
                when (event) {

                    is RegisterViewModel.UiEvent.ShowLoading -> loadingManager.showLoading(requireContext())
                    is RegisterViewModel.UiEvent.HideLoading -> loadingManager.dismissLoading()
                    is RegisterViewModel.UiEvent.ShowToast -> Toast.makeText(requireContext(), event.message, Toast.LENGTH_LONG).show()
                    is RegisterViewModel.UiEvent.AccountCreated ->{
                        loadingManager.dismissLoading {
                            successDialog.showSuccessDialog(requireContext()){
                                val action =
                                    RegisterFragmentDirections.actionRegisterFragmentToEmailVerifyFragment()
                                Navigation.findNavController(requireView()).navigate(action)
                            }
                        }
                    }

                }
            }
        }
    }



    private fun setupButtons() {
        binding.backButton.setOnClickListener {
            val action = RegisterFragmentDirections.actionRegisterFragmentToWelcomeFragment()
            Navigation.findNavController(requireView()).navigate(action)
        }

        binding.passwordVisibilityToggle.setOnClickListener {
            viewModel.togglePasswordVisibility(
                binding.registerPasswordET,
                binding.passwordVisibilityToggle
            )
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {


                override fun handleOnBackPressed() {
                    val action =
                        RegisterFragmentDirections.actionRegisterFragmentToWelcomeFragment()
                    Navigation.findNavController(requireView()).navigate(action)

                }
            })

        binding.registerButton.setOnClickListener {
            val name = binding.registerNameET.text.toString()
            val email = binding.registerEmailET.text.toString()
            val password = binding.registerPasswordET.text.toString()


            viewModel.register(name, email, password)


        }
    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

}