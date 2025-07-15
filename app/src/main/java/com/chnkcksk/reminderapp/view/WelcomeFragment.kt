package com.chnkcksk.reminderapp.view

import android.app.AlertDialog
import android.content.Context
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
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.chnkcksk.reminderapp.MainNavGraphDirections
import com.chnkcksk.reminderapp.R
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

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P){

            Glide.with(this)
                .load(R.drawable.welcome_image)
                .override(300,300)
                .diskCacheStrategy(DiskCacheStrategy.ALL) // Disk önbelleğini etkinleştir
                .format(DecodeFormat.PREFER_ARGB_8888) // Yüksek kalite formatı
                .into(binding.imageView)

        }else{
            binding.imageView.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.welcome_image))
        }



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