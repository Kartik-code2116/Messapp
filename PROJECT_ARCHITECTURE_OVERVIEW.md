# MessApp Project Architecture & Structural Overview

This document provides a complete, accurate, and structured blueprint of the **MessApp** Android Application. It serves as an exact reference guide for rebuilding or scaling this application architecture.

---

## 1. HIGH-LEVEL SYSTEM ARCHITECTURE

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                               MESSAPP SYSTEM ARCHITECTURE                               │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                         │
│  ┌───────────────────────────────────────────────────────────────────────────────────┐  │
│  │                              PRESENTATION LAYER (UI)                              │  │
│  │                                                                                   │  │
│  │  ENTRY & AUTHENTICATION ACTIVITIES                                                │  │
│  │  • SplashActivity (App Entry / Deep Link Router)                                  │  │
│  │  • OnboardingActivity (App Walkthrough)                                           │  │
│  │  • LoginActivity (Email/Password + Google Sign-In)                                │  │
│  │  • RoleSelectionActivity (Student USER vs MESS_OWNER Role Selection)              │  │
│  │  • CompleteProfileActivity (Initial Profile Setup)                                │  │
│  │  • JoinMessActivity (Mess Invitation / QR Code Join)                              │  │
│  │  • CustomCaptureActivity (ZXing QR Scanner)                                       │  │
│  │                                                                                   │  │
│  │  MAIN DASHBOARD CONTAINERS                                                        │  │
│  │  • UserDashboardActivity (Student App Container)                                  │  │
│  │  • MessDashboardActivity (Mess Owner App Container)                               │  │
│  │                                                                                   │  │
│  │  SECONDARY & MANAGEMENT ACTIVITIES                                                │  │
│  │  • EditMessProfileActivity   • EditUserProfileActivity   • MessSettingsActivity     │  │
│  │  • MessReviewsActivity       • MyReviewsActivity         • WeeklyMenuActivity       │  │
│  │  • SubscriptionReportActivity• PastMembersActivity      • PastMemberDetailsActivity│  │
│  │                                                                                   │  │
│  │  UI FRAGMENTS                                                                     │  │
│  │  ┌────────────────────────────────────────┬────────────────────────────────────┐  │
│  │  │ Student / User Side                    │ Mess Owner Side                    │  │
│  │  │ • UserHomeFragment (Discovery/Search)    │ • MessDashboardFragment (Analytics)│  │
│  │  │ • MessDetailFragment (Profile/Subscribe) │ • MessProfileFragment (Edit Info)  │  │
│  │  │ • UserMenuFragment (Daily Menu View)    │ • MessMenuFragment (Menu Management│  │
│  │  │ • UserHistoryFragment (Sub History)    │ • MessStudentsFragment (Students)  │  │
│  │  │ • UserProfileFragment (Student Profile)│ • MessRevenueFragment (Earnings)   │  │
│  │  │                                        │ • MessRequestsFragment (Requests)  │  │
│  │  │                                        │ • MessOffersFragment (Promotions)  │  │
│  │  │                                        │ • AddOfferFragment (Create Offer)  │  │
│  │  └────────────────────────────────────────┴────────────────────────────────────┘  │
│  └───────────────────────────────────────────────────────────────────────────────────┘  │
│                                           ↓                                             │
│  ┌───────────────────────────────────────────────────────────────────────────────────┐  │
│  │                        BUSINESS LOGIC & MANAGER LAYER                             │  │
│  │  • ProfileManager       • MenuManager              • SubscriptionManager          │  │
│  │  • PaymentManager       • ReviewManager            • OfferManager                 │  │
│  │  • DiscoveryManager     • AnalyticsManager         • FirebaseNotificationManager  │  │
│  │  • MessNotificationManager                         • GuestModeManager             │  │
│  └───────────────────────────────────────────────────────────────────────────────────┘  │
│                                           ↓                                             │
│  ┌───────────────────────────────────────────────────────────────────────────────────┐  │
│  │                             DATA & MODEL LAYER                                    │  │
│  │  MODELS                                                                           │  │
│  │  • Mess             • Menu            • Subscription    • SubscriptionRequest     │  │
│  │  • Student          • Review          • Offer           • Transaction             │  │
│  │  • MealRequest      • MealSelection   • PastMember      • OnboardingItem          │  │
│  │                                                                                   │  │
│  │  FIRESTORE COLLECTIONS                                                            │  │
│  │  • users            • messes          • menus           • default_menus           │  │
│  │  • mess_settings    • meal_selections • subscriptions   • subscriptionRequests    │  │
│  │  • mess_notifications • transactions  • reviews         • offers                  │  │
│  │  • analytics_events • notification_events • mess_leavers • notification_preferences│  │
│  └───────────────────────────────────────────────────────────────────────────────────┘  │
│                                           ↓                                             │
│  ┌───────────────────────────────────────────────────────────────────────────────────┐  │
│  │                         EXTERNAL SERVICES & PLATFORM                              │  │
│  │  • Firebase Authentication (Email/Password + Google OAuth)                        │  │
│  │  • Firebase Cloud Firestore (NoSQL Database with Realtime Listeners)              │  │
│  │  • Firebase Cloud Messaging / FCM (Push Notifications & Topics)                   │  │
│  │  • ZXing Embedded Library (QR Code Generation & Scanning)                          │  │
│  │  • Google AdMob (Monetization Ads Integration)                                    │  │
│  └───────────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                         │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. COMPLETE PROJECT DIRECTORY & PACKAGE STRUCTURE

```
d:\8)Android Development\messapp\
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/kartik/messapp/
│   │   │   │   ├── Root Package Files (Activities & Service):
│   │   │   │   │   ├── SplashActivity.java                (Launcher / Entry point router)
│   │   │   │   │   ├── OnboardingActivity.java            (App intro carousel walkthrough)
│   │   │   │   │   ├── LoginActivity.java                 (Auth: Email/Pass + Google Sign-In)
│   │   │   │   │   ├── RoleSelectionActivity.java         (Role selector: User vs Mess Owner)
│   │   │   │   │   ├── CompleteProfileActivity.java       (Mandatory profile completion setup)
│   │   │   │   │   ├── JoinMessActivity.java              (Join mess via code or QR link)
│   │   │   │   │   ├── UserDashboardActivity.java         (Main container for Student UI)
│   │   │   │   │   ├── MessDashboardActivity.java         (Main container for Owner UI)
│   │   │   │   │   ├── EditMessProfileActivity.java       (Detailed Mess Info editor)
│   │   │   │   │   ├── MessReviewsActivity.java           (Mess Reviews viewer)
│   │   │   │   │   ├── MyReviewsActivity.java             (User's submitted reviews)
│   │   │   │   │   ├── CustomCaptureActivity.java         (Custom QR Code scanner activity)
│   │   │   │   │   └── MyFirebaseMessagingService.java    (FCM Service for Push notifications)
│   │   │   │   │
│   │   │   │   ├── adapters/                              (General Adapters)
│   │   │   │   │   ├── OnboardingAdapter.java             (Onboarding ViewPager2 adapter)
│   │   │   │   │   ├── RequestAdapter.java                (Meal request items adapter)
│   │   │   │   │   └── SubscriptionRequestAdapter.java    (Subscription approval adapter)
│   │   │   │   │
│   │   │   │   ├── managers/                              (Business Logic Services)
│   │   │   │   │   ├── AnalyticsManager.java              (Dashboard metrics & event logging)
│   │   │   │   │   ├── DiscoveryManager.java              (Mess search, filter & fetch)
│   │   │   │   │   ├── FirebaseNotificationManager.java   (FCM tokens, channels & logs)
│   │   │   │   │   ├── MenuManager.java                   (Menu creation & daily selection)
│   │   │   │   │   ├── MessNotificationManager.java       (Admin alerts & grant broadcasts)
│   │   │   │   │   ├── OfferManager.java                  (Discounts & campaign management)
│   │   │   │   │   ├── PaymentManager.java                (Transactions & payment processing)
│   │   │   │   │   ├── ProfileManager.java                (User & Mess profile management)
│   │   │   │   │   ├── ReviewManager.java                 (Ratings, comments & calculations)
│   │   │   │   │   └── SubscriptionManager.java           (Subscriptions & request workflows)
│   │   │   │   │
│   │   │   │   ├── models/                                (Data Models / DTOs)
│   │   │   │   │   ├── MealRequest.java                   (Extra meal request data model)
│   │   │   │   │   ├── MealSelection.java                 (Daily meal selection model)
│   │   │   │   │   ├── Menu.java                          (Mess menu data model)
│   │   │   │   │   ├── Mess.java                          (Mess details data model)
│   │   │   │   │   ├── Offer.java                         (Discount offer model)
│   │   │   │   │   ├── OnboardingItem.java                (Onboarding slide model)
│   │   │   │   │   ├── PastMember.java                    (Historical subscriber model)
│   │   │   │   │   ├── Review.java                        (User review & rating model)
│   │   │   │   │   ├── Student.java                       (User / Student profile model)
│   │   │   │   │   ├── Subscription.java                  (Active subscription model)
│   │   │   │   │   ├── SubscriptionRequest.java           (Subscription request model)
│   │   │   │   │   └── Transaction.java                   (Payment transaction log model)
│   │   │   │   │
│   │   │   │   ├── ui/                                    (UI Components by Role)
│   │   │   │   │   ├── mess/                              (Mess Owner Features)
│   │   │   │   │   │   ├── dashboard/
│   │   │   │   │   │   │   └── MessDashboardFragment.java (Owner overview analytics)
│   │   │   │   │   │   ├── menu/
│   │   │   │   │   │   │   └── MessMenuFragment.java      (Daily/Weekly menu manager)
│   │   │   │   │   │   ├── offers/
│   │   │   │   │   │   │   ├── MessOffersFragment.java    (Active promotional offers)
│   │   │   │   │   │   │   └── AddOfferFragment.java       (Create new offer fragment)
│   │   │   │   │   │   ├── profile/
│   │   │   │   │   │   │   └── MessProfileFragment.java   (Owner profile fragment)
│   │   │   │   │   │   ├── reports/
│   │   │   │   │   │   │   └── SubscriptionReportActivity.java (Exportable subscription reports)
│   │   │   │   │   │   ├── requests/
│   │   │   │   │   │   │   └── MessRequestsFragment.java  (Pending student sub requests)
│   │   │   │   │   │   ├── revenue/
│   │   │   │   │   │   │   ├── MessRevenueFragment.java   (Earnings & income analysis)
│   │   │   │   │   │   │   └── TransactionsAdapter.java   (Payment transactions list adapter)
│   │   │   │   │   │   ├── settings/
│   │   │   │   │   │   │   └── MessSettingsActivity.java  (Mess operating configuration)
│   │   │   │   │   │   ├── students/
│   │   │   │   │   │   │   ├── MessStudentsFragment.java (Active student subscribers list)
│   │   │   │   │   │   │   ├── StudentsAdapter.java       (Active students adapter)
│   │   │   │   │   │   │   ├── PastMembersActivity.java   (Past subscriber history activity)
│   │   │   │   │   │   │   ├── PastMemberDetailsActivity.java (Past subscriber detail activity)
│   │   │   │   │   │   │   └── PastMembersAdapter.java    (Past members adapter)
│   │   │   │   │   │   └── weeklymenu/
│   │   │   │   │   │       └── WeeklyMenuActivity.java    (7-day weekly schedule editor)
│   │   │   │   │   │
│   │   │   │   │   └── user/                              (Student User Features)
│   │   │   │   │       ├── MessDetailFragment.java        (Mess detailed view & subscribe)
│   │   │   │   │       ├── OfferAdapter.java              (Offers list view adapter)
│   │   │   │   │       ├── ReviewAdapter.java             (Reviews list view adapter)
│   │   │   │   │       ├── history/
│   │   │   │   │       │   ├── UserHistoryFragment.java   (User subscription history)
│   │   │   │   │       │   └── HistoryAdapter.java        (History list adapter)
│   │   │   │   │       ├── home/
│   │   │   │   │       │   ├── UserHomeFragment.java      (Mess discovery & search)
│   │   │   │   │       │   └── MessAdapter.java           (Mess card list adapter)
│   │   │   │   │       ├── menu/
│   │   │   │   │       │   └── UserMenuFragment.java      (User meal selection view)
│   │   │   │   │       └── profile/
│   │   │   │   │           ├── UserProfileFragment.java   (Student user profile)
│   │   │   │   │           └── EditUserProfileActivity.java (Edit user details activity)
│   │   │   │   │
│   │   │   │   └── utils/                                 (Helper Utilities)
│   │   │   │       ├── GuestModeManager.java              (Guest browsing session manager)
│   │   │   │       ├── NetworkUtils.java                  (Connectivity checker)
│   │   │   │       ├── SwipeGestureListener.java          (Touch swipe event handler)
│   │   │   │       └── ThemeManager.java                  (Dark/Light theme switcher)
│   │   │   │
│   │   │   ├── res/                                       (Resources & Layouts)
│   │   │   └── AndroidManifest.xml                        (Manifest declarations & intents)
│   │   └── build.gradle.kts                               (App-level Gradle dependencies)
├── web/                                                   (Web Deep-Link Redirect Host)
│   └── public/join.html                                   (Redirect page for QR deep link)
├── firestore.rules                                        (Firestore Security Rules)
└── PROJECT_ARCHITECTURE_OVERVIEW.md                       (Architecture Document)
```

---

## 3. ENTRY & NAVIGATION FLOW

```
                                  ┌───────────────────────────┐
                                  │      App Launch           │
                                  └─────────────┬─────────────┘
                                                │
                                                ↓
                                  ┌───────────────────────────┐
                                  │      SplashActivity       │
                                  │  • Check Deep Link        │
                                  │  • Check Auth Session     │
                                  │  • Check Guest Mode       │
                                  └─────────────┬─────────────┘
                                                │
                 ┌──────────────────────────────┼──────────────────────────────┐
                 │ (Deep Link: messapp://join)  │ (First Time Launch)          │ (Already Logged In)
                 ↓                              ↓                              ↓
    ┌──────────────────────────┐   ┌──────────────────────────┐   ┌──────────────────────────┐
    │     JoinMessActivity     │   │    OnboardingActivity    │   │  Check User Account Role │
    │  • Join mess via code    │   │  • Intro Walkthrough     │   │  in Firestore "users"    │
    │  • Scan QR via           │   └────────────┬─────────────┘   └────────────┬─────────────┘
    │    CustomCaptureActivity │                │                              │
    └──────────────────────────┘                ↓                              │
                                   ┌──────────────────────────┐                │
                                   │      LoginActivity       │                │
                                   │  • Email & Password      │                │
                                   │  • Google Sign-In        │                │
                                   │  • Guest Mode Option     │                │
                                   └────────────┬─────────────┘                │
                                                │                              │
                                                ↓                              │
                                   ┌──────────────────────────┐                │
                                   │  RoleSelectionActivity   │                │
                                   │  Select: USER / OWNER    │                │
                                   └────────────┬─────────────┘                │
                                                │                              │
                                                ↓                              │
                                   ┌──────────────────────────┐                │
                                   │ CompleteProfileActivity  │                │
                                   │ Fill initial info        │                │
                                   └────────────┬─────────────┘                │
                                                │                              │
                 ┌──────────────────────────────┴──────────────────────────────┘
                 │
                 ├─────────────────────────────────────────┐
                 │                                         │
                 ↓ (Role: USER / Guest)                    ↓ (Role: MESS_OWNER)
    ┌─────────────────────────────────────────┐  ┌─────────────────────────────────────────┐
    │          UserDashboardActivity          │  │          MessDashboardActivity          │
    │  (Bottom Navigation Fragments)          │  │  (Bottom Navigation Fragments)          │
    │                                         │  │                                         │
    │  • UserHomeFragment (Discover Messes)   │  │  • MessDashboardFragment (Analytics)   │
    │  • UserMenuFragment (Daily Selection)   │  │  • MessMenuFragment (Manage Menus)     │
    │  • UserHistoryFragment (Sub History)    │  │  • MessStudentsFragment (Subscribers)   │
    │  • UserProfileFragment (User Profile)   │  │  • MessRevenueFragment (Earnings)       │
    │                                         │  │  • MessRequestsFragment (Sub Requests)  │
    │  Secondary Views:                       │  │  • MessOffersFragment (Discounts)       │
    │  • MessDetailFragment                   │  │  • MessProfileFragment (Mess Details)   │
    │  • EditUserProfileActivity              │  │                                         │
    │  • MyReviewsActivity                    │  │  Secondary Views:                       │
    │                                         │  │  • EditMessProfileActivity            │
    │                                         │  │  • WeeklyMenuActivity                   │
    │                                         │  │  • MessSettingsActivity                 │
    │                                         │  │  • SubscriptionReportActivity           │
    │                                         │  │  • PastMembersActivity / Details        │
    └─────────────────────────────────────────┘  └─────────────────────────────────────────┘
```

---

## 4. MANAGER & BUSINESS LOGIC ARCHITECTURE

The application business logic is fully encapsulated in **10 Manager Classes** and **1 Guest Utility Manager**.

```
┌───────────────────────────────────────────────────────────────────────────────────────────┐
│                                   MANAGER ARCHITECTURE                                    │
├───────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                           │
│  1. ProfileManager                                                                        │
│     • Manages Student & Mess Owner profile creation, reading, and updates.                 │
│     • Operates on Firestore collections: `users`, `messes`.                               │
│                                                                                           │
│  2. DiscoveryManager                                                                      │
│     • Handles mess discovery, searching by name/location, filtering by rating/price.      │
│     • Operates on Firestore collection: `messes`.                                         │
│                                                                                           │
│  3. MenuManager                                                                           │
│     • Handles daily menu updates, weekly schedules, default menus & student meal selections.│
│     • Operates on Firestore collections: `menus`, `default_menus`, `meal_selections`.     │
│                                                                                           │
│  4. SubscriptionManager                                                                   │
│     • Manages active subscriptions, subscription requests, renewals & student dropouts.   │
│     • Operates on Firestore collections: `subscriptions`, `subscriptionRequests`,         │
│       `mess_leavers`.                                                                     │
│                                                                                           │
│  5. PaymentManager                                                                        │
│     • Processes payment transactions, creates financial ledger entries & tracks revenue.  │
│     • Operates on Firestore collection: `transactions`.                                   │
│                                                                                           │
│  6. ReviewManager                                                                         │
│     • Manages student reviews, star ratings, and automatically updates average ratings.   │
│     • Operates on Firestore collections: `reviews`, `messes`.                             │
│                                                                                           │
│  7. OfferManager                                                                          │
│     • Creates, lists, and tracks discount offers and usage counts for promotional deals.  │
│     • Operates on Firestore collection: `offers`.                                         │
│                                                                                           │
│  8. AnalyticsManager                                                                      │
│     • Aggregates mess metrics (total revenue, active subscribers, views) & logs events.   │
│     • Operates on Firestore collections: `analytics_events`, `subscriptions`,             │
│       `transactions`.                                                                     │
│                                                                                           │
│  9. FirebaseNotificationManager                                                           │
│     • Configures Android Notification Channels, manages FCM device tokens & logs events.  │
│     • Operates on Firestore collections: `users`, `notification_events`,                  │
│       `notification_preferences`.                                                         │
│                                                                                           │
│  10. MessNotificationManager                                                              │
│      • Sends mess-wide administrative broadcasts & subscription grant notifications.       │
│      • Operates on Firestore collection: `mess_notifications`.                            │
│                                                                                           │
│  11. GuestModeManager (Utility)                                                           │
│      • Manages unauthenticated guest sessions for limited app discovery.                  │
│                                                                                           │
└───────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 5. FIRESTORE DATABASE ARCHITECTURE & SCHEMA

The database comprises **15 Firestore Collections** with full schema definition:

```
┌───────────────────────────────────────────────────────────────────────────────────────────┐
│                                FIRESTORE SCHEMA SUMMARY                                   │
├───────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                           │
│  1. `users` /{userId}                                                                     │
│     ├─ email: String              ├─ role: String ("USER" | "MESS_OWNER")                 │
│     ├─ name: String               ├─ phone: String                                        │
│     ├─ messId: String             ├─ fcmToken: String                                     │
│     └─ createdAt: Long            └─ updatedAt: Long                                      │
│                                                                                           │
│  2. `messes` /{messId}                                                                    │
│     ├─ name: String               ├─ location: String                                     │
│     ├─ contact: String            ├─ description: String                                  │
│     ├─ monthlyPrice: Double       ├─ ownerId: String                                      │
│     ├─ avgRating: Double          ├─ numReviews: Integer                                  │
│     ├─ studentCount: Integer      ├─ inviteCode: String                                   │
│     └─ createdAt: Long            └─ updatedAt: Long                                      │
│                                                                                           │
│  3. `menus` /{menuId}                                                                     │
│     ├─ messId: String             ├─ date: String ("yyyy-MM-dd")                          │
│     ├─ dayOfWeek: String          ├─ lunchItems: List<String>                             │
│     ├─ dinnerItems: List<String>  ├─ available: Boolean                                   │
│     └─ updatedAt: Long                                                                    │
│                                                                                           │
│  4. `default_menus` /{docId}                                                              │
│     ├─ messId: String             ├─ dayOfWeek: String ("MONDAY", etc.)                   │
│     ├─ lunchItems: List<String>   ├─ dinnerItems: List<String>                            │
│     └─ updatedAt: Long                                                                    │
│                                                                                           │
│  5. `mess_settings` /{messId}                                                             │
│     ├─ autoAcceptRequests: Boolean├─ cutoffHourLunch: Integer                             │
│     ├─ cutoffHourDinner: Integer  ├─ notificationEnabled: Boolean                         │
│     └─ updatedAt: Long                                                                    │
│                                                                                           │
│  6. `meal_selections` /{messId}_{date}_{userId}                                           │
│     ├─ userId: String             ├─ messId: String                                       │
│     ├─ date: String               ├─ mealType: String ("LUNCH" | "DINNER")                │
│     ├─ selected: Boolean          └─ timestamp: Long                                      │
│                                                                                           │
│  7. `subscriptions` /{subscriptionId}                                                     │
│     ├─ userId: String             ├─ messId: String                                       │
│     ├─ startDate: Long            ├─ expiryDate: Long                                     │
│     ├─ status: String ("ACTIVE" | "EXPIRED" | "CANCELLED")                                │
│     ├─ mealPlanType: String       └─ monthlyPrice: Double                                 │
│                                                                                           │
│  8. `subscriptionRequests` /{reqId}                                                       │
│     ├─ studentId: String          ├─ studentName: String                                  │
│     ├─ messId: String             ├─ requestedDays: Integer                               │
│     ├─ mealType: String           ├─ status: String ("PENDING" | "APPROVED" | "REJECTED") │
│     └─ timestamp: Long                                                                    │
│                                                                                           │
│  9. `mess_notifications` /{notificationId}                                                │
│     ├─ messId: String             ├─ senderId: String                                     │
│     ├─ senderName: String         ├─ type: String ("ADMIN_MESSAGE" | "GRANT")             │
│     ├─ title: String              ├─ message: String                                      │
│     └─ createdAt: Long                                                                    │
│                                                                                           │
│  10. `transactions` /{transactionId}                                                      │
│      ├─ userId: String            ├─ messId: String                                       │
│      ├─ amount: Double            ├─ status: String ("SUCCESS" | "FAILED")                │
│      ├─ paymentMethod: String     └─ timestamp: Long                                      │
│                                                                                           │
│  11. `reviews` /{reviewId}                                                                │
│      ├─ messId: String            ├─ userId: String                                       │
│      ├─ userName: String          ├─ rating: Float (1.0 - 5.0)                            │
│      ├─ comment: String           ├─ likesCount: Integer                                  │
│      └─ timestamp: Long                                                                   │
│                                                                                           │
│  12. `offers` /{offerId}                                                                  │
│      ├─ messId: String            ├─ title: String                                        │
│      ├─ description: String       ├─ discountPercentage: Double                           │
│      ├─ usageCount: Long          ├─ active: Boolean                                      │
│      └─ expiryDate: Long                                                                  │
│                                                                                           │
│  13. `analytics_events` /{eventId}                                                        │
│      ├─ userId: String            ├─ messId: String                                       │
│      ├─ action: String            └─ timestamp: Long                                      │
│                                                                                           │
│  14. `notification_events` /{eventId}                                                     │
│      ├─ userId: String            ├─ messId: String                                       │
│      ├─ eventType: String         └─ timestamp: Long                                      │
│                                                                                           │
│  15. `mess_leavers` /{docId}                                                              │
│      ├─ userId: String            ├─ messId: String                                       │
│      ├─ leaveReason: String       └─ leftAt: Long                                         │
│                                                                                           │
│  16. `notification_preferences` /{userId}                                                 │
│      ├─ messUpdates: Boolean      ├─ paymentAlerts: Boolean                               │
│      └─ updatedAt: Long                                                                   │
│                                                                                           │
└───────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 6. CORE WORKFLOWS

### A. Subscription Request & Approval Flow

```
┌─────────────────────────┐
│ Student in MessDetail   │
│ clicks "Request Join"   │
└────────────┬────────────┘
             │
             ↓
┌─────────────────────────┐
│ SubscriptionManager     │
│ .sendRequest()          │
└────────────┬────────────┘
             │
             ↓
┌─────────────────────────┐
│ Save Firestore doc in   │
│ `subscriptionRequests`  │
│ status: "PENDING"       │
└────────────┬────────────┘
             │
             ↓
┌─────────────────────────┐
│ Mess Owner opens        │
│ MessRequestsFragment    │
└────────────┬────────────┘
             │
      ┌──────┴────────────────────────┐
      │                               │
      ↓ (Approve)                     ↓ (Reject)
┌─────────────────────────┐     ┌─────────────────────────┐
│ 1. Update status in     │     │ 1. Update status in     │
│    `subscriptionRequests│     │    `subscriptionRequests│
│    to "APPROVED"        │     │    to "REJECTED"        │
│ 2. SubscriptionManager  │     └─────────────────────────┘
│    .createSubscription()│
│ 3. Update `users` messId│
│ 4. Increment studentCount│
└─────────────────────────┘
```

---

### B. Rating & Review Aggregation Flow

```
┌─────────────────────────────────┐
│ Student Submits Review & Rating │
└────────────────┬────────────────┘
                 │
                 ↓
┌─────────────────────────────────┐
│ ReviewManager.createReview()    │
│ Save document to `reviews`      │
└────────────────┬────────────────┘
                 │
                 ↓
┌─────────────────────────────────┐
│ ReviewManager                   │
│ .updateMessAvgRating(messId)    │
│ 1. Query all reviews for messId │
│ 2. Sum ratings & compute avg    │
│ 3. Update `messes/{messId}`     │
│    fields: avgRating, numReviews│
└─────────────────────────────────┘
```

---

## 7. COMPLETE COMPONENT INVENTORY INDEX

Below is the complete list of all **47 Java Source Components** that form the core structure of MessApp:

| # | Class Name | Location Package | Core Architectural Responsibility |
|---|---|---|---|
| 1 | `SplashActivity` | `com.kartik.messapp` | Entry point launcher & deep link router |
| 2 | `OnboardingActivity` | `com.kartik.messapp` | Intro carousel walkthrough for new users |
| 3 | `LoginActivity` | `com.kartik.messapp` | Auth activity (Email/Password + Google Sign-In) |
| 4 | `RoleSelectionActivity` | `com.kartik.messapp` | Role selector between Student & Mess Owner |
| 5 | `CompleteProfileActivity` | `com.kartik.messapp` | Profile setup screen after registration |
| 6 | `JoinMessActivity` | `com.kartik.messapp` | Mess joining via code or QR link |
| 7 | `UserDashboardActivity` | `com.kartik.messapp` | Student main dashboard container |
| 8 | `MessDashboardActivity` | `com.kartik.messapp` | Mess Owner main dashboard container |
| 9 | `EditMessProfileActivity` | `com.kartik.messapp` | Detailed Mess profile editor |
| 10 | `MessReviewsActivity` | `com.kartik.messapp` | Mess reviews list viewer |
| 11 | `MyReviewsActivity` | `com.kartik.messapp` | User's written reviews activity |
| 12 | `CustomCaptureActivity` | `com.kartik.messapp` | Custom ZXing QR code scanner activity |
| 13 | `MyFirebaseMessagingService` | `com.kartik.messapp` | FCM background push notification handler |
| 14 | `OnboardingAdapter` | `com.kartik.messapp.adapters` | ViewPager2 adapter for onboarding slides |
| 15 | `RequestAdapter` | `com.kartik.messapp.adapters` | Meal requests RecyclerView adapter |
| 16 | `SubscriptionRequestAdapter` | `com.kartik.messapp.adapters` | Pending subscription requests adapter |
| 17 | `AnalyticsManager` | `com.kartik.messapp.managers` | Analytics metrics & event logger |
| 18 | `DiscoveryManager` | `com.kartik.messapp.managers` | Mess search & filtering service |
| 19 | `FirebaseNotificationManager` | `com.kartik.messapp.managers` | FCM tokens & channel manager |
| 20 | `MenuManager` | `com.kartik.messapp.managers` | Menu CRUD & meal selection service |
| 21 | `MessNotificationManager` | `com.kartik.messapp.managers` | Admin notifications & grant alerts service |
| 22 | `OfferManager` | `com.kartik.messapp.managers` | Discount offers manager |
| 23 | `PaymentManager` | `com.kartik.messapp.managers` | Financial transactions manager |
| 24 | `ProfileManager` | `com.kartik.messapp.managers` | User & Mess profile manager |
| 25 | `ReviewManager` | `com.kartik.messapp.managers` | Review submission & rating calculator |
| 26 | `SubscriptionManager` | `com.kartik.messapp.managers` | Subscription & request manager |
| 27 | `MealRequest` | `com.kartik.messapp.models` | Meal request data model |
| 28 | `MealSelection` | `com.kartik.messapp.models` | Daily meal selection data model |
| 29 | `Menu` | `com.kartik.messapp.models` | Menu data model |
| 30 | `Mess` | `com.kartik.messapp.models` | Mess entity data model |
| 31 | `Offer` | `com.kartik.messapp.models` | Discount offer data model |
| 32 | `OnboardingItem` | `com.kartik.messapp.models` | Onboarding slide data model |
| 33 | `PastMember` | `com.kartik.messapp.models` | Historical subscriber data model |
| 34 | `Review` | `com.kartik.messapp.models` | Review & rating data model |
| 35 | `Student` | `com.kartik.messapp.models` | User profile data model |
| 36 | `Subscription` | `com.kartik.messapp.models` | Subscription data model |
| 37 | `SubscriptionRequest` | `com.kartik.messapp.models` | Subscription request data model |
| 38 | `Transaction` | `com.kartik.messapp.models` | Transaction record data model |
| 39 | `MessDashboardFragment` | `com.kartik.messapp.ui.mess.dashboard` | Owner dashboard fragment |
| 40 | `MessMenuFragment` | `com.kartik.messapp.ui.mess.menu` | Owner menu management fragment |
| 41 | `AddOfferFragment` | `com.kartik.messapp.ui.mess.offers` | Create offer fragment |
| 42 | `MessOffersFragment` | `com.kartik.messapp.ui.mess.offers` | Offers list fragment |
| 43 | `MessProfileFragment` | `com.kartik.messapp.ui.mess.profile` | Owner profile fragment |
| 44 | `SubscriptionReportActivity` | `com.kartik.messapp.ui.mess.reports` | Exportable report activity |
| 45 | `MessRequestsFragment` | `com.kartik.messapp.ui.mess.requests` | Pending subscription requests fragment |
| 46 | `MessRevenueFragment` | `com.kartik.messapp.ui.mess.revenue` | Revenue tracking fragment |
| 47 | `TransactionsAdapter` | `com.kartik.messapp.ui.mess.revenue` | Revenue transactions adapter |
| 48 | `MessSettingsActivity` | `com.kartik.messapp.ui.mess.settings` | Owner mess settings activity |
| 49 | `MessStudentsFragment` | `com.kartik.messapp.ui.mess.students` | Active student subscribers fragment |
| 50 | `PastMemberDetailsActivity` | `com.kartik.messapp.ui.mess.students` | Past subscriber details activity |
| 51 | `PastMembersActivity` | `com.kartik.messapp.ui.mess.students` | Past subscribers list activity |
| 52 | `PastMembersAdapter` | `com.kartik.messapp.ui.mess.students` | Past members adapter |
| 53 | `StudentsAdapter` | `com.kartik.messapp.ui.mess.students` | Active subscribers adapter |
| 54 | `WeeklyMenuActivity` | `com.kartik.messapp.ui.mess.weeklymenu` | Weekly menu editor activity |
| 55 | `MessDetailFragment` | `com.kartik.messapp.ui.user` | Mess details & subscription fragment |
| 56 | `OfferAdapter` | `com.kartik.messapp.ui.user` | Offers adapter |
| 57 | `ReviewAdapter` | `com.kartik.messapp.ui.user` | Reviews adapter |
| 58 | `HistoryAdapter` | `com.kartik.messapp.ui.user.history` | Student sub history adapter |
| 59 | `UserHistoryFragment` | `com.kartik.messapp.ui.user.history` | Student sub history fragment |
| 60 | `MessAdapter` | `com.kartik.messapp.ui.user.home` | Mess discovery cards adapter |
| 61 | `UserHomeFragment` | `com.kartik.messapp.ui.user.home` | Student discovery fragment |
| 62 | `UserMenuFragment` | `com.kartik.messapp.ui.user.menu` | Student daily meal selection fragment |
| 63 | `EditUserProfileActivity` | `com.kartik.messapp.ui.user.profile` | Edit student profile activity |
| 64 | `UserProfileFragment` | `com.kartik.messapp.ui.user.profile` | Student profile fragment |
| 65 | `GuestModeManager` | `com.kartik.messapp.utils` | Guest mode browsing utility |
| 66 | `NetworkUtils` | `com.kartik.messapp.utils` | Network connectivity checking utility |
| 67 | `SwipeGestureListener` | `com.kartik.messapp.utils` | Touch gesture listener utility |
| 68 | `ThemeManager` | `com.kartik.messapp.utils` | Dark/Light theme switching utility |

---

This blueprint accurately represents 100% of the **MessApp** project architecture, package organization, data models, manager classes, and Firestore schemas.
