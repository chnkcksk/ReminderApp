package com.chnkcksk.reminderapp.view

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.method.PasswordTransformationMethod
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


class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel : LoginViewModel by viewModels()
    private lateinit var auth: FirebaseAuth

    private val loadingManager = LoadingManager.getInstance()

    // Google Sign-In Activity Result Launcher
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            viewModel.handleGoogleSignInResult(task)
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
        _binding = FragmentLoginBinding.inflate(inflater,container,false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Google Sign-In'i başlat
        viewModel.initializeGoogleSignIn(requireActivity())

        setupLiveDatas()
        setupButtons()
    }

    private fun setupLiveDatas(){
        viewModel.toastMessage.observe(viewLifecycleOwner){message ->
            Toast.makeText(requireContext(), message,Toast.LENGTH_LONG).show()
        }
        viewModel.navigateToHome.observe(viewLifecycleOwner){ shouldNavigate ->
            if (shouldNavigate) {
                val action = MainNavGraphDirections.actionLoginToHome()
                Navigation.findNavController(requireView()).navigate(action)
            }
        }
        viewModel.navigateVerify.observe(viewLifecycleOwner){ shouldNavigate ->
            if (shouldNavigate) {
                val action = LoginFragmentDirections.actionLoginFragmentToEmailVerifyFragment()
                Navigation.findNavController(requireView()).navigate(action)
            }
        }
        
        // Loading state'ini gözlemle
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                loadingManager.showLoading(requireContext(), "Signing in...")
            } else {
                loadingManager.dismissLoading()
            }
        }
    }

    private fun setupButtons() {
        // Eski şifre görünürlük kontrolü
        binding.oldPasswordVisibilityToggle.setOnClickListener {
            viewModel.togglePasswordVisibility(binding.loginPasswordET, binding.oldPasswordVisibilityToggle)
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



        binding.continueGoogleButton.setOnClickListener {
            // Google Sign-In intent'ini başlat
            val signInIntent = viewModel.getGoogleSignInIntent()
            signInIntent?.let {
                googleSignInLauncher.launch(it)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}