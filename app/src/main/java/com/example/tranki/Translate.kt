package com.example.tranki

import android.content.Context
import android.util.Log
import androidx.core.text.HtmlCompat
import com.google.api.Service
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.translate.Language
import com.google.cloud.translate.Translate
import com.google.cloud.translate.Translate.TranslateOption
import com.google.cloud.translate.TranslateException
import com.google.cloud.translate.TranslateOptions
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream


object Translate {

    private const val CLOUD_API_FILE = "translate_api_creds.json"
    private const val GOOGLE_APPLICATION_CREDENTIALS = "GOOGLE_APPLICATION_CREDENTIALS"


    private fun translateService(credsJson: String): Triple<Translate?, Exception?, String?> {
        var translateOptions : TranslateOptions? = null
        var exception : Exception? = null
        var msg: String? = null
        runBlocking {
            launch(Dispatchers.IO) {
                try {
                    translateOptions = TranslateOptions.newBuilder().setCredentials(
                        GoogleCredentials.fromStream(
                            ByteArrayInputStream(credsJson.toByteArray())
                        )
                    ).build()
                } catch (e: Exception) {
                    translateOptions = null
                     msg = "Translate Builder Exception occurred: " +
                            (e.message ?: "Unknown Translate Builder Exception")
                    Log.e("Translation", msg, e)
                    exception = e
                }
            }}
        return Triple(translateOptions?.service, exception, msg)
    }

    private fun writeFile(context: Context, path: String, credsJson: String) {
        val outputStream = context.openFileOutput(path, Context.MODE_PRIVATE)
        outputStream.write(credsJson.toByteArray())
        outputStream.close()
    }

    fun test(context: Context, credsJson: String): Pair<Boolean, String> {

        if (credsJson.isBlank()) {
            return Pair(false, "Error: Blank string")
        }

        val translateServiceTriple = translateService(credsJson)
        if (translateServiceTriple.second != null) {
            return Pair(false, translateServiceTriple.third!!)
        }
        val translateService = translateServiceTriple.first!!

        val text = "Hello, world!"
        val targetLanguage = "es"

        var success = true
        var message = ""
        runBlocking {
            launch(Dispatchers.IO) {
                try {
                    val output = translateService.translate(
                        text,
                        Translate.TranslateOption.targetLanguage(targetLanguage)
                    )
                    Log.d("Translation", "translated text in test: $output")
                } catch (e: TranslateException) {
                    success = false
                    message = "Translate Exception occurred: " +
                            (e.message ?: "Unknown Translate Exception")
                    Log.e("Translation", message, e)
                }
            }
        }
        return Pair(success, message)
    }

    fun getLanguages(credsJson: String): List<Pair<String, String>> {
        val translateServiceTriple = translateService(credsJson)
        if (translateServiceTriple.second != null)
            throw translateServiceTriple.second!!

        val resultList = mutableListOf<Pair<String, String>>()
        val translateService = translateServiceTriple.first!!
        var languages : List<Language>? = null
        runBlocking {
            launch(Dispatchers.IO) {
                languages = translateService.listSupportedLanguages()
            }
        }
        for (l in languages!!) {
            resultList.add(Pair(l.code, l.name))
        }
        return  resultList
    }

    fun translate(credsJson: String, inputText: String, inputLang: String, outputLang: String): String {
        val translateServiceTriple = translateService(credsJson)
        if (translateServiceTriple.second != null)
            throw translateServiceTriple.second!!

        var ret : String = ""
        val translateService = translateServiceTriple.first!!
        runBlocking {
            launch(Dispatchers.IO) {
                val result = translateService.translate(
                    inputText,
                    TranslateOption.sourceLanguage(inputLang),
                    TranslateOption.targetLanguage(outputLang)
                )
                ret = result.translatedText
            }
        }
        return HtmlCompat.fromHtml(ret, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
    }
}