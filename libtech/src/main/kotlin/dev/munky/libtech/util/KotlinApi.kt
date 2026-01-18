package dev.munky.libtech.util

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class KotlinFirstApi(
    val reason: String = "Sorry",
    val substitute: String = ""
)