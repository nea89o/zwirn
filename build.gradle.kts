plugins {
    `java-library`
    `maven-publish`
}

group = "moe.nea"

allprojects {
    version = "1.0-SNAPSHOT"
    repositories {
        mavenCentral()
        maven("https://maven.architectury.dev")
        maven("https://maven.fabricmc.net")
        maven("https://maven.minecraftforge.net")
    }
}

dependencies {
    compileOnly("org.jetbrains:annotations:24.0.0")
    implementation("net.minecraftforge:srgutils:0.4.13")
    api("net.fabricmc:stitch:0.6.2")
    api("net.fabricmc:tiny-remapper:0.8.6")
    implementation("de.siegmar:fastcsv:3.0.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
}

java {
    this.toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    withSourcesJar()
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}