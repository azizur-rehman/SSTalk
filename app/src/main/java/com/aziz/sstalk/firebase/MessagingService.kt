package com.aziz.sstalk.firebase

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.graphics.BitmapFactory
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.app.Person
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.IconCompat
import android.util.Log
import com.aziz.sstalk.HomeActivity
import com.aziz.sstalk.MessageActivity
import com.aziz.sstalk.utils.FirebaseUtils
import com.aziz.sstalk.utils.Pref
import com.aziz.sstalk.utils.utils
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.aziz.sstalk.R
import java.io.File

class MessagingService: FirebaseMessagingService() {

    private val KEY_UNREAD = "unreadCount"
    private val KEY_SENDER = "senderUID"
    private val KEY_RECEIVER = "receiverUID"
    private val KEY_MSG_IDs = "messageIDs"
    private val KEY_SENDER_PHONE = "senderPhoneNumber"
    private val KEY_SENDER_PIC_URL = "senderPhotoURL"
    private val KEY_MESSAGES = "messages"
    private val KEY_IS_MUTED = "isMuted"

    private val MESSAGE_SEPERATOR = "<--MESSAGE_SEPERATOR-->"

    object NotificationDetail {
        val SINGLE_ID = 123456
        val MUlTIPLE_ID = 654321
    }


    override fun onMessageReceived(p0: RemoteMessage?) {
        super.onMessageReceived(p0)

        Log.d("MessagingService", "onMessageReceived: ${p0!!.data.toString()}")
        val data: MutableMap<String, String>? = p0.data ?: return

        val sender = data!![KEY_SENDER]!!
        val receiver = data[KEY_RECEIVER]!!

        if(FirebaseUtils.getUid() != receiver)
            return


        setAllMessageFromUserAsDelivered(receiver, sender, data[KEY_MSG_IDs]!!)

        if(!data.containsKey(KEY_MESSAGES)){
            Log.d("MessagingService", "onMessageReceived: a silent notification has been generated")
            return
        }


        if(Pref.getCurrentTargetUID(this) == sender) {
            Log.d("MessagingService", "onMessageReceived: currently chatting with -> $sender")
            return
        }



                    if( utils.isAppIsInBackground(this@MessagingService)
                        || Pref.getCurrentTargetUID(this@MessagingService) != sender){
                        //app is in background show notification
                        showNotification(p0)
                    }
                    else {

                        if(Pref.Notification.hasVibrationEnabled(this@MessagingService))
                            utils.vibrate(this@MessagingService)
                    }





    }


    private fun showNotification(remoteMessage: RemoteMessage){
        val data = remoteMessage.data

        val totalMessage = data[KEY_UNREAD]
        val sender = data[KEY_SENDER]
        val senderPhone = data[KEY_SENDER_PHONE]


        val name = utils.getNameFromNumber(this@MessagingService, senderPhone!!)

        val intent = Intent(this@MessagingService, MessageActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(FirebaseUtils.KEY_UID, sender)
            putExtra(utils.constants.KEY_IS_ONCE, true)
        }

        notify(name, intent, remoteMessage)

        Log.d("MessagingService", "showNotification: sender = $sender")



    }


    private fun notify(title:String, intent: Intent, remoteMessage: RemoteMessage){
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this@MessagingService, NotificationDetail.SINGLE_ID, intent, 0)

        val data = remoteMessage.data


        val notification = NotificationCompat.Builder(this@MessagingService)
            .setContentTitle(title)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentText("Tap to read")
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setColor(ContextCompat.getColor(this@MessagingService, R.color.colorPrimary))
            .setLargeIcon((BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)))
            .setPriority(android.app.Notification.PRIORITY_MAX)
            .setAutoCancel(true)


        val isSoundEnabled = Pref.Notification.hasSoundEnabled(this)
        val isVibrationEnabled = Pref.Notification.hasVibrationEnabled(this)


        Log.d("MessagingService", "notify: sound = $isSoundEnabled , vibration = $isVibrationEnabled")


        if(isSoundEnabled && isVibrationEnabled){
            notification.setDefaults(Notification.DEFAULT_ALL)
        }

        else if(isSoundEnabled && !isVibrationEnabled){
            notification.setDefaults(Notification.DEFAULT_SOUND)
        }

        else if(!isSoundEnabled && isVibrationEnabled){
            notification.setDefaults(Notification.DEFAULT_VIBRATE)
        }
        else{
            notification.setDefaults(Notification.DEFAULT_LIGHTS)
        }

        // for single message
        updateNotificationWithBigText(remoteMessage, notification)

        getAllUnreadMessages(notification, remoteMessage)

    }


    private fun getAllUnreadMessages(notificationCompatBuilder: NotificationCompat.Builder, remoteMessage: RemoteMessage){
        var unreadCount = 0
        var unreadConversation = 0





        FirebaseUtils.ref.allMessageStatusRootRef()
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                }

                override fun onDataChange(p0: DataSnapshot) {
                    //iterate through every node to check for unread messages

                    for((index,snapshot) in p0.children.withIndex()){
                        snapshot.ref.orderByChild("read")
                            .equalTo(false)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onCancelled(p0: DatabaseError) {
                                }

                                override fun onDataChange(p1: DataSnapshot) {


                                    if (!p1.exists()) return
                                    unreadCount += p1.childrenCount.toInt()
                                    unreadConversation++

                                    if (index == p0.childrenCount.toInt() - 1) {

                                        if(index == 0)
                                            return



                                        if(unreadConversation<=1) {
                                            //updateNotificationWithBigText(remoteMessage, notificationCompatBuilder)
                                            return
                                        }


                                        val title = "$unreadCount messages from $unreadConversation conversations"


                                        val intent = Intent(this@MessagingService, HomeActivity::class.java).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        }
                                        with(NotificationManagerCompat.from(this@MessagingService)){
                                            notify( NotificationDetail.MUlTIPLE_ID, notificationCompatBuilder.setStyle(NotificationCompat.BigTextStyle()
                                                .setBigContentTitle(title)
                                                .bigText("Tap to Read"))
                                                .setDefaults(Notification.DEFAULT_LIGHTS)
                                                .setSmallIcon(R.mipmap.ic_launcher_round)
                                                .setColor(ContextCompat.getColor(this@MessagingService, R.color.colorPrimary))
                                                .setLargeIcon((BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)))
                                                .setContentIntent(PendingIntent.getActivity(this@MessagingService, NotificationDetail.MUlTIPLE_ID, intent, 0))
                                                .setContentTitle(title)
                                                .build())
                                        }
                                    }
                                }
                            })
                    }

                }
            })
    }


    private fun updateNotificationWithBigText(
        remoteMessage: RemoteMessage,
        notificationCompatBuilder: NotificationCompat.Builder
    ){


        val profilePicFile = File(utils.getProfilePicPath(this)+remoteMessage.data[KEY_SENDER]!!+".jpg")

        val messages = getMessages(remoteMessage.data[KEY_MESSAGES]!!)


        val person = Person.Builder().setName(utils.getNameFromNumber(this,
            remoteMessage.data[KEY_SENDER_PHONE]!!))

           if(utils.hasStoragePermission(this)) {
               if(profilePicFile.exists()) {
                   person.setIcon(IconCompat.createWithBitmap(utils.getCircleBitmap(BitmapFactory.decodeFile(profilePicFile.path))))
//                   notificationCompatBuilder.setLargeIcon(utils.getCircleBitmap(BitmapFactory.decodeFile(profilePicFile.path)))
               }
               else{
                   Log.d("MessagingService", "updateNotificationWithBigText: profile doesn't exists")
               }
           }

        val style = NotificationCompat.MessagingStyle(person.build())


        messages.forEach {
//            Log.d("MessagingService", "updateNotificationWithBigText: messages = $it")
            if(it.trim().isNotEmpty() && messages.size<=5)
            style.addMessage(
                it.replace(MESSAGE_SEPERATOR,"").trim(), System.currentTimeMillis(), person.build()
            )
        }


        notificationCompatBuilder.setStyle(style)

        notificationCompatBuilder.setNumber(messages.size)

        with(NotificationManagerCompat.from(this@MessagingService)){
            notify( NotificationDetail.SINGLE_ID, notificationCompatBuilder.build())
        }


    }


    private fun setAllMessageFromUserAsDelivered(uid:String, targetUID:String, msgIDs:String){

        val ids = getIDs(msgIDs)

        ids.forEach {
            FirebaseUtils.ref.messageStatus(uid, targetUID, it)
                .child("delivered")
                .setValue(true)
        }
    }

    fun getIDs(msgIDsString: String): List<String> =
        msgIDsString.replace(",", " ").trim().split(" ")

    fun getMessages(messageString: String): List<String> =
        messageString.split(MESSAGE_SEPERATOR)
    //should remove message seperator on last value

    override fun onNewToken(p0: String?) {

        val token = p0


        Pref.storeToken(this, token!!)
        FirebaseUtils.updateFCMToken()


        super.onNewToken(p0)
    }


}