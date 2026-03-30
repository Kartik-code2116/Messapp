# Role Selection Button Crash - Troubleshooting Guide

## Issue Summary
App crashes when clicking buttons on the Role Selection screen.

## Root Cause Analysis

### 1. **Java Version Mismatch** ✅ FIXED
**Problem**: `build.gradle.kts` was configured for Java 17, but your system has JDK 21, causing compilation failure.
```
error: invalid source release: 21
```

**Solution Applied**:
- Updated `app/build.gradle.kts` to use `JavaVersion.VERSION_21`

### 2. **UI Layout Button Not Found** - MOST LIKELY CRASH CAUSE
**Problem**: If button IDs in the layout file don't match the code, `findViewById()` returns null and clicking causes NullPointerException.

**Files to Verify**:
- Layout buttons: [app/src/main/res/layout/activity_role_selection.xml](activity_role_selection.xml#L89)
  - Check that `btn_user` button exists at line 89
  - Check that `btn_mess_uncal` button exists at line 153
- Activity code: [app/src/main/java/com/example/messapp/RoleSelectionActivity.java](RoleSelectionActivity.java)

**Fix Applied**:
- Enhanced RoleSelectionActivity with:
  - Null checks before setting click listeners
  - Proper error logging (tag: "RoleSelectionActivity")
  - User-friendly error messages showing which UI element is missing
  - Exception handling around navigation

### 3. **Binding Issues**
**Problem**: View binding could fail if layout XML has syntax errors or missing references.

**How to Debug**:
1. **Check Build Output**: Run `./gradlew clean build` in terminal
   ```powershell
   cd d:\7)android projects\messapp
   .\gradlew clean build
   ```
   Look for errors like:
   - "resource not found" 
   - "unresolved style attribute"
   - "missing color resource"

2. **Check Layout Syntax**: Open [activity_role_selection.xml](app/src/main/res/layout/activity_role_selection.xml) and verify:
   - All opening tags have closing tags
   - All `android:id` attributes are properly formatted
   - Color resources referenced exist: `@color/blue_accent`, `@color/primary_color`, `@color/text_primary`, etc.

3. **Check Drawable Resources**: Verify these drawables exist:
   - `@drawable/ic_person`
   - `@drawable/ic_restaurant`
   - `@drawable/bg_circle_light_green`

## Step-by-Step Debugging

### Step 1: Clean and Rebuild
```powershell
cd d:\7)android projects\messapp
.\gradlew clean
.\gradlew build
```

If build fails, read the error output carefully - it will pinpoint the exact issue.

### Step 2: Check Logcat During Runtime
In Android Studio:
1. Open **Logcat** panel (View → Tool Windows → Logcat)
2. Filter by tag: "RoleSelectionActivity"
3. Click the role selection button
4. Look for error messages like:
   - `btn_mess_uncal button not found in layout`
   - `btn_user button not found in layout`
   - Exception stack trace showing the exact line crashing

### Step 3: Verify Resource Files
Check if all referenced colors and drawables exist:
```bash
# Check colors
findstr /R "blue_accent|primary_color|text_primary|text_secondary" app/src/main/res/values/colors.xml

# Check drawables
dir app/src/main/res/drawable | findstr /R "ic_person|ic_restaurant|bg_circle"
```

## Common Issues and Fixes

| Issue | Symptom | Fix |
|-------|---------|-----|
| Missing button ID | "button not found" error in Logcat | Ensure button IDs match exactly: `btn_user` and `btn_mess_uncal` |
| Missing color resource | Build fails with "unresolved color" | Define missing colors in `colors.xml` |
| Missing drawable | Button displays incorrectly | Create or import missing drawables |
| Invalid XML syntax | Build fails | Use Android Studio's XML validator (Ctrl+Shift+L) |
| DataBinding failure | Blank screen or crash | Check `ActivityRoleSelectionBinding` is generated correctly |

## Testing the Fix

1. **Build the app**:
   ```powershell
   .\gradlew build
   ```

2. **Run on emulator/device**:
   - Open app
   - Navigate through splash screen
   - Reach Role Selection screen
   - **Click both buttons** - should navigate to Login screen without crashing

3. **Monitor Logcat** for any error messages even if it appears to work

## Resources for Further Debugging

- Android Studio's **Debug Console**: Right-click crash → "Analyze Stack Trace"
- Firebase Crashlytics: If integrated, shows real user crashes
- Android Logcat filters: `E/RoleSelectionActivity` to isolate this activity's errors

## Next Steps if Still Crashing

1. Enable verbose logging: Add `android:debuggable="true"` to `AndroidManifest.xml` `<application>` tag
2. Check if `LoginActivity` successfully receives the ROLE intent extra:
   - It already has null-safety: `currentRole = getIntent().getStringExtra("ROLE");` (line 54)
3. Verify Firebase configuration: Ensure `google-services.json` is valid and in `app/` directory
4. Check device compatibility: Ensure minimum SDK 24 is being tested on
