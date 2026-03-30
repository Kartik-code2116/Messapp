# MessApp - Short Workflow Guide

## What is MessApp?
A two-sided Android app connecting **students (users)** with **mess owners** for meal subscriptions, menu viewing, and reviews.

---

## 🎯 Non-Technical Workflow (For Regular Users)

### **For Students/Users:**

**Step 1: Sign Up**
- Download the app
- Create an account with your email
- Choose "I'm a User/Student"
- Log in

**Step 2: Browse Mess Places**
- Open the Home screen
- See a list of all available mess places nearby
- Each shows: name, location, price, and star rating
- Search by location or filter by price range

**Step 3: View Details**
- Click on a mess you're interested in
- See their full details: address, contact, what they offer
- Check today's lunch and dinner menu
- Read reviews from other students
- See the average rating

**Step 4: Subscribe**
- Click the "Subscribe" button
- A payment process happens (simulated in this version)
- If successful, you're now a subscriber!
- Now you can see full menu and write reviews

**Step 5: Daily Usage**
- Open "My Menu" to see today's lunch & dinner
- Check your subscription history anytime
- Leave a review with star rating and comment
- Unsubscribe anytime you want

**Step 6: Notifications**
- Get push notifications when the mess updates menu
- Get alerts about special offers
- Receive messages from the mess owner

---

### **For Mess Owners:**

**Step 1: Set Up Your Mess**
- Sign up with email
- Choose "I'm a Mess Owner"
- Fill in your mess details:
  - Mess name (e.g., "Sharma's Mess")
  - Location address
  - Contact phone number
  - Monthly price per student
  - Description of what you offer

**Step 2: Daily Menu Management**
- Every day, enter the lunch menu before 10 AM
- Every day, enter the dinner menu before 4 PM
- After 10 AM, can't edit lunch anymore
- After 4 PM, can't edit dinner anymore
- (This ensures timely updates for students)

**Step 3: View Your Students**
- See how many students subscribed this month
- Check student names and contact info
- See who's active and who cancelled

**Step 4: Check Your Dashboard**
- Total money earned this month
- Number of students paying you
- Most popular dishes
- Trends over time

**Step 5: Handle Reviews**
- Students write reviews about your mess
- See what they liked or didn't like
- Your average rating is calculated automatically
- Higher ratings = more students will subscribe

**Step 6: Communicate**
- Send updates to all your current students
- They get instant push notifications
- Announce special meals, discounts, or any changes

---

### **Real-World Examples:**

**Scenario 1: A Student Subscribes**
```
Priya opens MessApp
  ↓
Sees "Sharma's Mess" with 4.5 stars (₹2000/month)
  ↓
Clicks to see menu → Lunch: Dal-Rice-Vegetables, Dinner: Roti-Curry
  ↓
Reads 25 reviews: "Great food!", "Love the variety", "Affordable"
  ↓
Clicks "Subscribe" → Payment successful
  ↓
Now she can see full menu daily and write reviews
  ↓
Tomorrow at 6 AM she checks app → Today's lunch is "Biryani & Raita"
  ↓
After eating, she rates 5 stars: "Amazing biryani! Worth it."
```

**Scenario 2: Mess Owner Updates Menu**
```
Raj (Mess Owner) wakes up at 8 AM
  ↓
Opens MessApp → Mess Menu section
  ↓
Types Lunch: "Aloo Paratha, Pickle, Lassi"
  ↓
Clicks "Save"
  ↓
All 30 students get a notification: "Menu updated! Check today's lunch"
  ↓
Students open app and see the new menu
  ↓
Students know what to expect when they come for lunch
```

**Scenario 3: A Student Reviews the Mess**
```
Harsh has been eating at "Golden Kitchen" for 2 months
  ↓
Opens the app → Goes to the mess details
  ↓
Clicks the 5-star button and writes: "Consistently good quality. Hygiene is great!"
  ↓
Clicks "Submit Review"
  ↓
His review appears immediately for other students to see
  ↓
The app automatically calculates: "Golden Kitchen now has 4.7/5 stars (85 reviews)"
  ↓
More students see the high rating and decide to subscribe
```

---

---

## Quick App Flow

### 🔐 **Authentication**
1. **SplashActivity** → Checks if user is logged in
2. **RoleSelectionActivity** → User chooses: "User" or "Mess Owner"
3. **LoginActivity** → Email/Password + Firebase Auth

---

### 👤 **USER WORKFLOW**

#### Home Screen (UserHomeFragment)
- Browse available messes
- Search and filter by location/price
- See ratings and reviews

#### Subscribe Flow
1. Click on a mess → View details
2. See menu items (lunch/dinner)
3. Click "Subscribe" → Simulated payment (95% success)
4. Automatic FCM topic subscription for notifications
5. Unlock full menu & ability to review

#### My Menu (UserMenuFragment)
- View today's lunch & dinner for subscribed mess
- Updated daily by mess owner

#### History (UserHistoryFragment)
- View all past subscriptions
- Dates and pricing

#### Profile (UserProfileFragment)
- User info and preferences

---

### 👨‍💼 **MESS OWNER WORKFLOW**

#### Dashboard (MessDashboardActivity)
- **Profile Tab** → Edit mess info (name, location, contact, description, price)
- **Menu Tab** → Update today's lunch/dinner (10 AM cutoff for lunch, 4 PM for dinner)
- **Students Tab** → View all current subscribers
- **Analytics Tab** → See total subscribers, revenue, trends

#### Special Features
- Can view all reviews customers posted
- Can edit profile anytime via EditMessProfileActivity
- Automatic student count updates

---

## Core Features

| Feature                 | Details                              |
|-------------------------|--------------------------------------|
| **Authentication**      | Firebase Auth (Email/Password)       |
| **Database**            | Firestore NoSQL                      |
| **Notifications**       | Firebase Cloud Messaging (FCM)       |
| **Payments**            | Simulated (95% success rate)         |
| **Subscriptions**       | Monthly, auto-expiry tracking        |
| **Reviews**             | 5-star rating system with comments   |
| **Menu**                | Daily lunch/dinner with time cutoffs |
| **Analytics**           | Subscriber count, revenue tracking   |

---

## Data Models (Firestore Collections)

```
messes/           → Mess details (name, location, price, rating)
menus/            → Daily lunch/dinner items
subscriptions/    → User-Mess subscription records
reviews/          → Ratings and comments
transactions/     → Payment records
users/            → User profiles (role, tokens)
offers/           → Discounts and promotions
notification_events/  → Event logs
```

---

## Key Managers

| Manager                         | Purpose                      |
|---------------------------------|------------------------------|
| **ProfileManager**              | Mess profile CRUD            |
| **MenuManager**                 | Create/update daily menus    |
| **SubscriptionManager**         | Handle subscriptions         |
| **PaymentManager**              | Process payments (simulated) |
| **ReviewManager**               | Manage ratings & comments    |
| **DiscoveryManager**            | Search & filter messes       |
| **AnalyticsManager**            | Dashboard stats              |
| **FirebaseNotificationManager** | Push notifications via FCM   |

---

## Important Flows

### 1️⃣ Subscribe
User → Click Subscribe → Payment (simulated) → Create subscription → FCM topic sub → Unlock menu

### 2️⃣ Review
User → Rate mess → Add comment → Submit → Manager calculates avg rating → Updates mess profile

### 3️⃣ Menu Update
Mess owner → Enter menu items → Save → Firestore update → Users see updated menu

### 4️⃣ Notification
Mess owner sends update → FCM topic "mess_[messId]" → All subscribers get push notification

---

## Tech Stack

- **Language:** Java 17
- **Framework:** Android (API 24-36)
- **UI:** Fragment Navigation, RecyclerView, ViewBinding
- **Backend:** Firebase (Auth, Firestore, Messaging, Storage)
- **Build:** Gradle KTS

---

## File Structure

```
app/src/main/java/com/example/messapp/
├── Activities/          → Login, Dashboard, Role Selection
├── Fragments/           → UI screens (Home, Menu, Profile, etc)
├── Managers/            → Business logic (9 manager classes)
├── Models/              → Data classes (Mess, Subscription, Review, etc)
├── Adapters/            → RecyclerView adapters
└── MyFirebaseMessagingService.java  → FCM handler
```

---

## Quick Reference

**User Can:**
- Browse messes
- Subscribe/Unsubscribe
- View menu items
- Post reviews
- See subscription history
- Manage preferences

**Mess Owner Can:**
- Create/edit profile
- Set daily menu with time cutoffs
- View subscribers
- See analytics (revenue, subscriber count)
- Receive customer reviews
- Send updates via FCM

---

## Status Codes & Constants

- **Subscription Status:** ACTIVE, EXPIRED, CANCELLED
- **Transaction Status:** SUCCESS, FAILED
- **User Roles:** USER, MESS_OWNER
- **Meal Types:** LUNCH, DINNER
- **Days:** MON, TUE, WED, THU, FRI, SAT, SUN

---

## Firebase Security Notes

- Users can only view their own data
- Mess owners can only edit their mess
- Firestore rules enforce role-based access
- FCM topics auto-manage subscriptions
