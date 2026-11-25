# Detailed Analysis of Patching Steps for Into the Dead 2 (v1.89.0)

## 1. Overview
This document serves as a severe, highly detailed technical breakdown of the issues encountered when patching *Into the Dead 2* (v1.89.0) and the exact steps taken to resolve them. It is designed to be a reference manual for updating the patch for future versions of the game.

When the application was initially patched and run, the Unity engine would freeze indefinitely on the initial splash screen without proceeding to download assets or load the game. Multiple intertwined issues were preventing a successful boot.

## 2. Ad Provider Callbacks (NoSuchMethodError)
### **The Problem**
Upon inspection of the initial logs (`adb logcat`), the Unity engine's startup sequence was being violently interrupted by `NoSuchMethodError` exceptions originating from the DEX files. The game uses multiple Ad Networks (LevelPlay, Tapjoy, etc.), and our `AdsPatch.kt` was attempting to hook or invoke methods on these SDKs that no longer existed or had changed their signatures in v1.89.0.

### **The Fixes**
We dumped the DEX files (`classes0.dex` through `classes10.dex`) using `dexdump` and analyzed the actual signatures of the ad SDKs included in this version.

*   **LevelPlay (`com.ironsource.unity.androidbridge.InterstitialAd`)**:
    *   *Old Behavior*: Expected a listener method called `onAdOpened`.
    *   *New Behavior*: The method was renamed/updated to `onAdDisplayed(Ljava/lang/String;)V`.
    *   *Action*: Updated `AdsPatch.kt` to intercept `onAdDisplayed` and added `InterstitialAdSetupListenerFingerprint` to locate the exact hook point.
*   **Tapjoy (`com.tapjoy.TJPlacement`)**:
    *   *Old Behavior*: Expected to retrieve the placement listener via a `getListener()` method.
    *   *New Behavior*: The `getListener()` method was stripped or obfuscated. The listener is now stored directly in a private instance field `b` of type `Lcom/tapjoy/TJPlacementListener;`.
    *   *Action*: Updated the Smali bytecode injection to read the listener directly using `iget-object vX, p0, Lcom/tapjoy/TJPlacement;->b:Lcom/tapjoy/TJPlacementListener;`.

## 3. Google Play Asset Delivery Bypass (Error -5)
### **The Problem**
After fixing the Ad crashes, the game still froze. Logcat revealed the following Unity error:
> `InvalidOperationException: PlayAssetDelivery AssetPackManager API is not available!`

Because Morphe (and any modding tool) re-signs the APK with a custom keystore, the Google Play Store's `AssetPackManager` refuses to serve asset packs (split APKs containing Unity bundles) to the app. It returns error `-5` (`API_NOT_AVAILABLE`). 
When Unity receives this error, it throws a C# exception. `GameBalancingManager.ApplyNewConfig` then hits a `NullReferenceException` because it expects the assets to exist, permanently freezing the game on a black/splash screen.

### **The Fix (`PatchPlayCore.kt`)**
To fix this, we had to heavily spoof the `PlayAssetDeliveryUnityWrapper` Java class to completely trick Unity into thinking all assets were already downloaded and available locally. 
Unity's `AssetBundleManager` has a built-in fallback: if Play Asset Delivery reports assets as "completed", it will look for them in the standard `Application.dataPath + "!/assets/"` folder.

We used Smali instruction replacement (`replaceInstructions` / `addInstructions`) to rewrite the following methods:
1.  **`playCoreApiMissing()`**: Forced to return `false`.
2.  **`a()`**: An internal sanity check method that throws a `RuntimeException`. We muted it to return `void`.
3.  **`getAssetPackPath()`**: Forced to return `""` (empty string). This triggers Unity's C# fallback path.
4.  **`getAssetPackState()` & `getAssetPackStates()`**: Completely rewritten to accept the asset pack names and immediately fire the `IAssetPackManagerStatusQueryCallback.onStatusResult` callback. We construct fake integer arrays reporting state `4` (COMPLETED) and error code `0` (NO_ERROR).
5.  **`downloadAssetPack()` & `downloadAssetPacks()`**: Rewritten to fire the `IAssetPackManagerDownloadStatusCallback.onStatusUpdate` callback immediately, reporting status `4` (COMPLETED), `100` percent downloaded, and `0` bytes transferred.

*Note for Future Updates: As long as the `PlayAssetDeliveryUnityWrapper` class structure does not change, this bypass will remain bulletproof. If Unity updates their PlayCore plugin, the registers (`v0`-`v11`) in the Smali bytecode may need to be adjusted based on the new method signatures.*

## 4. Native IAP Static Patch (`libil2cpp.so`)
### **The Problem**
In-App Purchases (IAP) logic is baked into the AOT-compiled C++ binary (`libil2cpp.so`). Modifying this requires Hex editing specific byte sequences (anchors).

### **The Fix**
We cross-referenced the 12 hex anchors in `NativeIapStaticPatch.kt` against the extracted `libil2cpp.so` (72,465,488 bytes) from v1.89.0. 
*   **Result**: All 12 anchors (e.g., `0x1f66ee0`, `0x1d76440`, etc.) generated exactly 1 match (`count = 1`) at the expected offsets. The bytecode signatures used by PikPok have not changed in this version. The patch was safely re-enabled (`default = true`).

## 5. Repository Identity
Replaced all residual `okish-morphe-patches` references with `xnd-morphe-patches` across `build.gradle.kts`, `settings.gradle.kts`, `.github` issue templates, and `README.md`.

## Summary
The combination of DEX obfuscation changes (Ad listeners) and Google Play Asset Delivery strict signature enforcement created a scenario where the Unity engine was deadlocking before the first frame. By correcting the DEX reflection targets and writing a custom, register-perfect Smali wrapper to spoof PlayCore statuses, the game successfully initializes, falls back to local asset bundles, and runs flawlessly.
