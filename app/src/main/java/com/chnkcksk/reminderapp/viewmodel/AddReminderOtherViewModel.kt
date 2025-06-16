package com.chnkcksk.reminderapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ViewModel sınıfı, Application bağlamı ile birlikte AndroidViewModel'dan türetilmiş.
class AddReminderOtherViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = Firebase.auth
    private val firestore = Firebase.firestore

    /**
     * UI ile haberleşmek için kullanılan olayları temsil eden mühürlü sınıf (sealed class).
     * Her olasılık ayrı bir class veya object olarak tanımlanmış.
     * Bu sayede type-safe (tip güvenli) şekilde farklı UI olayları yönetilebiliyor.
     */
    sealed class UiEvent {
        object ReminderAdded : UiEvent() // Başarıyla eklenme durumu
        object ShowLoading : UiEvent()   // Yükleme göstergesi aç
        object HideLoading : UiEvent()   // Yükleme göstergesini kapat
        data class ShowToast(val message: String) : UiEvent() // Toast mesajı göster
        object NavigateWorkspace : UiEvent()  // Workspace sayfasına geçiş isteği
        data class WorkspaceInformations(     // Workspace ile ilgili bilgileri UI'a gönder
            val workspaceName: String,
            val workspaceType: String,
        ) : UiEvent()
    }

    // UI'a gönderilecek eventleri tutmak için kullanılan akış (SharedFlow) - birden fazla kez tüketilebilir.
    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow() // Dışarıya sadece okunabilir versiyonu veriliyor.

    /**
     * Yeni bir "diğer" reminder (hatırlatıcı) eklemek için kullanılan fonksiyon.
     * Firestore'a veri ekleme işlemi coroutine içinde yapılır.
     */
    fun addOtherReminder(
        workspaceId: String,
        title: String,
        description: String,
        priority: String,
        date: String,
        time: String
    ) {
        viewModelScope.launch {

            // Kullanıcı girişi kontrolü
            val currentUser = auth.currentUser
            if (currentUser == null) {
                _uiEvent.emit(UiEvent.ShowToast("User login required"))
                return@launch
            }

            // Yükleme göstergesini UI'da aç
            _uiEvent.emit(UiEvent.ShowLoading)

            // Firestore'a eklenecek verinin hazırlanması
            val reminder = hashMapOf(
                "title" to title,
                "description" to description,
                "timestamp" to System.currentTimeMillis(),
                "isCompleted" to false,
                "priority" to priority,
                "date" to date,
                "time" to time
            )

            try {
                // Firestore işlemine await() ile coroutine destekli asenkron işlem
                firestore.collection("workspaces")
                    .document(workspaceId)
                    .collection("reminders")
                    .add(reminder)
                    .await()

                // Başarı durumunda UI'a bildirim gönder
                _uiEvent.emit(UiEvent.ReminderAdded)

            } catch (e: Exception) {
                // Hata durumunda loading kapatılır ve toast mesajı gösterilir
                _uiEvent.emit(UiEvent.HideLoading)
                _uiEvent.emit(UiEvent.ShowToast("Reminder could not be added"))
            }
        }
    }

    /**
     * Firestore'dan workspace bilgilerini çekmek için kullanılan fonksiyon.
     * Başarılı olursa UI'a WorkspaceInformations event'i gönderir.
     */
    fun getDatas(workspaceId: String) {
        viewModelScope.launch {

            // Yükleme göstergesini UI'da aç
            _uiEvent.emit(UiEvent.ShowLoading)

            try {
                // Firestore'dan workspace dokümanını çek
                val doc = firestore.collection("workspaces")
                    .document(workspaceId)
                    .get()
                    .await()

                // Workspace bilgilerini içeren UiEvent ile UI'a gönder
                _uiEvent.emit(
                    UiEvent.WorkspaceInformations(
                        workspaceName = doc.getString("workspaceName") ?: "",
                        workspaceType = doc.getString("workspaceType") ?: "",
                    )
                )

                // Yükleme göstergesini kapat
                _uiEvent.emit(UiEvent.HideLoading)

            } catch (e: Exception) {
                // Hata durumunda toast gösterilir ve yükleme kapanır
                _uiEvent.emit(UiEvent.ShowToast("${e.message}"))
                _uiEvent.emit(UiEvent.HideLoading)
            }
        }
    }
}
