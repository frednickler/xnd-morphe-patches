package app.intothedead2.patches.il2cpp

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.cloneMutable
import app.morphe.util.returnEarly
import app.intothedead2.patches.shared.Constants.COMPATIBILITY_INTO_THE_DEAD_2

val playCoreBypassPatch = bytecodePatch(
    name = "PlayCore Bypass",
    description = "Spoofs PlayAssetDeliveryUnityWrapper to always report assets as downloaded and available locally.",
    default = true
) {
    compatibleWith(COMPATIBILITY_INTO_THE_DEAD_2)
    execute {
        val clazz = mutableClassDefBy("Lcom/unity3d/player/PlayAssetDeliveryUnityWrapper;")
        
        clazz.methods.first { it.name == "playCoreApiMissing" }.returnEarly(false)
        clazz.methods.first { it.name == "a" && it.returnType == "V" && it.parameterTypes.isEmpty() }.returnEarly()
        clazz.methods.first { it.name == "getAssetPackPath" }.returnEarly("")
        
        // getAssetPackState
        val origGetState = clazz.methods.first { it.name == "getAssetPackState" }
        val getAssetPackState = origGetState.cloneMutable(additionalRegisters = 8)
        clazz.methods.remove(origGetState)
        clazz.methods.add(getAssetPackState)
        getAssetPackState.addInstructions(0, """
            move-object v0, p2
            const-wide/16 v1, 0x0
            const/4 v6, 0x1
            const/4 v7, 0x0
            new-array v3, v6, [Ljava/lang/String;
            aput-object p1, v3, v7
            new-array v4, v6, [I
            const/4 v8, 0x4
            aput v8, v4, v7
            new-array v5, v6, [I
            aput v7, v5, v7
            invoke-interface/range {v0 .. v5}, Lcom/unity3d/player/IAssetPackManagerStatusQueryCallback;->onStatusResult(J[Ljava/lang/String;[I[I)V
            return-void
        """.trimIndent())

        // getAssetPackStates
        val origGetStates = clazz.methods.first { it.name == "getAssetPackStates" }
        val getAssetPackStates = origGetStates.cloneMutable(additionalRegisters = 8)
        clazz.methods.remove(origGetStates)
        clazz.methods.add(getAssetPackStates)
        getAssetPackStates.addInstructions(0, """
            move-object v0, p2
            const-wide/16 v1, 0x0
            move-object v3, p1
            array-length v6, v3
            new-array v4, v6, [I
            new-array v5, v6, [I
            const/4 v7, 0x0
            :loop_start
            if-ge v7, v6, :loop_end
            const/4 v8, 0x4
            aput v8, v4, v7
            const/4 v8, 0x0
            aput v8, v5, v7
            add-int/lit8 v7, v7, 0x1
            goto :loop_start
            :loop_end
            invoke-interface/range {v0 .. v5}, Lcom/unity3d/player/IAssetPackManagerStatusQueryCallback;->onStatusResult(J[Ljava/lang/String;[I[I)V
            return-void
        """.trimIndent())
        
        // downloadAssetPack
        val origDownload = clazz.methods.first { it.name == "downloadAssetPack" }
        val downloadAssetPack = origDownload.cloneMutable(additionalRegisters = 8)
        clazz.methods.remove(origDownload)
        clazz.methods.add(downloadAssetPack)
        downloadAssetPack.addInstructions(0, """
            move-object v0, p2
            move-object v1, p1
            const/4 v2, 0x4
            const-wide/16 v3, 0x0
            const-wide/16 v5, 0x0
            const/16 v7, 0x64
            const/4 v8, 0x0
            invoke-interface/range {v0 .. v8}, Lcom/unity3d/player/IAssetPackManagerDownloadStatusCallback;->onStatusUpdate(Ljava/lang/String;IJJII)V
            return-void
        """.trimIndent())
        
        // downloadAssetPacks
        val origDownloads = clazz.methods.first { it.name == "downloadAssetPacks" }
        val downloadAssetPacks = origDownloads.cloneMutable(additionalRegisters = 8)
        clazz.methods.remove(origDownloads)
        clazz.methods.add(downloadAssetPacks)
        downloadAssetPacks.addInstructions(0, """
            move-object v9, p1
            array-length v10, v9
            const/4 v11, 0x0
            :loop_start_dl
            if-ge v11, v10, :loop_end_dl
            aget-object v1, v9, v11
            move-object v0, p2
            const/4 v2, 0x4
            const-wide/16 v3, 0x0
            const-wide/16 v5, 0x0
            const/16 v7, 0x64
            const/4 v8, 0x0
            invoke-interface/range {v0 .. v8}, Lcom/unity3d/player/IAssetPackManagerDownloadStatusCallback;->onStatusUpdate(Ljava/lang/String;IJJII)V
            add-int/lit8 v11, v11, 0x1
            goto :loop_start_dl
            :loop_end_dl
            return-void
        """.trimIndent())
        
        clazz.methods.first { it.name == "requestToUseMobileData" }.returnEarly()
        clazz.methods.first { it.name == "removeAssetPack" }.returnEarly()
        clazz.methods.first { it.name == "cancelAssetPackDownload" }.returnEarly()
        clazz.methods.first { it.name == "cancelAssetPackDownloads" }.returnEarly()
    }
}
