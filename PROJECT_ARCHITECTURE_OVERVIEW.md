# MessApp Project Architecture Overview

## 1. HIGH-LEVEL SYSTEM ARCHITECTURE

```
┌─────────────────────────────────────────────────────────────────┐
│                    MESSAPP ANDROID APPLICATION                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              USER INTERFACE LAYER                        │   │
│  │  ┌─────────────────────────────────────────────────────┐ │   │
│  │  │           Activities (Entry Points)                 │ │   │
│  │  │  • SplashActivity → LoginActivity → RoleSelection   │ │   │
│  │  │  • MainActivity (Router)                            │ │   │
│  │  │  • UserDashboardActivity                            │ │   │
│  │  │  • MessDashboardActivity                            │ │   │
│  │  │  • EditMessProfileActivity                          │ │   │
│  │  └─────────────────────────────────────────────────────┘ │   │
│  │                           ↓                              │   │
│  │  ┌─────────────────────────────────────────────────────┐ │   │
│  │  │         Fragments (UI Components)                   │ │   │
│  │  │  User Side:                                         │ │   │
│  │  │  • UserHomeFragment (Mess Discovery)                │ │   │
│  │  │  • MessDetailFragment (View Details/Reviews)        │ │   │
│  │  │  • UserMenuFragment (Menu View)                     │ │   │
│  │  │  • UserHistoryFragment (Subscription History)       │ │   │
│  │  │  • UserProfileFragment (User Profile)               │ │   │
│  │  │  • EditUserProfileActivity (Profile Editor)         │ │   │
│  │  │                                                     │ │   │
│  │  │  Mess Owner Side:                                   │ │   │
│  │  │  • MessProfileFragment (Edit Profile)               │ │   │
│  │  │  • MessMenuFragment (Manage Menu)                   │ │   │
│  │  │  • MessStudentsFragment (View Subscribers)          │ │   │
│  │  │  • MessDashboardFragment (Overview & Analytics)     │ │   │
│  │  │  • MessRevenueFragment (Revenue Tracking)           │ │   │
│  │  │  • MessRequestsFragment (Subscription Requests)     │ │   │
│  │  │  • MessOffersFragment (Manage Offers)               │ │   │
│  │  │  • AddOfferFragment (Create Offers)                 │ │   │
│  │  └─────────────────────────────────────────────────────┘ │   │
│  └──────────────────────────────────────────────────────────┘   │
│                           ↓                                     │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │          BUSINESS LOGIC LAYER (Manager Classes)          │   │
│  │  ┌─────────────────────────────────────────────────────┐ │   │
│  │  │  Profile    Menu       Subscription   Payment       │ │   │
│  │  │  Manager    Manager    Manager        Manager       │ │   │
│  │  │     ↓         ↓           ↓              ↓          │ │   │
│  │  │  Review     Offer      Discovery   Analytics        │ │   │
│  │  │  Manager    Manager    Manager      Manager         │ │   │
│  │  │     ↓         ↓           ↓              ↓          │ │   │
│  │  │         FirebaseNotificationManager                 │ │   │
│  │  └─────────────────────────────────────────────────────┘ │   │
│  └──────────────────────────────────────────────────────────┘   │
│                           ↓                                     │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │          DATA LAYER (Models & Firebase)                  │   │
│  │  ┌─────────────────────────────────────────────────────┐ │   │
│  │  │   Models              Firestore Collections         │ │   │
│  │  │   --------            ---------------------         │ │   │
│  │  │  • Mess               • messes/                     │ │   │
│  │  │  • Menu               • menus/                      │ │   │
│  │  │  • Subscription       • subscriptions/              │ │   │
│  │  │  • Review             • reviews/                    │ │   │
│  │  │  • Offer              • offers/                     │ │   │
│  │  │  • Student            • users/                      │ │   │
│  │  │  • Transaction        • transactions/               │ │   │
│  │  │                       • notification_events/        │ │   │
│  │  │                       • notification_preferences/   │ │   │
│  │  └─────────────────────────────────────────────────────┘ │   │
│  └──────────────────────────────────────────────────────────┘   │
│                           ↓                                     │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              EXTERNAL SERVICES                           │   │
│  │  • Firebase Authentication (Email/Password + Google)     │   │
│  │  • Firebase Firestore (NoSQL Database)                   │   │
│  │  • Firebase Cloud Messaging (Push Notifications)         │   │
│  │  • Firebase Storage (Optional for images)                │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. ACTIVITY FLOW DIAGRAM

```
┌─────────────────┐
│  SplashActivity │ (Shows for 2-3 seconds)
│   (Intro)       │
└────────┬────────┘
         │
         ↓
┌─────────────────────────────────────────────┐
│        LoginActivity                        │
│  • Email & Password Input                   │
│  • Google Sign-in Authentication            │
│  • Firebase Authentication                  │
│  • Role Check (User/Mess Owner)             │
└────────┬────────────────────────────────────┘
         │
         ↓
┌────────────────────────────────────────────────┐
│     RoleSelectionActivity                      │
│  • Select Role: User OR Mess Owner             │
│  • Store role in Firebase User Document        │
└────────┬───────────────────────────────────────┘
         │
         ├─────── USER ────────┬───────── MESS OWNER ────────┐
         │                     │                             │
         ↓                     ↓                             ↓
    ┌────────────┐    ┌──────────────────┐    ┌──────────────────────┐
    │ UserDash   │    │ MessDashboard    │    │ MessDashboardActivity│
    │ Activity   │    │ Activity         │    │ (Owner Dashboard)    │
    │            │    │                  │    │                      │
    │ Contains:  │    │ Contains:        │    │ Contains:            │
    │ • Home     │    │ • Profile        │    │ • Profile            │
    │ • Menu     │    │ • Menu           │    │ • Menu               │
    │ • History  │    │ • Students       │    │ • Students           │
    │ • Profile  │    │ • Revenue        │    │ • Revenue            │
    │ • Discover │    │ • Requests       │    │ • Requests           │
    │            │    │ • Offers         │    │ • Offers             │
    │            │    │ • Analytics      │    │ • Analytics          │
    └────────────┘    └──────────────────┘    └──────────────────────┘
         │                    │                             │
         └────────┬───────────┴─────────────────────────────┘
                  │
         ┌────────┴───────────────┐
         │                        │
         ↓                        ↓
    ┌──────────────────┐   ┌──────────────────────────────┐
    │ EditMessProfile  │   │ MyFirebaseMessagingService   │
    │ Activity (Opt)   │   │ (FCM Service)                │
    │                  │   │ • Receives Push Notifications│
    │ • Detailed edit  │   │ • Topic Subscriptions        │
    │ • Photos/Images  │   │ • Message Handling           │
    └──────────────────┘   └──────────────────────────────┘
```

---

## 3. MANAGER CLASSES & THEIR RELATIONSHIPS

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    MANAGER CLASSES INTERACTION DIAGRAM                  │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌──────────────────┐                                                   │
│  │ ProfileManager   │                                                   │
│  │──────────────────┤                                                   │
│  │ • fetchMessProf  │───────┐                                           │
│  │ • updateProfile  │       │                                           │
│  │ • getCurrentUser │       │ Reads/Writes                              │
│  │ • isComplete     │       │ Firestore                                 │
│  └──────────────────┘       │ "messes"                                  │
│                             │ collection                                │
│                             │                                           │
│  ┌──────────────────┐       │                                           │
│  │ MenuManager      │       │                                           │
│  │──────────────────┤       │                                           │
│  │ • createMenu     │───────┼──→ ┌─────────────────┐                    │
│  │ • getWeeklyMenu  │       │    │   FireStore     │                    │
│  │ • updateAvail    │       │    │                 │                    │
│  │ • addMealItems   │       │    │ Collections:    │                    │
│  └──────────────────┘       │    │ • messes        │                    │
│                             │    │ • menus         │                    │
│  ┌──────────────────┐       │    │ • subscriptions │                    │
│  │SubscriptionMngr  │       │    │ • reviews       │                    │
│  │──────────────────┤       │    │ • offers        │                    │
│  │ • createSub      │───────┤    │ • users         │                    │
│  │ • hasActiveSub   │       │    │ • transactions  │                    │
│  │ • renewSub       │       │    │ • events        │                    │
│  │ • cancelSub      │       │    └─────────────────┘                    │
│  └──────────────────┘       │                                           │
│                             │                                           │
│  ┌──────────────────┐       │                                           │
│  │ PaymentManager   │       │                                           │
│  │──────────────────┤       │                                           │
│  │ • processPayment │───────┤ (95% success rate)                        │
│  │ • getTransact    │       │ Creates subscription                      │
│  │ • calcRevenue    │       │ on success                                │
│  │ • getSubCount    │       │                                           │
│  └──────────────────┘       │                                           │
│         ↑                   │                                           │
│         │ Uses              │                                           │
│         │                   │                                           │
│  ┌──────────────────┐       │                                           │
│  │ ReviewManager    │       │                                           │
│  │──────────────────┤       │                                           │
│  │ • createReview   │───────┤ (Updates avg rating)                      │
│  │ • getReviews     │       │                                           │
│  │ • getAvgRating   │       │                                           │
│  │ • likeReview     │       │                                           │
│  │ • updateAvgRating│       │                                           │
│  └──────────────────┘       │                                           │
│                             │                                           │
│  ┌──────────────────┐       │                                           │
│  │ OfferManager     │       │                                           │
│  │──────────────────┤       │                                           │
│  │ • createOffer    │───────┤                                           │
│  │ • getOffers      │       │                                           │
│  │ • trackUsage     │       │                                           │
│  │ • isOfferValid   │       │                                           │
│  └──────────────────┘       │                                           │
│                             │                                           │
│  ┌──────────────────┐       │                                           │
│  │ DiscoveryManager │       │                                           │
│  │──────────────────┤       │                                           │
│  │ • searchMesses   │───────┤ (Client-side search)                      │
│  │ • advancedSearch │       │                                           │
│  │ • getTopRated    │       │                                           │
│  │ • getByPrice     │       │                                           │
│  │ • getPopular     │       │                                           │
│  └──────────────────┘       │                                           │
│                             │                                           │
│  ┌──────────────────┐       │                                           │
│  │ AnalyticsManager │       │                                           │
│  │──────────────────┤       │                                           │
│  │ • getDashboard   │───────┤ (Aggregates data)                         │
│  │ • getTotalSubs   │       │                                           │
│  │ • getRevenue     │       │                                           │
│  │ • trackPageView  │       │                                           │
│  └──────────────────┘       │                                           │
│                             │                                           │
│  ┌─────────────────────────┐│                                           │
│  │ FirebaseNotification    ││                                           │
│  │ Manager (Backbone)      ││                                           │
│  │─────────────────────────┤│                                           │
│  │ • createNotifChannel    ││                                           │
│  │ • getFCMToken           ││                                           │
│  │ • subscribeToTopic      ││─────┬─ All Managers                       │
│  │ • saveDeviceToken       ││     │ use FCM for                         │
│  │ • sendTestNotif         ││     │ notifications                       │
│  │ • logEvent              ││─────┘                                     │
│  │ • getNotifPreferences   ││                                           │
│  └─────────────────────────┘│                                           │
│                             │                                           │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 4. USER FLOW - DISCOVER & SUBSCRIBE

```
┌──────────────────────┐
│  User Opens App      │
└──────────┬───────────┘
           │
           ↓
┌──────────────────────────────────────────┐
│  UserHomeFragment                        │
│  ┌────────────────────────────────────┐  │
│  │ 1. DiscoveryManager.searchMesses() │  │
│  │    (or getAllMesses)               │  │
│  └────────────────────────────────────┘  │
└──────────┬───────────────────────────────┘
           │
           ↓
    ┌──────────────────┐
    │  RecyclerView    │
    │  displays messes │
    │  (item_mess_     │
    │   card.xml)      │
    └──────────┬───────┘
               │
               ↓ (User clicks on mess)
┌──────────────────────────────────────────┐
│  MessDetailFragment                      │
│  ┌────────────────────────────────────┐  │
│  │ 1. ProfileManager.fetchMessProfile │  │
│  │    (Mess name, location, etc)      │  │
│  │                                    │  │
│  │ 2. MenuManager.getWeeklyMenu()     │  │
│  │    (Lunch & dinner items)          │  │
│  │                                    │  │
│  │ 3. ReviewManager.getMessReviews()  │  │
│  │    (All reviews for this mess)     │  │
│  │                                    │  │
│  │ 4. SubscriptionManager             │  │
│  │    .hasActiveSubscription()        │  │
│  │    (Check if already subscribed)   │  │
│  └────────────────────────────────────┘  │
└──────────┬───────────────────────────────┘
           │
           ├─────── User Already Subscribed ─────┐
           │                                     │
           ↓                                     │
┌────────────────────────────────────┐           │
│ Show:                              │           │
│ ✓ "Unsubscribe" Button             │          │
│ ✓ Menu items                       │          │
│ ✓ All reviews                      │          │
│ ✓ Rating bar (for new review)      │          │
└────────────────────────────────────┘           │
           │                                     │
           │                    ┌────────────────┴─────────┐
           │                    │                          │
           │                    ↓                          │
           │        ┌──────────────────────────────────┐   │
           │        │ Show:                            │   │
           │        │ "Subscribe" Button               │   │
           │        │ (Locked) Menu items              │   │
           │        │ (Locked) Reviews                 │   │
           │        └──────────────────────────────────┘   │
           │                    │                          │
           │                    ↓ (User clicks Subscribe)  │
           │        ┌──────────────────────────────────┐   │
           │        │ PaymentManager.processPayment()  │   │
           │        │ (Simulated 95% success)          │   │
           │        └──────────────────────────────────┘   │
           │                    │                          │
           │                    ├─ SUCCESS ────┐           │
           │                    │              │           │
           │                    ↓              ↓           │
           │        ┌──────────────────┐ ┌──────────────┐  │
           │        │ SubscriptionMngr │ │ Analytics    │  │
           │        │ .createSub()     │ │ .getDashboard│  │
           │        └──────────────────┘ └──────────────┘  │
           │                    │                          │
           │                    ↓                          │
           │        ┌──────────────────────────────────┐   │
           │        │ FCM Topic Subscribe              │   │
           │        │ mess_{messId}                    │   │
           │        └──────────────────────────────────┘   │
           │                    │                          │
           │                    ↓                          │
           │        ┌──────────────────────────────────┐   │
           │        │ UI Updates:                      │   │
           │        │ ✓ Button → "Unsubscribe"         │   │
           │        │ ✓ Unlock menu items              │   │
           │        │ ✓ Show all reviews               │   │
           │        │ ✓ Allow review submission        │   │
           │        └──────────────────────────────────┘   │
           │                                                │
           └────────────────────────────────────────────────┘
```

---

## 5. MESS OWNER FLOW - CREATE MENU & VIEW ANALYTICS

```
┌──────────────────────────────────┐
│  Mess Owner Opens App            │
│  (Select Role: Mess Owner)       │
└──────────────┬───────────────────┘
               │
               ↓
┌──────────────────────────────────────────────┐
│  MessDashboardActivity                       │
│  (Container with multiple fragments)         │
└──────────────┬───────────────────────────────┘
               │
    ┌──────────┴──────────┬──────────────┬──────────────┐
    │                     │              │              │
    ↓                     ↓              ↓              ↓
┌──────────┐   ┌────────────┐   ┌─────────────┐   ┌──────────┐
│ Profile  │   │    Menu    │   │  Students   │   │Analytics │
│Fragment  │   │  Fragment  │   │  Fragment   │   │Fragment  │
└────┬─────┘   └─────┬──────┘   └──────┬──────┘   └────┬─────┘
     │               │                 │               │
     │               ↓                 │               ↓
     │      ┌────────────────────┐     │    ┌────────────────────┐
     │      │ MenuManager        │     │    │ AnalyticsManager   │
     │      │ .createMenu()      │     │    │ .getDashboard()    │
     │      │ .updateMenuAvail() │     │    │ .getTotalSubs()    │
     │      │ .getWeeklyMenu()   │     │    │ .getMonthlyRev()   │
     │      │ .addMealItems()    │     │    │ .trackPageView()   │
     │      └────────────────────┘     │    └────────────────────┘
     │               │                 │              │
     │               ↓                 │              ↓
     │      ┌────────────────────┐     │    ┌────────────────────┐
     │      │ Firestore Update   │     │    │ Firestore Query    │
     │      │ menus/ collection  │     │    │ • subscriptions/   │
     │      └────────────────────┘     │    │ • transactions/    │
     │                                 │    │ • analytics_events/│
     │               ┌─────────────────┘    └────────────────────┘
     │               │
     ├──────────────────────────────────────────────┐
     │                                              │
     ↓                                              ↓
┌──────────────────────┐                ┌─────────────────────┐
│ ProfileManager       │                │ StudentAdapter      │
│ .updateProfile()     │                │ • Shows subscribers │
│                      │                │ • Query from        │
│ Updates:             │                │   subscriptions/    │
│ • Name               │                │   collection        │
│ • Location           │                └─────────────────────┘
│ • Contact            │                        │
│ • Description        │                        ↓
│ • Price              │                ┌─────────────────────┐
└──────────────────────┘                │ SubscriptionManager │
         │                              │ .getUserSubs()      │
         ↓                              │ .renewSubscription()│
┌──────────────────────┐                │ .cancelSub()        │
│ ProfileManager       │                └─────────────────────┘
│ .isComplete()        │
│                      │
│ Validates if owner   │
│ completed setup      │
└──────────────────────┘

     ┌────────────────────────────────────────────────────────┐
     │         EditMessProfileActivity (Optional)             │
     │  (More detailed profile editing interface)             │
     │  • Upload mess photos                                  │
     │  • Edit detailed description                           │
     │  • Set operating hours                                 │
     └────────────────────────────────────────────────────────┘
```

---

## 6. REVIEW & RATING SYSTEM FLOW

```
┌─────────────────────────────────────────┐
│  User viewing MessDetailFragment        │
│  (Already subscribed to mess)           │
└──────────────────┬──────────────────────┘
                   │
                   ↓
       ┌──────────────────────────────┐
       │ ReviewManager                │
       │ .getMessReviews(messId)      │
       │                              │
       │ Fetches all reviews in       │
       │ Firestore for this mess      │
       └──────────────┬───────────────┘
                      │
                      ↓
        ┌──────────────────────────────┐
        │ RecyclerView(item_review.xml)|
        │                              │
        │ Each review shows:           │
        │ • User name                  │
        │ • Rating (stars)             │
        │ • Comment                    │
        │ • Timestamp                  │
        │ • Like count (with like btn) │
        └──────────────┬───────────────┘
                       │
                       ↓
       ┌──────────────────────────────┐
       │ User submits new review:     │
       │ 1. Select rating (1-5 stars) │
       │ 2. Enter comment             │
       │ 3. Click "Submit Review"     │
       └──────────────┬───────────────┘
                      │
                      ↓
       ┌──────────────────────────────────────┐
       │ ReviewManager                        │
       │ .createReview()                      │
       │                                      │
       │ ┌────────────────────────────────┐   │
       │ │ Inside createReview:           │   │
       │ │ 1. Create Review object        │   │
       │ │ 2. Set messId, userId, rating, │   │
       │ │    comment, timestamp          │   │
       │ │ 3. Save to Firestore reviews/  │   │
       │ │ 4. Call updateMessAvgRating()  │   │
       │ └────────────────────────────────┘   │
       └──────────────┬─────────────────────-─┘
                      │
                      ↓
       ┌─────────────────────────────────────┐
       │ ReviewManager                       │
       │ .updateMessAvgRating(messId)        │
       │                                     │
       │ ┌────────────────────────────────┐  │
       │ │ 1. Get all reviews for mess    │  │
       │ │ 2. Calculate average rating    │  │
       │ │ 3. Count total reviews         │  │
       │ │ 4. Update Firestore:           │  │
       │ │    messes/{messId}             │  │
       │ │    ├─ avgRating = 4.5          │  │
       │ │    └─ numReviews = 15          │  │
       │ └────────────────────────────────┘  │
       └──────────────┬──────────────────────┘
                      │
                      ↓
       ┌───────────────────────────────-───┐
       │ Firestore Collections Updated:    │
       │                                   │
       │ reviews/ (new review added)       │
       │ messes/ (avgRating updated)       │
       └──────────────┬────────────────────┘
                      │
                      ↓
       ┌──────────────────────────────────┐
       │ UI Updates:                      │
       │ ✓ New review appears in list     │
       │ ✓ Average rating recalculates    │
       │ ✓ Review form clears             │
       │ ✓ Toast: "Review submitted!"     │
       └──────────────────────────────────┘
```

---

## 7. PAYMENT & SUBSCRIPTION FLOW

```
┌─────────────────────────────────────┐
│  User clicks "Subscribe" button     │
│  (From MessDetailFragment)          │
└──────────────┬──────────────────────┘
               │
               ↓
┌─────────────────────────────────────┐
│ PaymentManager                      │
│ .processPayment(userId, messId,     │
│                 amount, days)       │
│                                     │
│ ┌────────────────────────────────┐  │
│ │ Random Success Check:          │  │
│ │ if (Math.random() < 0.95)      │  │
│ │    → SUCCESS (95% chance)      │  │
│ │ else                           │  │
│ │    → FAILURE (5% chance)       │  │
│ └────────────────────────────────┘  │
└──────────────┬──────────────────────┘
               │
        ┌──────┴─────────┐
        │                │
        ↓                ↓
   ┌─────────┐      ┌─────────┐
   │ SUCCESS │      │ FAILURE │
   └────┬────┘      └────┬────┘
        │                │
        ↓                ↓
   ┌──────────────┐  ┌──────────────┐
   │ Create:      │  │ Toast:       │
   │ Transaction  │  │ "Payment     │
   │ object       │  │ failed,      │
   │ • txnId      │  │ try again"   │
   │ • userId     │  │              │
   │ • messId     │  │ Return       │
   │ • amount     │  │ failure msg  │
   │ • status     │  └──────────────┘
   │ • timestamp  │
   └────┬────────┘
        │
        ↓
   ┌────────────────────────────┐
   │ SubscriptionManager        │
   │ .createSubscription()      │
   │                            │
   │ Create Subscription:       │
   │ • subscriptionId (UUID)    │
   │ • userId                   │
   │ • messId                   │
   │ • startDate = today        │
   │ • expiryDate = today + day │
   │ • status = "ACTIVE"        │
   │ • monthlyPrice             │
   └────┬───────────────────────┘
        │
        ↓
   ┌──────────────────────────────┐
   │ Save to Firestore:           │
   │                              │
   │ transactions/                │
   │ └─ {txnId}: Transaction      │
   │                              │
   │ subscriptions/               │
   │ └─ {subId}: Subscription     │
   │                              │
   │ users/{userId}               │
   │ ├─ messId: update to current │
   │ └─ subscriptions: add subId  │
   │                              │
   │ messes/{messId}              │
   │ └─ studentCount: increment   │
   └────┬──────────────────────--─┘
        │
        ↓
   ┌──────────────────────────────┐
   │ FirebaseNotificationManager  │
   │ .subscribeToTopic(messId)    │
   │                              │
   │ Topic: "mess_{messId}"       │
   │ (FCM subscription)           │
   └────┬────────────────────--───┘
        │
        ↓
   ┌──────────────────────────────┐
   │ AnalyticsManager             │
   │ .trackPageView()             │
   │                              │
   │ Log analytics event:         │
   │ • userId                     │
   │ • messId                     │
   │ • action: "subscription"     │
   │ • timestamp                  │
   └────┬──────────────────────--─┘
        │
        ↓
   ┌──────────────────────────────┐
   │ UI Updates:                  │
   │ ✓ Button: "Subscribed"       │
   │ ✓ Unlock menu items          │
   │ ✓ Show all reviews           │
   │ ✓ Enable review submission   │
   │ ✓ Toast: "Subscribed!"       │
   └──────────────────────────────┘
```

---

## 8. NOTIFICATIONS (FCM) FLOW

```
┌─────────────────────────────────────────┐
│  App Startup                            │
└──────────────┬──────────────────────────┘
               │
               ↓
┌─────────────────────────────────────────┐
│  FirebaseNotificationManager            │
│  .createNotificationChannel()           │
│                                         │
│  (Android 8+ requirement)               │
│  Creates channel:                       │
│  • NOTIFICATION_CHANNEL_ID              │
│  • Importance: HIGH                     │
│  • Description: "Mess App Notifications"|
└──────────────┬──────────────────────────┘
               │
               ↓
┌─────────────────────────────────────────┐
│  FirebaseNotificationManager            │
│  .getFCMToken()                         │
│                                         │
│  Get unique device token from Firebase  │
└──────────────┬──────────────────────────┘
               │
               ↓
┌─────────────────────────────────────────┐
│  FirebaseNotificationManager            │
│  .saveDeviceToken(userId, token)        │
│                                         │
│  Save to Firestore: users/{userId}      │
│  └─ fcmToken: "abc123xyz..."            │
└──────────────┬──────────────────────────┘
               │
               ↓
┌─────────────────────────────────────────┐
│  FirebaseNotificationManager            │
│  .subscribeToMessNotifications(messId)  │
│                                         │
│  FCM Topic: "mess_{messId}"             │
│  (User subscribes to mess updates)      │
└──────────────┬──────────────────────────┘
               │
               │
        ┌──────┴──────────────────────────┐
        │                                 │
        │        (When owner sends msg)   │
        │                                 │
        ↓                                 ↓
┌──────────────────────────────┐  ┌──────────────────────────────┐
│ Firestore (Backend)          │  │ MyFirebaseMessagingService   │
│ Update menu, send message    │  │ (Listening for FCM messages) │
│ to topic "mess_{messId}"     │  │                              │
└──────────────────────────────┘  │ ┌────────────────────────────┐
                                  │ │ onMessageReceived()        │
                                  │ │ • Gets notification data   │
                                  │ │ • Creates notification     │
                                  │ │ • Shows in system tray     │
                                  │ │ • Logs event               │
                                  │ └────────────────────────────┘
                                  └──────────────┬───────────────┘
                                                  │
                                                  ↓
                                   ┌──────────────────────────────┐
                                   │ FirebaseNotificationManager  │
                                   │ .logNotificationEvent()      │
                                   │                              │
                                   │ Save to Firestore:           │
                                   │ notification_events/         │
                                   │ • eventId                    │
                                   │ • userId                     │
                                   │ • messId                     │
                                   │ • eventType (msg, update)    │
                                   │ • timestamp                  │
                                   └──────────────────────────────┘
```

---

## 9. ADAPTER & RECYCLERVIEW BINDING

```
┌─────────────────────────────────────────┐
│  RecyclerView in Fragment               │
│  (e.g., UserHomeFragment)               │
└──────────────┬──────────────────────────┘
               │
               ↓
       ┌──────────────────────────────┐
       │ DiscoveryManager             │
       │ .searchMesses()              │
       │                              │
       │ Returns: List<Mess>          │
       └──────────────┬───────────────┘
                      │
                      ↓
       ┌───────────────────────────-───┐
       │ MessAdapter (Custom)          │
       │ ┌────────────────────────────┐│
       │ │ class MessAdapter extends  ││
       │ │ RecyclerView.Adapter       ││
       │ │                            ││
       │ │ onCreateViewHolder()       ││
       │ │ ├─ Inflate item_mess_card  ││
       │ │ │  (.xml layout)           ││
       │ │ └─ Create ViewHolder       ││
       │ │                            ││
       │ │ onBindViewHolder()         ││
       │ │ ├─ Bind data to views      ││
       │ │ ├─ Set click listeners     ││
       │ │ └─ Load images (optional)  ││
       │ │                            ││
       │ │ getItemCount()             ││
       │ │ └─ Return list size        ││
       │ └────────────────────────────┘│
       └──────────────┬─────────────-──┘
                      │
                      ↓
       ┌──────────────────────────────┐
       │ item_mess_card.xml Layout    │
       │                              │
       │ ┌────────────────────────────┐
       │ │ CardView                   │
       │ │ ├─ ImageView (Mess photo)  │
       │ │ ├─ TextView (Name)         │
       │ │ ├─ TextView (Location)     │
       │ │ ├─ RatingBar (Avg rating)  │
       │ │ ├─ TextView (Price)        │
       │ │ └─ Button (View Details)   │
       │ └────────────────────────────┘
       └──────────────┬───────────────┘
                      │
               (Repeated for each item)
```

---

## 10. FIRESTORE DATABASE SCHEMA

```
┌─────────────────────────────────────────────────────────────┐
│              FIRESTORE DATABASE STRUCTURE                   │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Collection: messes                                         │
│  ├─ {messId}                                                │
│  │  ├─ name: String                                         │
│  │  ├─ location: String                                     │
│  │  ├─ contact: String                                      │
│  │  ├─ description: String                                  │
│  │  ├─ monthlyPrice: Double                                 │
│  │  ├─ ownerId: String (Reference to user)                  │
│  │  ├─ avgRating: Double (Updated by ReviewManager)         │
│  │  ├─ numReviews: Integer (Updated by ReviewManager)       │
│  │  ├─ studentCount: Integer (Updated by SubscriptionMngr)  │
│  │  ├─ createdAt: Timestamp                                 │
│  │  └─ updatedAt: Timestamp                                 │
│  │                                                          │
│  Collection: menus                                          │
│  ├─ {menuId}                                                │
│  │  ├─ messId: String (Reference to mess)                   │
│  │  ├─ dayOfWeek: String (MON, TUE, etc)                    │
│  │  ├─ meals: Array [String] (Lunch, Dinner)                │
│  │  ├─ items: Array [String] (Food items)                   │
│  │  ├─ available: Boolean                                   │
│  │  ├─ createdAt: Timestamp                                 │
│  │  └─ updatedAt: Timestamp                                 │
│  │                                                          │
│  Collection: subscriptions                                  │
│  ├─ {subscriptionId}                                        │
│  │  ├─ userId: String (Reference to user)                   │
│  │  ├─ messId: String (Reference to mess)                   │
│  │  ├─ startDate: Long (milliseconds)                       │
│  │  ├─ expiryDate: Long (milliseconds)                      │
│  │  ├─ status: String (ACTIVE, EXPIRED, CANCELLED)          │
│  │  └─ monthlyPrice: Double                                 │
│  │                                                          │
│  Collection: reviews                                        │
│  ├─ {reviewId}                                              │
│  │  ├─ messId: String (Reference to mess)                   │
│  │  ├─ userId: String (Reference to user)                   │
│  │  ├─ rating: Float (1-5)                                  │
│  │  ├─ comment: String                                      │
│  │  ├─ userName: String                                     │
│  │  ├─ likes: Integer (Incremented by likeReview)           │
│  │  ├─ timestamp: Long (milliseconds)                       │
│  │  └─ createdAt: Timestamp                                 │
│  │                                                          │
│  Collection: offers                                         │
│  ├─ {offerId}                                               │
│  │  ├─ messId: String (Reference to mess)                   │
│  │  ├─ title: String                                        │
│  │  ├─ description: String                                  │
│  │  ├─ discountPercentage: Double                           │
│  │  ├─ active: Boolean (Set by OfferManager)                │
│  │  ├─ usageCount: Long (Incremented when applied)          │
│  │  ├─ expiryDate: Long (milliseconds)                      │
│  │  ├─ createdAt: Timestamp                                 │
│  │  └─ updatedAt: Timestamp                                 │
│  │                                                          │
│  Collection: transactions                                   │
│  ├─ {transactionId}                                         │
│  │  ├─ userId: String                                       │
│  │  ├─ messId: String                                       │
│  │  ├─ amount: Double                                       │
│  │  ├─ status: String (SUCCESS, FAILED)                     │
│  │  ├─ timestamp: Long                                      │
│  │  └─ createdAt: Timestamp                                 │
│  │                                                          │
│  Collection: users                                          │
│  ├─ {userId} (Firebase Auth uid)                            │
│  │  ├─ email: String                                        │
│  │  ├─ role: String (USER, MESS_OWNER)                      │
│  │  ├─ messId: String (Current subscription, for users)     │
│  │  ├─ fcmToken: String (Device token)                      │
│  │  ├─ subscriptions: Array [String] (Subscription IDs)     │
│  │  ├─ lastUpdated: Long                                    │
│  │  ├─ createdAt: Timestamp                                 │
│  │  └─ updatedAt: Timestamp                                 │
│  │                                                          │
│  Collection: notification_events                            │
│  ├─ {eventId}                                               │
│  │  ├─ userId: String                                       │
│  │  ├─ messId: String                                       │
│  │  ├─ eventType: String                                    │
│  │  ├─ timestamp: Long                                      │
│  │  └─ createdAt: Timestamp                                 │
│  │                                                          │
│  Collection: notification_preferences                       │
│  ├─ {userId}                                                │
│  │  ├─ messUpdates: Boolean                                 │
│  │  ├─ reviewNotifications: Boolean                         │
│  │  ├─ paymentAlerts: Boolean                               │
│  │  └─ updatedAt: Timestamp                                 │
│  │                                                          │
└─────────────────────────────────────────────────────────────┘
```

---

## 11. DEPENDENCY INJECTION DIAGRAM

```
┌─────────────────────────────────────────────────────────────┐
│              DEPENDENCIES & INITIALIZATION                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Application Context                                        │
│         │                                                   │
│         ├─→ Activity/Fragment                               │
│         │       │                                           │
│         │       ├─→ ProfileManager(context)                 │
│         │       ├─→ MenuManager(context)                    │
│         │       ├─→ SubscriptionManager(context)            │
│         │       ├─→ PaymentManager(context)                 │
│         │       ├─→ ReviewManager(context)                  │
│         │       ├─→ OfferManager(context)                   │
│         │       ├─→ DiscoveryManager(context)               │
│         │       ├─→ AnalyticsManager(context)               │
│         │       └─→ FirebaseNotificationManager(context)    │
│         │                                                   │
│         └─→ All Managers internally:                        │
│                 • Initialize FirebaseFirestore.getInstance()│
│                 • Initialize FirebaseAuth.getInstance()     │
│                 • Initialize FirebaseMessaging (FCM only)   │
│                                                             │
│  Global Firebase Instances (Singletons):                    │
│  ┌────────────────────────────────────────┐                 │
│  │ FirebaseFirestore.getInstance()        │                 │
│  │ (Shared across all managers)           │                 │
│  └────────────────────────────────────────┘                 │
│  ┌────────────────────────────────────────┐                 │
│  │ FirebaseAuth.getInstance()             │                 │
│  │ (Shared across all managers)           │                 │
│  └────────────────────────────────────────┘                 │
│  ┌────────────────────────────────────────┐                 │
│  │ FirebaseMessaging.getInstance()        │                 │
│  │ (Used by notification manager)         │                 │
│  └────────────────────────────────────────┘                 │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 12. CALLBACK PATTERN & ASYNC OPERATIONS

```
┌─────────────────────────────────────────────────────────┐
│           ASYNCHRONOUS CALLBACK PATTERN                 │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  Fragment/Activity                                      │
│         │                                               │
│         ↓                                               │
│  profileManager.fetchMessProfile(messId, new            │
│      ProfileManager.ProfileCallback() {                 │
│          @Override                                      │
│          public void onSuccess(Mess mess) {             │
│              // Update UI with mess data                │
│              binding.name.setText(mess.getName());      │
│          }                                              │
│                                                         │
│          @Override                                      │
│          public void onFailure(String error) {          │
│              // Show error toast                        │
│              Toast.makeText(...).show();                │
│          }                                              │
│      });                                                │
│         │                                               │
│         ↓ (Async callback registered)                   │
│         │                                               │
│         ↓ (Manager executes Firestore query)            │
│    ProfileManager                                       │
│         │                                               │
│         ├─ db.collection("messes")                      │
│         │  .document(messId)                            │
│         │  .get()                                       │
│         │                                               │
│         ↓ (Waiting for response...)                     │
│         │                                               │
│    ┌────────────┐                                       │
│    │ Firestore  │ (Cloud Database)                      │
│    │ Returns    │                                       │
│    └────────────┘                                       │
│         ↓ (Success or Failure)                          │
│         │                                               │
│         ├─ .addOnSuccessListener() {                    │
│         │      callback.onSuccess(mess);                │
│         │  }                                            │
│         │                                               │
│         ├─ .addOnFailureListener() {                    │
│         │      callback.onFailure(exception.msg);       │
│         │  }                                            │
│         │                                               │
│         ↓ (Callback invoked on main thread)             │
│         │                                               │
│    Fragment/Activity (back in UI thread)                │
│         │                                               │
│         ├─ onSuccess: UI updates                        │
│         └─ onFailure: Error shown                       │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

---

## 13. COMPLETE REQUEST-RESPONSE CYCLE

```
┌─────────────────────────────────────────────────────────────┐
│          USER ACTION → MANAGER → FIRESTORE → UI UPDATE      │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Example: User subscribes to a mess                         │
│                                                             │
│  1. USER ACTION                                             │
│     ┌──────────────────────────────────────┐                │
│     │ Click "Subscribe" Button             │                │
│     │ MessDetailFragment                   │                │
│     └──────────────────┬───────────────────┘                │
│                        │                                    │
│  2. MANAGER CALL                                            │
│     ┌──────────────────────────────────────┐                │
│     │ PaymentManager.processPayment        │                │
│     │   userId, messId, amount, days,      │                │
│     │   new PaymentCallback()              │                │
│     │     onSuccess() { ... }              │                │
│     │     onFailure() { ... }              │                │
│     │                                      │                │
│     └──────────────────┬───────────────────┘                │
│                        │                                    │
│  3. FIREBASE WRITE                                          │
│     ┌──────────────────────────────────────┐                │
│     │ db.collection("transactions")        │                │
│     │   .document(txnId)                   │                │
│     │   .set(transactionData)              │                │
│     │   .addOnSuccessListener() →          │                │
│     │   SubscriptionManager.createSub()    │                │
│     └──────────────────┬───────────────────┘                │
│                        │                                    │
│  4. CALLBACK TRIGGERED                                      │
│     ┌──────────────────────────────────────┐                │
│     │ onSuccess(Payment successful)        │                │
│     │                                      │                │
│     │ SubscriptionManager.createSubscription:               │
│     │ • Create subscription object         │                │
│     │ • Save to Firestore subscriptions/   │                │
│     │ • Update user's messId field         │                │
│     │ • Increment mess studentCount        │                │
│     └──────────────────┬───────────────────┘                │
│                        │                                    │
│  5. FCM NOTIFICATION                                        │
│     ┌──────────────────────────────────────┐                │
│     │ FirebaseNotificationManager          │                │
│     │ .subscribeToTopic("mess_" + messId)  │                │
│     │                                      │                │
│     │ Device now subscribed to push        │                │
│     │ notifications for this mess          │                │
│     └──────────────────┬───────────────────┘                │
│                        │                                    │
│  6. UI UPDATE                                               │
│     ┌──────────────────────────────────────┐                │
│     │ In onSuccess() callback:             │                │
│     │                                      │                │
│     │ binding.btnSubscribe.setText(        │                │
│     │   "Unsubscribe"                      │                │
│     │ );                                   │                │
│     │ binding.menuItems.setVisibility(     │                │
│     │   View.VISIBLE                       │                │
│     │ );                                   │                │
│     │ Toast.makeText(                      │                │
│     │   "Subscribed successfully!",        │                │
│     │   Toast.LENGTH_SHORT                 │                │
│     │ ).show();                            │                │
│     │                                      │                │
│     │ Fragment refreshes data:             │                │
│     │ • Fetch menu items                   │                │
│     │ • Fetch reviews                      │                │
│     │ • Update UI state                    │                │
│     └──────────────────────────────────────┘                │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## NEW: Detailed Feature Updates (v2.0)

### **Revenue Tracking System**
- **MessRevenueFragment**: Dedicated UI for mess owners to track earnings
- Shows monthly/daily revenue breakdown
- Integrated with PaymentManager for transaction history
- AnalyticsManager aggregates revenue data

### **Subscription Request Management**
- **MessRequestsFragment**: Handles pending subscription requests
- Owners can approve/reject subscription requests
- New model: **SubscriptionRequest** tracks request status
- Automated notifications on approval/rejection

### **Advanced Offer System**
- **MessOffersFragment**: Browse and manage active offers
- **AddOfferFragment**: Create new promotional offers
- Tracks offer usage/redemption
- Time-based offer expiry (automatic deactivation)
- Integration with PaymentManager for discount calculations

### **Google Sign-in Authentication**
- **LoginActivity** now supports Google OAuth authentication
- Uses Google ID credentials library
- Automatic user profile creation on first sign-in
- Optional login method alongside email/password

### **User Profile Management**
- **EditUserProfileActivity**: Dedicated activity for profile editing
- Update user preferences and settings
- Manage notification preferences
- Upload/change profile picture (via Firebase Storage)

---

## Summary of Key Points

1. **Layered Architecture**           : UI → Managers → Firestore → Models
2. **Async Operations**               : All Firestore operations use callbacks (non-blocking)
3. **Managers as Service Layer**      : Each manager handles specific business logic
4. **Firestore as Central DB**        : Single source of truth for all data
5. **FCM for Real-time Notifications**: Users notified of events via push
6. **Role-based Access**              : USER vs MESS_OWNER determines available features
7. **Modular Components**             : Activities, Fragments, Adapters work independently
8. **ViewBinding**                    : Type-safe view references throughout app
9. **RecyclerView Adapters**          : Display dynamic lists of data
10. **Transaction Flow**              : Payment → Subscription → Analytics → Notification
11. **Revenue Tracking**              : Real-time financial reporting for mess owners
12. **Offer Management**              : Dynamic promotional system with usage tracking
13. **Multi-auth Support**            : Email/Password + Google Sign-in options
14. **Request System**                : Approval workflow for subscription management

This architecture ensures clean separation of concerns, easy maintenance, and scalability!
