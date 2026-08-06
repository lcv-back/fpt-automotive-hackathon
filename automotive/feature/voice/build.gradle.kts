import java.io.BufferedInputStream
import java.net.URI
import java.util.zip.ZipInputStream

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

fun downloadAndUnpackVoskModel(
    zipName: String,
    unpackedFolderName: String,
    destDir: java.io.File,
) {
    val marker = destDir.resolve("conf/model.conf")
    if (marker.exists()) {
        println("Vosk model already present at ${destDir.absolutePath}")
        return
    }
    val url = "https://alphacephei.com/vosk/models/$zipName"
    val zipFile = destDir.parentFile.resolve(zipName)
    destDir.parentFile.mkdirs()
    println("Downloading Vosk model from $url …")
    URI(url).toURL().openStream().use { input ->
        zipFile.outputStream().use { output -> input.copyTo(output) }
    }
    val extractRoot = destDir.parentFile.resolve("vosk-model-unpack-$unpackedFolderName")
    if (extractRoot.exists()) extractRoot.deleteRecursively()
    extractRoot.mkdirs()
    ZipInputStream(BufferedInputStream(zipFile.inputStream())).use { zis ->
        var entry = zis.nextEntry
        while (entry != null) {
            val target = extractRoot.resolve(entry.name)
            if (entry.isDirectory) {
                target.mkdirs()
            } else {
                target.parentFile.mkdirs()
                target.outputStream().use { output -> zis.copyTo(output) }
            }
            zis.closeEntry()
            entry = zis.nextEntry
        }
    }
    val unpacked = extractRoot.resolve(unpackedFolderName)
    require(unpacked.resolve("conf/model.conf").exists()) {
        "Failed to unpack Vosk model $zipName"
    }
    if (destDir.exists()) destDir.deleteRecursively()
    require(unpacked.renameTo(destDir)) { "Failed to move $unpacked → $destDir" }
    zipFile.delete()
    extractRoot.deleteRecursively()
    println("Vosk model saved to ${destDir.absolutePath}")
}

val voskEnDir = layout.projectDirectory.dir("src/main/assets/model-en-us")
val voskViDir = layout.projectDirectory.dir("src/main/assets/model-vi")

val downloadVoskEnModel by tasks.registering {
    group = "vosk"
    description = "Download Vosk small en-US model into assets if missing"
    notCompatibleWithConfigurationCache("Downloads Vosk model over the network")
    outputs.file(voskEnDir.file("conf/model.conf"))
    doLast {
        downloadAndUnpackVoskModel(
            zipName = "vosk-model-small-en-us-0.15.zip",
            unpackedFolderName = "vosk-model-small-en-us-0.15",
            destDir = voskEnDir.asFile,
        )
    }
}

val downloadVoskViModel by tasks.registering {
    group = "vosk"
    description = "Download Vosk Vietnamese vn-0.4 model into assets if missing"
    notCompatibleWithConfigurationCache("Downloads Vosk model over the network")
    outputs.file(voskViDir.file("conf/model.conf"))
    doLast {
        // Ban SMALL, khong phai ban lon — co chu dinh.
        //
        // Ca hai cung WER 15.70 tren VIVOS theo cong bo cua Alphacephei, nhung
        // chung khac nhau o cach dung do thi:
        //
        //   vosk-model-vn-0.4        HCLG.fst (158 MB)              do thi TINH
        //   vosk-model-small-vn-0.4  HCLr.fst + Gr.fst + disambig   do thi DONG
        //
        // Chi do thi dong moi nhan grammar luc chay. Voi do thi tinh, Vosk AM
        // THAM bo qua grammar va van giai ma tren toan bo 19.529 tu — do la ly
        // do cau "tang nhiet do len hai muoi tu do" tung ra
        // "chang nhiet do len ha my tu do".
        //
        // Doi sang ban nay de CommandVocabulary that su co tac dung. Kem theo:
        // APK nho hon 46 MB.
        downloadAndUnpackVoskModel(
            zipName = "vosk-model-small-vn-0.4.zip",
            unpackedFolderName = "vosk-model-small-vn-0.4",
            destDir = voskViDir.asFile,
        )
    }
}

val embeddingDir = layout.projectDirectory.dir("src/main/assets/embeddings")
val embeddingModel = embeddingDir.file("model_quantized.onnx")
val embeddingVocab = embeddingDir.file("vocab.txt")
val embeddingModelUrl =
    "https://huggingface.co/Xenova/all-MiniLM-L6-v2/resolve/main/onnx/model_quantized.onnx"
val embeddingVocabUrl =
    "https://huggingface.co/Xenova/all-MiniLM-L6-v2/resolve/main/vocab.txt"

val downloadEmbeddingModel by tasks.registering {
    group = "embedding"
    description = "Download MiniLM ONNX + vocab into assets if missing"
    notCompatibleWithConfigurationCache("Downloads embedding model over the network")
    outputs.files(embeddingModel, embeddingVocab)
    doLast {
        val dir = embeddingDir.asFile
        dir.mkdirs()
        val modelFile = embeddingModel.asFile
        val vocabFile = embeddingVocab.asFile
        if (!modelFile.exists()) {
            println("Downloading MiniLM ONNX from $embeddingModelUrl …")
            URI(embeddingModelUrl).toURL().openStream().use { input ->
                modelFile.outputStream().use { output -> input.copyTo(output) }
            }
        }
        if (!vocabFile.exists()) {
            println("Downloading MiniLM vocab from $embeddingVocabUrl …")
            URI(embeddingVocabUrl).toURL().openStream().use { input ->
                vocabFile.outputStream().use { output -> input.copyTo(output) }
            }
        }
        println("Embedding assets ready in ${dir.absolutePath}")
    }
}

tasks.named("preBuild").configure {
    dependsOn(downloadVoskEnModel, downloadVoskViModel, downloadEmbeddingModel)
}

android {
    namespace = "com.sopa.viva_automotive.feature.voice"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 32
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }

    androidResources {
        noCompress += "onnx"
    }
}

dependencies {
    implementation(project(":voice-core"))
    implementation(project(":core:common"))
    implementation(project(":core:ui"))
    implementation(project(":core:database"))
    implementation(project(":vehicle-service:api"))

    implementation(libs.vosk.android)
    implementation(libs.onnxruntime.android)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    // `org.json` trong android.jar chỉ là stub, mọi lời gọi ném "not mocked" khi
    // chạy trên JVM. Bản thật này để `CommandVocabularyTest` kiểm được rằng
    // grammar sinh ra thực sự parse được — thứ mà Vosk sẽ im lặng bỏ qua nếu hỏng.
    testImplementation(libs.org.json)
}
