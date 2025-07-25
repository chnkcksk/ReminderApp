package com.chnkcksk.reminderapp.view

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.method.PasswordTransformationMethod
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.chnkcksk.reminderapp.MainNavGraphDirections
import com.chnkcksk.reminderapp.R
import com.chnkcksk.reminderapp.databinding.FragmentLoginBinding
import com.chnkcksk.reminderapp.util.LoadingManager
import com.chnkcksk.reminderapp.viewmodel.LoginViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels()
    private lateinit var auth: FirebaseAuth

    private val loadingManager = LoadingManager.getInstance()

    // Google Sign-In Activity Result Launcher
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("LoginFragment", "Google Sign-In result received: ${result.resultCode}")
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d("LoginFragment", "Google Sign-In successful, processing result")
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            viewModel.handleGoogleSignInResult(task)
        } else {
            // Google Sign-In iptal edildi veya başarısız oldu
            Log.e("LoginFragment", "Google Sign-In failed or cancelled")
            showToast("Google Sign-In was cancelled or failed")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Google Sign-In'i başlat
        viewModel.initializeGoogleSignIn(requireActivity())

        setupObserves()
        setupButtons()
    }

    private fun setupObserves() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.uiEvent.collect { event ->

                when (event) {

                    is LoginViewModel.UiEvent.ShowLoading -> loadingManager.showLoading(
                        requireContext()
                    )

                    is LoginViewModel.UiEvent.HideLoading -> loadingManager.dismissLoading()
                    is LoginViewModel.UiEvent.ShowToast -> showToast(event.message)
                    is LoginViewModel.UiEvent.NavigateHome -> {
                        val action = MainNavGraphDirections.actionLoginToHome()
                        Navigation.findNavController(requireView()).navigate(action)
                    }

                    is LoginViewModel.UiEvent.NavigateVerify -> {
                        val action =
                            LoginFragmentDirections.actionLoginFragmentToEmailVerifyFragment()
                        Navigation.findNavController(requireView()).navigate(action)
                    }

                    is LoginViewModel.UiEvent.VerifyEmail -> {
                        loadingManager.dismissLoading {
                            showToast("Please verify your email before logging in.")

                            val action =
                                LoginFragmentDirections.actionLoginFragmentToEmailVerifyFragment()
                            Navigation.findNavController(requireView()).navigate(action)
                        }
                    }

                    is LoginViewModel.UiEvent.GoogleUser -> {
                        showToast(event.message)
                        val action = MainNavGraphDirections.actionLoginToHome()
                        Navigation.findNavController(requireView()).navigate(action)
                    }


                }

            }
        }


    }


    private fun setupButtons() {
        // Eski şifre görünürlük kontrolü
        binding.oldPasswordVisibilityToggle.setOnClickListener {
            viewModel.togglePasswordVisibility(
                binding.loginPasswordET,
                binding.oldPasswordVisibilityToggle
            )
        }

        binding.loginButton.setOnClickListener {
            val email = binding.loginEmailET.text.toString()
            val password = binding.loginPasswordET.text.toString()


            viewModel.login(email, password)


        }

        binding.backButton.setOnClickListener {
            val action = LoginFragmentDirections.actionLoginFragmentToWelcomeFragment()
            Navigation.findNavController(requireView()).navigate(action)
        }

        binding.forgotPasswTV.setOnClickListener {
            val action = LoginFragmentDirections.actionLoginFragmentToPasswordResetFragment()
            Navigation.findNavController(requireView()).navigate(action)
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {


                override fun handleOnBackPressed() {
                    val action = LoginFragmentDirections.actionLoginFragmentToWelcomeFragment()
                    Navigation.findNavController(requireView()).navigate(action)

                }
            })


        binding.continueGoogleButton.setOnClickListener {
            Log.d("LoginFragment", "Google Sign-In button clicked")
            // Google Sign-In intent'ini başlat
            val signInIntent = viewModel.getGoogleSignInIntent()
            if (signInIntent != null) {
                Log.d("LoginFragment", "Launching Google Sign-In intent")
                googleSignInLauncher.launch(signInIntent)
            } else {
                Log.e("LoginFragment", "Google Sign-In intent is null")
                showToast("Google Sign-In is not available")
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }


    private fun goBack() {
        val action = LoginFragmentDirections.actionLoginFragmentToWelcomeFragment()
        Navigation.findNavController(requireView()).navigate(action)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}