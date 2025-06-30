package com.chnkcksk.reminderapp.view

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.navigation.Navigation
import com.chnkcksk.reminderapp.MainNavGraphDirections
import com.chnkcksk.reminderapp.databinding.FragmentWelcomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings


class WelcomeFragment : Fragment() {

    private var _binding: FragmentWelcomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth:FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentWelcomeBinding.inflate(inflater,container,false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        checkSession()





        binding.loginButton.setOnClickListener {
            val action = WelcomeFragmentDirections.actionWelcomeFragmentToLoginFragment()
            Navigation.findNavController(requireView()).navigate(action)
        }

        binding.signupButton.setOnClickListener {
            val action = WelcomeFragmentDirections.actionWelcomeFragmentToRegisterFragment()
            Navigation.findNavController(view).navigate(action)
        }




    }

    fun checkSession() {
        val user = auth.currentUser
        if (user != null) {
            if (user.isEmailVerified) {
                val action = MainNavGraphDirections.actionWelcomeToHome()
                Navigation.findNavController(requireView()).navigate(action)
            } else {
                val action = WelcomeFragmentDirections.actionWelcomeFragmentToEmailVerifyFragment()
                Navigation.findNavController(requireView()).navigate(action)

            }
        } else {

        }
    }








    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

}