pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        jcenter()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        jcenter()
        maven {
            setUrl("https://maven.aliyun.com/nexus/content/groups/public/")
        }
        maven { setUrl("https://maven.aliyun.com/repository/google") }
        maven { setUrl("https://maven.aliyun.com/repository/public") } //jcenter public 阿里最新地址
        maven { setUrl("https://jitpack.io") }
    }
}



rootProject.name = "CameraX-Samples"

include(":camerax-app")
include(":camerax-mlkit-app")
