package com.chnkcksk.reminderapp.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.Navigation
import com.chnkcksk.reminderapp.R
import com.chnkcksk.reminderapp.databinding.FragmentRegisterBinding
import com.chnkcksk.reminderapp.util.LoadingManager
import com.chnkcksk.reminderapp.viewmodel.RegisterViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase


class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RegisterViewModel by viewModels()

    private val loadingManager= LoadingManager.getInstance()

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
        setupLiveDatas()
    }

    fun setupLiveDatas() {

        viewModel.toastMessage.observe(viewLifecycleOwner) { message ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }

        viewModel.navigateVerify.observe(viewLifecycleOwner){ navigate ->
            if (navigate == true){
                val action = RegisterFragmentDirections.actionRegisterFragmentToEmailVerifyFragment()
                Navigation.findNavController(requireView()).navigate(action)
            }
        }

        viewModel.isloading.observe(viewLifecycleOwner){ isLoading ->

            if (isLoading==true){
                loadingManager.showLoading(requireContext())
            }else{
                loadingManager.dismissLoading()
            }

        }


    }

    private fun setupButtons() {
        binding.backButton.setOnClickListener {
            val action = RegisterFragmentDirections.actionRegisterFragmentToWelcomeFragment()
            Navigation.findNavController(requireView()).navigate(action)
        }

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