# 📚 Shelfie — แอปคลังหนังสือส่วนตัว & อ่านหนังสือออนไลน์

<p align="center">
  <b>Shelfie</b> คือแอปพลิเคชัน Android สำหรับค้นหา บันทึก และอ่านหนังสือออนไลน์ ที่พัฒนาด้วย <b>Jetpack Compose</b> และ <b>Material Design 3</b> พร้อมเชื่อมต่อกับ <b>Open Library REST API</b> และ <b>Firebase Authentication</b>
</p>

---

## ✨ ฟีเจอร์หลัก (Key Features)

- **🔍 ค้นหาหนังสือออนไลน์ (Discover & Search)**
  - ค้นหาหนังสือจากฐานข้อมูล **Open Library API**
  - จัดหมวดหมู่หนังสือยอดนิยม เช่น นิยาย, พัฒนาตนเอง, เทคโนโลยี, วิทยาศาสตร์, ประวัติศาสตร์ ฯลฯ
  - แสดงผลเรื่องย่อ, ผู้แต่ง, และรูปภาพปกหนังสืออย่างสวยงาม

- **📖 โปรแกรมอ่านหนังสือออนไลน์ (E-Reader)**
  - ดึงเนื้อหาหนังสือจริงมาให้อ่านออนไลน์ผ่าน **Internet Archive API**
  - รองรับการปรับขนาดตัวอักษร (Small, Medium, Large, Extra Large)
  - ปรับโหมดสีการอ่านได้หลากหลาย (Light, Sepia, Dark)
  - บันทึกหน้าอ่านล่าสุดอัตโนมัติ สลับหน้าได้ง่ายดาย

- **📚 คลังหนังสือส่วนตัว (Personal Library)**
  - บันทึกหนังสือที่ชื่นชอบลงในคลัง
  - ติดตามสถานะการอ่าน: **อยากอ่าน (Want to Read)**, **กำลังอ่าน (Reading)**, **อ่านจบแล้ว (Finished)**
  - กรองและค้นหาหนังสือในคลังได้อย่างรวดเร็ว

- **🛒 ระบบตะกร้าสินค้า & ชำระเงินจำลอง (Cart & Checkout)**
  - เพิ่มหนังสือลงในตะกร้าคำนวณราคาสินค้า
  - จำลองการชำระเงินผ่านหลายช่องทาง (PromptPay, Credit/Debit Card, Mobile Banking)
  - ย้ายหนังสือเข้าคลังทันทีเมื่อชำระเงินสำเร็จ

- **🔐 ระบบบัญชีผู้ใช้ (Authentication & Multi-mode Auth)**
  - **Firebase Authentication:** เชื่อมต่อระบบสมัครสมาชิก/เข้าสู่ระบบด้วย อีเมลและรหัสผ่าน บนคลาวด์จริงเมื่อใส่ `google-services.json`
  - **Local Storage Mode:** สลับไปใช้การเก็บข้อมูลผู้ใช้ในเครื่องอัตโนมัติหากไม่มีไฟล์ `google-services.json` (เหมาะสำหรับการทดสอบ Offline)

- **🎨 การปรับแต่งและตั้งค่า (Profile & Preferences)**
  - แก้ไขข้อมูลส่วนตัว
  - สลับโหมดการแสดงผล (Light Theme / Dark Theme / System Default)
  - บันทึกการตั้งค่าทั้งหมดลงใน **DataStore Preferences**

---

## 🛠 เทคโนโลยีที่ใช้ (Tech Stack & Architecture)

| หมวดหมู่ | เทคโนโลยี / ไลบรารี |
| :--- | :--- |
| **Language** | [Kotlin](https://kotlinlang.org/) (JVM 17) |
| **UI Framework** | [Jetpack Compose](https://developer.android.com/jetpack/compose) + [Material Design 3](https://m3.material.io/) |
| **Architecture** | MVVM (Model-View-ViewModel) + Single Activity |
| **Async & State** | Kotlin Coroutines, `StateFlow`, `SharedFlow`, `collectAsStateWithLifecycle` |
| **Networking** | [Retrofit 2](https://square.github.io/retrofit/) + [OkHttp 3](https://square.github.io/okhttp/) + Gson |
| **Image Loading** | [Coil Compose](https://coil-kt.github.io/coil/compose/) |
| **Local Storage** | AndroidX [DataStore Preferences](https://developer.android.com/topic/libraries/architecture/datastore) |
| **Cloud Service** | [Firebase Authentication](https://firebase.google.com/docs/auth) (Firebase BoM `33.7.0`) |
| **Navigation** | [Navigation Compose](https://developer.android.com/jetpack/compose/navigation) |

---

## 📁 โครงสร้างโปรเจกต์ (Project Structure)

```text
com.example.shelfie
├── data/
│   ├── auth/              # ระบบยืนยันตัวตน (Firebase & Local Auth Repository)
│   ├── local/             # UserDataStore (การบันทึกข้อมูลในเครื่องด้วย DataStore)
│   ├── model/             # Data Models (Book, User, Profile, Category ฯลฯ)
│   └── remote/            # OpenLibraryApi & ArchiveTextSource (Retrofit)
├── ui/
│   ├── components/        # Reusable Compose Components (TopBar, BottomNav, BookCard ฯลฯ)
│   ├── screens/           # หน้าจอหลักทั้งหมด
│   │   ├── AuthScreen.kt          # หน้าเข้าสู่ระบบ / สมัครสมาชิก
│   │   ├── DiscoverScreen.kt      # หน้าค้นหาหนังสือ
│   │   ├── BookDetailScreen.kt    # หน้ารายละเอียดหนังสือ
│   │   ├── LibraryScreen.kt       # หน้าคลังหนังสือส่วนตัว
│   │   ├── CartScreen.kt          # หน้าตะกร้าสินค้า & ชำระเงิน
│   │   ├── ReaderScreen.kt        # หน้าอ่านหนังสือ
│   │   └── ProfileScreen.kt       # หน้าโปรไฟล์ & ตั้งค่า
│   ├── theme/             # Material3 Color, Typography, Shape, Theme
│   ├── AppViewModel.kt    # ViewModel กลางควบคุม State ทั้งหมดของแอป
│   └── ShelfieApp.kt      # Main Composables & Navigation Host
└── MainActivity.kt        # Entry Point ของแอปพลิเคชัน
```

---

## 🚀 วิธีการติดตั้งและรันโปรเจกต์ (Getting Started)

### สิ่งที่ต้องมีก่อนเริ่ม (Prerequisites)
- **Android Studio** (เวอร์ชัน Ladybug หรือใหม่กว่า)
- **JDK 17**
- **Android SDK Level 35** (รองรับ Minimum SDK 26 ขึ้นไป)

### ขั้นตอนการรันโปรเจกต์
1. Clone โปรเจกต์นี้ลงในเครื่องของคุณ:
   ```bash
   git clone https.github.com/THINDXR/Mobile-App.git
   cd Mobile-App
   ```
2. เปิดโปรเจกต์ด้วย **Android Studio**
3. รอให้ Gradle Sync เรียบร้อย
4. เลือก Emulator หรืออุปกรณ์จริง แล้วกด **Run (Shift + F10)**

### 🔑 การตั้งค่า Firebase Authentication (Option)
- แอปถูกออกแบบให้ทำงานได้ทันทีโดยไม่ต้องตั้งค่า Firebase (จะใช้ระบบ Local Auth ในเครื่อง)
- หากต้องการใช้ **Firebase Authentication** จริง:
  1. สร้างโปรเจกต์บน [Firebase Console](https://console.firebase.google.com/)
  2. เพิ่มแอป Android โดยระบุ Package Name เป็น `com.example.shelfie`
  3. ดาวน์โหลดไฟล์ `google-services.json` นำมาวางไว้ในโฟลเดอร์ `app/` (เช่น `app/google-services.json`)
  4. แอปจะสลับไปใช้ Firebase Authentication บนคลาวด์โดยอัตโนมัติ!

---

## 📝 License

Distributed under the MIT License. See `LICENSE` for more information.
