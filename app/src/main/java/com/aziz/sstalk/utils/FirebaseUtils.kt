package com.aziz.sstalk.utils

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.os.Handler
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.aziz.sstalk.ImagePreviewActivity
import com.aziz.sstalk.models.Models
import com.aziz.sstalk.utils.FirebaseUtils.ref.user
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import com.aziz.sstalk.R
import com.aziz.sstalk.utils.FirebaseUtils.ref.allMessageStatus
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.nex3z.notificationbadge.NotificationBadge
import com.squareup.picasso.Callback
import com.squareup.picasso.MemoryPolicy
import org.jetbrains.anko.alert
import org.jetbrains.anko.browse
import org.jetbrains.anko.toast
import java.io.File
import java.lang.Exception
import java.util.*


object FirebaseUtils {


        val NODE_MESSAGES = "Messages"
        val NODE_USER = "users"
        val NODE_BLOCKED_LIST = "Block_list"
        val NODE_MESSAGE_STATUS = "Message_Status"
        val NODE_USER_ACTIVITY_STATUS = "User_Status"
        val NODE_TOKEN = "FCM_Tokens"
        val NODE_INDIVIDUAL_NOTIFICATION_SETTING = "Mute_Notification"

        val VAL_ONLINE = "Online"
        val VAL_OFFLINE = "Offline"
        val VAL_TYPING = "Typing..."

        val NODE_FILE = "Files"


        val KEY_STATUS = "status"
        val KEY_ENABLED = "enabled"
        val KEY_UID = "uid"
        val NODE_LAST_MESSAGE = "LastMessage"
        val KEY_REVERSE_TIMESTAMP = "reverseTimeStamp"
        val KEY_TIME_IN_MILLIS = "timeInMillis"
        val KEY_PHONE = "phone"
        val KEY_PROFILE_PIC_URL = "profile_pic_url"
        val KEY_NAME = "name"

        val KEY_BLOCKED = "blocked"

        val KEY_FILE_LOCAL_PATH = "file_local_path"


        val user_voda = "vHv8TSqbS2YBHZJXS5X5Saz4acC2"
        val user_jio = "LPVjVKbpTzeUDpank04sxkoparE2"


        object ref {

            private fun root() : DatabaseReference {

                try{
                    FirebaseDatabase.getInstance().setPersistenceEnabled(true)
                    FirebaseDatabase.getInstance().reference
                        .child(NODE_MESSAGES)
                        .keepSynced(true)
                }
                catch (e:Exception){ }

                return FirebaseDatabase.getInstance().reference
            }

            fun fileRef(): DatabaseReference = root().child(NODE_FILE)

            fun getChatQuery(uid :String, targetUID: String) : Query{
                return root()
                    .child(NODE_MESSAGES)
                    .child(uid)
                    .child(targetUID)
                    .orderByChild(KEY_TIME_IN_MILLIS)
            }

            fun getChatRef(uid :String, targetUID: String) : DatabaseReference{
                return root()
                    .child(NODE_MESSAGES)
                    .child(uid)
                    .child(targetUID)

            }

            fun lastMessage(uid :String) : DatabaseReference{
                return root()
                    .child(NODE_LAST_MESSAGE)
                    .child(uid)
            }


            fun user(uid : String): DatabaseReference  = root().child(NODE_USER).child(uid)

            fun allUser(): DatabaseReference  = root().child(NODE_USER)


            fun profilePicStorageRef(uid: String): StorageReference = FirebaseStorage.getInstance()
                .reference.child("profile_pics").child(uid)

            fun FCMToken(uid: String):DatabaseReference =
                root()
                    .child(NODE_TOKEN)
                    .child(uid)


            //will return a boolean snapshot
            fun blockedUser(uid: String, targetUID: String): DatabaseReference = root()
                .child(NODE_BLOCKED_LIST)
                .child(uid)
                .child(targetUID)
                .child(KEY_BLOCKED)

            //will return a boolean snapshot
            fun getBlockedUserListQuery(uid: String): Query = root()
                .child(NODE_BLOCKED_LIST)
                .child(uid)
                .orderByChild(KEY_BLOCKED).equalTo(true)

            fun allMessageStatus(uid: String, targetUID: String):DatabaseReference =
                root()
                    .child(NODE_MESSAGE_STATUS)
                    .child(uid)
                    .child(targetUID)

            fun allMessageStatusRootRef():DatabaseReference =
                    root().child(NODE_MESSAGE_STATUS)
                        .child(FirebaseUtils.getUid())
                    //.child("vHv8TSqbS2YBHZJXS5X5Saz4acC2")

            fun messageStatus(uid: String, targetUID: String, messageID: String):DatabaseReference =
                allMessageStatus(uid, targetUID)
                .child(messageID)

            fun userStatus(uid: String):DatabaseReference = root().child(NODE_USER_ACTIVITY_STATUS)
                .child(uid)

            //this will return a boolean snapshot
            fun notificationMute(uid: String):DatabaseReference =
                    root().child(NODE_INDIVIDUAL_NOTIFICATION_SETTING)
                        .child(FirebaseUtils.getUid())
                        .child(uid)
                        .child(KEY_ENABLED)

            //this will return a snapshot array
            fun getNotificationMuteRootRef():DatabaseReference =
                root().child(NODE_INDIVIDUAL_NOTIFICATION_SETTING)
                    .child(FirebaseUtils.getUid())
        }


    fun loadProfilePic(context: Context, uid: String, imageView: ImageView){

        imageView.setImageResource(R.drawable.contact_placeholder)

            if(uid.isEmpty())
                return



        if(utils.hasStoragePermission(context)){


            val file= File(utils.getProfilePicPath(context)+uid+".jpg")
            if(file.exists()){
                Picasso.get().load(file)
                    .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE).into(imageView)
                imageView.setOnClickListener {
                    context.startActivity(Intent(context, ImagePreviewActivity::class.java)
                        .putExtra(utils.constants.KEY_LOCAL_PATH, file.path))
                }
            }

        }

            ref.user(uid)
                .child(KEY_PROFILE_PIC_URL)
                .addValueEventListener(object : ValueEventListener {

                    override fun onDataChange(p0: DataSnapshot) {
                        if (p0.exists()) {
                            val link: String? = p0.getValue(String::class.java)

                            if(Pref.Profile.isProfileUrlSame(context, uid, link.toString())
                                && utils.hasStoragePermission(context)){


                                    val file= File(utils.getProfilePicPath(context)+uid+".jpg")
                                    if(file.exists()){
                                        Picasso.get().load(file)
                                            .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE).into(imageView)
                                        imageView.setOnClickListener {
                                            context.startActivity(Intent(context, ImagePreviewActivity::class.java)
                                                .putExtra(utils.constants.KEY_LOCAL_PATH, file.path))
                                        }
                                        return
                                    }

                            }
                            else {

                                if(link!!.isEmpty())
                                    return

                                    Picasso.get().load(link)
                                        .placeholder(R.drawable.contact_placeholder)
                                        .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
                                        .into(imageView, object : Callback {
                                            override fun onSuccess() {
                                                if (utils.hasStoragePermission(context)) {
                                                    utils.saveBitmapToProfileFolder(
                                                        context,
                                                        (imageView.drawable as BitmapDrawable).bitmap,
                                                        uid
                                                    )
                                                    Pref.Profile.setProfileUrl(context, uid, link.toString())
                                                }
                                            }

                                            override fun onError(e: Exception?) {
                                            }


                                        })


                                imageView.setOnClickListener {
                                    context.startActivity(
                                        Intent(context, ImagePreviewActivity::class.java)
                                            .putExtra(utils.constants.KEY_IMG_PATH, link)
                                    )
                                }
                            }
                        }
                    }

                    override fun onCancelled(p0: DatabaseError) {

                    }
                })
        }


    fun loadProfileThumbnail(context: Context, uid:String, imageView: ImageView){

        imageView.setImageResource(R.drawable.contact_placeholder)


        if(uid.isEmpty())
            return


        if(utils.hasStoragePermission(context)){
            val file= File(utils.getProfilePicPath(context)+uid+".jpg")
            if(file.exists()){
                Picasso.get().load(file)
                    .resize(60,60)
                    .centerCrop()
                    .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
                    .into(imageView)
            }

        }




        ref.user(uid)
            .child(KEY_PROFILE_PIC_URL)
            .addValueEventListener(object : ValueEventListener {

                override fun onDataChange(p0: DataSnapshot) {
                    if (p0.exists()) {
                        val link: String? = p0.getValue(String::class.java)


                        if(Pref.Profile.isProfileUrlSame(context, uid, link.toString())
                            && utils.hasStoragePermission(context)){
                                val file= File(utils.getProfilePicPath(context)+uid+".jpg")
                                if(file.exists()){
                                    Picasso.get().load(file)
                                        .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
                                        .resize(60,60)
                                        .centerCrop()
                                        .into(imageView)
                                }

                            return
                        }
                        else {
                            //download profile pic
                            Log.d("FirebaseUtils", "onDataChange:,  profile url has changed, loading from web")

                            if(link!!.isEmpty())
                                return

                            Picasso.get().load(link)
                                .placeholder(R.drawable.contact_placeholder)
                                .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
                                .into(imageView, object : Callback {
                                    override fun onSuccess() {
                                        if (utils.hasStoragePermission(context)) {
                                            utils.saveBitmapToProfileFolder(context,
                                                (imageView.drawable as BitmapDrawable).bitmap,
                                                uid)
                                            Pref.Profile.setProfileUrl(context, uid, link.toString())
                                        }
                                    }

                                    override fun onError(e: Exception?) {
                                    }

                                })
                        }
                    }
                }

                override fun onCancelled(p0: DatabaseError) {

                }
            })
    }

    fun isLoggedIn() : Boolean = FirebaseAuth.getInstance().currentUser != null


    //todo Remove this else condition when production
    //below is the id for my mobile number(Shanu)
    fun getUid() : String = if (isLoggedIn())  FirebaseAuth.getInstance().uid.toString() else utils.constants.debugUserID

    fun setUserDetailFromUID(context : Context,
            textView: TextView,
            uid: String,
            shouldQueryFromContacts: Boolean){

            user(uid)
                .child(KEY_PHONE)
                .addValueEventListener(object : ValueEventListener {
                    override fun onCancelled(p0: DatabaseError) {}

                    override fun onDataChange(snapshot: DataSnapshot) {

                        if(!snapshot.exists()){
                            textView.text = ""
                            return
                        }


                        var phone = snapshot.getValue(String::class.java)


                        textView.text = phone



                        if(shouldQueryFromContacts){

                            textView.text = utils.getNameFromNumber(context, phone!!)


                        }
                    }

                })
        }


    fun setLastMessage(targetUID: String, textView: TextView, messageStatusImageView:ImageView){

        textView.text  = ""

        ref.getChatRef(getUid(), targetUID)
            .limitToLast(1)
            .addValueEventListener(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {}

                override fun onDataChange(p0: DataSnapshot) {

                    messageStatusImageView.visibility = View.GONE
                    var messageModel:Models.MessageModel? = null
                    var messageID = ""
                    for(item in p0.children) {
                        messageModel = item.getValue(Models.MessageModel::class.java)
                        messageID = item.key!!
                    }

                    if(p0.exists()) {
                        textView.text = messageModel!!.message//.replace("\n"," ")
                        textView.visibility = View.VISIBLE


                        if(messageModel.from == FirebaseUtils.getUid()){
                            messageStatusImageView.visibility = View.VISIBLE
                            setDeliveryStatusTick(targetUID, messageID, messageStatusImageView)
                        }
                        else{
                            messageStatusImageView.visibility = View.GONE
                        }

                        if(messageModel.messageType == utils.constants.FILE_TYPE_IMAGE){
                            textView.text = ("\uD83D\uDDBC Image")
                        }
                        else if(messageModel.messageType == utils.constants.FILE_TYPE_VIDEO)
                            textView.text = "\uD83C\uDFA5 Video"
                        else if(messageModel.messageType == utils.constants.FILE_TYPE_LOCATION)
                            textView.text = "\uD83D\uDCCC ${if(messageModel.caption.isEmpty()) " Location" else messageModel.caption}"

                    }
                    else {
                        textView.text = ""
                        textView.visibility = View.GONE
                    }
                }
            })
    }

    fun setUnreadCount(targetUID: String, notificationBadge: NotificationBadge, vararg boldTextViews: TextView ){

        var initialTypeface:Typeface? = null
        notificationBadge.visibility = View.GONE

        if(boldTextViews.isNotEmpty())
            initialTypeface = boldTextViews[0].typeface


        allMessageStatus(FirebaseUtils.getUid(), targetUID)
            .orderByChild("read")
            .equalTo(false)
            .addValueEventListener(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                }

                override fun onDataChange(p0: DataSnapshot) {


                    if(p0.childrenCount.toInt() == 0) {
                        notificationBadge.visibility = View.GONE
                        boldTextViews.forEach {
                            it.setTypeface(null, Typeface.NORMAL)
                        }
                    } else {
                        boldTextViews.forEach {
                            it.setTypeface(null, Typeface.BOLD)
                        }

                        //setting current message to delivered if haven't
                        p0.ref.child("delivered").setValue(true)

                        notificationBadge.visibility = View.VISIBLE
                        notificationBadge.setNumber(p0.childrenCount.toInt(), true)
                    }
                }
            })
    }

    fun setMeAsOnline(){
        FirebaseUtils.ref.userStatus(FirebaseUtils.getUid())
            .setValue(Models.UserActivityStatus(FirebaseUtils.VAL_ONLINE, System.currentTimeMillis()))
    }

    fun setMeAsOffline(){
        FirebaseUtils.ref.userStatus(FirebaseUtils.getUid())
            .setValue(Models.UserActivityStatus(FirebaseUtils.VAL_OFFLINE, System.currentTimeMillis()))
    }

    fun setMeAsTyping(){
        FirebaseUtils.ref.userStatus(FirebaseUtils.getUid())
            .setValue(Models.UserActivityStatus(FirebaseUtils.VAL_TYPING, System.currentTimeMillis()))
    }

    fun setDeliveryStatusTick(
        targetUID: String, messageID: String,
        messageStatusImageView: ImageView
    ){

        messageStatusImageView.alpha = 0.8f

        ref.messageStatus(targetUID,FirebaseUtils.getUid(), messageID)
            .addValueEventListener(object : ValueEventListener{
                override fun onCancelled(p0: DatabaseError) {

                }

                override fun onDataChange(p0: DataSnapshot) {


                    val isForConversation = (messageStatusImageView.id == R.id.delivery_status_last_msg)

                    if(p0.exists()){
                        when {
                            p0.getValue(Models.MessageStatus::class.java)!!.read ->
                                messageStatusImageView.setImageResource(if(isForConversation) R.drawable.ic_read_green else R.drawable.ic_read_round)
                            p0.getValue(Models.MessageStatus::class.java)!!.delivered ->
                                messageStatusImageView.setImageResource(if(isForConversation) R.drawable.ic_delivered_tick else R.drawable.ic_delivered_round)
                            else -> messageStatusImageView.setImageResource(if(isForConversation) R.drawable.ic_tick_sent_grey_24dp else R.drawable.ic_sent_round)
                        }

                    }
                    else{
                        messageStatusImageView.setImageResource(R.drawable.ic_message_pending_gray_24dp)
                    }
                }
            })

    }


    fun setMessageStatusToDB(messageID: String, uid: String,targetUID: String, isDelivered:Boolean, isRead:Boolean){
        Log.d(
            "FirebaseUtils",
            "setMessageStatusToDB: setting values to $uid -> $targetUID as $isDelivered, $isRead on $messageID"
        )

        ref.messageStatus(uid,targetUID,messageID)
            .setValue(Models.MessageStatus(FirebaseUtils.getUid(), isRead, isDelivered, messageID,
                if(FirebaseUtils.isLoggedIn()) FirebaseAuth.getInstance().currentUser!!.phoneNumber!! else "1234567890",
                if(FirebaseUtils.isLoggedIn()) FirebaseAuth.getInstance().currentUser!!.photoUrl.toString() else ""))
    }


    fun setReadStatusToMessage(messageID: String, targetUID: String){


        try {

          Handler().postDelayed({
              Log.d(
                  "FirebaseUtils",
                  "setReadStatusToMessage: setting read status to  -> $targetUID as  $messageID " +
                          "after 1 sec delay"
              )
              ref.messageStatus(FirebaseUtils.getUid(), targetUID, messageID)
                  .child("read")
                  .setValue(true)

              ref.messageStatus(FirebaseUtils.getUid(), targetUID, messageID)
                  .child("delivered")
                  .setValue(true)
          },1000)

        }
        catch (e:Exception){
            Log.d("FirebaseUtils", "setReadStatusToMessage: error = ${e.message}")
        }
    }



    fun setUserOnlineStatus(context: Context, uid: String, textView: TextView){

        if(textView.text == VAL_ONLINE)
            return

        ref.userStatus(uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                }

                override fun onDataChange(p0: DataSnapshot) {

                    textView.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(context, R.drawable.shape_bubble_offline),
                        null, null,null)
                    textView.compoundDrawablePadding = 20

                    if(!p0.exists()){
                        textView.text = VAL_OFFLINE
                        return
                    }

                    val userStatus = p0.getValue(Models.UserActivityStatus::class.java)!!

                    if(userStatus.status == VAL_ONLINE || userStatus.status == VAL_TYPING){
                        textView.text = userStatus.status
                        textView.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(context, R.drawable.shape_bubble_online),
                            null, null,null)
                    }
                    else{
                        val time = utils.getLocalTime(userStatus.timeInMillis)
                        var timeString = time

                        timeString = if(DateFormatter.isYesterday(Date(userStatus.timeInMillis)))
                            "on Yesterday $time"
                        else if(DateFormatter.isToday(Date(userStatus.timeInMillis)))
                            "at $time"
                        else if(DateFormatter.isCurrentYear(Date(userStatus.timeInMillis)))
                            "on "+utils.getLocalDate(userStatus.timeInMillis)
                        else
                            "on "+utils.getLocalDateWithYear(userStatus.timeInMillis)

                        textView.text = "last seen $timeString"
                    }
                }
            })
    }


    fun setUserOnlineStatus(uid: String, imageView: ImageView){


        imageView.visibility = View.GONE


        ref.userStatus(uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                }

                override fun onDataChange(p0: DataSnapshot) {


                    imageView.visibility = View.GONE

                    if(!p0.exists()){
                        return
                    }

                    val userStatus = p0.getValue(Models.UserActivityStatus::class.java)!!

                    if(userStatus.status == VAL_ONLINE || userStatus.status == VAL_TYPING){
                        imageView.visibility = View.VISIBLE
                    }

                }
            })
    }
    fun updateFCMToken() {
        FirebaseInstanceId.getInstance()
            .instanceId
            .addOnCompleteListener {
                 if(!it.isSuccessful)
                     return@addOnCompleteListener

                    ref.FCMToken(FirebaseUtils.getUid()).child(it.result!!.id)
                        .setValue(it.result!!.token)
            }
    }


    fun deleteCurrentToken(){
        FirebaseInstanceId.getInstance()
            .instanceId
            .addOnCompleteListener {
                if(!it.isSuccessful)
                    return@addOnCompleteListener

                ref.FCMToken(FirebaseUtils.getUid()).child(it.result!!.id)
                    .removeValue().addOnSuccessListener {
                        Log.d("FirebaseUtils", "deleteCurrentToken: token removed")
                    }
            }
    }


    fun storeFileMetaData(file:Models.File){
        FirebaseUtils.ref.fileRef()
            .child(file.fileID)
            .setValue(file)
    }


    fun setMuteImageIcon(uid: String, imageView: ImageView){
        imageView.visibility = View.GONE
        ref.notificationMute(uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                }

                override fun onDataChange(p0: DataSnapshot) {
                    if(p0.exists()){
                        if(p0.getValue(Boolean::class.java)!!) {
                            imageView.visibility = View.VISIBLE
                            return
                        }
                    }
                    imageView.visibility = View.GONE

                }
            })
    }



    fun checkForUpdate(context: Context){
        val key_app_code = "App_Version_Code"

        //this will return an int
        FirebaseDatabase.getInstance().getReference(key_app_code)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                }

                override fun onDataChange(p0: DataSnapshot) {

                    val versionCode = p0.getValue(Int::class.java)!!
                    Log.d("FirebaseUtils", "onDataChange: current version = ${com.aziz.sstalk.BuildConfig.VERSION_CODE}")
                    Log.d("FirebaseUtils", "onDataChange: available version = $versionCode")

                    if(versionCode > com.aziz.sstalk.BuildConfig.VERSION_CODE){
                        //show update dialog
                        context.alert {
                            positiveButton("Yes"){
                                context.browse(utils.constants.APP_LINK)
                            }
                            negativeButton("No"){
                            }
                            title = "Update available"
                            message = "A New update has been available for SS Talk"
                            isCancelable = false
                        }
                            .show()
                    }
                    else{
                        context.toast("No update available")
                    }

                }
            })

    }

}