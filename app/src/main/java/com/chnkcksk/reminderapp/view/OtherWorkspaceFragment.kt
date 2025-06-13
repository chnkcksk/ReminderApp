package com.chnkcksk.reminderapp.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.chnkcksk.reminderapp.R
import com.chnkcksk.reminderapp.adapter.ReminderAdapter
import com.chnkcksk.reminderapp.databinding.FragmentAddWorkspaceBinding
import com.chnkcksk.reminderapp.databinding.FragmentOtherWorkspaceBinding
import com.chnkcksk.reminderapp.util.LoadingManager
import com.chnkcksk.reminderapp.viewmodel.OtherWorkspaceViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch


class OtherWorkspaceFragment : Fragment() {

    private var _binding: FragmentOtherWorkspaceBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth

    private val loadingManager = LoadingManager.getInstance()

    private val viewModel: OtherWorkspaceViewModel by viewModels()

    private lateinit var workspaceId: String
    private lateinit var previousScreen: String

    private lateinit var ownerId: String
    private lateinit var editableType: String

    private var isWorkspaceNameLoaded = false
    private var isEditableTypeLoaded = false
    private var isOwnerIdLoaded = false

    private var lastRefreshTime = 0L
    private val REFRESH_COOLDOWN = 5000L // 5 saniye

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth

        arguments?.let {
            workspaceId = OtherWorkspaceFragmentArgs.fromBundle(it).workspaceId
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentOtherWorkspaceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.otherAddFAB.isVisible = false


        //veriuleri cektigimiz fonksiyon
        lifecycleScope.launch {
            viewModel.loadWorkspaceData(workspaceId)
        }


        setupButtons()
        setupLiveDatas()
    }

    private fun checkAndSetupReminders() {
        if (isEditableTypeLoaded && isOwnerIdLoaded && isWorkspaceNameLoaded) {
            setupOtherReminders()
            if (editableType == "Read only" && ownerId != auth.currentUser?.uid){
                binding.otherAddFAB.isVisible = false
            }
            else{
                binding.otherAddFAB.isVisible = true
            }

        }




    }

    private fun setupOtherReminders() {
        lifecycleScope.launch {
            viewModel.loadRemindersList(workspaceId)
        }


        var owner = false

        if (ownerId==auth.currentUser?.uid){
            owner = true
        }

        val adapter = ReminderAdapter(
            requireContext(),
            workspaceId,
            editableType,
            owner,
            ArrayList()
        ) { reminder ->
            // editableType = "read-only" ve ownerId != currentUserId ise tıklanamaz
            if (editableType == "Read only" && ownerId != auth.currentUser?.uid) {
                Toast.makeText(
                    requireContext(),
                    "Bu workspace sadece görüntülenebilir.",
                    Toast.LENGTH_SHORT
                ).show()

            } else {
                val reminderId = reminder.id
                val action =
                    OtherWorkspaceFragmentDirections.actionOtherWorkspaceFragmentToEditReminderOtherFragment(
                        workspaceId,
                        reminderId
                    )
                Navigation.findNavController(requireView()).navigate(action)
            }
        }

        binding.otherWorkspaceRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.otherWorkspaceRecyclerView.adapter = adapter

        viewModel.reminderList.observe(viewLifecycleOwner) { reminderList ->
            adapter.updateList(reminderList)
        }
    }

    private fun setupButtons() {

        binding.backButton.setOnClickListener {
            goBack()
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastRefreshTime > REFRESH_COOLDOWN) {
                lifecycleScope.launch {
                    viewModel.loadWorkspaceData(workspaceId)
                }

                Toast.makeText(requireContext(),"Workspace data refreshed",Toast.LENGTH_SHORT).show()
                lastRefreshTime = currentTime
            }
            binding.swipeRefreshLayout.isRefreshing = false
        }

        binding.editWorkspaceButton.setOnClickListener {
            val action =
                OtherWorkspaceFragmentDirections.actionOtherWorkspaceFragmentToEditWorkspaceFragment(
                    workspaceId
                )
            Navigation.findNavController(requireView()).navigate(action)
        }

        binding.otherAddFAB.setOnClickListener {
            val action =
                OtherWorkspaceFragmentDirections.actionOtherWorkspaceFragmentToAddReminderOtherFragment(
                    workspaceId
                )
            Navigation.findNavController(requireView()).navigate(action)
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {


                override fun handleOnBackPressed() {
                    goBack()

                }
            })


    }

    private fun setupLiveDatas() {
        viewModel.workspaceName.observe(viewLifecycleOwner) { workspaceName ->
            binding.workspaceNameTV.text = workspaceName
            isWorkspaceNameLoaded = true
            checkAndSetupReminders()
        }
        viewModel.editableType.observe(viewLifecycleOwner) { it ->
            editableType = it
            isEditableTypeLoaded = true
            checkAndSetupReminders()
        }
        viewModel.ownerId.observe(viewLifecycleOwner) {
            ownerId = it
            isOwnerIdLoaded = true
            checkAndSetupReminders()
        }
    }

    private fun goBack() {
        val action = OtherWorkspaceFragmentDirections.actionOtherWorkspaceFragmentToHomeFragment()
        Navigation.findNavController(requireView()).navigate(action)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }


}