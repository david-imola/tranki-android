package com.example.tranki

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object Anki {

    private const val version = 6


    fun test(url: String, key: String?): Triple<Boolean, String, String?> {
        val versionEndpoint = "$url/version"
        val postData = if (key == null) {
            """{
                "action": "version",
                "version": $version
            }"""
        } else {
            """{
                "action": "version",
                "key": "$key",
                "version": $version
            }"""
        }

        val client = OkHttpClient()
        var request: Request
        try {
            request = Request.Builder()
                .url(versionEndpoint)
                .post(postData.toRequestBody("application/json".toMediaType()))
                .build()
        } catch (e: Exception) {
            val message = "AnkiConnect Builder Exception occurred: " +
                    (e.message ?: "Unknown AnkiConnect Builder Exception")
            Log.e("AnkiConnect", message, e)
            return Triple(false, message, null)
        }

        var result: Triple<Boolean, String, String?>? = null
        runBlocking {
            launch(Dispatchers.IO) {
                result = try {
                    var success = false
                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string()
                    if (response.isSuccessful) {
                        val jsonBody = JSONObject(responseBody)
                        if (jsonBody.getInt("result") == version && jsonBody.isNull("error"))
                            success = true
                    }
                    Triple(success, response.message, responseBody)
                } catch (e: Exception) {
                    val message = "AnkiConnect Client Execute Exception occurred: " +
                            (e.message ?: "Unknown AnkiConnect Client Execute Exception")
                    Log.e("AnkiConnect", message, e)
                    Triple(false, message, null)
                }
            }
        }
        return result!!
    }


    fun decks(url: String, key: String?): ArrayList<String>? {
        val decksEndpoint = "$url/getDecks"

        @Serializable
        data class Request(val action: String, val version: Int, val key: String?)

        val requestJson = Json.encodeToString(Request("deckNames", version, key))

        val client = OkHttpClient()
        var request = okhttp3.Request.Builder()
            .url(decksEndpoint)
            .post(requestJson.toRequestBody("application/json".toMediaType()))
            .build()


        var result: JSONObject? = null
        runBlocking {
            launch(Dispatchers.IO) {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                if (response.isSuccessful) {
                    val jsonBody = JSONObject(responseBody)
                    if (!jsonBody.isNull("result") && jsonBody.isNull("error"))
                        result = jsonBody
                }
            }
        }
        if (result == null)
            return null

        val resultList = ArrayList<String>()
        val l = result!!.getJSONArray("result")!!
        for (i in 0 until l.length()) {
            val item = l.getString(i)
            resultList.add(item)
        }
        return resultList
    }

    fun sync(url: String, key: String?, deck: String, items: List<Pair<String, String>>): String? {
        val addCardsEndpoint = "$url/addNotes"

        //@Serializable
        //data class DuplicateScopeOptions(val deckName: String, val checkChildren: Boolean, val checkAllModels: Boolean)
        @Serializable
        data class Options(val allowDuplicate: Boolean, val duplicateScope: String)

        @Serializable
        data class Fields(val Front: String, val Back: String)

        @Serializable
        data class Note(
            val deckName: String,
            val modelName: String,
            val fields: Fields,
            val options: Options
        )

        @Serializable
        data class Params(val notes: List<Note>)

        @Serializable
        data class Request(
            val action: String,
            val version: Int,
            val key: String?,
            val params: Params
        )

        val notes = ArrayList<Note>()
        for ((front, back) in items) {
            notes.add(
                Note(
                    deck,"Basic", Fields(front, back),
                    Options(false, "deck")
                )
            )
        }

        val req = Request("addNotes", version, key, Params(notes))
        val requestJson = Json.encodeToString(req)


        val client = OkHttpClient()
        var request = okhttp3.Request.Builder()
            .url(addCardsEndpoint)
            .post(requestJson.toRequestBody("application/json".toMediaType()))
            .build()


        var error: String? = null
        runBlocking {
            launch(Dispatchers.IO) {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                if (response.isSuccessful) {
                    val jsonBody = JSONObject(responseBody)
                    if (jsonBody.isNull("result") || !jsonBody.isNull("error")) {
                        error = try {
                            jsonBody.getString("error")
                        } catch (e: Exception) {
                            e.message
                        }
                    }
                } else
                    error = "failed or nonexistent response"
            }
        }
        return error
    }

}