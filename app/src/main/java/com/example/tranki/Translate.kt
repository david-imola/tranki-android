package com.example.tranki


import android.content.Context
import android.widget.Toast
import com.google.android.gms.tasks.OnSuccessListener
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference


class Translate {

    private lateinit var options: TranslatorOptions
    private lateinit var translator: Translator

    fun init(sourceLanguage: String, targetLanguage: String, onSuccess: () -> Unit, onFailure: (e: Exception) -> Unit) {
        options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLanguage)
            .setTargetLanguage(targetLanguage)
            .build()
        translator = Translation.getClient(options)

        val conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()
        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    fun getAllLanguages(): List<String?> {
        return TranslateLanguage.getAllLanguages()
    }

    fun translate(inputText: String, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        translator.translate(inputText).addOnSuccessListener { translatedText ->
            // Translation successful.
            onSuccess(translatedText)
        }.addOnFailureListener { exception ->
            onFailure(exception)
        }
    }

}
