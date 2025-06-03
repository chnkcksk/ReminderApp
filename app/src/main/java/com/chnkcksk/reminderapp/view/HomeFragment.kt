package com.chnkcksk.reminderapp.view

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chnkcksk.reminderapp.MainNavGraphDirections
import com.chnkcksk.reminderapp.R
import com.chnkcksk.reminderapp.adapter.DrawerMenuAdapter
import com.chnkcksk.reminderapp.databinding.FragmentHomeBinding
import com.chnkcksk.reminderapp.databinding.NavDrawerContentBinding
import com.chnkcksk.reminderapp.databinding.NavDrawerHeaderBinding
import com.chnkcksk.reminderapp.model.DrawerMenuItem
import com.chnkcksk.reminderapp.util.LoadingManager
import com.chnkcksk.reminderapp.viewmodel.HomeViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var drawerMenuAdapter: DrawerMenuAdapter
    private lateinit var drawerToggle: ActionBarDrawerToggle

    private val loadingManager = LoadingManager.getInstance()
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupDrawerMenu()
        setupButtons()
        checkSession()
    }

    private fun setupToolbar() {
        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.title = "Home"
        //(requireActivity() as AppCompatActivity).supportActionBar?.setDisplayShowTitleEnabled(false)




        drawerToggle = ActionBarDrawerToggle(
            requireActivity(),
            binding.drawerLayout,
            binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )

        binding.drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()
    }

    private fun setupDrawerMenu() {
        // Header setup
        val headerBinding = NavDrawerHeaderBinding.bind(binding.navHeader.root)
        headerBinding.userNameText.text = auth.currentUser?.displayName ?: "Kullanıcı"
        //headerBinding.profileImage.setImageResource()

        // Content setup
        val contentBinding = NavDrawerContentBinding.bind(binding.navContent.root)

        contentBinding.personalWorkspaceButton.setOnClickListener {

            binding.drawerLayout.closeDrawers()
        }
        contentBinding.addWorkspaceButton.setOnClickListener {

        }
        contentBinding.notificationSettingsButton.setOnClickListener {

        }
        contentBinding.appPreferencesButton.setOnClickListener {

        }
        contentBinding.passwordChangeButton.setOnClickListener {

        }


        // RecyclerView setup
        val menuItems = listOf(
            DrawerMenuItem(1, "Hatırlatıcılarım", R.drawable.baseline_group_24),
            DrawerMenuItem(2, "Kategoriler", R.drawable.baseline_group_24),
            DrawerMenuItem(3, "İstatistikler", R.drawable.baseline_group_24),
            DrawerMenuItem(4, "Yardım", R.drawable.baseline_group_24)
        )

        drawerMenuAdapter = DrawerMenuAdapter(menuItems) { item ->
            when (item.id) {
                1 -> Toast.makeText(context, "Hatırlatıcılarım", Toast.LENGTH_SHORT).show()
                2 -> Toast.makeText(context, "Kategoriler", Toast.LENGTH_SHORT).show()
                3 -> Toast.makeText(context, "İstatistikler", Toast.LENGTH_SHORT).show()
                4 -> Toast.makeText(context, "Yardım", Toast.LENGTH_SHORT).show()
            }
            binding.drawerLayout.closeDrawers()
        }

        contentBinding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = drawerMenuAdapter
        }

        // Logout button click listener
        contentBinding.btnLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Are You Sure?")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes") { _, _ ->
                    val action = MainNavGraphDirections.actionHomeToLogin()
                    Navigation.findNavController(requireView()).navigate(action)
                    signOut()
                }
                .setNegativeButton("No") { _, _ ->

                }.show()
        }
    }

    fun checkSession() {
        val user = auth.currentUser
        if (user != null) {
            if (user.isEmailVerified) {
                Toast.makeText(requireContext(), "Kullanici girdi", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(requireContext(), "Kullanici girdi ama", Toast.LENGTH_LONG).show()

            }
        } else {
            Toast.makeText(requireContext(), "Kullanici yok", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupButtons() {

        binding.homeAddFAB.setOnClickListener {
            val action = HomeFragmentDirections.actionHomeFragmentToAddReminderFragment()
            Navigation.findNavController(requireView()).navigate(action)
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                private var backPressCount = 0
                private val backPressThreshold = 2000 // 2 saniye içinde iki kez basılması gerekiyor

                override fun handleOnBackPressed() {
                    if (backPressCount == 0) {
                        // İlk basış
                        backPressCount++
                        Toast.makeText(requireContext(), "Press again to exit", Toast.LENGTH_SHORT)
                            .show()

                        // Belirli bir süre içinde ikinci basış olmazsa sayacı sıfırla
                        Handler(Looper.getMainLooper()).postDelayed({
                            backPressCount = 0
                        }, backPressThreshold.toLong())
                    } else {
                        // İkinci basış - istediğiniz işlemi yapın
                        backPressCount = 0
                        // Örnek: Uygulamadan çık
                        requireActivity().finish()

                        // Veya fragment'ı kapat/geri git
                        // isEnabled = false
                        // requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                }
            })
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
        _binding = null
    }


}