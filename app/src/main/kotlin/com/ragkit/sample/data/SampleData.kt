package com.ragkit.sample.data

import com.ragkit.core.model.RagDocument

/**
 * Predefined sample documents in Turkish across meeting, shopping, health, and travel categories.
 */
object SampleData {
    val initialNotes: List<RagDocument> = listOf(
        RagDocument(
            text = "Yarın sabah saat 10:00'da Q3 bütçe planlaması için yönetim kurulu ile strateji toplantısı yapılacak. Sunum slaytları hazır olmalı.",
            source = "toplantı",
            metadata = mapOf("category" to "İş", "priority" to "Yüksek")
        ),
        RagDocument(
            text = "Yazılım mimarisi gözden geçirme toplantısı Çarşamba günü saat 14:30'da çevrimiçi gerçekleştirilecek. Mikroservis geçişi ve API güvenliği ele alınacak.",
            source = "toplantı",
            metadata = mapOf("category" to "İş", "priority" to "Orta")
        ),
        RagDocument(
            text = "Sprint değerlendirme ve retrospektif toplantısı Cuma 16:00'da yapılacak. Yeni mobil sürümdeki kullanıcı geri bildirimleri tartışılacak.",
            source = "toplantı",
            metadata = mapOf("category" to "İş", "priority" to "Normal")
        ),
        RagDocument(
            text = "Haftalık market alışverişi listesi: 2 litre süt, organik yumurta, tam buğday ekmeği, sızma zeytinyağı, domates ve salatalık.",
            source = "alışveriş",
            metadata = mapOf("category" to "Ev", "priority" to "Yüksek")
        ),
        RagDocument(
            text = "Elektronik mağazasından alınacaklar: USB-C şarj adaptörü, Bluetooth kablosuz kulaklık ve çalışma masası için LED aydınlatma lambası.",
            source = "alışveriş",
            metadata = mapOf("category" to "Teknoloji", "priority" to "Düşük")
        ),
        RagDocument(
            text = "Kırtasiye alışveriş listesi: A5 boyutunda çizgili not defteri, fosforlu kalem seti, tükenmez kalem ve renkli yapışkanlı kağıtlar.",
            source = "alışveriş",
            metadata = mapOf("category" to "Ofis", "priority" to "Düşük")
        ),
        RagDocument(
            text = "Yıllık rutin kardiyoloji ve kan tahlili kontrolü için hastaneden Pazartesi günü saat 09:00'a randevu alındı. 8 saat aç gelinmesi gerekiyor.",
            source = "sağlık",
            metadata = mapOf("category" to "Kişisel", "priority" to "Kritik")
        ),
        RagDocument(
            text = "Her gün düzenli olarak en az 2 litre su tüketilmeli, akşamları 45 dakika tempolu yürüyüş yapılmalı ve D vitamini takviyesi alınmalı.",
            source = "sağlık",
            metadata = mapOf("category" to "Alışkanlık", "priority" to "Orta")
        ),
        RagDocument(
            text = "Diş hekimi randevusu gelecek Perşembe günü saat 15:00'te. Diş temizliği ve 6 aylık periyodik kontrol yapılacak.",
            source = "sağlık",
            metadata = mapOf("category" to "Kişisel", "priority" to "Yüksek")
        ),
        RagDocument(
            text = "Roma seyahat planı: Kolezyum ve Vatikan Müzesi biletleri önceden rezerve edilecek. Termini tren istasyonuna yakın otel tutuldu.",
            source = "seyahat",
            metadata = mapOf("category" to "Tatil", "priority" to "Yüksek")
        ),
        RagDocument(
            text = "Kapadokya hafta sonu kaçamağı için balon turu rezervasyonu yapılacak. Göreme Açık Hava Müzesi ve Uçhisar Kalesi gezilecek rotalar arasında.",
            source = "seyahat",
            metadata = mapOf("category" to "Gezi", "priority" to "Normal")
        ),
        RagDocument(
            text = "Yurt dışı uçuşu öncesi kontrol listesi: Pasaport geçerlilik süresi, Schengen vizesi, seyahat sağlık sigortası ve online check-in işlemi.",
            source = "seyahat",
            metadata = mapOf("category" to "Hazırlık", "priority" to "Kritik")
        )
    )
}
