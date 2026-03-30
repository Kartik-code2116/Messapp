# Quick Fix Checklist - Role Selection Crash

## ✅ Already Fixed
- [x] Java version mismatch (17 → 21)
- [x] Added defensive null-checks in RoleSelectionActivity
- [x] Added error logging and user-friendly messages

## 🔍 Verify These Immediately

### 1. Build Succeeds
```powershell
cd d:\7)android projects\messapp
.\gradlew clean build
```
**Expected**: `BUILD SUCCESSFUL`
**If fails**: Post the error message

### 2. Layout File Is Valid
- Open `app/src/main/res/layout/activity_role_selection.xml`
- Verify buttons exist:
  - ✓ `android:id="@+id/btn_user"` (around line 89)
  - ✓ `android:id="@+id/btn_mess_uncal"` (around line 153)

### 3. Color & Drawable Resources Exist
**Check these files have all referenced colors/drawables:**
- `app/src/main/res/values/colors.xml`
  - Should have: `blue_accent`, `primary_color`, `text_primary`, `text_secondary`
- `app/src/main/res/drawable/`
  - Should have: `ic_person`, `ic_restaurant`, `bg_circle_light_green`

### 4. Test on Device/Emulator
1. Run: `.\gradlew build` then run app from Android Studio
2. Navigate to Role Selection screen
3. Click "Continue as User" button
4. **Should navigate to Login screen** ✓

## 🚨 If Still Crashing

1. **Check Logcat** (View → Tool Windows → Logcat):
   - Filter: `RoleSelectionActivity`
   - Look for error messages
   - Post any error stack traces

2. **Run with stack trace**:
   ```powershell
   .\gradlew build --stacktrace
   ```

3. **Provide this info**:
   - Full error/crash log from Logcat
   - Output of `./gradlew build --stacktrace`
   - Which button causes crash (User or Owner)
   - At what point it crashes (clicking button, navigating, etc.)
