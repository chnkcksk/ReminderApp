package com.chnkcksk.reminderapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chnkcksk.reminderapp.model.ChatMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = Firebase.auth
    private val firestore = Firebase.firestore

    sealed class UiEvent {
        object ShowLoading : UiEvent()
        object HideLoading : UiEvent()
        data class ShowToast(val message: String) : UiEvent()

        object ClearMessage : UiEvent()
    }

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()


    fun sendMessage(workspaceId: String, messageText: String) {

        viewModelScope.launch {

            val currentUser = auth.currentUser

            if (currentUser == null) {
                _uiEvent.emit(UiEvent.ShowToast("Error!"))
                return@launch
            }

            val message = ChatMessage(
                senderId = currentUser.uid,
                senderName = currentUser.displayName ?: "",
                message = messageText,
                timestamp = System.currentTimeMillis()
            )


            try {

                firestore.collection("workspaces")
                    .document(workspaceId)
                    .collection("messages")
                    .add(message)
                    .await()

                //islem basariliysa
                _uiEvent.emit(UiEvent.ClearMessage)


            } catch (e: Exception) {
                //islem basarisizsa
                _uiEvent.emit(UiEvent.ShowToast("Message failed to send: ${e.message}"))
            }


        }

    }

    fun listenForMessages(workspaceId: String): Flow<List<ChatMessage>> = callbackFlow {


        val listener = firestore.collection("workspaces")
            .document(workspaceId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }


                val messages = snapshot?.documents?.mapNotNull { docSnapshot ->
                    docSnapshot.toObject(ChatMessage::class.java)?.copy(id = docSnapshot.id)
                } ?: emptyList()
                println("🔥 Gelen mesajlar: $messages")  // 👈 Kontrol için log ekle
                trySend(messages)

            }

        awaitClose { listener.remove() }


    }

    fun deleteMessage(workspaceId: String, chatMessage: ChatMessage) {

        viewModelScope.launch {

            try {

                firestore.collection("workspaces")
                    .document(workspaceId)
                    .collection("messages")
                    .document(chatMessage.id)
                    .delete()
                    .await()



            }
            catch (e:Exception){
                _uiEvent.emit(UiEvent.ShowToast("Message could not be deleted: ${e.message}"))

            }

        }




    }

}