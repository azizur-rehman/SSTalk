package com.aziz.sstalk.firebase

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
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

class MessagingService: FirebaseMessagingService() {

    private val KEY_UNREAD = "unreadCount"
    private val KEY_SENDER = "senderUID"
    private val KEY_RECEIVER = "receiverUID"
    private val KEY_MSG_IDs = "messageIDs"
    val NotificationID = 13213



    override fun onMessageReceived(p0: RemoteMessage?) {
        super.onMessageReceived(p0)

        Log.d("MessagingService", "onMessageReceived: ${p0!!.data.toString()}")
        val data: MutableMap<String, String>? = p0.data ?: return

        //todo change to FirebaseUtils.getUid


        setAllMessageFromUserAsDelivered(data!![KEY_RECEIVER]!!, data[KEY_SENDER]!!, data[KEY_MSG_IDs]!!)



        FirebaseUtils.ref.getNotificationMuteRef(data[KEY_SENDER]!!)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                }

                override fun onDataChange(snapshot: DataSnapshot) {
                    if(snapshot.exists()){
                        if(snapshot.getValue(Boolean::class.java)!!)
                            return
                    }
                    if(utils.isAppIsInBackground(this@MessagingService)){
                        //app is in background show notification
                        showNotification(p0)
                    }
                    else {

                        if(Pref.Notification.hasVibrationEnabled(this@MessagingService))
                            utils.vibrate(this@MessagingService)
                    }
                }
            })




    }


    private fun showNotification(remoteMessage: RemoteMessage){
        val data = remoteMessage.data

        val totalMessage = data[KEY_UNREAD]
        val sender = data[KEY_SENDER]

        Log.d("MessagingService", "showNotification: sender = $sender")

            FirebaseUtils.ref.getUserRef(sender!!)
                .child(FirebaseUtils.KEY_PHONE)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onCancelled(p0: DatabaseError) {
                    }

                    override fun onDataChange(p0: DataSnapshot) {

                        if(!p0.exists())
                            return

                        val phoneNumber = p0.getValue(String::class.java)
                        val name = utils.getNameFromNumber(this@MessagingService, phoneNumber!!)
                        val title = "$totalMessage ${if(totalMessage!!.toInt()==1) "message" else "messages"} from $name"

                        Log.d("MessagingService", "onDataChange: sending notification with title = $title")
                        val intent = Intent(this@MessagingService, MessageActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            putExtra(FirebaseUtils.KEY_UID, sender)
                        }

                        notify(title, intent)


                    }
                })


    }


    private fun notify(title:String, intent: Intent){
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this@MessagingService, NotificationID, intent, 0)

        val notification = NotificationCompat.Builder(this@MessagingService)
            .setContentTitle(title)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentText("Tap to read")
            .setDefaults(Notification.DEFAULT_ALL)
            .setStyle(NotificationCompat.BigTextStyle()
                .setBigContentTitle(title)
                .bigText("Tap to Read"))
            .setContentIntent(pendingIntent)
            .setPriority(Notification.PRIORITY_MAX)
            .setAutoCancel(true)



//        if(!Pref.Notification.hasVibrationEnabled(this@MessagingService)) {
//            //if(Build.VERSION.SDK_INT<Build.VERSION_CODES.O)
//            notification.setVibrate(longArrayOf(0L))
//
//        }
//
//        if(Pref.Notification.hasSoundEnabled(this@MessagingService))
//            notification.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))


        with(NotificationManagerCompat.from(this@MessagingService)){
            notify(NotificationID, notification.build())
        }

        getAllUnreadMessages(notification)
    }


    private fun getAllUnreadMessages(notificationCompatBuilder: NotificationCompat.Builder){
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

                                        val intent = Intent(this@MessagingService, HomeActivity::class.java).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        }

                                        val title = "$unreadCount messages from $unreadConversation conversations"
                                        with(NotificationManagerCompat.from(this@MessagingService)){
                                            notify(NotificationID, notificationCompatBuilder.setStyle(NotificationCompat.BigTextStyle()
                                                .setBigContentTitle(title)
                                                .bigText("Tap to Read"))
                                                .setContentIntent(PendingIntent.getActivity(this@MessagingService, NotificationID, intent, 0))
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


        super.onNewToken(p0)
    }
}