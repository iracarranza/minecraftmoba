plugins { java }
group = "com.minecraftmoba"
version = "0.1.0"
repositories { mavenCentral(); maven("https://repo.papermc.io/repository/maven-public/") }
dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("io.netty:netty-transport:4.2.7.Final")
}
java { toolchain.languageVersion.set(JavaLanguageVersion.of(21)) }
tasks.withType<JavaCompile>().configureEach { options.release.set(21) }
tasks.jar { manifest.attributes["paperweight-mappings-namespace"] = "mojang" }
