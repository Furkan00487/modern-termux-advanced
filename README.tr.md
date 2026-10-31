# Modern Termux

[![Sürüm](https://img.shields.io/github/v/release/Furkan00487/modern-termux?label=s%C3%BCr%C3%BCm&color=00E5FF)](https://github.com/Furkan00487/modern-termux/releases/latest)
[![Release Derleme](https://github.com/Furkan00487/modern-termux/actions/workflows/release_build.yml/badge.svg)](https://github.com/Furkan00487/modern-termux/actions/workflows/release_build.yml)
[![Birim Testleri](https://github.com/Furkan00487/modern-termux/actions/workflows/run_tests.yml/badge.svg)](https://github.com/Furkan00487/modern-termux/actions/workflows/run_tests.yml)
[![Lisans: GPL v3](https://img.shields.io/badge/lisans-GPL--3.0-blue.svg)](LICENSE)

🌐 **[English](README.md)** | **[Türkçe](README.tr.md)**

---

**Modern Termux**, [Termux](https://github.com/termux/termux-app) Android terminal emülatörü ve Linux ortamının gelişmiş bir çatalıdır (fork). Dahili X11 grafik desteği, sıfır boşta CPU tüketimi sağlayan olay güdümlü yaşam döngüsü, çekmecede özel X11 kısayolları, kullanıma hazır ses ve mikrofon altyapısı ve resmi imzalı kararlı sürüm paketleriyle birlikte gelir.

> Bu proje, [termux/termux-app](https://github.com/termux/termux-app) projesinin bir çatalıdır. Terminal emülasyon motoru, Linux kabuk ortamı ve paket ekosistemi orijinal upstream Termux projesine ve katkıda bulunanlarına aittir.

---

## İçindekiler

- [Neden Modern Termux?](#neden-modern-termux)
- [Temel Özellikler](#temel-özellikler)
- [Hızlı Başlangıç: X11 Grafik Masaüstü](#hızlı-başlangıç-x11-grafik-masaüstü)
- [Ses ve Mikrofon Yapılandırması](#ses-ve-mikrofon-yapılandırması)
- [Kullanıcı Arayüzü ve Çekmece Kısayolları](#kullanıcı-arayüzü-ve-çekmece-kısayolları)
- [Dahili CLI Komutları](#dahili-cli-komutları)
- [Kurulum](#kurulum)
- [İndirilen Dosyayı Doğrulama](#indirilen-dosyayı-doğrulama)
- [Sürüm Yükseltme ve Kaldırma](#sürüm-yükseltme-ve-kaldırma)
- [Kaynaktan Derleme](#kaynaktan-derleme)
- [Hata Ayıklama (Debugging)](#hata-ayıklama-debugging)
- [Katkıda Bulunma](#katkıda-bulunma)
- [Bağlantılar ve Kaynaklar](#bağlantılar-ve-kaynaklar)
- [Lisans](#lisans)

---

## Neden Modern Termux?

| Yetenek / Özellik | Standart Termux | Modern Termux |
| :--- | :--- | :--- |
| **X11 Grafik Desteği** | Ayrı bir Termux:X11 APK'sı kurmayı gerektirir | **Tek uygulamada dahili `:x11` motoru** |
| **Durum Çubuğu İkonu** | Sabit terminal ikonu (`>_`) | **Dinamik ikon**: Grafik ekrandayken otomatik `X_` olur, terminalde `>_` simgesine döner |
| **Pil ve İşlemci Verimliliği** | 200 ms aralıklı `serverWatchdog` döngüsüyle işlemciyi sürekli uyandırır | **Olay güdümlü (Event-driven)**: Watchdog kaldırıldı, %0 arka plan yükü |
| **X11 Yönetimi ve Ayarlar** | Manuel geçiş, ayrı uygulama ayar menüsü | **Çekmecede tek tıkla geçiş** + **Uzun basarak anında ayarlar** |
| **Klavye Düzenleri (XKB)** | Harici paket kurulumu veya manuel ayar gerektirir | **Dahili XKB arşivi (`xkb.zip`)** ilk açılışta otomatik kurulur |
| **Mikrofon ve Ses** | İzin sorunları, harici script arayışı | **Doğrudan `RECORD_AUDIO` izni** + `termux-setup-microphone` scripti |
| **Görsel Tema ve Vurgu** | Standart koyu gri/siyah tema | **Neon Camgöbeği (`#00E5FF`)** modern tema ve simgeler |
| **Paket İmzası** | Genellikle imzasız veya genel test anahtarlarıyla sunulur | **GitHub Actions üzerinde** özel Release Keystore ile çift imzalı (v2 & v3) |
| **Açılış Mesajı (MOTD)** | Standart İngilizce metin | **Modern Türkçe MOTD**, her sürüm yükseltmesinde otomatik eşitlenir |

---

## Temel Özellikler

- **Dahili X11 Ekran Sunucusu:** Termux:X11 motoru doğrudan uygulamanın içerisine gömülmüştür. Yan uygulama kurma, izin karmaşası veya imza uyuşmazlığı yaşanmaz.
- **Dahili GPU Donanım Hızlandırma Desteği:** X11 Tercihlerinde varsayılan olarak açık gelen GPU köprüsü ile uygulamalar (Firefox, Chromium, 3D oyunlar, Blender, MPV) telefonun yerel Mali/Adreno GPU'sundan (Vulkan/Zink) tam performans alır.
- **Dinamik Bildirim Çubuğu Simgesi:** Kalıcı servis bildirimi mevcut çalışma alanınızı gösterir: X11 ekranı aktifken Neon Cyan `X_`, terminal oturumlarında ise Neon Cyan `>_` simgesi görüntülenir.
- **Pil Dostu Olay Güdümlü Yaşam Döngüsü:** Termux:X11'deki işlemciyi yoran 200 milisaniyelik `serverWatchdog` yoklama döngüsü tamamen kaldırılmıştır. `onActivityResumed` ve `onActivityPaused` yaşam döngüsüyle sıfır ek yükle çalışır.
- **Kullanıma Hazır Ses ve Mikrofon:** Uygulama `RECORD_AUDIO` iznine sahiptir; ses kaydı ve çalma işlemleri için `termux-setup-microphone` komutu dahili olarak sunulur.
- **Önceden Paketlenmiş XKB Klavye Yapılandırması:** X11 klavye haritaları doğrudan varlıklar (assets) içerisine gömülmüştür ve `$PREFIX/share/` altına otomatik açılır.
- **İmzalı ve Güvenli Tekil Sürümler:** Her sürüm GitHub Actions üzerinde arm64-v8a mimarisine özel olarak derlenir ve v2/v3 şemalarıyla doğrulanır.

---

## Hızlı Başlangıç: X11 Grafik Masaüstü

Modern Termux ile XFCE4 gibi tam donanımlı Linux masaüstü ortamlarını doğrudan çalıştırabilirsiniz.

### 1. Masaüstü Ortamını Kurun

Terminalde şu komutları çalıştırın:

```bash
pkg update
pkg install xfce4 xfce4-terminal
```

### 2. Dahili X11 Sunucusunu Başlatın

X11 sunucusunu arka planda başlatın:

```bash
termux-x11 :0 &
```

### 3. Masaüstü Oturumunu Çalıştırın

Ekran değişkenini tanımlayın ve XFCE oturumunu başlatın:

```bash
export DISPLAY=:0
startxfce4 &
```

### 4. Grafik Ekrana Geçin

- Ekranın sol kenarından sağa doğru kaydırarak **Gezinme Çekmecesini (Drawer)** açın.
- Masaüstü ortamınıza anında geçmek için Neon Cyan **X11 butonuna** (`X_`) dokunun.

---

## Ses ve Mikrofon Yapılandırması

Modern Termux yerleşik mikrofon iznine (`RECORD_AUDIO`) sahiptir. PulseAudio ve ALSA kullanarak hem terminal hem de X11 uygulamaları için çift yönlü (oynatma ve kayıt) ses altyapısı kurabilirsiniz.

### 1. Mikrofon İznini Verin

Dahili mikrofon yetkilendirme komutunu çalıştırın:

```bash
termux-setup-microphone
```

Açılan sistem iletişim kutusunda **İzin Ver** (Uygulamayı kullanırken) seçeneğini işaretleyin.

### 2. Ses Paketlerini Kurun

```bash
pkg install pulseaudio alsa-utils
```

### 3. PulseAudio Sunucusunu OpenSL ES ile Başlatın

Android donanımına doğrudan erişmek ve TCP köprüsü kurmak için PulseAudio'yu şu komutla çalıştırın:

```bash
pulseaudio --start \
    --load="module-native-protocol-tcp auth-ip-acl=127.0.0.1 auth-anonymous=1" \
    --load="module-sles-sink" \
    --load="module-sles-source" \
    --exit-idle-time=-1
```

> [!TIP]
> Bu komutu `~/.bashrc` dosyanıza veya X11 başlatma betiğinize ekleyerek her açılışta ses sunucusunun otomatik devreye girmesini sağlayabilirsiniz.

### 4. Ses Çalma ve Kayıt Testi

- **Mikrofon Kayıt Testi (ALSA):**
  5 saniyelik ses kaydı alın:
  ```bash
  arecord -D pulse -f cd -d 5 deneme.wav
  ```
- **Kayıt Oynatma Testi (ALSA):**
  Kaydettiğiniz sesi dinleyin:
  ```bash
  aplay -D pulse deneme.wav
  ```
- **PulseAudio Doğrudan Araçları:**
  `parec` ve `paplay` ile de test edebilirsiniz:
  ```bash
  parec --format=s16le --rate=44100 --channels=2 > ses_kaydi.raw
  paplay --format=s16le --rate=44100 --channels=2 ses_kaydi.raw
  ```

---

## Kullanıcı Arayüzü ve Çekmece Kısayolları

Terminalin sol gezinme çekmecesi X11 ortamı için özel kontroller içerir:

- **Çekmeceyi Açma:** Terminal ekranını sol kenardan sağa kaydırın veya çekmece çubuğuna dokunun.
- **X11 Butonuna Dokunma (Tek Tık):** Neon Cyan `X_` butonuna tek dokunuş grafik X11 ekranını anında ön plana getirir.
- **X11 Butonuna Uzun Basma (`onLongClick`):** `X_` butonuna basılı tuttuğunuzda doğrudan **X11 Tercihleri** (`LoriePreferences`) açılır. Buradan:
  - Ekran çözünürlüğü, ölçekleme ve DPI ayarlarını,
  - Dokunmatik modlarını (İzleme dörtgeni / Trackpad, Doğrudan Dokunma veya Sanal Fare),
  - Yazılımsal ve Donanımsal işleme (Renderer) seçeneklerini,
  - Sanal klavye ve ek tuş çubuğu davranışlarını yapılandırabilirsiniz.

---

## Dahili CLI Komutları

Modern Termux, `$PREFIX/bin` içerisine otomatik olarak şu araçları yerleştirir:

### `termux-x11`
Gömülü motor ve yerel kütüphaneleri kullanarak X11 görüntü sunucusunu başlatır:
```bash
termux-x11 :0 [seçenekler]
```

### `termux-x11-preference`
X11 ayarlarını doğrudan terminalden veya scriptler içerisinden okumanızı ve değiştirmenizi sağlar:
```bash
# Tüm ayarları ve mevcut değerleri listeleyin:
termux-x11-preference list

# Ayarları güncelleyin (örn. ölçekleme ve tam ekran):
termux-x11-preference displayScale:100 fullscreen:true
```

### `termux-x11-env`
X11 uygulamalarını telefonun donanım GPU'sunu (Vulkan/Zink köprüsü) kullanarak hızlandırılmış olarak başlatır:
```bash
# Donanım hızlandırmalı Firefox başlatın:
termux-x11-env firefox

# Genel herhangi bir 3D/X11 uygulamasını GPU ile çalıştırın:
termux-x11-env glxgears
```

### `termux-setup-microphone`
Android sistemine yayın (broadcast) göndererek mikrofon izni isteme iletişim kutusunu tetikler (`android.permission.RECORD_AUDIO`).

### `termux-motd-refresh`
**Modern Termux:MOTD** eklentisi kuruluysa anlık Pil, RAM, Depolama ve Çalışma Süresi bilgilerini içeren dinamik karşılama ekranını yeniler ve basar. Eklenti kurulu değilse kırmızı renkli açıklayıcı bir uyarı verir.

---

## Kurulum

Modern Termux, **Android 7.0 veya daha yeni** bir sürüm ve **`arm64-v8a`** işlemci mimarisi gerektirir.

1. [En Son Sürümler (Releases)](https://github.com/Furkan00487/modern-termux/releases/latest) sayfasını açın.
2. `termux-app_<surum>-apt-android-7-github-release_arm64-v8a.apk` ve ilgili `..._sha256sums` dosyasını indirin.
3. [İndirmeyi doğrulayın](#indirilen-dosyayı-doğrulama) ve APK dosyasını kurun.

### Diğer Termux Sürümleriyle Uyumluluk

Modern Termux kendine özel bir imza anahtarıyla imzalanmıştır. Android güvenlik politikası gereği F-Droid, Google Play veya resmi GitHub sürümleri üzerine doğrudan güncelleme yapılamaz (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`).

Geçiş yapmak için:
1. Gerekirse mevcut Termux verilerinizi [yedekleyin](https://wiki.termux.com/wiki/Backing_up_Termux).
2. Kurulu Termux uygulamasını ve varsa eklentilerini (Termux:API vb.) kaldırın.
3. Modern Termux APK'sını yükleyin.

---

## İndirilen Dosyayı Doğrulama

Dosyaların bulunduğu dizinde SHA-256 sağlama toplamını kontrol edin:

```bash
sha256sum -c termux-app_<surum>-apt-android-7-github-release_sha256sums
```

APK imzasını `apksigner` ile kontrol edin:

```bash
apksigner verify --verbose termux-app_<surum>-apt-android-7-github-release_arm64-v8a.apk
```

Çıktıda hem **v2** hem de **v3** imza şemalarının geçerli (`true`) olduğu görülmelidir:

```
Verified using v2 scheme (APK Signature Scheme v2): true
Verified using v3 scheme (APK Signature Scheme v3): true
```

---

## Sürüm Yükseltme ve Kaldırma

- **Sürüm Yükseltme:** Yeni bir Modern Termux APK'sını eskisinin üzerine kurduğunuzda verileriniz ve kurulu paketleriniz korunur. `versionCode` arttığında uygulama otomatik olarak "Başlangıç paketleri güncelleniyor…" diyaloğunu açar, MOTD'yi ve dahili scriptleri yeniler.
- **Kaldırma:** Cihazınızda **Ayarlar** -> **Uygulamalar** -> **Modern Termux** yolunu izleyerek uygulamayı kaldırabilirsiniz.

---

## Kaynaktan Derleme

```bash
# Alt modüllerle birlikte depoyu klonlayın
git clone --recursive https://github.com/Furkan00487/modern-termux.git
cd modern-termux

# Birim testlerini çalıştırın
./gradlew testDebugUnitTest

# Sürüm APK'sını derleyin
./gradlew downloadBootstraps assembleRelease
```

Yerel X11 bileşenleri alt modül (submodule) olarak bağlandığından depoyu `--recursive` parametresiyle klonlayın. CI üzerindeki sürüm imzalama adımı `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD` ve `KEYSTORE_ALIAS` secret'larını kullanır. Yerel derlemede bu anahtarlar bulunmadığında `assembleRelease` imzasız APK üretir; bu APK'yı cihazınıza yüklemeden önce kendi anahtarınızla imzalamanız gerekir.

---

## Hata Ayıklama (Debugging)

Modern Termux varsayılan olarak Türkçe arayüz kullanır. Sorun giderme günlüklerini toplamak için:

1. **Ayarları Açın:**
   - Ekranı sol kenardan kaydırıp dişli simgesine ⚙️ (**Ayarlar**) dokunun, ya da
   - Terminal ekranına basılı tutup **Ayarlar** seçeneğini belirleyin.
2. **Günlük Düzeyine Gidin:**
   - **Modern Termux** başlığına dokunun.
   - **Hata Ayıklama** seçeneğine dokunun.
   - **Günlük Kaydı** altında bulunan **Günlük Düzeyi** seçeneğini açın.
3. **Düzeyi Değiştirin:**
   - **Ayrıntılı (Verbose)** veya **Hata Ayıklama (Debug)** seçin.
   - İşlem bittikten sonra aşırı log yazımını ve pil tüketimini önlemek için yeniden **Normal** düzeyine getirin.

Günlükleri doğrudan Termux içerisinden incelemek için:
```bash
logcat -d | grep -i "com.termux"
```

Veya bağlı bir bilgisayardan ADB üzerinden:
```bash
adb logcat -s "termux:*"
```

---

## Katkıda Bulunma

- Değişikliklerinizi odaklı tutun ve PR açmadan önce `./gradlew testDebugUnitTest` komutunu çalıştırın.
- Commit mesajlarında [Conventional Commits](https://www.conventionalcommits.org) standardını kullanın (örn. `feat: dinamik bildirim ikonu eklendi`).
- Yeni bir sürüm hazırlarken `app/build.gradle` ve alt modül gradle dosyalarındaki `versionCode` (+1 artış) ve `versionName` (SemVer `major.minor.patch`) değerlerini güncelleyin.
- Modüler mimariyi korumak adına ortak yardımcı sınıfları `termux-shared` kütüphanesi içerisine yerleştirin.

---

## Bağlantılar ve Kaynaklar

- [Termux Wiki](https://wiki.termux.com/wiki/)
- [Termux Paket Deposu](https://github.com/termux/termux-packages)
- [Termux Dosya Sistemi Mimarisi](https://github.com/termux/termux-packages/wiki/Termux-file-system-layout)
- [RUN_COMMAND Intent ile Komut Çalıştırma](https://github.com/termux/termux-app/wiki/RUN_COMMAND-Intent)

---

## Lisans

Modern Termux, [GNU General Public License v3.0](LICENSE) altında lisanslanmıştır.
- [Terminal Emulator for Android](https://github.com/jackpal/Android-Terminal-Emulator) projesinden alınan terminal emülasyon kodları Apache 2.0 lisansına tabidir (`terminal-view` ve `terminal-emulator`).
- Diğer lisans detayları için `termux-shared/LICENSE.md` dosyasını inceleyebilirsiniz.
