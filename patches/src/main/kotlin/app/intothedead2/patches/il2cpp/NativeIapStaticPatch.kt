package app.intothedead2.patches.il2cpp

import app.intothedead2.patches.shared.Constants.COMPATIBILITY_INTO_THE_DEAD_2
import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patches.all.misc.hex.HexPatchBuilder
import app.morphe.patches.all.misc.hex.hexPatch

/**
 * Static (on-disk) native IAP patch for Into the Dead 2 1.89.0.
 *
 * Replaces the old approach of shipping a helper .so that patched libil2cpp.so in memory at
 * runtime: the 12 decision points are rewritten directly in lib/arm64-v8a/libil2cpp.so while
 * the APK is being built. Nothing is loaded at runtime and there is no window after launch
 * where purchases are not yet intercepted.
 *
 * Targets (arm64, file offsets for 1.89.0):
 *   verdict                  0x1D76420  0x36000448 -> nop
 *   rcptst-dr2               0x1D76440  0xB9001A88 -> str wzr, [x20, #0x18]
 *   rcptst-base              0x202832C  0xB9001A88 -> str wzr, [x20, #0x18]
 *   complete-gate            0x2028BC8  0xB9401908 -> mov w8, #0
 *   subs-verdict             0x202B5F4  0x36000520 -> nop
 *   vip-gate-0/1             0x1DC4910  0xF81E0FFE / 0xA9014FF4 -> mov w0, #1 ; ret
 *   no-expiry                0x1DC1C10  0xA9BE57FE -> ret
 *   subs-rewards-0/1         0x1DC2930  0xD10103FF / 0xA9015FFE -> mov w0, #1 ; ret
 *   busy-0/1                 0x20297A0  0xD10143FF / 0xA90167FE -> mov w0, #0 ; ret
 *
 * Anchors were generated against the 1.89.0 split_config.arm64_v8a.apk libil2cpp.so
 * (72,465,488 B): every 12-24 byte pattern occurs exactly once in the file, spans do not
 * overlap, and the replacement set is verified order-independent.
 *
 * Updating for a new game version: re-derive the RVAs with Il2CppDumper, confirm the current
 * word at each site, then regenerate these anchors (see
 * analysis/com.pikpok.dr2.play/notes/native-engine-so-embedding.md). hexPatch throws a
 * PatchException when a pattern is missing, so a stale anchor fails the build instead of
 * producing an APK whose native patches silently do nothing.
 */
private const val LIB = "lib/arm64-v8a/libil2cpp.so"

private val iapSites: HexPatchBuilder.() -> Unit = {
        // verdict @ file 0x1d76420
        "E8 03 00 2A 60 1A 40 F9 48 04 00 36 40 19 00 B4 34 19 00 B4" asPatternTo
        "E8 03 00 2A 60 1A 40 F9 1F 20 03 D5 40 19 00 B4 34 19 00 B4" inFile LIB

        // rcptst-dr2 @ file 0x1d76440
        "E8 17 9F 1A 08 79 1F 53 88 1A 00 B9 49 18 00 B4 2A 26 01 B0" asPatternTo
        "E8 17 9F 1A 08 79 1F 53 9F 1A 00 B9 49 18 00 B4 2A 26 01 B0" inFile LIB

        // rcptst-base @ file 0x202832c
        "E8 17 9F 1A 08 79 1F 53 88 1A 00 B9 69 12 00 B4 8A 10 01 F0" asPatternTo
        "E8 17 9F 1A 08 79 1F 53 9F 1A 00 B9 69 12 00 B4 8A 10 01 F0" inFile LIB

        // complete-gate @ file 0x2028bc8
        "08 11 40 F9 28 44 00 B4 08 19 40 B9 08 21 00 34 69 12 40 F9" asPatternTo
        "08 11 40 F9 28 44 00 B4 08 00 80 52 08 21 00 34 69 12 40 F9" inFile LIB

        // subs-verdict @ file 0x202b5f4
        "E1 03 1F AA 6C A4 01 94 20 05 00 36 08 11 01 B0 E1 03 13 AA" asPatternTo
        "E1 03 1F AA 6C A4 01 94 1F 20 03 D5 08 11 01 B0 E1 03 13 AA" inFile LIB

        // vip-gate-0 @ file 0x1dc4910
        "F8 F8 7C 14 A0 CE F6 97 FE 0F 1E F8" asPatternTo
        "F8 F8 7C 14 A0 CE F6 97 20 00 80 52" inFile LIB

        // vip-gate-1 @ file 0x1dc491c
        "F4 4F 01 A9 F4 3A 01 F0 F3 03 00 AA 88 12 7C 39" asPatternTo
        "C0 03 5F D6 F4 3A 01 F0 F3 03 00 AA 88 12 7C 39" inFile LIB

        // no-expiry @ file 0x1dc1c10
        "E3 D9 F6 97 E0 D9 F6 97 FE 57 BE A9 F4 4F 01 A9 15 3B 01 D0" asPatternTo
        "E3 D9 F6 97 E0 D9 F6 97 C0 03 5F D6 F4 4F 01 A9 15 3B 01 D0" inFile LIB

        // subs-rewards-0 @ file 0x1dc2930
        "00 01 40 F9 F4 4F 42 A9 FE 57 41 A9 FF C3 00 91 C0 03 5F D6 FF 03 01 D1" asPatternTo
        "00 01 40 F9 F4 4F 42 A9 FE 57 41 A9 FF C3 00 91 C0 03 5F D6 20 00 80 52" inFile LIB

        // subs-rewards-1 @ file 0x1dc2948
        "FE 5F 01 A9 F6 57 02 A9 F4 4F 03 A9 14 3B 01 B0" asPatternTo
        "C0 03 5F D6 F6 57 02 A9 F4 4F 03 A9 14 3B 01 B0" inFile LIB

        // busy-0 @ file 0x20297a0
        "C0 03 5F D6 FC 3A ED 97 FF 43 01 D1" asPatternTo
        "C0 03 5F D6 FC 3A ED 97 00 00 80 52" inFile LIB

        // busy-1 @ file 0x20297ac
        "FE 67 01 A9 F8 5F 02 A9 F6 57 03 A9 F4 4F 04 A9 F4 27 01 90" asPatternTo
        "C0 03 5F D6 F8 5F 02 A9 F6 57 03 A9 F4 4F 04 A9 F4 27 01 90" inFile LIB
}

@Suppress("unused")
val nativeIapStaticPatch = rawResourcePatch(
    name = "Native IAP Patch",
    description = "Patches the game's native code directly so purchases and subscriptions " +
        "(including VIP) are approved on your own device, with no helper library loaded at " +
        "runtime.",
    default = true
) {
    compatibleWith(COMPATIBILITY_INTO_THE_DEAD_2)

    dependsOn(hexPatch(block = iapSites))
}
