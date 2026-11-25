package app.intothedead2.patches.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags

// ---------------------------------------------------------------------------
// T4 — Rewarded ads, instant grant (LevelPlay new-API bridge, classes7).
//
// The C# LevelPlayRewardedAd.ShowAd entry lands in RewardedAd.showAd. The
// patch fakes availability (isAdReady, isPlacementCapped, loadAd) and fires a
// synthetic displayed-rewarded-closed lifecycle in showAd, resolving the
// reward name and amount live via getReward so no placement or reward-name
// constant is ever hardcoded.
// ---------------------------------------------------------------------------

// RewardedAd.isAdReady()Z — public. Forwards to LevelPlayRewardedAd.isAdReady.
// DEX: classes7/com/ironsource/unity/androidbridge/RewardedAd (.registers 2).
// Single filter: only caller of LevelPlayRewardedAd.isAdReady in this class.
object RewardedAdIsAdReadyFingerprint : Fingerprint(
    definingClass = "Lcom/ironsource/unity/androidbridge/RewardedAd;",
    name = "isAdReady",
    returnType = "Z",
    accessFlags = listOf(AccessFlags.PUBLIC),
    parameters = listOf(),
    filters = listOf(
        methodCall(
            definingClass = "Lcom/unity3d/mediation/rewarded/LevelPlayRewardedAd;",
            name = "isAdReady",
        )
    )
)

// RewardedAd.isPlacementCapped(String)Z — public static. Forwards to
// LevelPlayRewardedAd.isPlacementCapped. Smali: RewardedAd.smali (.registers 1).
// Single filter: only caller of LevelPlayRewardedAd.isPlacementCapped here.
object RewardedAdIsPlacementCappedFingerprint : Fingerprint(
    definingClass = "Lcom/ironsource/unity/androidbridge/RewardedAd;",
    name = "isPlacementCapped",
    returnType = "Z",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    parameters = listOf("Ljava/lang/String;"),
    filters = listOf(
        methodCall(
            definingClass = "Lcom/unity3d/mediation/rewarded/LevelPlayRewardedAd;",
            name = "isPlacementCapped",
        )
    )
)

// RewardedAd.loadAd()V — public. Forwards to LevelPlayRewardedAd.loadAd.
// Smali: RewardedAd.smali (.registers 2).
// Single filter: only caller of LevelPlayRewardedAd.loadAd in this class.
object RewardedAdLoadAdFingerprint : Fingerprint(
    definingClass = "Lcom/ironsource/unity/androidbridge/RewardedAd;",
    name = "loadAd",
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC),
    parameters = listOf(),
    filters = listOf(
        methodCall(
            definingClass = "Lcom/unity3d/mediation/rewarded/LevelPlayRewardedAd;",
            name = "loadAd",
        )
    )
)

// RewardedAd.setupRewardedListener(IUnityRewardedAdListener)V — private.
// Called from the constructor with the C# proxy; wraps it in RewardedAd$1 and
// registers it via LevelPlayRewardedAd.setListener. The patch stores the proxy
// in a new instance field here so showAd can fire the lifecycle directly.
// Smali: RewardedAd.smali (.registers 4). Filter order matches smali:
// anonymous listener build first, then setListener.
object RewardedAdSetupListenerFingerprint : Fingerprint(
    definingClass = "Lcom/ironsource/unity/androidbridge/RewardedAd;",
    name = "setupRewardedListener",
    returnType = "V",
    accessFlags = listOf(AccessFlags.PRIVATE),
    parameters = listOf("Lcom/ironsource/unity/androidbridge/IUnityRewardedAdListener;"),
    filters = listOf(
        methodCall(
            definingClass = "Lcom/ironsource/unity/androidbridge/RewardedAd\$1",
            name = "<init>",
        ),
        methodCall(
            definingClass = "Lcom/unity3d/mediation/rewarded/LevelPlayRewardedAd;",
            name = "setListener",
        )
    )
)

// RewardedAd.showAd(String)V — public. Forwards to
// LevelPlayRewardedAd.showAd(Activity, String). Smali: RewardedAd.smali
// (.registers 4, iget mRewardedAd + iget mActivity + showAd).
// Single filter: only caller of LevelPlayRewardedAd.showAd in this class.
object RewardedAdShowAdFingerprint : Fingerprint(
    definingClass = "Lcom/ironsource/unity/androidbridge/RewardedAd;",
    name = "showAd",
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC),
    parameters = listOf("Ljava/lang/String;"),
    filters = listOf(
        methodCall(
            definingClass = "Lcom/unity3d/mediation/rewarded/LevelPlayRewardedAd;",
            name = "showAd",
        )
    )
)

// ---------------------------------------------------------------------------
// T5 — Startup interstitial kill (LevelPlay new-API bridge, classes7).
//
// InterstitialAd.setupInterstitialListener captures the C# proxy listener and
// InterstitialAd.showAd(String)V fires synthetic onAdDisplayed + onAdClosed
// callbacks with the live placementName so C# does not stall waiting on close.
// Banners are intentionally NOT covered here.
// ---------------------------------------------------------------------------
object InterstitialAdSetupListenerFingerprint : Fingerprint(
    definingClass = "Lcom/ironsource/unity/androidbridge/InterstitialAd;",
    name = "setupInterstitialListener",
    returnType = "V",
    accessFlags = listOf(AccessFlags.PRIVATE),
    parameters = listOf("Lcom/ironsource/unity/androidbridge/IUnityInterstitialAdListener;"),
    filters = listOf(
        methodCall(
            definingClass = "Lcom/ironsource/unity/androidbridge/InterstitialAd\$1",
            name = "<init>",
        ),
        methodCall(
            definingClass = "Lcom/unity3d/mediation/interstitial/LevelPlayInterstitialAd;",
            name = "setListener",
        )
    )
)

object InterstitialAdShowAdFingerprint : Fingerprint(
    definingClass = "Lcom/ironsource/unity/androidbridge/InterstitialAd;",
    name = "showAd",
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC),
    parameters = listOf("Ljava/lang/String;"),
    filters = listOf(
        methodCall(
            definingClass = "Lcom/unity3d/mediation/interstitial/LevelPlayInterstitialAd;",
            name = "showAd",
        )
    )
)

// ---------------------------------------------------------------------------
// L3 — Tapjoy choke point (covers both TapjoyConnectUnity.showPlacementContent
// and the SDK-internal AppLaunch auto-show path, classes3).
//
// TJPlacement.showContent()V — public. Sole funnel for bridge placements,
// the AppLaunch auto-show chain, and InsufficientCurrency placements.
// DEX: classes3/com/tapjoy/TJPlacement (.registers 4).
// Filter order matches DEX: log literal first, then the isContentAvailable
// gate, then the TJCorePlacement.c render call.
// ---------------------------------------------------------------------------
object TJPlacementShowContentFingerprint : Fingerprint(
    definingClass = "Lcom/tapjoy/TJPlacement;",
    name = "showContent",
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC),
    parameters = listOf(),
    filters = listOf(
        string("showContent() called for placement "),
        methodCall(
            definingClass = "Lcom/tapjoy/TJCorePlacement;",
            name = "isContentAvailable",
        ),
        methodCall(
            definingClass = "Lcom/tapjoy/TJCorePlacement;",
            name = "c",
        )
    )
)

// ---------------------------------------------------------------------------
// L2 — Meta Audience Network direct show kill, parity (classes4).
//
// InterstitialAd.show()Z — public, plus the
// show(InterstitialShowAdConfig)Z overload. Both forward to
// InterstitialAdApi.show. Smali: classes4/com/facebook/ads/InterstitialAd.smali
// (.registers 2 and .registers 3). The fingerprint parameters pin each
// overload; com.facebook.ads API names are SDK stable.
// ---------------------------------------------------------------------------
object MetaInterstitialShowFingerprint : Fingerprint(
    definingClass = "Lcom/facebook/ads/InterstitialAd;",
    name = "show",
    returnType = "Z",
    accessFlags = listOf(AccessFlags.PUBLIC),
    parameters = listOf(),
    filters = listOf(
        methodCall(
            definingClass = "Lcom/facebook/ads/internal/api/InterstitialAdApi;",
            name = "show",
        )
    )
)

object MetaInterstitialShowWithConfigFingerprint : Fingerprint(
    definingClass = "Lcom/facebook/ads/InterstitialAd;",
    name = "show",
    returnType = "Z",
    accessFlags = listOf(AccessFlags.PUBLIC),
    parameters = listOf("Lcom/facebook/ads/InterstitialAd\$InterstitialShowAdConfig;"),
    filters = listOf(
        methodCall(
            definingClass = "Lcom/facebook/ads/internal/api/InterstitialAdApi;",
            name = "show",
        )
    )
)
