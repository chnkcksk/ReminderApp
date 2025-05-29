package com.chnkcksk.reminderapp.view

import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import androidx.fragment.app.viewModels
import androidx.navigation.Navigation
import com.chnkcksk.reminderapp.R
import com.chnkcksk.reminderapp.databinding.FragmentLoginBinding
import com.chnkcksk.reminderapp.viewmodel.LoginViewModel


class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel : LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        binding.backButton.setOnClickListener {
            val action = LoginFragmentDirections.actionLoginFragmentToWelcomeFragment()
            Navigation.findNavController(view).navigate(action)
        }


        setupButtons()
    }

    private fun setupButtons() {
        // Eski şifre görünürlük kontrolü
        binding.oldPasswordVisibilityToggle.setOnClickListener {
            viewModel.togglePasswordVisibility(binding.loginPasswordET, binding.oldPasswordVisibilityToggle)
        }


    }



    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

}