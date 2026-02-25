# Receipt Backup - Project Guide

## What This App Does

A dual-feature Android app:
1. **Receipt Management** - Store, organize, and backup receipts to Firebase with image uploads
2. **Loyalty Card Wallet** - Store loyalty/membership cards offline with barcode scanning and display

## Project Structure

```
app/src/main/java/com/hadley/receiptbackup/
├── auth/                    # Google Sign-In (GoogleAuthManager.kt)
├── data/
│   ├── local/               # DataStore persistence (cards, receipts, settings, nav)
│   ├── model/               # Data classes (LoyaltyCard, ReceiptItem)
│   ├── repository/          # ViewModels (MVVM state holders)
│   └── sync/                # WorkManager background sync
├── navigation/              # AppNavHost + MainScaffold (drawer-based nav)
├── ui/
│   ├── screens/             # Full composable screens
│   ├── components/          # Reusable composables
│   └── theme/               # Material3 theming
└── utils/                   # BarcodeUtils, ColorUtils, LoyaltyCardImageManager
```

## Architecture

**MVVM with Jetpack Compose** and unidirectional data flow:
- **StateFlow** in ViewModels for reactive state
- **Single Activity** (`MainActivity`) with Compose `setContent`
- **Navigation Compose** for screen routing
- **AppScaffoldState** via CompositionLocal for shared scaffold state

## Tech Stack

| Category | Library/Tool |
|---|---|
| Language | Kotlin 2.2.10 |
| UI | Jetpack Compose + Material3 |
| Auth | Google Sign-In (Credentials API) + Firebase Auth |
| Database | Firebase Firestore |
| Storage | Firebase Storage (images) |
| Local storage | DataStore Preferences + Moshi JSON |
| Image loading | Coil 2.5.0 |
| Barcode scanning | Google MLKit |
| Barcode generation | ZXing 3.5.3 |
| Camera | CameraX 1.3.4 |
| Background work | WorkManager 2.9.0 |
| Networking | OkHttp 4.12.0 |

**SDK:** Min 33 (Android 13), Target/Compile 35 (Android 15), Java 11

## Key ViewModels

- **`LoyaltyCardViewModel`** (`data/repository/`) - Card CRUD, Firestore sync, drag-to-reorder
- **`ReceiptItemViewModel`** (`data/repository/`) - Receipt CRUD, image caching/downloads, upload tracking

## Data Storage

### Local (DataStore)
- `LoyaltyCardDataStore` - Serializes card list to JSON (Moshi)
- `ReceiptItemDataStore` - Serializes receipt list (Moshi with custom LocalDate adapter)
- `SettingsDataStore` - Theme mode (SYSTEM/LIGHT/DARK)
- `NavigationPreferences` - Last active screen route

### Remote (Firebase)
```
Firestore: users/{uid}/cards/{cardId}
           users/{uid}/receipts/{itemId}
Storage:   loyalty_card_images/{uid}/{cardId}.jpg
           users/{uid}/images/{randomId}.jpg
```

## Notable Features & Patterns

### Image Processing (`utils/`)
- `LoyaltyCardImageManager` - Upload/download with local file cache in `filesDir/loyalty_card_images/`
- `ColorUtils` - 4-corner background color sampling + smart bitmap trimming with tolerance
- Images are compressed (JPEG 85% for cards, 75% for receipts) and resized (max 256px height)
- Automatic background color extraction for card theming
- Readable text color calculated from background luminance

### Barcode Handling
- **Scanning:** MLKit (`BarcodeScannerDialog.kt`)
- **Generation:** ZXing - 600x600 for 2D codes, 900x300 for 1D codes
- 12 supported formats (QR, EAN-13, UPC-A, Code 128, etc.)
- Configurable barcode width: full or 50%

### Drag-to-Reorder
- Loyalty cards support pointer-input drag reordering
- Disabled when search is active
- Sort order normalized on load

### Background Sync
- `ReceiptSyncWorker` (WorkManager) with network constraints
- Handles pending uploads on app restart
- Progress tracking in 5% increments

## Build Configuration

Dependencies managed via **version catalog** at `gradle/libs.versions.toml`.

`local.properties` must contain:
```
serverClientId=<your-google-oauth-client-id>
```

Google Services (`google-services.json`) required in `app/` for Firebase.

## Running the App

Standard Android Studio build. Requires:
1. Firebase project with Firestore + Storage + Auth enabled
2. `google-services.json` in `app/`
3. `serverClientId` in `local.properties`
