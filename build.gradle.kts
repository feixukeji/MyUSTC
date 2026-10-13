plugins {
    id("com.android.application") version "9.4.1" apply false
}

tasks.register<Delete>("clean") {
    description = ""
    delete(rootProject.layout.buildDirectory)
}
