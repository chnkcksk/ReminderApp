package com.chnkcksk.reminderapp.view

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.chnkcksk.reminderapp.R
import com.chnkcksk.reminderapp.databinding.FragmentAddWorkspaceBinding
import com.chnkcksk.reminderapp.databinding.FragmentHomeBinding
import com.chnkcksk.reminderapp.util.LoadingManager
import com.chnkcksk.reminderapp.util.SuccessDialog
import com.chnkcksk.reminderapp.viewmodel.AddWorkspaceViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


class AddWorkspaceFragment : Fragment() {

    private var _binding: FragmentAddWorkspaceBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth

    private val loadingManager = LoadingManager.getInstance()
    private val successDialog = SuccessDialog()

    private val viewModel: AddWorkspaceViewModel by viewModels()

    private lateinit var joinCode: String
    private lateinit var typedJoinCode: String
    private lateinit var workspaceId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAddWorkspaceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        setupObserves()

        setupInvisible()
        setupSpinners()
        setupButtons()
    }


    private fun setupInvisible() {
        binding.workspaceTypeSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val selectedWorkspaceType = parent.getItemAtPosition(position).toString()

                    if (selectedWorkspaceType == "Personal") {
                        // editableTypeSpinner'ı "editable" yap ve devre dışı bırak
                        val adapter = binding.editableTypeSpinner.adapter as ArrayAdapter<String>
                        val editablePosition = adapter.getPosition("Editable")
                        if (editablePosition != -1) {
                            binding.editableTypeSpinner.setSelection(editablePosition)
                        }
                        binding.editableTypeSpinner.isEnabled = false
                    } else {
                        // Diğer durumlarda yeniden etkinleştir
                        binding.editableTypeSpinner.isEnabled = true
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // Gerekirse boş bırakılabilir
                }
            }

    }

    private fun setupObserves() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.uiEvent.collect { event ->
                when (event) {
                    is AddWorkspaceViewModel.UiEvent.WorkspaceJoined ->{
                        loadingManager.dismissLoading {
                            successDialog.showSuccessDialog(requireContext()){
                                //joincode

                                navigateNewWorkspace()
                            }
                        }
                    }

                    is AddWorkspaceViewModel.UiEvent.WorkspaceCreated ->{
                        loadingManager.dismissLoading {
                            successDialog.showSuccessDialog(requireContext()){
                                //BuildDialog
                                buildJoinCodeDialog()

                                navigateNewWorkspace()
                            }
                        }
                    }

                    is AddWorkspaceViewModel.UiEvent.ShowLoading -> loadingManager.showLoading(requireContext())
                    is AddWorkspaceViewModel.UiEvent.HideLoading -> loadingManager.dismissLoading()
                    is AddWorkspaceViewModel.UiEvent.ShowToast -> Toast.makeText(requireContext(), event.message, Toast.LENGTH_LONG).show()

                    is AddWorkspaceViewModel.UiEvent.WorkspaceInformation ->{
                        joinCode = event.joinCode
                        workspaceId = event.workspaceId
                    }


                }
            }
        }

    }

    private fun navigateNewWorkspace(){
        val action =
            AddWorkspaceFragmentDirections.actionAddWorkspaceFragmentToOtherWorkspaceFragment(
                workspaceId
            )
        Navigation.findNavController(requireView()).navigate(action)
    }

    private fun buildJoinCodeDialog(){
        val workspaceSelectedType = binding.workspaceTypeSpinner.selectedItem

        if (workspaceSelectedType == "Group") {
            val dialogBuilder = AlertDialog.Builder(requireContext(), R.style.MyDialogTheme)
            dialogBuilder.setTitle("Join Code")

            // joinCode'u büyük ve ortalanmış gösteren TextView
            val messageView = TextView(requireContext()).apply {
                text = joinCode
                textSize = 24f
                gravity = Gravity.CENTER
                setPadding(32, 48, 32, 48)
            }

            dialogBuilder.setView(messageView)
            dialogBuilder.setPositiveButton("Okay") { _, _ ->
            }
            dialogBuilder.setNegativeButton("Copy", null)
            dialogBuilder.setNeutralButton("Share", null)

            val alertDialog = dialogBuilder.create()
            alertDialog.apply {
                setOnShowListener {
                    // Butonların metin rengini değiştir
                    getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.primary_text_color)
                    )
                    getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.secondary_color)
                    )
                    getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL)?.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.primary_color)
                    )
                }
            }
            alertDialog.show()

            // Butonlar burada oluşturulduktan sonra dialog kapanmasın diye listener'ları ayrı set ediyoruz:
            val copyButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            val shareButton = alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL)



            copyButton.setOnClickListener {
                val clipboard =
                    requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Join Code", joinCode)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(requireContext(), "Join code copied!", Toast.LENGTH_SHORT).show()
                // dialog.dismiss() çağrılmadığı için kapanmaz
            }

            shareButton.setOnClickListener {
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, "Join code: $joinCode")
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, "Share join code via")
                startActivity(shareIntent)
                // dialog.dismiss() yok => kapanmaz
            }
        }
    }



    private fun setupButtons() {
        binding.apply {

            joinWorkspaceButton.setOnClickListener {
                typedJoinCode = workspaceCodeET.text.toString()

                lifecycleScope.launch {
                    viewModel.joinWorkspace(typedJoinCode)
                }
            }


            backButton.setOnClickListener {
                goBack()
            }
            addWorkspaceButton.setOnClickListener {

                val workspaceName = binding.workspaceNameET.text.toString()
                val editableType = binding.editableTypeSpinner.selectedItem.toString()
                val workspaceType = binding.workspaceTypeSpinner.selectedItem.toString()

                if (workspaceName.isNotEmpty()) {

                    lifecycleScope.launch {

                        viewModel.createWorkspace(workspaceName, editableType, workspaceType)

                    }


                } else {
                    Toast.makeText(
                        requireContext(),
                        "Please fill workspace name",
                        Toast.LENGTH_LONG
                    ).show()
                }

            }
            requireActivity().onBackPressedDispatcher.addCallback(
                viewLifecycleOwner,
                object : OnBackPressedCallback(true) {


                    override fun handleOnBackPressed() {
                        goBack()

                    }
                })
        }
    }


    private fun setupSpinners() {

        //listeyi tanimla
        val byOthers = resources.getStringArray(R.array.editable_type)
        val adapterOne =
            ArrayAdapter(requireContext(), R.layout.custom_spinner_item, byOthers)
        adapterOne.setDropDownViewResource(R.layout.custom_spinner_item)
        binding.editableTypeSpinner.adapter = adapterOne

        val workspaceType = resources.getStringArray(R.array.workspace_type)
        val adapterTwo =
            ArrayAdapter(requireContext(), R.layout.custom_spinner_item, workspaceType)
        adapterTwo.setDropDownViewResource(R.layout.custom_spinner_item)
        binding.workspaceTypeSpinner.adapter = adapterTwo

    }

    private fun goBack() {
        val action = AddWorkspaceFragmentDirections.actionAddWorkspaceFragmentToHomeFragment()
        Navigation.findNavController(requireView()).navigate(action)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }


}