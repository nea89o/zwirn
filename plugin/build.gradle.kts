plugins {
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "1.2.1"
}


dependencies {
    api(project(":"))
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("net.minecraftforge:srgutils:0.4.13")
    implementation("cuchaz:enigma-cli:2.4.1")
}


gradlePlugin {
    val plugin by plugins.creating {
        id = "moe.nea.zwirn"
        implementationClass = "moe.nea.zwirn.plugin.ZwirnPlugin"
    }
}

