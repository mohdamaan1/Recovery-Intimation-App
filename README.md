# Recovery Intimation - Enterprise Debt Management CRM 🏦📱

An offline-first, high-performance Android application engineered for bank recovery agents and financial collection officers. Built entirely with **Kotlin** and **Jetpack Compose** (Material 3), this CRM streamlines the debt recovery process through automated, tone-based SMS intimations.

---

## 🚀 Key Features

* **Smart Hero-Centric Dashboard:** A clean, expressive UI that prioritizes critical data—Account Numbers and Overdue Amounts—for quick decision-making.
* **Intelligent Tone-Based SMS Automation:** An underlying logic engine calculates the proximity to the due date and generates context-aware messages:
  * 🟢 **Polite Tone:** For upcoming payments (1-3 days away).
  * 🟠 **Urgent Tone:** For payments due today.
  * 🔴 **Strict Tone:** For overdue accounts, emphasizing legal implications.
* **Guarantor Liability Pressure:** Automatically generates and queues secondary warning messages for loan guarantors, a critical strategy in banking recovery.
* **Zero-Cloud Architecture (Privacy First):** Built with a strict offline-first philosophy. All sensitive financial records (Borrower Details, Loan Amounts) are stored locally using **Room Database**. No data is uploaded to external servers.
* **Custom Bank Branding:** Allows agents to sign off messages with their specific institution's name (e.g., SBI, HDFC, J&K Bank).

---

## 🛠️ Tech Stack & Architecture

This project demonstrates modern Android development practices:

* **Language:** Kotlin
* **UI Toolkit:** Jetpack Compose (Material 3 Expressive Design)
* **Local Storage:** Room Database (SQLite abstraction)
* **Asynchronous Programming:** Kotlin Coroutines & StateFlow
* **Architecture:** MVVM (Model-View-ViewModel) pattern
* **Background Tasks:** Android Services (for reliable SMS delivery)
* **Permissions Handling:** Accommodates strict Google Play policies using modern `ActivityResultContracts`.

  ---
## ⚙️ Installation & Setup

1. Clone the repository:
   ```bash
   git clone [https://github.com/mohdamaan1/Recovery-Intimation-App.git](https://github.com/mohdamaan1/Recovery-Intimation-App.git)
Open the project in Android Studio (Iguana or newer recommended).

Sync Gradle files.

Build and run the app on an emulator or physical device.

🔒 Permission Details
The app requests SEND_SMS and READ_PHONE_STATE strictly for its core CRM functions. The app uses user-confirmed dialogs before initiating any SMS actions. It does not request READ_SMS or WRITE_SMS to maintain user privacy.

Developed by Mohd Amaan (JustForPixel) Dedicated to building logical, high-utility Android solutions.

1. Clone the repository:
   ```bash
   git clone [https://github.com/mohdamaan1/Recovery-Intimation-App.git](https://github.com/mohdamaan1/Recovery-Intimation-App.git)
