plugins {
    id("java-library")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    compileOnly("com.google.errorprone:error_prone_annotations:2.36.0")
    implementation(libs.annotation)
}
