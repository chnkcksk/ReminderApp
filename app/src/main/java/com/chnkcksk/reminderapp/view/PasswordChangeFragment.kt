package com.chnkcksk.reminderapp.view

import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.viewModels
import androidx.navigation.Navigation
import com.chnkcksk.reminderapp.R
import com.chnkcksk.reminderapp.databinding.FragmentLoginBinding
import com.chnkcksk.reminderapp.databinding.FragmentPasswordChangeBinding
import com.chnkcksk.reminderapp.util.LoadingManager
import com.chnkcksk.reminderapp.viewmodel.LoginViewModel
import com.chnkcksk.reminderapp.viewmodel.PasswordChangeViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase


class PasswordChangeFragment : Fragment() {

    private var _binding: FragmentPasswordChangeBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth

    private val loadingManager = LoadingManager.getInstance()

    private val viewModel: PasswordChangeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPasswordChangeBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
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
            if (isLoading) {
                loadingManager.showLoading(requireContext())
            } else {
                loadingManager.dismissLoading()
            }
        }
        viewModel.navigateHome.observe(viewLifecycleOwner) { isNavigate ->
            if (isNavigate) {
                goBack()
            }

        }

        viewModel.clearFields.observe(viewLifecycleOwner) { clearFields ->
            if (clearFields == true) {
                binding.apply {
                    oldPasswET.text.clear()
                    newPasswET.text.clear()
                    newPasswAgainET.text.clear()
                }
            }
        }

    }

    private fun setupButtons() {

        binding.apply {
            oldPasswordVisibilityToggle.setOnClickListener {
                viewModel.togglePasswordVisibility(oldPasswET, oldPasswordVisibilityToggle)
            }

            oldPasswordVisibilityToggle2.setOnClickListener {
                viewModel.togglePasswordVisibility(newPasswET, oldPasswordVisibilityToggle2)
            }

            oldPasswordVisibilityToggle3.setOnClickListener {
                viewModel.togglePasswordVisibility(newPasswAgainET, oldPasswordVisibilityToggle3)
            }

            changePasswButton.setOnClickListener {

                val oldPassw = oldPasswET.text.toString()
                val newPassw = newPasswET.text.toString()
                val newPasswAgain = newPasswAgainET.text.toString()

                viewModel.reAuthenticateAndChangePassword(oldPassw, newPassw, newPasswAgain)
            }

            backButton.setOnClickListener {
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


    }

    private fun goBack() {
        val action = PasswordChangeFragmentDirections.actionPasswordChangeFragmentToHomeFragment()
        Navigation.findNavController(requireView()).navigate(action)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }


}