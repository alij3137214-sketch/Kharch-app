# Kharch

A simple money app for your phone. It works fully offline: your data never leaves your phone.

## What it does

- **First-run questions.** A few simple questions (what you want help with, how money comes to you, bills, what you spend on) build your first plan and decide what your Home screen shows first.
- **Write down what you spend, get, or move.** Money in each place (Cash, Bank, Easypaisa...) is tracked, and moving money between places works.
- **You can spend today.** Your monthly limit is shared across the days that are left. Money you do not spend today moves to tomorrow, and unpaid bills are set aside first.
- **Limits and alerts.** A monthly limit and limits for each kind of spending. A warning appears on screen and as a phone notification when you get close or go over.
- **Bills** that repeat every month, **piggy bank goals**, and daily or evening reminders.
- **Price in hours of work.** "That is about 2 hours of work."
- **Wait list.** Put what you want on a list and wait a few days before you decide.
- **Udhaar.** Money given and taken, with part payments and a reminder message to send.
- **Committee (kameti / BC).** When to pay, and when it is your turn to get the money.
- **Charts** for week, month and year, fair comparison with before, and a **picture to share** (with an option to hide the amounts).
- **Delete data** from the small bin icon: one month, all records, or everything.

## Build

Needs JDK 17 or newer and the Android SDK (API 36).

```
./gradlew assembleDebug        # app
./gradlew testDebugUnitTest    # tests
```

The debug build is signed with `debug.keystore` in the project folder (not stored in git). Create one with:

```
keytool -genkeypair -keystore debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
```

A release build is signed only when `KEYSTORE_PATH`, `STORE_PASSWORD` and `KEY_PASSWORD` are set (and optionally `KEY_ALIAS`).

Screenshot tests write pictures of every screen to `app/build/preview/` when run with `-Proborazzi.test.record=true`.

## Notes

- Database upgrades keep existing records (see `KharchDatabase.MIGRATION_4_5` and its test).
- There is no AI and no internet permission. All the maths is plain rules you can read in `domain/MoneyMath.kt` and `data/profile/BudgetPlanner.kt`.
