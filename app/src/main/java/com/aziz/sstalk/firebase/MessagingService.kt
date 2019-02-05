package com.aziz.sstalk.firebase

import android.util.Log
import com.aziz.sstalk.utils.FirebaseUtils
import com.aziz.sstalk.utils.Pref
import com.aziz.sstalk.utils.utils
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MessagingService: FirebaseMessagingService() {

    override fun onMessageReceived(p0: RemoteMessage?) {
        super.onMessageReceived(p0)

        Log.d("MessagingService", "onMessageReceived: ${p0!!.data.toString()}")
        var data = p0.data


        if(utils.isAppIsInBackground(this))
            Log.d("MessagingService", "onMessageReceived: App is in background")
        else {
            Log.d("MessagingService", "onMessageReceived: App is in foreground")
            utils.vibrate(this)
        }

    }




    override fun onNewToken(p0: String?) {

        val token = p0


        Pref.storeToken(this, token!!)


        super.onNewToken(p0)
    }
}