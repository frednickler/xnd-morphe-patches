group = "okish"

patches {
    about {
        name = "frednickler Patches"
        description = "Patches for apps I like"
        source = "https://github.com/frednickler/xnd-morphe-patches"
        author = "frednickler"
        contact = "na"
        website = "https://github.com/frednickler/xnd-morphe-patches"
        license = "GPLv3"
    }
}

dependencies {
    // Used by JsonGenerator.
    implementation(libs.gson)

    implementation(libs.morphe.patches.library)
}

tasks {
    register<JavaExec>("generatePatchesList") {
        description = "Build patch with patch list"

        dependsOn("buildAndroid")

        classpath = sourceSets["main"].runtimeClasspath
        mainClass.set("app.morphe.util.PatchListGeneratorKt")
    }

    // Used by gradle-semantic-release-plugin.
    publish {
        dependsOn("generatePatchesList")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs = listOf("-Xcontext-parameters")
    }
}
