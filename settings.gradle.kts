rootProject.name = "sxt-template"

dependencyResolutionManagement {

    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)

    repositories {
        mavenCentral()
        mavenLocal()
        maven {
            name = "Github"
            url = uri("https://maven.pkg.github.com/sicarx/*")
            credentials {
                username = System.getenv("MAVEN_USER")
                password = System.getenv("MAVEN_SECRET")
            }
        }
    }
}

pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        maven {
            name = "Github"
            url = uri("https://maven.pkg.github.com/sicarx/*")
            credentials {
                username = System.getenv("MAVEN_USER")
                password = System.getenv("MAVEN_SECRET")
            }
        }
        gradlePluginPortal()
    }
}