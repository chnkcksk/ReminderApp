package com.chnkcksk.reminderapp.view

import android.app.AlertDialog
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.chnkcksk.reminderapp.R
import com.chnkcksk.reminderapp.adapter.ChatAdapter
import com.chnkcksk.reminderapp.databinding.FragmentChatBinding
import com.chnkcksk.reminderapp.util.LoadingManager
import com.chnkcksk.reminderapp.util.NetworkHelper
import com.chnkcksk.reminderapp.viewmodel.ChatViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase


class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth

    private val loadingManager = LoadingManager.getInstance()

    private lateinit var workspaceId: String

    private val viewModel: ChatViewModel by viewModels()

    private lateinit var chatAdapter: ChatAdapter

    private lateinit var soundPool: SoundPool
    private var soundIdPush: Int = 0
    private var soundIdDelete: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth

        arguments?.let {
            workspaceId = ChatFragmentArgs.fromBundle(it).workspaceId
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!NetworkHelper.isInternetAvailable(requireContext())) {
            NetworkHelper.showNoInternetDialog(requireContext(), requireView(), requireActivity())
        }

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val navInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Yalnızca alt boşluk uygula (klavye için)
            v.setPadding(0, 0, 0, imeInsets.bottom)
            insets
        }


        setupRecyclerView()

        setupObserves()
        setupButtons()
        setupSoundPool()
    }

    private fun setupRecyclerView() {

        chatAdapter = ChatAdapter { chatMessage ->

            AlertDialog.Builder(binding.root.context)
                .setTitle("Delete Message")
                .setMessage("Do you want to delete this message?")
                .setPositiveButton("Delete") { _, _ ->
                    // Firebase veya Room gibi kullandığın yerden silme işlemi yap
                    viewModel.deleteMessage(workspaceId, chatMessage)
                }
                .setNegativeButton("İptal", null)
                .show()


        }
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true    // ✅ Listeyi en alttan başlatır
            reverseLayout = false  // ✅ Normal sıralama (eski → yeni)
        }
        binding.chatRecyclerView.adapter = chatAdapter


        observeMessages()
    }

    private fun observeMessages() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {

            viewModel.listenForMessages(workspaceId).collect() { messages ->
                chatAdapter.submitList(messages)
                binding.chatRecyclerView.scrollToPosition(messages.size - 1)
            }

        }
    }

    private fun setupObserves() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.uiEvent.collect { event ->

                when (event) {
                    is ChatViewModel.UiEvent.ShowLoading -> loadingManager.showLoading(
                        requireContext()
                    )

                    is ChatViewModel.UiEvent.HideLoading -> loadingManager.dismissLoading()
                    is ChatViewModel.UiEvent.ShowToast -> Toast.makeText(
                        requireContext(),
                        event.message,
                        Toast.LENGTH_SHORT
                    ).show()

                    is ChatViewModel.UiEvent.MessageSended -> {
                        binding.messageET.text.clear()

                        soundPool.play(soundIdPush, 1f, 1f, 1, 0, 1f)
                    }

                    is ChatViewModel.UiEvent.MessageDeleted ->{
                        soundPool.play(soundIdDelete, 1f, 1f, 1, 0, 1f)
                    }

                }

            }
        }
    }

    private fun setupSoundPool() {

        // SoundPool yapılandırması
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(1) // Aynı anda kaç ses çalınabileceği
            .setAudioAttributes(audioAttributes)
            .build()

        soundIdPush = soundPool.load(requireContext(), R.raw.message_ping_sound, 1)
        soundIdDelete = soundPool.load(requireContext(), R.raw.swoosh_sound, 1)

    }

    private fun setupButtons() {

        binding.apply {

            sendButton.setOnClickListener {


                val messageText = messageET.text.toString().trim()

                if (messageText.isEmpty()) {
                    return@setOnClickListener
                }

                viewModel.sendMessage(workspaceId, messageText)

            }
            backButton.setOnClickListener {
                goBack()
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

    private fun goBack() {
        val action = ChatFragmentDirections.actionChatFragmentToOtherWorkspaceFragment(workspaceId)
        Navigation.findNavController(requireView()).navigate(action)
    }

    override fun onDestroy() {
        super.onDestroy()
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        _binding = null
    }


}