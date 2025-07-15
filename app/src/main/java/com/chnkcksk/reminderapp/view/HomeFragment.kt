package com.chnkcksk.reminderapp.view

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
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
import androidx.recyclerview.widget.ItemTouchHelper
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
import com.chnkcksk.reminderapp.util.AuthManager
import com.chnkcksk.reminderapp.util.LoadingManager
import com.chnkcksk.reminderapp.util.NetworkHelper
import com.chnkcksk.reminderapp.viewmodel.HomeViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Tasks
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
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

    private var reminderList = ArrayList<Reminder>()
    private var workspaceList = ArrayList<DrawerMenuItem>()

    private lateinit var reminderAdapter: ReminderAdapter

    // Bildirim izni için NotificationPermissionManager
    private lateinit var permissionManager: NotificationPermissionManager

    private lateinit var sharedPref: SharedPreferences


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth

        userName = auth.currentUser?.displayName.toString()

        permissionManager = NotificationPermissionManager.getInstance()
            .registerPermissionLauncher(this)

        sharedPref = requireContext().getSharedPreferences("MyPrefs", MODE_PRIVATE)
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

        if (!NetworkHelper.isInternetAvailable(requireContext())) {
            NetworkHelper.showNoInternetDialog(requireContext(), requireView(), requireActivity())
        }else{
            if (auth.currentUser == null){
                signOut()
            }
        }

        viewModel.getUserProviderData()

        checkUpdate()



        setupObserves()
        requestNotificationPermission()
        setupToolbar()
        setupButtons()
        setupReminders()
        setupDrawerMenu()

    }

    private fun checkUpdate(){
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600) // 1 saatte bir yenile
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(mapOf("min_required_version" to 1))

        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->

                if (!isAdded || context == null) return@addOnCompleteListener

                if (task.isSuccessful) {
                    val minVersion = remoteConfig.getLong("min_required_version")

                    val currentVersion = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        requireContext().packageManager
                            .getPackageInfo(requireContext().packageName, 0).longVersionCode
                    } else {
                        @Suppress("DEPRECATION")
                        requireContext().packageManager
                            .getPackageInfo(requireContext().packageName, 0).versionCode.toLong()
                    }

                    if (currentVersion < minVersion) {
                        showUpdateDialog()
                        return@addOnCompleteListener
                    }
                }

            }
    }

    private fun showUpdateDialog() {
        if (!isAdded || context == null) return // Fragment hâlâ aktif mi?

        val packageName = requireContext().packageName

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Güncelleme Gerekli")
            .setMessage("Uygulamanın yeni bir sürümü mevcut. Devam etmek için lütfen güncelleyin.")
            .setCancelable(false)
            .setPositiveButton("Güncelle") { _, _ ->
                val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))

                try {
                    startActivity(marketIntent)
                } catch (e: Exception) {
                    startActivity(webIntent)
                }

                activity?.finish() // Güvenli versiyon
            }
            .show()
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


    private fun setupObserves() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.uiEvent.collect { event ->
                when (event) {
                    is HomeViewModel.UiEvent.ShowLoading -> loadingManager.showLoading(
                        requireContext()
                    )

                    is HomeViewModel.UiEvent.HideLoading -> loadingManager.dismissLoading()
                    is HomeViewModel.UiEvent.ShowToast -> Toast.makeText(
                        requireContext(),
                        event.message,
                        Toast.LENGTH_LONG
                    ).show()


                    is HomeViewModel.UiEvent.ReminderList -> {
                        reminderList.clear()
                        reminderList.addAll(event.reminderList)

                        // Kaydedilen sıralama tercihini al → listeyi sırala
                        val savedPosition = sharedPref.getInt("sortBy", 0)
                        sortReminderList(savedPosition)
                    }

                    is HomeViewModel.UiEvent.WorkspaceList -> {
                        workspaceList.clear()
                        workspaceList.addAll(event.workspaceList)
                        drawerMenuAdapter.updateList(workspaceList)
                    }
                }
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

    private fun setupReminders() {


        viewModel.loadRemindersList()


        reminderAdapter = ReminderAdapter(
            context = requireContext(),
            workspaceId = "personalWorkspace",
            isReadOnly = "Editable",
            owner = true,
            homeReminderList = ArrayList(),

            onItemClick = { reminder ->

                // Fragment burada kontrolü eline alıyor
                val workspaceId = "personalWorkspace" // Eğer bu sabitse
                val reminderId = reminder.id

                val action = HomeFragmentDirections.actionHomeFragmentToEditReminderFragment(
                    workspaceId,
                    reminderId
                )
                Navigation.findNavController(requireView()).navigate(action)

            },

            onItemDelete = { reminder, position ->
                // İsteğe bağlı: Silme işlemi için ek callback
                // Bu callback opsiyonel, adapter kendi Firebase silme işlemini hallediyor
            }

        )

        binding.homeRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.homeRecyclerView.adapter = reminderAdapter

        // ItemTouchHelper'ı oluştur ve RecyclerView'e bağla
        val itemTouchHelper = ItemTouchHelper(reminderAdapter.getSwipeCallback())
        itemTouchHelper.attachToRecyclerView(binding.homeRecyclerView)

        setupSpinner()

    }

    private fun setupSpinner() {
        val sortTypes = resources.getStringArray(R.array.sort_values)

        val adapter = ArrayAdapter(requireContext(), R.layout.custom_home_spinner, sortTypes)
        adapter.setDropDownViewResource(R.layout.custom_home_spinner)
        binding.sortSpinner.adapter = adapter


        val editor = sharedPref.edit()

        // SharedPreferences'tan kaydedilen değeri oku → spinner pozisyonu ayarla
        val savedPosition = sharedPref.getInt("sortBy", 0)
        binding.sortSpinner.setSelection(savedPosition)

        // İlk açıldığında listeyi kaydedilen sıraya göre sırala
        sortReminderList(savedPosition)


        binding.sortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                editor.putInt("sortBy", position).apply()
                sortReminderList(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}


        }

    }

    private fun sortReminderList(position: Int) {
        val sortedList = when (position) {
            0 -> reminderList.sortedByDescending { it.timestamp.toLongOrNull() ?: 0L } //Date
            1 -> reminderList.sortedBy { it.timestamp.toLongOrNull() ?: 0L } //Date-Reverse
            2 -> reminderList.sortedBy {
                return@sortedBy when (it.priority.lowercase()) {
                    "high" -> 0
                    "medium" -> 1
                    "low" -> 2
                    "none" -> 3
                    else -> 4 // bilinmeyen öncelik en sona
                }
            } //Priority
            3 -> reminderList.sortedWith(
                compareByDescending<Reminder> { it.reminder }
                    .thenByDescending { it.timestamp.toLongOrNull() ?: 0L }
            ) // Reminder
            else -> reminderList
        }

        Log.d("ReminderCheck", "Sorted List: ${sortedList.map { "${it.title} - ${it.reminder}" }}")

        reminderAdapter.updateList(ArrayList(sortedList))
    }


    // HomeFragment.kt - setupDrawerMenu() fonksiyonunda debugging ekleyin

    private fun setupDrawerMenu() {

        viewModel.loadWorkspaces()



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
            backgroundColor = Color.parseColor("#EBAB16"),
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


        viewModel.isGoogleUser.observe(viewLifecycleOwner) { isGoogle ->
            contentBinding.passwordChangeButton.isVisible = !isGoogle
        }


        contentBinding.passwordChangeButton.setOnClickListener {
            val action = HomeFragmentDirections.actionHomeFragmentToPasswordChangeFragment()
            Navigation.findNavController(requireView()).navigate(action)
        }

        // Workspace adapter'ını boş DrawerMenuItem listesi ile başlat
        drawerMenuAdapter = DrawerMenuAdapter(ArrayList<DrawerMenuItem>()) { item ->
            // Workspace item'a tıklandığında yapılacak işlemler
            // Toast.makeText(requireContext(), "Selected workspace: ${item.title}", Toast.LENGTH_SHORT).show()

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




        if (workspaceList != null && workspaceList.isNotEmpty()) {
            drawerMenuAdapter.updateList(workspaceList)

        } else {
            println("Workspace list is empty or null")
        }


        // Logout button click listener
        contentBinding.btnLogout.setOnClickListener {
            AlertDialog.Builder(requireContext(), R.style.MyDialogTheme)
                .setTitle("Are You Sure?")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes") { _, _ ->
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
                            ContextCompat.getColor(requireContext(), R.color.primary_text_color)
                        )
                        getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(
                            ContextCompat.getColor(requireContext(), R.color.secondary_color)
                        )
                    }
                }
                .show()
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
            textSize = size / 2.5f
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
                    val action = MainNavGraphDirections.actionHomeToLogin()
                    Navigation.findNavController(requireView()).navigate(action)
                }
            } catch (e: Exception) {
                // Hata durumunda kullanıcıya bilgi ver
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "An error occurred while logging out: ${e.localizedMessage}",
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