package com.project.instagram.Navigation.util

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.project.instagram.Navigation.model.PushDTO
import com.squareup.okhttp.*
import java.io.IOException

class FcmPush {
    var JSON = MediaType.parse("application/json; charset=utf-8")
    var url = "https://fcm.googleapis.com/fcm/send"
    var serverKey = "AIzaSyCHQBX1Y_mxHPg9fZzEWQ_TBUduX1_y4vo"
    var gson: Gson? = null
    var okHttpClient: OkHttpClient? = null

    companion object {
        var instance = FcmPush()
    }

    init {
        gson = Gson()
        okHttpClient = OkHttpClient()
    }

    fun sendMessage(destinationUid: String, title: String, message: String) {
        FirebaseFirestore.getInstance().collection("pushTokens").document(destinationUid).get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result?.get("pushToken").toString()

                    val pushDTO = PushDTO()
                    pushDTO.to = token
                    pushDTO.notification.title = title
                    pushDTO.notification.body = message

                    val body = RequestBody.create(JSON, gson?.toJson(pushDTO))
                    var request = Request.Builder().addHeader("content-Type", "application/json")
                        .addHeader("Authorization", "key=" + serverKey).url(url).post(body).build()

                    okHttpClient?.newCall(request)?.enqueue(object : Callback {
                        override fun onFailure(request: Request?, e: IOException?) {
                            Log.d("FCM_PUSH", "FCM FAIL")
                        }

                        override fun onResponse(response: Response?) {
                            Log.d("FCM_PUSH", response?.body()?.string())
                            println(response?.body()?.string())
                        }

                    })
                }
            }
    }
}