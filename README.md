# To-Minder - Hatırlatıcı ve Sohbet Uygulaması

<div align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" alt="To-Minder Logo" width="120" height="120">
  <h3>Modern Hatırlatıcı ve Takım İşbirliği Uygulaması</h3>
</div>

## 📱 Uygulama Hakkında

**To-Minder**, kullanıcıların görevlerini yönetebilecekleri, hatırlatıcılar oluşturabilecekleri ve takım arkadaşlarıyla gerçek zamanlı sohbet edebilecekleri kapsamlı bir mobil uygulamadır. Firebase backend'i kullanarak modern ve güvenilir bir deneyim sunar.

### 🎯 Ana Özellikler

- **📝 Görev Yönetimi**: Hatırlatıcılar oluşturma, düzenleme ve takip etme
- **🏢 Çalışma Alanı Sistemi**: Kişisel ve grup çalışma alanları
- **💬 Gerçek Zamanlı Sohbet**: Takım üyeleriyle anlık mesajlaşma
- **🔔 Akıllı Bildirimler**: Zamanlanmış hatırlatıcı bildirimleri
- **📱 Ana Ekran Widget**: Hızlı erişim için widget desteği
- **🔐 Güvenli Kimlik Doğrulama**: Firebase Auth ile Google Girişi
- **🌙 Modern Arayüz**: Material Design 3 prensipleri

## 🛠️ Kullanılan Teknolojiler

### **Temel Teknolojiler**
- **Kotlin** (2.1.0) - Ana programlama dili
- **Android** - Mobil platform
- **Java 11** - JVM hedefi

### **Geliştirme Araçları**
- **Android Gradle Plugin** (8.7.3) - Build sistemi
- **Gradle** - Bağımlılık yönetimi
- **View Binding** - UI binding
- **Safe Args** - Type-safe navigation

### **Mimari ve Arayüz**
- **MVVM Mimari** - Model-View-ViewModel pattern
- **Navigation Component** (2.7.7) - Fragment navigation
- **Material Design** (1.12.0) - UI bileşenleri
- **ConstraintLayout** (2.2.1) - Layout sistemi
- **RecyclerView** - Liste yönetimi
- **SwipeRefreshLayout** (1.1.0) - Pull-to-refresh

### **Backend ve Veritabanı**
- **Firebase BOM** (33.12.0) - Bill of Materials
- **Firebase Authentication** - Kullanıcı kimlik doğrulama
- **Firebase Realtime Database** (21.0.0) - Gerçek zamanlı veri senkronizasyonu
- **Firebase Firestore** - NoSQL veritabanı
- **Firebase Storage** - Dosya depolama
- **Firebase Analytics** - Analitik takibi

### **Kimlik Doğrulama**
- **Google Play Services Auth** (20.7.0) - Google Girişi
- **AndroidX Credentials** (1.3.0) - Kimlik bilgisi yönetimi
- **Google Identity** (1.1.1) - Google ID entegrasyonu

### **Arka Plan İşlemleri ve Bildirimler**
- **WorkManager** (2.10.1) - Arka plan görevleri
- **AlarmManager** - Kesin alarm zamanlaması
- **NotificationManager** - Push bildirimleri
- **SoundPool** - Ses geri bildirimi

### **Arayüz Geliştirmeleri**
- **Lottie** (6.1.0) - Animasyon kütüphanesi
- **Özel Fontlar** - Public Sans font ailesi
- **Edge-to-Edge** - Modern ekran desteği

### **Test**
- **JUnit** (4.13.2) - Birim testleri
- **AndroidX Test** (1.2.1) - Enstrümantasyon testleri
- **Espresso** (3.6.1) - UI testleri

## 📋 Sistem Gereksinimleri

- **Minimum SDK**: 23 (Android 6.0 Marshmallow)
- **Hedef SDK**: 34 (Android 14)
- **Derleme SDK**: 35
- **Kotlin Sürümü**: 2.1.0
- **Java Sürümü**: 11

## 🚀 Kurulum

### Ön Gereksinimler
- Android Studio Arctic Fox veya üzeri
- JDK 11
- Google Play Services
- Firebase Console hesabı

### Adımlar

1. **Repository'yi klonlayın**
   ```bash
   git clone https://github.com/chnkcksk/ReminderApp.git
   cd ReminderApp
   ```

2. **Firebase projesini oluşturun**
   - [Firebase Console](https://console.firebase.google.com/)'a gidin
   - Yeni proje oluşturun
   - Android uygulaması ekleyin
   - `google-services.json` dosyasını indirin

3. **Firebase yapılandırması**
   - İndirilen `google-services.json` dosyasını `app/` klasörüne yerleştirin
   - ⚠️ **Bu dosyayı paylaşmayın, kendi Firebase projenizden alın**
   - Firebase Console'da Authentication, Realtime Database ve Firestore'u etkinleştirin

4. **Projeyi derleyin**
   ```bash
   ./gradlew build
   ```

5. **Uygulamayı çalıştırın**
   ```bash
   ./gradlew installDebug
   ```

## 📱 Uygulama Özellikleri

### 🔐 Kimlik Doğrulama
- Google Girişi entegrasyonu
- Firebase Authentication
- Güvenli oturum yönetimi

### 📝 Hatırlatıcı Yönetimi
- Hatırlatıcı oluşturma ve düzenleme
- Öncelik seviyeleri (Yok, Düşük, Orta, Yüksek)
- Tarih ve saat belirleme
- Tamamlanma durumu takibi
- Sıralama seçenekleri (Tarih, Öncelik, Hatırlatıcı)

### 🏢 Çalışma Alanı Sistemi
- Kişisel çalışma alanları
- Grup çalışma alanları
- Çalışma alanı oluşturma ve yönetimi
- Üye davet etme sistemi

### 💬 Gerçek Zamanlı Sohbet
- Çalışma alanı bazlı sohbet odaları
- Anlık mesajlaşma
- Mesaj silme özelliği
- Ses efektleri
- Otomatik kaydırma

### 🔔 Bildirim Sistemi
- Zamanlanmış hatırlatıcı bildirimleri
- WorkManager ile arka plan işlemleri
- Özelleştirilebilir bildirim kanalları
- Titreşim ve ses desteği

### 📱 Widget Desteği
- Ana ekran widget'ı
- Otomatik güncelleme (saatlik)
- Manuel yenileme
- Son güncelleme zamanı gösterimi

### 🎨 Kullanıcı Arayüzü
- Material Design 3
- Karanlık/Aydınlık tema desteği
- Edge-to-edge ekran
- Responsive tasarım
- Lottie animasyonları

## 🏗️ Proje Yapısı

```
app/src/main/java/com/chnkcksk/reminderapp/
├── adapter/          # RecyclerView adaptörleri
├── model/           # Veri modelleri
├── permissions/     # İzin yönetimi
├── util/           # Yardımcı sınıflar
├── view/           # UI fragment'ları ve activity'leri
├── viewmodel/      # ViewModel'ler (MVVM)
├── widgets/        # Ana ekran widget'ları
└── worker/         # Arka plan worker'ları
```

## 🔧 Konfigürasyon

### Firebase Yapılandırması
```kotlin
// google-services.json dosyası gerekli
// Firebase Console'dan indirilmelidir
// ⚠️ Bu dosyayı paylaşmayın!
```

### Bildirim İzinleri
```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.VIBRATE" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
```

## 🧪 Test

### Birim Testleri
```bash
./gradlew test
```

### Enstrümantasyon Testleri
```bash
./gradlew connectedAndroidTest
```

## 📝 Lisans

Bu proje MIT Lisansı ile lisanslanmıştır.

## 🤝 Katkıda Bulunma

1. Bu repository'yi fork edin
2. Yeni bir branch oluşturun (`git checkout -b feature/yeni-ozellik`)
3. Değişikliklerinizi commit edin (`git commit -am 'Yeni özellik eklendi'`)
4. Branch'inizi push edin (`git push origin feature/yeni-ozellik`)
5. Pull Request oluşturun

## 📞 İletişim

- **Geliştirici**: Cihan Koçakuşak
- **GitHub**: [@chnkcksk](https://github.com/chnkcksk)
