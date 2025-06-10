package com.chnkcksk.reminderapp.view

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.Navigation
import com.chnkcksk.reminderapp.R
import com.chnkcksk.reminderapp.databinding.FragmentEditReminderBinding
import com.chnkcksk.reminderapp.databinding.FragmentEditWorkspaceBinding
import com.chnkcksk.reminderapp.util.LoadingManager
import com.chnkcksk.reminderapp.viewmodel.EditWorkspaceViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class EditWorkspaceFragment : Fragment() {

    private var _binding: FragmentEditWorkspaceBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private var workspaceId: String? = null

    private val loadingManager = LoadingManager.getInstance()

    private val viewModel: EditWorkspaceViewModel by viewModels()

    private var isPersonal: Boolean = false

    private var kickOthers: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth
        firestore = Firebase.firestore

        arguments?.let {
            workspaceId = EditWorkspaceFragmentArgs.fromBundle(it).workspaceId
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentEditWorkspaceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.editWorkspaceButton.isVisible= false
        binding.linearLayout11.isVisible = false



        viewModel.fetchWorkspaceMemberNames(workspaceId!!)
        viewModel.getWorkspaceData(workspaceId!!)

        setupInvisible()
        setupSpinners()
        setupLiveDatas()
        setupButtons()
    }

    private fun setupInvisible() {
        binding.editWorkspaceTypeSpinner.onItemSelectedListener =
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
                        val adapter =
                            binding.editWorkspaceEditableSpinner.adapter as ArrayAdapter<String>
                        val editablePosition = adapter.getPosition("Editable")
                        if (editablePosition != -1) {
                            binding.editWorkspaceEditableSpinner.setSelection(editablePosition)
                        }
                        //binding.linearLayout11.isVisible = false
                        binding.editWorkspaceEditableSpinner.isEnabled = false
                    } else {
                        //binding.linearLayout11.isVisible = true
                        // Diğer durumlarda yeniden etkinleştir
                        binding.editWorkspaceEditableSpinner.isEnabled = true
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // Gerekirse boş bırakılabilir
                }
            }

    }

    private fun setupToOwner(){
        binding.deleteWorkspaceButton.setImageResource(R.drawable.baseline_delete_outline_24)
        binding.deleteWorkspaceButton.setOnClickListener {
            deleteDialog()
        }
        binding.editWorkspaceButton.isVisible = true
    }

    private fun setupToGuest(){

        binding.editWorkspaceButton.isVisible = false

        binding.workspaceNameEditET.isClickable = false
        binding.workspaceNameEditET.isFocusable = false


        binding.editWorkspaceEditableSpinner.isEnabled = false
        binding.editWorkspaceTypeSpinner.isEnabled = false

        binding.deleteWorkspaceButton.setImageResource(R.drawable.baseline_exit_to_app_24)
        binding.deleteWorkspaceButton.setOnClickListener {
            AlertDialog.Builder(requireContext(), R.style.MyDialogTheme)
                .setTitle("Leave")
                .setMessage("Are you sure you want to leave the workspace?")
                .setPositiveButton("Yes") { _, _ ->
                    viewModel.quitWorkspace(workspaceId!!)
                    val action =
                        EditWorkspaceFragmentDirections.actionEditWorkspaceFragmentToHomeFragment()
                    Navigation.findNavController(requireView()).navigate(action)
                }
                .setNegativeButton("No", null)
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

    private fun setupLiveDatas() {
        viewModel.toastMessage.observe(viewLifecycleOwner) { message ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading == true) {
                loadingManager.showLoadingQuick(requireContext())
            } else {
                loadingManager.dismissLoading()
            }
        }
        viewModel.navigateWorkspace.observe(viewLifecycleOwner) { isNavigate ->
            if (isNavigate) {
                goBack()
            }
        }
        viewModel.workspaceName.observe(viewLifecycleOwner) {
            binding.workspaceNameEditET.setText(it)
        }
        viewModel.workspaceType.observe(viewLifecycleOwner) {
            if (it == "Personal") {
                binding.linearLayout11.isVisible = false
                isPersonal = true
            }else{
                binding.linearLayout11.isVisible = true
            }
            val selectedIndex = when (it) {
                "Group" -> 0
                "Personal" -> 1
                else -> 0 // default
            }
            binding.editWorkspaceTypeSpinner.setSelection(selectedIndex)
        }
        viewModel.workspaceCode.observe(viewLifecycleOwner) {
            binding.workspaceCodeTV.text = it
        }
        viewModel.navigateHome.observe(viewLifecycleOwner) {
            if (it) {
                val action =
                    EditWorkspaceFragmentDirections.actionEditWorkspaceFragmentToHomeFragment()
                Navigation.findNavController(requireView()).navigate(action)
            }
        }
        viewModel.ownerId.observe(viewLifecycleOwner) { ownerId ->

            val currentUser = auth.currentUser

            if (currentUser != null) {

                val userId = currentUser.uid

                if (ownerId != userId) {

                    setupToGuest()
                } else {
                   setupToOwner()

                }

            }


        }
        viewModel.memberNames.observe(viewLifecycleOwner) {
            val formattedText = it.joinToString(separator = "\n") { name -> "- $name" }
            binding.personListTV.text = formattedText
        }
        viewModel.editableType.observe(viewLifecycleOwner) {

            val selectedIndex = when (it) {
                "Editable" -> 0
                "Read only" -> 1
                else -> 0 // default
            }
            binding.editWorkspaceEditableSpinner.setSelection(selectedIndex)

        }
    }

    private fun setupSpinners() {

        //listeyi tanimla
        val byOthers = resources.getStringArray(R.array.editable_type)
        val adapterOne =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, byOthers)
        adapterOne.setDropDownViewResource(R.layout.custom_spinner_item)
        binding.editWorkspaceEditableSpinner.adapter = adapterOne

        val workspaceType = resources.getStringArray(R.array.workspace_type)
        val adapterTwo =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, workspaceType)
        adapterTwo.setDropDownViewResource(R.layout.custom_spinner_item)
        binding.editWorkspaceTypeSpinner.adapter = adapterTwo

    }

    private fun cancelEditAndGoBack(){
        androidx.appcompat.app.AlertDialog.Builder(requireContext(), R.style.MyDialogTheme)
            .setTitle("Are you sure?")
            .setMessage("Are you sure you want to cancel the edit and leave?")
            .setPositiveButton("Yes"){_,_ ->
                goBack()
            }
            .setNegativeButton("No",null)
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

    private fun setupButtons() {
        binding.backButton.setOnClickListener {
            cancelEditAndGoBack()
        }


        binding.workspaceCodeTV.setOnClickListener {
            val textToCopy = binding.workspaceCodeTV.text.toString()

            val clipboard =
                requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Join Code", textToCopy)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(requireContext(), "Join code copied!", Toast.LENGTH_SHORT).show()

        }


        binding.shareCodeButton.setOnClickListener {
            val textToShare = binding.workspaceCodeTV.text.toString()

            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "Join code: $textToShare")
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, "Share join code via")
            startActivity(shareIntent)
        }


        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {


                override fun handleOnBackPressed() {
                    cancelEditAndGoBack()

                }
            })

        binding.editWorkspaceButton.setOnClickListener {
            val editedWorkspaceName = binding.workspaceNameEditET.text.toString()
            val wT = binding.editWorkspaceTypeSpinner.selectedItem.toString()
            val eT = binding.editWorkspaceEditableSpinner.selectedItem.toString()

            if (isPersonal == false && wT == "Personal") {
                AlertDialog.Builder(requireContext(), R.style.MyDialogTheme)
                    .setTitle("Are You Sure")
                    .setMessage("")
                    .setPositiveButton("Yes") { _, _ ->
                        kickOthers = true
                        viewModel.editWorkspace(
                            workspaceId!!,
                            editedWorkspaceName,
                            wT,
                            eT,
                            kickOthers
                        )
                    }.setNegativeButton("No", null)
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

            } else {
                viewModel.editWorkspace(workspaceId!!, editedWorkspaceName, wT, eT, kickOthers)
            }

        }

    }

    private fun deleteDialog() {
        AlertDialog.Builder(requireContext(),R.style.MyDialogTheme)
            .setTitle("Delete")
            .setMessage("Are you sure you want to delete the workspace?")
            .setPositiveButton("Yes") { _, _ ->
                viewModel.deleteWorkspace(workspaceId!!)
                val action =
                    EditWorkspaceFragmentDirections.actionEditWorkspaceFragmentToHomeFragment()
                Navigation.findNavController(requireView()).navigate(action)
            }
            .setNegativeButton("No", null)
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

    private fun goBack() {
        val action =
            EditWorkspaceFragmentDirections.actionEditWorkspaceFragmentToOtherWorkspaceFragment(
                workspaceId!!
            )
        Navigation.findNavController(requireView()).navigate(action)
    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }


}