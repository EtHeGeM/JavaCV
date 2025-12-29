# 🚗 ALPR Tuning Tool - Plaka Tanıma Optimizasyon Aracı

![Java](https://img.shields.io/badge/Java-17-orange)
![OpenCV](https://img.shields.io/badge/OpenCV-4.9.0-blue)
![Tesseract](https://img.shields.io/badge/Tesseract-5.11.0-green)
![License](https://img.shields.io/badge/License-GPL--3.0-blue)

##  İçindekiler

- [Proje Hakkında](#-proje-hakkında)
- [Özellikler](#-özellikler)
- [Gereksinimler](#-gereksinimler)
- [Kurulum](#-kurulum)
- [Kullanım](#-kullanım)
- [Proje Yapısı](#-proje-yapısı)
- [Java Sınıfları ve Metodları](#-java-sınıfları-ve-metodları)
- [Parametreler ve Optimizasyon](#-parametreler-ve-optimizasyon)
- [Debug Çıktıları](#-debug-çıktıları)
- [İyileştirme Önerileri](#-iyileştirme-önerileri)
- [Katkıda Bulunanlar](#-katkıda-bulunanlar)

---

##  Proje Hakkında

**ALPR Tuning Tool**, plaka tanıma sistemlerinde kullanılan görüntü işleme parametrelerinin optimizasyonunu kolaylaştırmak için geliştirilmiş bir akademik projedir.

###  Önemli Not

> Bu proje **tam bir plaka tanıma sistemi değildir**. Asıl amacı, plaka tanıma sistemlerinde kullanılan:
> - Görüntü işleme parametrelerini (blur, canny, dilate vb.)
> - Tespit algoritmalarını (Haar Cascade, Geometric Detection)
> - OCR ayarlarını
> 
> **gerçek zamanlı olarak test etmek ve optimize etmek** için bir araç sunmaktır.

### Neden Bu Araca İhtiyaç Var?

Plaka tanıma sistemleri geliştirirken en zorlu kısım, farklı aydınlatma koşulları, kamera açıları ve plaka türleri için optimal parametreleri bulmaktır. Bu araç:

1. **Görsel Geri Bildirim**: Her parametre değişikliğinin etkisini anında görmenizi sağlar
2. **İkili Tespit Karşılaştırması**: Haar Cascade ve Geometrik tespit yöntemlerini yan yana karşılaştırır
3. **Otomatik Kayıt**: Ayarlarınızı otomatik kaydeder, böylece en iyi parametrelere geri dönebilirsiniz
4. **Debug Çıktıları**: Her işlem adımının görüntüsünü kaydeder

---

##  Özellikler

###  İkili Tespit Sistemi (Dual Detection)

| Yöntem | Açıklama | Renk Kodu |
|--------|----------|-----------|
| **Haar Cascade** | Önceden eğitilmiş cascade classifier ile hızlı tespit | 🟢 Yeşil |
| **Geometric Detection** | Kontur analizi ve aspect ratio filtreleme | 🔵 Mavi |
| **High Confidence** | Her iki yöntemin de tespit ettiği bölgeler | 🔴 Kırmızı |

### 🎛 Ayarlanabilir Parametreler

- **Bilateral Filter Kernel**: Görüntü yumuşatma
- **Canny Threshold 1 & 2**: Kenar tespiti hassasiyeti
- **Dilate Kernel & Iterations**: Morfolojik genişletme
- **Aspect Ratio (Min/Max)**: Plaka en-boy oranı filtresi

###  Ek Özellikler

- ✅ Gerçek zamanlı önizleme (6 farklı mod)
- ✅ OCR sonuçlarını görüntüleme
- ✅ Türk plaka formatı doğrulama
- ✅ Otomatik konfigürasyon kaydetme
- ✅ CSV formatında sonuç raporlama
- ✅ Batch işleme desteği

---

##  Gereksinimler

### Sistem Gereksinimleri

- **İşletim Sistemi**: Windows 10/11, Linux, macOS
- **Java**: JDK 17 veya üzeri
- **RAM**: Minimum 4GB (8GB önerilir)
- **Disk**: 500MB boş alan

### Yazılım Bağımlılıkları

| Bağımlılık | Versiyon | Açıklama |
|------------|----------|----------|
| OpenCV | 4.9.0 | Görüntü işleme kütüphanesi |
| Tess4J | 5.11.0 | Tesseract OCR Java wrapper |
| SLF4J | 2.0.9 | Loglama facade |
| Logback | 1.4.14 | Loglama implementasyonu |

---

##  Kurulum

### 1. Repoyu Klonlayın

```bash
git clone https://github.com/YOUR_USERNAME/JavaCV.git
cd JavaCV
```

### 2. Tesseract OCR Kurulumu

#### Windows
```bash
# Chocolatey ile:
choco install tesseract

# veya manuel olarak:
# https://github.com/UB-Mannheim/tesseract/wiki adresinden indirin
```

#### Linux (Ubuntu/Debian)
```bash
sudo apt-get update
sudo apt-get install tesseract-ocr tesseract-ocr-eng
```

#### macOS
```bash
brew install tesseract
```

### 3. Tessdata Dosyası

`eng.traineddata` dosyasının `tessdata/` klasöründe olduğundan emin olun:

```
JavaCV/
├── tessdata/
│   └── eng.traineddata   ← Bu dosya gerekli!
```

İndirmek için: [tessdata repository](https://github.com/tesseract-ocr/tessdata)

### 4. Maven ile Derleme

```bash
# Bağımlılıkları indir ve derle
mvn clean install

# Testleri atlamak için:
mvn clean install -DskipTests
```

### 5. Çalıştırma

#### GUI Modu (Tuning Tool)
```bash
mvn exec:java -Dexec.mainClass="com.alpr.TuningGUI"
```

#### CLI Modu (Batch İşleme)
```bash
# Tek görüntü işleme
mvn exec:java -Dexec.mainClass="com.alpr.Main" -Dexec.args="path/to/image.jpg"

# Klasördeki tüm görüntüleri işleme
mvn exec:java -Dexec.mainClass="com.alpr.Main" -Dexec.args="src/plates"
```

#### JAR Olarak Çalıştırma
```bash
# JAR oluştur
mvn package

# Çalıştır
java -jar target/license-plate-recognition-1.0-SNAPSHOT.jar
```

---

##  Kullanım

### GUI Arayüzü

1. **Load Image**: Test edilecek görüntüyü yükleyin
2. **Parametreleri Ayarlayın**: Slider'ları kullanarak parametreleri değiştirin
3. **Detect**: Plaka tespiti çalıştırın (Auto seçiliyse otomatik çalışır)
4. **Run OCR**: Tespit edilen plakalarda OCR çalıştırın
5. **Preview Mode**: Farklı işlem adımlarını görüntüleyin

### Önizleme Modları

| Mod | Açıklama |
|-----|----------|
| Original + Overlays | Orijinal görüntü + tespit kutuları |
| Grayscale | Gri tonlamalı görüntü |
| Filtered | Bilateral filter uygulanmış |
| Canny Edges | Kenar tespiti sonucu |
| Dilated | Morfolojik genişletme sonrası |
| Detection Result | Tüm tespit sonuçları |

### Klavye Kısayolları

- **Ctrl+O**: Görüntü yükle
- **Ctrl+Q**: Çıkış

---

##  Proje Yapısı

```
JavaCV/
├──  pom.xml                          # Maven yapılandırması
├──  alpr_config.properties           # Otomatik kaydedilen ayarlar
├──  alpr_summary.csv                 # Özet sonuç raporu
├──  haarcascade_russian_plate_number.xml  # Haar Cascade modeli
│
├──  src/
│   ├──  main/
│   │   ├──  java/com/alpr/
│   │   │   ├──  TuningGUI.java       # Ana GUI uygulaması
│   │   │   ├──  PlateDetector.java   # Plaka tespit motoru
│   │   │   ├──  OcrService.java      # Tesseract OCR servisi
│   │   │   ├──  DetectionResult.java # Tespit sonuç modeli
│   │   │   ├──  Main.java            # CLI giriş noktası
│   │   │   └──  TestImageGenerator.java # Test görüntüsü oluşturucu
│   │   └──  resources/
│   └──  plates/                      # Test görüntüleri klasörü
│
├──  tessdata/
│   └──  eng.traineddata              # Tesseract dil dosyası
│
├──  debug_output/                    # Debug görüntüleri
│   ├──  step1_grayscale/
│   ├──  step2_filtered/
│   ├──  step3_canny/
│   ├──  step3b_dilated/
│   ├──  step4_detected_plate/
│   ├──  step5_ocr_preprocessed/
│   ├──  haar_plates/                 # Haar ile tespit edilen plakalar
│   └──  geo_plates/                  # Geometrik tespit edilen plakalar
│
└──  target/                          # Maven build çıktıları
```

---

##  Java Sınıfları ve Metodları

### 1. TuningGUI.java

Ana GUI uygulaması - Swing tabanlı parametre ayarlama ve önizleme aracı.

#### Yapılandırma Metodları

| Metod | Açıklama |
|-------|----------|
| `loadConfig()` | `alpr_config.properties` dosyasından ayarları yükler |
| `saveConfig()` | Mevcut ayarları dosyaya kaydeder |
| `scheduleAutoSave()` | Debounce mekanizması ile otomatik kayıt zamanlar (1sn gecikme) |
| `applyConfig()` | Yüklenen ayarları UI bileşenlerine uygular |

#### UI Oluşturma Metodları

| Metod | Açıklama |
|-------|----------|
| `initializeUI()` | Ana pencere ve bileşenleri oluşturur |
| `createControlPanel()` | Sol taraftaki kontrol panelini oluşturur |
| `createPreviewPanel()` | Sağ taraftaki görüntü önizleme panelini oluşturur |
| `createStatusBar()` | Alt durum çubuğunu oluşturur |
| `createMenuBar()` | Menü çubuğunu oluşturur |
| `createSlider(min, max, value, majorTick, name)` | Parametre slider'ı oluşturur |
| `createSliderPanel(label, slider, valueLabel)` | Slider için etiketli panel oluşturur |
| `createTitledBorder(title)` | Başlıklı kenarlık oluşturur |
| `createLegendItem(text, color)` | Renk açıklaması etiketi oluşturur |

#### İşlem Metodları

| Metod | Açıklama |
|-------|----------|
| `loadImage()` | Dosya seçici ile görüntü yükler |
| `processImage()` | Plaka tespitini çalıştırır ve sonuçları günceller |
| `updatePreview()` | Seçilen moda göre önizlemeyi günceller |
| `runOcrOnAllDetections()` | Tüm tespit edilen plakalarda OCR çalıştırır |
| `calculateConfidence(text)` | OCR sonucunun güvenilirlik puanını hesaplar |
| `resetDefaults()` | Tüm parametreleri varsayılan değerlere döndürür |
| `clearOcrResults()` | OCR sonuç alanlarını temizler |
| `matToBufferedImage(mat)` | OpenCV Mat'ı BufferedImage'a dönüştürür |
| `checkResources()` | Haar Cascade dosyasının varlığını kontrol eder |

#### İç Sınıf: ImagePanel

| Metod | Açıklama |
|-------|----------|
| `setImage(image)` | Görüntülecek görüntüyü ayarlar |
| `setDetectionResults(results, showHaar, showGeo, showOverlap)` | Tespit sonuçlarını ve görünürlük ayarlarını belirler |
| `clearOverlays()` | Overlay'leri temizler |
| `paintComponent(g)` | Görüntü ve tespit kutularını çizer |
| `drawDetectionRect(g2d, rect, color, scale, offsetX, offsetY, label)` | Tek bir tespit dikdörtgeni çizer |

---

### 2. PlateDetector.java

Çift tespit sistemini içeren ana plaka tespit motoru.

#### Yapılandırma Metodları

| Metod | Açıklama |
|-------|----------|
| `createDebugDirectories()` | Debug çıktı klasörlerini oluşturur |
| `initializeHaarClassifier()` | Haar Cascade sınıflandırıcısını yükler |
| `saveDebugImage(stepDir, image, imageName)` | Debug görüntüsünü kaydeder |

#### Parametre Setter'ları

| Metod | Açıklama |
|-------|----------|
| `setBlurKernel(size)` | Bilateral filter kernel boyutunu ayarlar (tek sayı) |
| `setCannyThreshold1(threshold)` | Canny alt eşik değerini ayarlar |
| `setCannyThreshold2(threshold)` | Canny üst eşik değerini ayarlar |
| `setMinAspectRatio(ratio)` | Minimum en-boy oranını ayarlar |
| `setMaxAspectRatio(ratio)` | Maksimum en-boy oranını ayarlar |
| `setDilateKernelSize(size)` | Dilate kernel boyutunu ayarlar |
| `setDilateIterations(iterations)` | Dilate iterasyon sayısını ayarlar |
| `setHaarScaleFactor(factor)` | Haar ölçekleme faktörünü ayarlar |
| `setHaarMinNeighbors(neighbors)` | Haar minimum komşu sayısını ayarlar |

#### Parametre Getter'ları

| Metod | Açıklama |
|-------|----------|
| `getBlurKernel()` | Blur kernel değerini döndürür |
| `getCannyThreshold1()` | Canny threshold 1 değerini döndürür |
| `getCannyThreshold2()` | Canny threshold 2 değerini döndürür |
| `getMinAspectRatio()` | Minimum aspect ratio değerini döndürür |
| `getMaxAspectRatio()` | Maximum aspect ratio değerini döndürür |
| `getDilateKernelSize()` | Dilate kernel boyutunu döndürür |
| `getDilateIterations()` | Dilate iterasyon sayısını döndürür |
| `isHaarAvailable()` | Haar cascade'in kullanılabilir olup olmadığını döndürür |

#### Ara Görüntü Getter'ları

| Metod | Açıklama |
|-------|----------|
| `getOriginalImage()` | Orijinal yüklenen görüntüyü döndürür |
| `getLastGrayImage()` | Gri tonlamalı görüntüyü döndürür |
| `getLastFilteredImage()` | Filtrelenmiş görüntüyü döndürür |
| `getLastEdgeImage()` | Canny kenar görüntüsünü döndürür |
| `getLastDilatedImage()` | Dilate edilmiş görüntüyü döndürür |
| `getLastContourImage()` | Kontur görselleştirme görüntüsünü döndürür |
| `getLastResults()` | Son tespit sonuçlarını döndürür |
| `getCurrentImageName()` | Mevcut görüntü adını döndürür |

#### Görüntü İşleme Metodları

| Metod | Açıklama |
|-------|----------|
| `loadImage(imagePath)` | Görüntüyü dosyadan yükler |
| `preprocess()` | Görüntü ön işleme pipeline'ını çalıştırır |
| `preprocessImageWithOriginal(imagePath)` | Görüntüyü yükler, işler ve debug kayıtları oluşturur |

#### Tespit Metodları

| Metod | Açıklama |
|-------|----------|
| `detectAll()` | Her iki tespit yöntemini de çalıştırır ve sonuçları birleştirir |
| `detectWithHaar()` | Haar Cascade ile plaka tespiti yapar |
| `detectWithGeometric()` | Geometrik/Kontur analizi ile plaka tespiti yapar |
| `cropPlateWithPadding(rect, padding)` | Plaka bölgesini padding ile kırpar |
| `findHighConfidenceDetections()` | Her iki yöntemin de tespit ettiği bölgeleri bulur |

#### Perspektif Dönüşüm Metodları

| Metod | Açıklama |
|-------|----------|
| `fourPointTransform(image, pts)` | 4 noktalı perspektif dönüşümü uygular |
| `orderPoints(pts)` | Köşe noktalarını sıralar (sol-üst, sağ-üst, sağ-alt, sol-alt) |
| `indexOfMin(arr)` | Dizideki minimum değerin indeksini bulur |
| `indexOfMax(arr)` | Dizideki maksimum değerin indeksini bulur |
| `distance(p1, p2)` | İki nokta arasındaki mesafeyi hesaplar |

#### Görselleştirme Metodları

| Metod | Açıklama |
|-------|----------|
| `createContourVisualization()` | Tespit sonuçlarını renkli kutularla görselleştirir |
| `getDetectionStats()` | Tespit istatistiklerini [haar, geo, overlap] olarak döndürür |

---

### 3. OcrService.java

Tesseract OCR entegrasyonu ve plaka metni tanıma servisi.

#### Yapılandırma Metodları

| Metod | Açıklama |
|-------|----------|
| `initializeTesseract()` | Tesseract motorunu başlatır ve ayarları uygular |
| `findTessdataPath()` | tessdata klasörünü otomatik olarak bulur |
| `ensureDebugDir()` | Debug çıktı klasörünün varlığını garantiler |

#### OCR Metodları

| Metod | Açıklama |
|-------|----------|
| `recognizePlate(plateMat, imageName)` | Plaka görüntüsünde OCR çalıştırır ve sonucu döndürür |
| `recognizePlate(plateMat)` | Görüntü adı olmadan OCR çalıştırır |
| `preprocessForOcr(plate, imageName)` | OCR için görüntü ön işleme (resize, threshold) |
| `cleanResult(raw)` | OCR sonucunu temizler (büyük harf, alfanumerik) |

#### Yardımcı Metodlar

| Metod | Açıklama |
|-------|----------|
| `setTessdataPath(path)` | Tessdata yolunu ayarlar |
| `getTessdataPath()` | Mevcut tessdata yolunu döndürür |
| `setWhitelist(whitelist)` | Tanınacak karakter listesini ayarlar |
| `setLanguage(lang)` | OCR dilini ayarlar |
| `isWindows()` | İşletim sisteminin Windows olup olmadığını kontrol eder |
| `isMac()` | İşletim sisteminin macOS olup olmadığını kontrol eder |

---

### 4. DetectionResult.java

Tek bir plaka tespitini temsil eden veri modeli.

#### Enum: MethodType

| Değer | Açıklama |
|-------|----------|
| `HAAR` | Haar Cascade ile tespit edilmiş |
| `GEOMETRIC` | Geometrik analiz ile tespit edilmiş |

#### Constructor'lar

| Constructor | Açıklama |
|-------------|----------|
| `DetectionResult(bounds, method)` | Temel constructor (confidence = 1.0) |
| `DetectionResult(bounds, method, confidence)` | Confidence değeri ile constructor |

#### Getter/Setter Metodları

| Metod | Açıklama |
|-------|----------|
| `getBounds()` | Tespit dikdörtgenini (Rect) döndürür |
| `getMethod()` | Tespit yöntemini döndürür |
| `getConfidence()` | Güvenilirlik değerini döndürür |
| `getCroppedPlate()` | Kırpılmış plaka görüntüsünü döndürür |
| `setCroppedPlate(mat)` | Kırpılmış plaka görüntüsünü ayarlar |
| `getOcrResult()` | OCR sonucunu döndürür |
| `setOcrResult(text)` | OCR sonucunu ayarlar |

#### Hesaplama Metodları

| Metod | Açıklama |
|-------|----------|
| `calculateIoU(other)` | Başka bir tespit ile IoU hesaplar |
| `calculateIoU(r1, r2)` | (static) İki dikdörtgen arasında IoU hesaplar |
| `overlaps(other, threshold)` | Belirtilen eşiği aşan örtüşme olup olmadığını kontrol eder |

---

### 5. Main.java

Komut satırı arayüzü ve batch işleme için giriş noktası.

#### Ana Metodlar

| Metod | Açıklama |
|-------|----------|
| `main(args)` | Uygulama giriş noktası |
| `processDirectory(directory)` | Klasördeki tüm görüntüleri işler |
| `processAndPrintResult(imagePath)` | Tek bir görüntüyü işler ve sonucu yazdırır |
| `processImage(imagePath, expectedPlate)` | Tam ALPR pipeline'ını çalıştırır |

#### Yardımcı Metodlar

| Metod | Açıklama |
|-------|----------|
| `extractExpectedPlate(fileName)` | Dosya adından beklenen plakayı çıkarır |
| `countMatchingChars(expected, actual)` | Eşleşen karakter sayısını hesaplar |
| `calculateScore(text, expected)` | OCR sonucunun puanını hesaplar |

#### Raporlama Metodları

| Metod | Açıklama |
|-------|----------|
| `printFinalSummary()` | Konsola detaylı özet raporu yazdırır |
| `exportResultsToCSV()` | Sonuçları timestamped CSV dosyasına kaydeder |
| `exportSummaryRow(timestamp)` | Özet satırını alpr_summary.csv'ye ekler |
| `truncate(str, maxLen)` | Metni belirtilen uzunlukta keser |
| `getStatusSymbol(result)` | Sonuç durumu için sembol döndürür (✓, ~, ✗) |

---

### 6. TestImageGenerator.java

Test görüntüsü oluşturma utility sınıfı.

| Metod | Açıklama |
|-------|----------|
| `generateTestImage(outputPath)` | Sentetik test görüntüsü oluşturur (640x480, simüle plaka) |

---

## ️ Parametreler ve Optimizasyon

### Görüntü İşleme Pipeline'ı

```
Orijinal Görüntü
       ↓
1. Grayscale Dönüşüm
       ↓
2. CLAHE (Contrast Limited Adaptive Histogram Equalization)
       ↓
3. Bilateral Filter (blur kernel)
       ↓
4. Canny Edge Detection (threshold1, threshold2)
       ↓
5. Morphological Closing (21x5 kernel)
       ↓
6. Dilation (dilate kernel, iterations)
       ↓
7. Contour Analysis + Aspect Ratio Filtering
       ↓
Tespit Edilen Plakalar
```

### Parametre Önerileri

| Parametre | Varsayılan | Düşük Değer Etkisi | Yüksek Değer Etkisi |
|-----------|------------|--------------------|--------------------|
| **Blur Kernel** | 11 | Daha fazla gürültü | Detay kaybı |
| **Canny T1** | 50 | Daha fazla kenar | Daha az kenar |
| **Canny T2** | 150 | Daha fazla kenar | Daha az kenar |
| **Dilate Kernel** | 3 | İnce kenarlar | Kalın kenarlar |
| **Dilate Iter** | 2 | Boşluklar | Birleşen alanlar |
| **Min AR** | 2.0 | Kare bölgeler dahil | Sadece dikdörtgenler |
| **Max AR** | 7.0 | Dar plakalar hariç | Geniş bölgeler dahil |

### Türk Plaka Formatı

Türk plakaları şu formattadır: `[2 rakam][1-3 harf][2-4 rakam]`

Örnekler:
- `34ABC123` (İstanbul)
- `06XY1234` (Ankara)
- `35A12` (İzmir)

---

##  Debug Çıktıları

### Klasör Yapısı

| Klasör | İçerik |
|--------|--------|
| `step1_grayscale/` | Gri tonlamalı görüntüler |
| `step2_filtered/` | Bilateral filter sonrası |
| `step3_canny/` | Canny edge detection sonrası |
| `step3b_dilated/` | Dilation sonrası |
| `step4_detected_plate/` | Tespit edilen plaka bölgeleri |
| `step5_ocr_preprocessed/` | OCR için hazırlanmış görüntüler |
| `haar_plates/` | Haar ile tespit edilen plakalar |
| `geo_plates/` | Geometrik tespit edilen plakalar |

### CSV Çıktıları

1. **alpr_results_YYYYMMDD_HHMMSS.csv**: Detaylı sonuç raporu
2. **alpr_summary.csv**: Parametre karşılaştırma için özet satırları

---

##  İyileştirme Önerileri

### Kısa Vadeli

- [ ] GPU hızlandırma (CUDA) desteği
- [ ] Daha fazla Haar Cascade modeli ekleme
- [ ] Türkçe plaka için özel OCR eğitimi
- [ ] Real-time video akışı desteği

### Orta Vadeli

- [ ] Deep Learning tabanlı tespit (YOLO, SSD)
- [ ] Çoklu plaka eşzamanlı tespit
- [ ] Karakter segmentasyonu
- [ ] Plate recognition confidence scoring

### Uzun Vadeli

- [ ] REST API servisi
- [ ] Veritabanı entegrasyonu
- [ ] Web arayüzü
- [ ] Mobile uygulama

---

##  Katkıda Bulunanlar

**ALPR Academic Project Team:**

- Mert Özbay
- Defne Öktem
- Ata Atay
- Ayşe Ceren Sarıgül
- Aylin Baki
- Ahmad Ali al Ghazi

---

##  Lisans

Bu proje **GNU General Public License v3.0 (GPL-3.0)** altında lisanslanmıştır.

Bu lisans şunları sağlar:
- ✅ Ticari kullanım
- ✅ Değiştirme
- ✅ Dağıtım
- ✅ Patent kullanımı
- ✅ Özel kullanım

Koşullar:
- 📋 Kaynak kodu açık olmalı
- 📋 Lisans ve telif hakkı bildirimi dahil edilmeli
- 📋 Aynı lisans kullanılmalı (copyleft)
- 📋 Değişiklikler belirtilmeli

Detaylar için [LICENSE](LICENSE) dosyasına veya [GNU GPL v3.0](https://www.gnu.org/licenses/gpl-3.0.html) sayfasına bakın.

---

##  Sorun Giderme

### Sık Karşılaşılan Hatalar

#### "Haar Cascade file not found"
```bash
# Haar cascade dosyasının proje kök dizininde olduğundan emin olun
ls haarcascade_russian_plate_number.xml
```

#### "Tesseract not found"
```bash
# Windows için PATH'e ekleyin veya tessdata klasörünü kontrol edin
dir tessdata\eng.traineddata
```

#### "Could not load OpenCV"
```bash
# Maven bağımlılıklarını yeniden indirin
mvn clean install -U
```

### Destek

Sorunlar için GitHub Issues kullanın veya proje ekibiyle iletişime geçin.

---

<p align="center">
  <i>Plaka tanıma sistemlerinde optimizasyon için geliştirilmiştir.</i>
</p>
