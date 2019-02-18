package com.aziz.sstalk.firebase

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.app.Person
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
import com.aziz.sstalk.models.Models
import java.io.File

class MessagingService: FirebaseMessagingService() {

    private val KEY_UNREAD = "unreadCount"
    private val KEY_SENDER = "senderUID"
    private val KEY_RECEIVER = "receiverUID"
    private val KEY_MSG_IDs = "messageIDs"
    private val KEY_SENDER_PHONE = "senderPhoneNumber"

    object NotificationDetail {
        val ID = 13213
    }


    override fun onMessageReceived(p0: RemoteMessage?) {
        super.onMessageReceived(p0)

        Log.d("MessagingService", "onMessageReceived: ${p0!!.data.toString()}")
        val data: MutableMap<String, String>? = p0.data ?: return

        //todo change to FirebaseUtils.getUid

        if(FirebaseUtils.getUid() != data!![KEY_RECEIVER]!!)
            return

        setAllMessageFromUserAsDelivered(data[KEY_RECEIVER]!!, data[KEY_SENDER]!!, data[KEY_MSG_IDs]!!)


        if(Pref.getCurrentTargetUID(this) == data[KEY_SENDER]!!) {
            Log.d("MessagingService", "onMessageReceived: currently chatting with -> ${data[KEY_SENDER]}]")
            return
        }


        FirebaseUtils.ref.getNotificationMuteRef(data[KEY_SENDER]!!)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                }

                override fun onDataChange(snapshot: DataSnapshot) {
                    if(snapshot.exists()) {
                        if (snapshot.getValue(Boolean::class.java)!!) {
                            Log.d("MessagingService", "onDataChange: muted")
                            return
                        }
                    }
                    if(//true
                        utils.isAppIsInBackground(this@MessagingService)
                    ){
                        //app is in background show notification
                        Log.d("MessagingService", "onDataChange: is in background")
                        showNotification(p0)
                    }
                    else {

                        if(Pref.Notification.hasVibrationEnabled(this@MessagingService)
                        )
                            utils.vibrate(this@MessagingService)
                    }
                }
            })




    }


    private fun showNotification(remoteMessage: RemoteMessage){
        val data = remoteMessage.data

        val totalMessage = data[KEY_UNREAD]
        val sender = data[KEY_SENDER]
        val senderPhone = data[KEY_SENDER_PHONE]


        val name = utils.getNameFromNumber(this@MessagingService, senderPhone!!)
        val title = "$totalMessage ${if(totalMessage!!.toInt()==1) "message" else "messages"} from $name"

        Log.d("MessagingService", "onDataChange: sending notification with title = $title")
        val intent = Intent(this@MessagingService, MessageActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(FirebaseUtils.KEY_UID, sender)
            putExtra(utils.constants.KEY_IS_ONCE, true)
        }

        notify(title, intent, remoteMessage)

        Log.d("MessagingService", "showNotification: sender = $sender")



    }


    private fun notify(title:String, intent: Intent, remoteMessage: RemoteMessage){
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this@MessagingService, NotificationDetail.ID, intent, 0)

        val data = remoteMessage.data


        val notification = NotificationCompat.Builder(this@MessagingService)
            .setContentTitle(title)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentText("Tap to read")
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
            //.setPriority(android.app.Notification.PRIORITY_MAX)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)


//        if(!Pref.NotificationDetail.hasVibrationEnabled(this@MessagingService)) {
//            //if(Build.VERSION.SDK_INT<Build.VERSION_CODES.O)
//            notification.setVibrate(longArrayOf(0L))
//
//        }
//
//        if(Pref.NotificationDetail.hasSoundEnabled(this@MessagingService))
//            notification.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))


        with(NotificationManagerCompat.from(this@MessagingService)){
            notify(  NotificationDetail.ID, notification.build())
        }

        updateNotificationWithBigText(remoteMessage, notification)
        getAllUnreadMessages(notification, remoteMessage)

    }


    private fun getAllUnreadMessages(notificationCompatBuilder: NotificationCompat.Builder, remoteMessage: RemoteMessage){
        var unreadCount = 0
        var unreadConversation = 0



        FirebaseUtils.ref.getMyAllMessageStatusRootRef()
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
                                           // updateNotificationWithBigText(remoteMessage, notificationCompatBuilder)
                                            return
                                        }


                                        val title = "$unreadCount messages from $unreadConversation conversations"


                                        val intent = Intent(this@MessagingService, HomeActivity::class.java).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        }
                                        with(NotificationManagerCompat.from(this@MessagingService)){
                                            notify( NotificationDetail.ID, notificationCompatBuilder.setStyle(NotificationCompat.BigTextStyle()
                                                .setBigContentTitle(title)
                                                .bigText("Tap to Read"))
                                                .setContentIntent(PendingIntent.getActivity(this@MessagingService, NotificationDetail.ID, intent, 0))
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

        val messageIDs = getIDs(remoteMessage.data[KEY_MSG_IDs]!!)

        val profilePicFile = File(utils.getProfilePicPath(this)+remoteMessage.data[KEY_SENDER]!!+".jpg")



        val person = Person.Builder().setName(utils.getNameFromNumber(this,
            remoteMessage.data[KEY_SENDER_PHONE]!!))

           if(utils.hasStoragePermission(this)) {
               if(profilePicFile.exists()) {
                   person.setIcon(IconCompat.createWithBitmap(BitmapFactory.decodeFile(profilePicFile.path)))
                   notificationCompatBuilder.setLargeIcon(BitmapFactory.decodeFile(profilePicFile.path))
               } //.createWithBitmap(BitmapFactory.decodeFile(profilePicFile.path)))
           }

        val style = NotificationCompat.MessagingStyle(person.build())

        messageIDs.forEachIndexed { index, item ->
            FirebaseUtils.ref.getChatRef(FirebaseUtils.getUid(), remoteMessage.data[KEY_SENDER]!!)
                .child(item)
                .addListenerForSingleValueEvent(object : ValueEventListener{
                    override fun onCancelled(p0: DatabaseError) {}

                    override fun onDataChange(p0: DataSnapshot) {
                        if(!p0.exists())
                            return

                        val model = p0.getValue(Models.MessageModel::class.java)
                          var message=model!!.message

                        if(model.messageType == utils.constants.FILE_TYPE_IMAGE)
                            message = "\uD83D\uDDBC Image"
                        else if(model.messageType == utils.constants.FILE_TYPE_VIDEO)
                            message = "\uD83C\uDFA5 Video"
                        else if(model.messageType == utils.constants.FILE_TYPE_LOCATION)
                            message = "\uD83D\uDCCC Location"

                        style.addMessage(message, model.timeInMillis, utils.getNameFromNumber(this@MessagingService,
                            remoteMessage.data[KEY_SENDER_PHONE]!!))

                        Log.d("MessagingService", "onDataChange: updating notification with messages = $message")

                        if(index == messageIDs.lastIndex){
                            notificationCompatBuilder.setStyle(style)
                           // notificationCompatBuilder.setOnlyAlertOnce(true)


                            with(NotificationManagerCompat.from(this@MessagingService)){
                                notify( NotificationDetail.ID, notificationCompatBuilder.build())
                            }
                        }

                    }

                })
        }

    }


    private fun setAllMessageFromUserAsDelivered(uid:String, targetUID:String, msgIDs:String){

        val ids = getIDs(msgIDs)

        ids.forEach {
            FirebaseUtils.ref.getMessageStatusRef(uid, targetUID, it)
                .child("delivered")
                .setValue(true)
        }
    }

    fun getIDs(msgIDsString: String): List<String> =
        msgIDsString.replace(",", " ").trim().split(" ")

    override fun onNewToken(p0: String?) {

        val token = p0


        Pref.storeToken(this, token!!)
        FirebaseUtils.updateFCMToken()


        super.onNewToken(p0)
    }
}