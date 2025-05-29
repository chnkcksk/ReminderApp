package com.chnkcksk.reminderapp.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.chnkcksk.reminderapp.MainNavGraphDirections
import com.chnkcksk.reminderapp.R
import com.chnkcksk.reminderapp.databinding.FragmentEmailVerifyBinding
import com.chnkcksk.reminderapp.util.LoadingManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class EmailVerifyFragment : Fragment() {

    private var _binding: FragmentEmailVerifyBinding? = null
    private val binding get() = _binding!!

    private val loadingManager = LoadingManager.getInstance()

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentEmailVerifyBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupText()
        setupButtons()


    }

    private fun setupText() {
        val user = auth.currentUser
        val email = user?.email

        if (email != null) {
            binding.emailTV.text = email
        } else {
            binding.emailTV.text = ""
        }
    }

    private fun setupButtons() {
        binding.backButton.setOnClickListener {
            signOut()
            val action = EmailVerifyFragmentDirections.actionEmailVerifyFragmentToLoginFragment()
            Navigation.findNavController(requireView()).navigate(action)

        }

        binding.resendEmailButton.setOnClickListener {
            resendEmail()
        }

        binding.checkVerifyButton.setOnClickListener {
            checkVerify()
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {

                    val action =
                        EmailVerifyFragmentDirections.actionEmailVerifyFragmentToLoginFragment()
                    Navigation.findNavController(requireView()).navigate(action)
                }
            })

    }

    private fun checkVerify() {
        val user = auth.currentUser
        val email = user?.email

        loadingManager.showLoading(requireContext())

        user?.reload()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val refreshedUser = auth.currentUser
                if (refreshedUser != null && refreshedUser.isEmailVerified) {
                    Toast.makeText(
                        requireContext(),
                        "Email verified! You can login now.",
                        Toast.LENGTH_LONG
                    ).show()

                    // Kullanıcı bilgisini Firestore'da güncelle
                    refreshedUser.uid.let { uid ->
                        Firebase.firestore.collection("Users").document(uid)
                            .update("emailVerified", true)
                            .addOnSuccessListener {
                                loadingManager.dismissLoading()
                                // Giriş sayfasına yönlendir
                                signOut()
                                val action =
                                    EmailVerifyFragmentDirections.actionEmailVerifyFragmentToLoginFragment()
                                Navigation.findNavController(requireView()).navigate(action)
                            }
                    }
                } else{
                    loadingManager.dismissLoading()
                    Toast.makeText(
                        requireContext(),
                        "Email not verified yet. Please check your inbox and spam folder.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

    }


    private fun resendEmail() {

        val user = auth.currentUser
        val email = user?.email

        loadingManager.showLoading(requireContext())

        user?.sendEmailVerification()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                loadingManager.dismissLoading()
                Toast.makeText(
                    requireContext(),
                    "Verification email resent to $email",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                loadingManager.dismissLoading()
                Toast.makeText(
                    requireContext(),
                    "Failed to send verification email: ${task.exception?.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }

        }
    }

    private fun signOut() {
        lifecycleScope.launch {
            try {
                // Firebase sign out
                auth.signOut()

                // Google Sign-In sign out
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(requireContext().getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()

                val googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

                // Google Sign-In çıkışını suspend fonksiyon olarak çağır
                withContext(Dispatchers.IO) {
                    Tasks.await(googleSignInClient.signOut())
                }

                // Başarılı çıkış sonrası welcome ekranına yönlendir
                withContext(Dispatchers.Main) {
                    //Navigasyon
                }
            } catch (e: Exception) {
                // Hata durumunda kullanıcıya bilgi ver
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Çıkış yapılırken bir hata oluştu: ${e.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        auth.signOut()
        _binding = null
    }


}