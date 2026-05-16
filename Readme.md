<h1 align="center">MessApp 🍲</h1>

<p align="center">
  <strong>A dual-sided Android platform connecting students with mess (cafeteria) owners.</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green.svg" alt="Platform: Android">
  <img src="https://img.shields.io/badge/Language-Java_17-orange.svg" alt="Language: Java">
  <img src="https://img.shields.io/badge/Min_SDK-24-blue.svg" alt="Min SDK">
  <img src="https://img.shields.io/badge/Backend-Firebase-FFCA28.svg" alt="Backend">
</p>

---

## 📖 Overview

**MessApp** is an Android application designed to bridge the gap between students seeking reliable meal subscriptions and mess owners looking for an efficient management platform. It provides a seamless experience for students to discover, subscribe, and review local mess services, while empowering mess owners with a dashboard to manage daily menus, track subscribers, and analyze revenue.

---

## ✨ Key Features

### 🎓 For Students (Users)
* **Discover & Explore:** Browse nearby mess services with location, price, and rating filters.
* **Smart Subscriptions:** Subscribe to a mess with simulated payment handling and automatic tracking of active days.
* **Daily Menu Access:** View the specific lunch and dinner menus updated daily by the mess owner.
* **Review System:** Share experiences through a 5-star rating system with written comments to help other students.
* **Real-time Notifications:** Receive instant FCM push notifications regarding menu updates and special offers.
* **Guest Mode:** Explore the app and view available mess services without creating an account.

### 👨‍🍳 For Mess Owners (Admins)
* **Professional Dashboard:** A centralized hub to manage all mess operations seamlessly.
* **Daily Menu Management:** Update lunch (before 10 AM) and dinner (before 4 PM) menus easily.
* **Subscriber Tracking:** View a list of all active students, tracking overall member counts.
* **Business Analytics:** Monitor total revenue, subscriber trends, and popular dishes directly from the app.
* **Direct Communication:** Send global announcements and updates to all subscribers via push notifications.
* **Profile Management:** Keep business information, pricing, and descriptions up-to-date.

---

## 🛠️ Technology Stack

* **Language:** Java 17
* **Framework:** Android SDK (API 24 to 36)
* **Architecture:** Component-based UI with Fragment Navigation and ViewBinding
* **Backend Integration:**
  * **Firebase Authentication:** Secure Email/Password login.
  * **Firebase Cloud Firestore:** NoSQL database for real-time syncing of menus, reviews, and subscriptions.
  * **Firebase Cloud Messaging (FCM):** Topic-based push notifications.
  * **Firebase Storage:** Cloud storage for profile and mess images.
* **UI/UX Components:** 
  * Material Design 3 (MaterialCardView, BottomNavigationView, DrawerLayout, NavigationView)
  * Glide for efficient image caching and loading.
  * SwipeRefreshLayout for dynamic data refresh.

---

## 🏗️ Project Architecture

```
app/src/main/java/com/example/messapp/
├── adapters/           # RecyclerView Adapters for dynamic lists
├── fragments/          # Base/legacy UI fragments
├── managers/           # Business logic handlers (MenuManager, SubscriptionManager)
├── models/             # POJO Data classes (Mess, Subscription, Review)
├── ui/                 # UI layers grouped by role
│   ├── mess/           # Admin fragments (Dashboard, Students, Analytics)
│   └── user/           # Student fragments (Home, Menu, History, Profile)
├── utils/              # Helper utilities (ThemeManager)
├── *Activity.java      # Root level activities (Login, Dashboards, Splash, RoleSelection)
└── MyFirebaseMessagingService.java # FCM Service handler
```

---

## 🚀 Setup and Installation

### Prerequisites
1. **Android Studio** (Koala or newer recommended).
2. A valid **Firebase Project** configured with Firestore, Auth, Storage, and FCM.

### Steps to Run
1. **Clone the repository:**
   ```bash
   git clone https://github.com/yourusername/messapp.git
   ```
2. **Open the project** in Android Studio.
3. **Add Firebase Credentials:**
   * Download the `google-services.json` file from your Firebase console.
   * Place the file in the `app/` directory of the project.
4. **Sync Gradle:** Wait for Android Studio to download all dependencies.
5. **Run the App:** Click the run button (`Shift + F10`) to install the app on an emulator or physical device running Android 7.0 (API 24) or higher.

---

## 🗄️ Database Structure (Firestore)

The NoSQL database relies on the following core collections:
* `users/` - Stores user profiles and role definitions (`USER` or `MESS_OWNER`).
* `messes/` - Stores mess business profiles, ratings, and pricing.
* `menus/` - Daily menu items updated by owners.
* `subscriptions/` - Tracks active and expired subscriptions linking users to messes.
* `reviews/` - User-generated reviews tied to specific mess profiles.

---

## 🛡️ Security & Privacy
* Firestore Security Rules are implemented to ensure users can only modify their own profiles.
* Mess Owners have exclusive write access to their specific mess profiles and daily menus.
* Push notifications are securely routed using FCM topics based on subscription status.

---

## 🤝 Contributing
Contributions, issues, and feature requests are welcome! 
If you find a bug or have an idea, feel free to open an issue or submit a pull request.

## 📄 License
This project is for educational and portfolio purposes. All rights reserved.
