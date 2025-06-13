package com.chnkcksk.reminderapp.view

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.navigation.Navigation
import com.chnkcksk.reminderapp.R
import com.chnkcksk.reminderapp.databinding.FragmentPasswordResetBinding
import com.chnkcksk.reminderapp.databinding.FragmentRegisterBinding
import com.chnkcksk.reminderapp.util.LoadingManager
import com.chnkcksk.reminderapp.util.SuccessDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase


class PasswordResetFragment : Fragment() {

    private var _binding: FragmentPasswordResetBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth

    private val loadingManager = LoadingManager.getInstance()
    private val successDialog = SuccessDialog()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPasswordResetBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        setupButtons()
    }

    private fun setupButtons() {
        binding.backButton.setOnClickListener {
            val action =
                PasswordResetFragmentDirections.actionPasswordResetFragmentToLoginFragment()
            Navigation.findNavController(requireView()).navigate(action)
        }

        binding.sendResetLinkButton.setOnClickListener {
            sendResetLink()
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {


                override fun handleOnBackPressed() {
                    val action =
                        PasswordResetFragmentDirections.actionPasswordResetFragmentToLoginFragment()
                    Navigation.findNavController(requireView()).navigate(action)

                }
            })
    }

    @SuppressLint("NewApi")
    private fun sendResetLink() {

        val email = binding.resetPasswET.text.toString()

        if (email!=null){
            loadingManager.showLoading(requireContext())

            // Cihaz dilini al
            val locale = requireContext().resources.configuration.locales[0]
            // Firebase dil ayarını güncelle
            auth.setLanguageCode(locale.language)

            Firebase.auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful){
                        loadingManager.dismissLoading()
                        Toast.makeText(
                            requireContext(),
                            "Password reset link has been sent to $email address",
                            Toast.LENGTH_LONG
                        ).show()
                        successDialog.showSuccessDialog(requireContext())
                        val action = PasswordResetFragmentDirections.actionPasswordResetFragmentToLoginFragment()
                        Navigation.findNavController(requireView()).navigate(action)
                    }else{
                        loadingManager.dismissLoading()
                        Toast.makeText(
                            requireContext(),
                            "Password reset failed: ${task.exception?.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        } else{
            Toast.makeText(requireContext(), "Please try again", Toast.LENGTH_LONG).show()
        }



    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }


}