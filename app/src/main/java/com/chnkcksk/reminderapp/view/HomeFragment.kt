package com.chnkcksk.reminderapp.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
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
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chnkcksk.reminderapp.MainNavGraphDirections
import com.chnkcksk.reminderapp.R
import com.chnkcksk.reminderapp.adapter.DrawerMenuAdapter
import com.chnkcksk.reminderapp.adapter.ReminderAdapter
import com.chnkcksk.reminderapp.databinding.FragmentHomeBinding
import com.chnkcksk.reminderapp.databinding.NavDrawerContentBinding
import com.chnkcksk.reminderapp.databinding.NavDrawerHeaderBinding
import com.chnkcksk.reminderapp.model.DrawerMenuItem
import com.chnkcksk.reminderapp.model.Reminder
import com.chnkcksk.reminderapp.permissions.NotificationPermissionManager
import com.chnkcksk.reminderapp.util.LoadingManager
import com.chnkcksk.reminderapp.viewmodel.HomeViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Tasks
import com.google.android.material.snackbar.Snackbar
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
    private lateinit var userName: String

    private lateinit var drawerMenuAdapter: DrawerMenuAdapter
    private lateinit var drawerToggle: ActionBarDrawerToggle

    private val loadingManager = LoadingManager.getInstance()
    private val viewModel: HomeViewModel by viewModels()


    // Bildirim izni için NotificationPermissionManager
    private lateinit var permissionManager: NotificationPermissionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth

        userName = auth.currentUser?.displayName.toString()

        permissionManager = NotificationPermissionManager.getInstance()
            .registerPermissionLauncher(this)
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


        viewModel.getUserProviderData()

        requestNotificationPermission()
        setupReminders()
        setupLiveDatas()
        setupToolbar()
        setupDrawerMenu()
        setupButtons()
        checkSession()

    }


    private fun requestNotificationPermission() {
        context?.let { ctx ->
            permissionManager.checkNotificationPermission(
                ctx,
                object : NotificationPermissionManager.NotificationPermissionCallback {
                    override fun onPermissionGranted(notificationContent: NotificationPermissionManager.NotificationContent) {
//                        Toast.makeText(context, "Notifications opened successfully", Toast.LENGTH_SHORT).show()
                    }

                    override fun onPermissionDenied() {
                        // İzin verilmedi
                    }

                    override fun onNotificationsDisabled() {
                        // Bildirimler kapalı
                    }

                    override fun onSettingsOpened() {
                        // Ayarlar açıldı
                    }
                },
                /*
                // Özelleştirilmiş bildirim içeriği
                NotificationPermissionManager.NotificationContent(
                    title = "Reminder Notification",
                    message = "Example of notification from your reminder app",
                    channelId = "reminder_channel",
                    channelName = "Reminder Notifications",
                    channelDescription = "Reminder app notifications",
                    delaySeconds = 3
                )

                 */
            )
        }
    }

    private fun setupReminders() {

        lifecycleScope.launch {
            viewModel.loadRemindersList()
        }


        val adapter = ReminderAdapter(
            requireContext(),
            "personalWorkspace",
            "Editable",
            true,
            ArrayList()
        ) { reminder ->

            // Fragment burada kontrolü eline alıyor
            val workspaceId = "personalWorkspace" // Eğer bu sabitse
            val reminderId = reminder.id

            val action = HomeFragmentDirections.actionHomeFragmentToEditReminderFragment(
                workspaceId,
                reminderId
            )
            Navigation.findNavController(requireView()).navigate(action)

        }
        binding.homeRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.homeRecyclerView.adapter = adapter

        viewModel.reminderList.observe(viewLifecycleOwner) { reminderList ->
            adapter.updateList(reminderList)
        }

    }

    private fun setupLiveDatas() {
        viewModel.toastMessage.observe(viewLifecycleOwner) { message ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }

        viewModel.isloading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                loadingManager.showLoading(requireContext())
            } else {
                loadingManager.dismissLoading()
            }
        }


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


    // HomeFragment.kt - setupDrawerMenu() fonksiyonunda debugging ekleyin

    private fun setupDrawerMenu() {
        lifecycleScope.launch {
            viewModel.loadWorkspaces()
        }


        // Header setup
        val headerBinding = NavDrawerHeaderBinding.bind(binding.navHeader.root)
        headerBinding.userNameText.text = userName
        val initials = userName
            .split(" ")
            .mapNotNull { it.firstOrNull()?.toString()?.uppercase() }
            .joinToString("")
            .take(2)

        val avatarBitmap = createInitialsAvatar(
            initials = initials,
            size = 200,
            backgroundColor = Color.parseColor("#DFCEA0"),
            textColor = Color.WHITE
        )

        headerBinding.profileImage.setImageBitmap(avatarBitmap)

        // Content setup
        val contentBinding = NavDrawerContentBinding.bind(binding.navContent.root)

        contentBinding.personalWorkspaceButton.setOnClickListener {
            binding.drawerLayout.closeDrawers()
        }
        contentBinding.addWorkspaceButton.setOnClickListener {
            val action = HomeFragmentDirections.actionHomeFragmentToAddWorkspaceFragment()
            Navigation.findNavController(requireView()).navigate(action)
        }

        contentBinding.appPreferencesButton.setOnClickListener {
            val action = HomeFragmentDirections.actionHomeFragmentToAppPreferencesFragment()
            Navigation.findNavController(requireView()).navigate(action)
        }

        viewModel.isGoogleUser.observe(viewLifecycleOwner) { isGoogleUser ->
            if (isGoogleUser == true) {
                contentBinding.passwordChangeButton.isVisible = false
            }
        }

        contentBinding.passwordChangeButton.setOnClickListener {
            val action = HomeFragmentDirections.actionHomeFragmentToPasswordChangeFragment()
            Navigation.findNavController(requireView()).navigate(action)
        }

        // Workspace adapter'ını boş DrawerMenuItem listesi ile başlat
        drawerMenuAdapter = DrawerMenuAdapter(ArrayList<DrawerMenuItem>()) { item ->
            // Workspace item'a tıklandığında yapılacak işlemler
            Toast.makeText(
                requireContext(),
                "Selected workspace: ${item.title}",
                Toast.LENGTH_SHORT
            ).show()

            val action = HomeFragmentDirections.actionHomeFragmentToOtherWorkspaceFragment(item.id)
            Navigation.findNavController(requireView()).navigate(action)
            // Burada workspace'e göre veri yükleme işlemleri yapabilirsiniz
            // Örneğin: loadWorkspaceReminders(item.id)
            binding.drawerLayout.closeDrawers()
        }

        contentBinding.drawerRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = drawerMenuAdapter
        }

        // Workspace listesini observe et - DEBUG EKLEYIN
        viewModel.workspaceList.observe(viewLifecycleOwner) { workspaceList ->
            // DEBUG: Veri gelip gelmediğini kontrol edin
            android.util.Log.d("HomeFragment", "Workspace list size: ${workspaceList?.size}")
            workspaceList?.forEach { workspace ->
                android.util.Log.d(
                    "HomeFragment",
                    "Workspace: ${workspace.title} - Type: ${workspace.workspaceType}"
                )
            }

            if (workspaceList != null && workspaceList.isNotEmpty()) {
                drawerMenuAdapter.updateList(workspaceList)
                android.util.Log.d(
                    "HomeFragment",
                    "Adapter updated with ${workspaceList.size} items"
                )
            } else {
                android.util.Log.d("HomeFragment", "Workspace list is empty or null")
            }
        }

        // Logout button click listener
        contentBinding.btnLogout.setOnClickListener {
            AlertDialog.Builder(requireContext(),R.style.MyDialogTheme)
                .setTitle("Are You Sure?")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes") { _, _ ->
                    val action = MainNavGraphDirections.actionHomeToLogin()
                    Navigation.findNavController(requireView()).navigate(action)
                    signOut()
                }
                .setNegativeButton("No") { _, _ ->
                    // Do nothing
                }
                .setCancelable(false)
                .create()
                .apply {
                    setOnShowListener {
                        // Butonların metin rengini değiştir
                        getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(
                            ContextCompat.getColor(requireContext(), R.color.primary_text_color))
                        getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(
                            ContextCompat.getColor(requireContext(), R.color.secondary_color))
                    }
                }
                .show()
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

    fun createInitialsAvatar(
        initials: String,
        size: Int,
        backgroundColor: Int,
        textColor: Int
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Arka plan
        val paint = Paint().apply {
            color = backgroundColor
            isAntiAlias = true
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        // Yazı (baş harfler)
        paint.apply {
            color = textColor
            textSize = size / 2f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        val xPos = size / 2f
        val yPos = size / 2f - (paint.descent() + paint.ascent()) / 2
        canvas.drawText(initials, xPos, yPos, paint)

        return bitmap
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