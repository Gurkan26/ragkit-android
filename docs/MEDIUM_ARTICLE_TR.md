# Android'de Cihaz Üzerinde Anlamsal Arama (Semantic Search) SDK'sı Geliştirmek: RagKit

> *Google MediaPipe, Room SQLite ve Kotlin Coroutines kullanarak bulutsuz, sıfır gecikmeli ve gizlilik odaklı bir On-Device Vektör Arama motorunun mimarisi.*

---

![RagKit Banner](https://raw.githubusercontent.com/Gurkan26/ragkit-android/main/docs/images/ragkit_search.png)

Hiç notlar uygulamanızda *"doktor randevusu"* diye arama yapıp, notunuza *"Salı günü çocuk doktoru kontrolü"* yazdığınız için **sıfır sonuçla** karşılaştığınız oldu mu?

Yıllardır mobil arama deneyimi klasik anahtar kelime eşleştirmelerine (`SQL LIKE %arama%` veya temel Full-Text Search) hapsolmuş durumda. Bu yöntemler hızlı olsa da oldukça kırılgandır; eş anlamlı kelimeleri, kavramsal çağrışımları veya doğal dille sorulan soruları anlayamazlar.

Günümüzde geliştiricilerin bu soruna ilk refleksi genellikle bir bulut LLM API'sine (OpenAI, Gemini vb.) başvurmak oluyor. Ancak mobil dünyada aramayı buluta taşımak beraberinde 3 ciddi problem getirir:
1. **Gizlilik ve KVKK/GDPR Uyumsuzluğu**: Kullanıcıların kişisel günlüklerini, özel mesajlarını veya finansal notlarını harici bir sunucuya göndermek güvenlik ve güven bariyeri yaratır.
2. **Ağ Bağımlılığı ve Gecikme**: Mobil cihazlar metroda, uçakta veya çekim gücünün düşük olduğu yerlerde sıkça çevrimdışı kalır. Her aramada 4G üzerinden dönen bir yükleme çarkı (spinner) kullanıcı deneyimini zedeler.
3. **Sürekli Artan Altyapı Maliyetleri**: Her tuş vuruşunda veya debounced aramada harici bir vektör veritabanına ve embedding API'sine yapılan çağrılar ciddi bulut faturaları üretir.

Bu problemleri **tamamen Android cihaz üzerinde**, hiçbir sunucuya ihtiyaç duymadan çözmek amacıyla modern Kotlin ile açık kaynak **RagKit** SDK'sını geliştirdim.

Bu makalede; Android platformunda sıfırdan cihaz içi bir vektör arama motoru (On-Device Vector Search Engine) inşa ederken aldığım mimari kararları, arkasındaki matematiği ve karşılaştığım mühendislik zorluklarını tüm detaylarıyla paylaşıyorum.

---

## 🧠 Temel Mantık: Cihaz Üzerinde Anlamsal Arama Nasıl Çalışır?

Anlamsal arama (Semantic Search), metinleri yüksek boyutlu bir geometrik uzayda matematiksel vektörlere (embedding) dönüştürür. Anlamca birbirine yakın olan cümleler, aynı karakterleri içermeseler bile bu vektör uzayında birbirlerine çok yakın noktalara düşerler.

### 1. Vektör Temsilleri (Embeddings)
Kullanıcı bir not kaydettiğinde, cihaz üzerindeki bir makine öğrenmesi modeli (örneğin Google MediaPipe'ın Universal Sentence Encoder modeli) bu metni ondalıklı sayılardan oluşan yoğun bir diziye (vektör) çevirir:

$$\text{"Acil poliklinik randevusu"} \longrightarrow [0.042, -0.189, 0.731, \dots, -0.012]$$
$$\text{"Perşembe günü hastane muayenesi"} \longrightarrow [0.039, -0.175, 0.718, \dots, -0.015]$$

### 2. Kosinüs Benzerliği (Cosine Similarity)
Kullanıcı arama yaptığında, arama sorgusunun vektörü ile veritabanındaki kayıtlı vektörler arasındaki **açısal benzerlik (Kosinüs Benzerliği)** hesaplanır:

$$\text{Cosine Similarity} = \frac{\mathbf{A} \cdot \mathbf{B}}{\|\mathbf{A}\| \|\mathbf{B}\|} = \frac{\sum_{i=1}^{n} A_i B_i}{\sqrt{\sum_{i=1}^{n} A_i^2} \sqrt{\sum_{i=1}^{n} B_i^2}}$$

RagKit'te bu matematiksel değer, `0.0` (tamamen alakasız) ile `1.0` (birebir aynı anlam) arasında normalize edilmiş bir güven skoruna dönüştürülür. Belirlediğiniz eşik değerini (örneğin `0.70` / %70) aşan sonuçlar anında kullanıcıya sunulur.

---

## 📐 RagKit'in Modüler Mimarisi

Kaliteli bir Android SDK'sı, entegre edildiği ana uygulamaya asla zorunlu ve hantal bağımlılıklar dayatmamalıdır. Örneğin, eğer uygulama Room yerine SQLDelight veya MediaPipe yerine ONNX Runtime kullanmak istiyorsa, mimari bunu zahmetsizce değiştirmeye izin vermelidir.

Bu yüzden RagKit'i 3 bağımsız modüle ayırdık:

```
                  +-----------------------------------+
                  |         Android Uygulamanız       |
                  +-----------------------------------+
                                    |
             +----------------------+----------------------+
             |                                             |
             v                                             v
+-----------------------+                      +-----------------------+
|  ragkit-storage-room  |                      | ragkit-embedding-     |
|   (Room SQLite +      |                      |    mediapipe          |
|  Kosinüs Benzerliği)  |                      | (TextEmbedder API)    |
+-----------------------+                      +-----------------------+
             |                                             |
             +----------------------+----------------------+
                                    |
                                    v
                  +-----------------------------------+
                  |            ragkit-core            |
                  |  (Arayüzler, Modeller, DSL API)   |
                  +-----------------------------------+
```

### 1. `ragkit-core` (Hafif ve Bağımsız Çekirdek)
Tüm temel arayüzleri (`EmbeddingEngine`, `RagStorage`, `TextChunker`), veri modellerini (`RagDocument`, `TextChunk`, `RagSearchResult`) ve Kotlin DSL builder yapılandırmasını içerir. Harici hiçbir ağır kütüphaneye dayanmaz, yalnızca Kotlin Coroutines kullanır.

### 2. `ragkit-storage-room` (Vektör Depolama)
Android Jetpack Room 2.7+ kullanarak SQLite üzerinde vektör saklama ve arama işlemlerini yönetir. Vektörleri hantal JSON metinleri yerine **Little-Endian binary BLOB** formatında saklayarak disk ve bellek kullanımını minimuma indirir.

### 3. `ragkit-embedding-mediapipe` (Cihaz İçi ML Motoru)
Google'ın **MediaPipe Tasks Text Embedder** altyapısını soyutlar. İlk açılışta modeli arka planda güvenle indiren (**Download-on-First-Use**) ve SHA-256 doğrulamasını yapan dahili bir mekanizmaya sahiptir; böylece ilk APK boyutunu şişirmez.

---

## ⚡ Karşılaşılan Mühendislik Zorlukları ve Çözümleri

### 1. Akıllı Metin Parçalama (Smart Text Chunking)
Uzun bir makale, sözleşme veya çok paragraflı bir not tek bir parça halinde vektörleştirilirse anlamsal ayrıntılar kaybolur.

RagKit, sliding-window (kayan pencere) mantığıyla çalışan bir `SimpleTextChunker` algoritması içerir:
1. Metni önce **paragraf** sınırlarından (`\n\n`) böler.
2. Eğer bir paragraf izin verilen boyutu (`maxChunkSize`) aşıyorsa, **cümle sonu noktalama işaretlerinden** (`.`, `!`, `?`) böler.
3. Cümle de çok uzunsa kelime bütünlüğünü bozmadan **boşluklardan** böler.
4. Birbirini takip eden parçalar arasında ayarlanabilir bir **örtüşme payı (overlap)** bırakır (örneğin 50 karakter). Böylece parçalama sınırına denk gelen fikirler anlam kaybına uğramaz.

```kotlin
val chunker = SimpleTextChunker(
    maxChunkSize = 500, // Parça başına azami karakter
    chunkOverlap = 50   // Bitişik parçalar arasındaki örtüşme miktarı
)
```

---

### 2. SQLite İçinde Yüksek Hızlı Vektör Sorgusu
SQLite üzerinde yerel bir `HNSW` vektör indeksi bulunmadığı için binlerce vektörü mobil işlemciyi kilitlemeden taramak kritik bir optimizasyon gerektirir.

1. **Kompakt BLOB Dönüşümü**:
   Float dizilerini doğrudan byte dizisine çevirmek için `ByteBuffer` kullanılır:
   ```kotlin
   @TypeConverter
   fun fromFloatArray(array: FloatArray?): ByteArray? {
       if (array == null) return null
       val buffer = ByteBuffer.allocate(array.size * 4).order(ByteOrder.LITTLE_ENDIAN)
       for (f in array) buffer.putFloat(f)
       return buffer.array()
   }
   ```
2. **Arka Plan Dispatching**:
   Vektör taramaları ve kosinüs benzerliği hesaplamaları UI iş parçacığını (Main thread) engellememek için `Dispatchers.IO` üzerinde koşturulur. Modern ARMv8/ARMv9 işlemcilerde 5.000 parçalık bir veritabanını taramak **15 milisaniyenin altındadır**!

---

### 3. Android Bellek Yönetimi ve Model Boşaltma
Mobil cihazlarda arka plan bellek baskısı kaçınılmazdır. Sistem kritik bellek durumuna (`TRIM_MEMORY_RUNNING_CRITICAL`) girdiğinde, ağır bir ML modelini RAM'de tutmak uygulamanın işletim sistemi tarafından sonlandırılmasına (kill) yol açabilir.

RagKit bu durumu lifecycle-aware `close()` metodu ile çözer:
```kotlin
override fun onTrimMemory(level: Int) {
    if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
        ragKit.close() // MediaPipe C++ tensörlerini bellekten güvenle tahliye eder
    }
}
```

---

## 🚀 3 Satır Kod ile RagKit Entegrasyonu

RagKit'i herhangi bir Android projesine dahil etmek son derece basittir.

### 1. Bağımlılıkları Ekleyin
```kotlin
// build.gradle.kts
dependencies {
    implementation("io.github.gurkan26:ragkit-core:0.1.0")
    implementation("io.github.gurkan26:ragkit-storage-room:0.1.0")
    implementation("io.github.gurkan26:ragkit-embedding-mediapipe:0.1.0")
}
```

### 2. RagKit'i Başlatın
Kotlin DSL oluşturucusu ile:

```kotlin
val ragKit = RagKit.create(context) {
    embeddingEngine(MediaPipeEmbeddingEngine(context))
    storage(RoomRagStorage(context))
    config {
        maxChunkSize = 500
        chunkOverlap = 50
        minScore = 0.40f // Sadece %40 ve üzeri anlamsal benzerlikleri getir
    }
}

lifecycleScope.launch {
    ragKit.initialize().onSuccess {
        Log.d("RagKit", "Cihaz içi AI motoru hazır!")
    }
}
```

### 3. İndeksleyin ve Arayın
```kotlin
// 1. Herhangi bir metni metadata ile indeksleyin
val document = RagDocument(
    text = "Cuma günü saat 16:00'da takım sprint retrospektif toplantısı yapılacak.",
    source = "takvim",
    metadata = mapOf("departman" to "yazilim", "oncelik" to "yuksek")
)
ragKit.index(document)

// 2. Anlamsal arama yapın (birebir kelime eşleşmesi gerekmez!)
val results = ragKit.search("mühendislik değerlendirme görüşmesi ne zaman?")

results.getOrNull()?.forEach { result ->
    println("Eşleşme: ${result.text} (Benzerlik Skoru: %${(result.score * 100).toInt()})")
}
```

### 4. Jetpack Compose ile Reaktif Arama (Flow)
Modern Jetpack Compose mimarilerinde arama çubuğunu doğrudan reaktif bir `Flow` akışına bağlayabilirsiniz:

```kotlin
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
val searchResults = searchQuery
    .debounce(300)
    .flatMapLatest { query ->
        ragKit.searchFlow(query, limit = 10, minScore = 0.50f)
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
```
Veritabanına yeni bir not kaydedildiğinde veya güncellendiğinde, aktif arama sonuçları otomatik olarak yeniden hesaplanır ve Compose UI anında yenilenir!

---

## 📱 Canlı Demo: Android Emülatörü (Pixel 7 Pro) Testi

SDK'nın pratik çalışabilirliğini doğrulamak amacıyla geliştirdiğimiz Jetpack Compose örnek uygulaması üzerinde testler gerçekleştirdik:

| 1. Cihaz İçi İndeksleme & Durum | 2. Gerçek Zamanlı Anlamsal Arama |
| :---: | :---: |
| ![RagKit Home](https://raw.githubusercontent.com/Gurkan26/ragkit-android/main/docs/images/ragkit_home.png) | ![RagKit Search](https://raw.githubusercontent.com/Gurkan26/ragkit-android/main/docs/images/ragkit_search.png) |

Kullanıcı arama kutusuna `"butce"` yazdığında, sistem anlamsal yakınlık üzerinden şu eşleşmeleri tespit etti:
- **%85 Eşleşme**: *"Yarın sabah saat 10:00'da Q3 bütçe planlaması için yönetim kurulu ile strateji toplantısı yapılacak..."*
- **%84 Eşleşme**: *"Yazılım mimarisi gözden geçirme toplantısı..."*

Sıfır internet trafiği. Sıfır harici API anahtarı. Tam veri gizliliği.

---

## 📊 Performans ve Kaynak Tüketimi Özeti

| Metrik | Ölçüm Değeri (Pixel 7 Pro / Tensor G2) |
| :--- | :--- |
| **Model Yükleme Süresi** | ~120ms (dahili önbellekten) |
| **Vektör Üretim Süresi (Embedding)** | Cümle başına ~12ms |
| **Arama Süresi (1.000 Chunk)** | < 5ms (Bellek İçi Kosinüs Benzerliği) |
| **Bellek (RAM) Tüketimi** | ~35MB ek ayak izi |
| **Disk Alanı** | 100 boyutlu chunk başına ~400 byte BLOB |

---

## 🔮 RagKit İçin Yol Haritası (Roadmap)

Cihaz içi yapay zekâ mobil dünyada henüz emekleme aşamasında. RagKit için planladığımız gelecek geliştirmeler:
- **HNSW (Hierarchical Navigable Small World)**: 100.000+ vektör içeren büyük veri setleri için C++ tabanlı Yaklaşık En Yakın Komşu (ANN) indeksi.
- **Hibrit Arama (Hybrid Search)**: Klasik BM25 anahtar kelime araması ile Vektör aramasını birleştiren RRF (Reciprocal Rank Fusion) algoritması.
- **Kotlin Multiplatform (KMP)**: CoreML entegrasyonu ile aynı API'yi iOS ekosistemine taşımak.

---

## 🤝 Açık Kaynak ve Katkı Sağlama

RagKit tamamen açık kaynaklı olup **Apache 2.0 Lisansı** ile sunulmaktadır.

- ⭐️ **GitHub Reposu**: [https://github.com/Gurkan26/ragkit-android](https://github.com/Gurkan26/ragkit-android)
- 📦 **Detaylı Dokümantasyon**: Kurulum adımları ve mimari detaylar için [README](https://github.com/Gurkan26/ragkit-android#readme) dosyasını inceleyebilirsiniz.

Eğer bu proje ilginizi çektiyse ve Android ekosisteminde cihaz içi yapay zekâ çalışmalarını desteklemek isterseniz, **GitHub üzerinde projeye bir yıldız (⭐️) bırakabilir** ve deneyimlerinizi yorumlarda paylaşabilirsiniz!

---

*Yazar: Gürkan Şentürk — Android Developer & Open Source Enthusiast.*
