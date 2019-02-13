package com.aziz.sstalk.utils

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.aziz.sstalk.ImagePreviewActivity
import com.aziz.sstalk.models.Models
import com.aziz.sstalk.utils.FirebaseUtils.ref.getUserRef
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import com.aziz.sstalk.R
import com.aziz.sstalk.utils.FirebaseUtils.ref.getAllMessageStatusRef
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.nex3z.notificationbadge.NotificationBadge
import com.squareup.picasso.Callback
import com.squareup.picasso.MemoryPolicy
import java.io.File
import java.lang.Exception


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

            private fun getRootRef() : DatabaseReference {

                try{
                    FirebaseDatabase.getInstance().setPersistenceEnabled(true)
                    FirebaseDatabase.getInstance().reference
                        .child(NODE_MESSAGES)
                        .keepSynced(true)
                }
                catch (e:Exception){
                    Log.e("ref", "getRootRef: "+e.message.toString() )}

                return FirebaseDatabase.getInstance().reference
            }

            fun getChatQuery(uid :String, targetUID: String) : Query{
                return getRootRef()
                    .child(NODE_MESSAGES)
                    .child(uid)
                    .child(targetUID)
                    .orderByChild(KEY_TIME_IN_MILLIS)
            }

            fun getChatRef(uid :String, targetUID: String) : DatabaseReference{
                return getRootRef()
                    .child(NODE_MESSAGES)
                    .child(uid)
                    .child(targetUID)

            }

            fun getLastMessageRef( uid :String) : DatabaseReference{
                return getRootRef()
                    .child(NODE_LAST_MESSAGE)
                    .child(uid)
            }


            fun getUserRef(uid : String): DatabaseReference  = getRootRef().child(NODE_USER).child(uid)

            fun getAllUserRef(): DatabaseReference  = getRootRef().child(NODE_USER)


            fun getProfilePicStorageRef(uid: String): StorageReference = FirebaseStorage.getInstance()
                .reference.child("profile_pics").child(uid)

            fun getFCMTokenRef(uid: String):DatabaseReference =
                getRootRef()
                    .child(NODE_TOKEN)
                    .child(uid)


            //will return a boolean snapshot
            fun getBlockedUserRef(uid: String, targetUID: String): DatabaseReference = getRootRef()
                .child(NODE_BLOCKED_LIST)
                .child(uid)
                .child(targetUID)
                .child(KEY_BLOCKED)

            //will return a boolean snapshot
            fun getBlockedUserListQuery(uid: String): Query = getRootRef()
                .child(NODE_BLOCKED_LIST)
                .child(uid)
                .orderByChild(KEY_BLOCKED).equalTo(true)

            fun getAllMessageStatusRef(uid: String, targetUID: String):DatabaseReference =
                getRootRef()
                    .child(NODE_MESSAGE_STATUS)
                    .child(uid)
                    .child(targetUID)

            fun getMyAllMessageStatusRootRef():DatabaseReference =
                    getRootRef().child(NODE_MESSAGE_STATUS)
                        .child(FirebaseUtils.getUid())
                    //.child("vHv8TSqbS2YBHZJXS5X5Saz4acC2")

            fun getMessageStatusRef(uid: String, targetUID: String, messageID: String):DatabaseReference =
                getAllMessageStatusRef(uid, targetUID)
                .child(messageID)

            fun getUserStatusRef(uid: String):DatabaseReference = getRootRef().child(NODE_USER_ACTIVITY_STATUS)
                .child(uid)

            //this will return a boolean snapshot
            fun getNotificationMuteRef(uid: String):DatabaseReference =
                    getRootRef().child(NODE_INDIVIDUAL_NOTIFICATION_SETTING)
                        .child(FirebaseUtils.getUid())
                        .child(uid)
                        .child(KEY_ENABLED)
        }


    fun uploadProfilePic(context: Context, file: File, storageRef: StorageReference,
        dbRef: DatabaseReference,
        toastAfterUploadIfAny: String){


        val dialog = ProgressDialog(context)
        dialog.setMessage("Wait a moment...")
        dialog.setCancelable(false)
        dialog.show()



        Log.d("FirebaseUtils", "uploadImage: File size = "+file.length()/1024)




        val uploadTask = storageRef.putFile(utils.getUriFromFile(context, file))

        uploadTask.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    throw it
                }
            }
            Log.d("FirebaseUtils", "uploadedImage: size = "+task.result!!.bytesTransferred/1024)
            return@Continuation storageRef.downloadUrl
        })
            .addOnCompleteListener { task ->
                dialog.dismiss()
                if(task.isSuccessful) {
                    val link = task.result



                    dbRef.setValue(link.toString())
                        .addOnSuccessListener {
                         //   isProfileChanged = false
                            if(toastAfterUploadIfAny.isNotEmpty())
                               utils.toast(context, toastAfterUploadIfAny) }




                }
                else
                    utils.toast(context, task.exception!!.message.toString())
            }

            .addOnSuccessListener {
                dialog.dismiss()


            }
            .addOnFailureListener{
                dialog.dismiss()
            }



    }


    fun loadProfilePic(context: Context, uid: String, imageView: ImageView){


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

            ref.getUserRef(uid)
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




        ref.getUserRef(uid)
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

            getUserRef(uid)
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

        if(boldTextViews.isNotEmpty())
            initialTypeface = boldTextViews[0].typeface


        getAllMessageStatusRef(FirebaseUtils.getUid(), targetUID)
            .orderByChild("read")
            .equalTo(false)
            .addValueEventListener(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                }

                override fun onDataChange(p0: DataSnapshot) {

                    Log.d("FirebaseUtils", "onDataChange: unread count = ${p0.childrenCount}")

                    if(p0.childrenCount.toInt() == 0) {
                        boldTextViews.forEach {
                            it.setTypeface(null, Typeface.NORMAL)
                            notificationBadge.visibility = View.INVISIBLE
                        }
                    } else {
                        boldTextViews.forEach {
                            it.setTypeface(null, Typeface.BOLD)
                        }

                        notificationBadge.visibility = View.VISIBLE
                        notificationBadge.setNumber(p0.childrenCount.toInt(), true)
                    }
                }
            })
    }

    fun setMeAsOnline(){
        FirebaseUtils.ref.getUserStatusRef(FirebaseUtils.getUid())
            .setValue(Models.UserActivityStatus(FirebaseUtils.VAL_ONLINE, System.currentTimeMillis()))
    }

    fun setMeAsOffline(){
        FirebaseUtils.ref.getUserStatusRef(FirebaseUtils.getUid())
            .setValue(Models.UserActivityStatus(FirebaseUtils.VAL_OFFLINE, System.currentTimeMillis()))
    }

    fun setMeAsTyping(){
        FirebaseUtils.ref.getUserStatusRef(FirebaseUtils.getUid())
            .setValue(Models.UserActivityStatus(FirebaseUtils.VAL_TYPING, System.currentTimeMillis()))
    }

    fun setDeliveryStatusTick(
        targetUID: String, messageID: String,
        messageStatusImageView: ImageView
    ){

        messageStatusImageView.alpha = 0.8f

        ref.getMessageStatusRef(targetUID,FirebaseUtils.getUid(), messageID)
            .addValueEventListener(object : ValueEventListener{
                override fun onCancelled(p0: DatabaseError) {

                }

                override fun onDataChange(p0: DataSnapshot) {


                    if(p0.exists()){
                        if(p0.getValue(Models.MessageStatus::class.java)!!.read)
                            messageStatusImageView.setImageResource(R.drawable.ic_read_round)
                        else if(p0.getValue(Models.MessageStatus::class.java)!!.delivered)
                            messageStatusImageView.setImageResource(R.drawable.ic_delivered_round)
                        else
                            messageStatusImageView.setImageResource(R.drawable.ic_sent_round)

                    }
                    else{
                        messageStatusImageView.setImageResource(R.drawable.ic_message_pending_gray_24dp)
                    }
                }
            })

    }


    fun setMessageStatusToDB(messageID: String, uid: String,targetUID: String, isDelivered:Boolean, isRead:Boolean){
        ref.getMessageStatusRef(uid,targetUID,messageID)
            .setValue(Models.MessageStatus(FirebaseUtils.getUid(), isRead, isDelivered, messageID))

    }


    fun setUserOnlineStatus(context: Context, uid: String, textView: TextView){

        if(textView.text == VAL_ONLINE)
            return

        ref.getUserStatusRef(uid)
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
                        textView.text = "last seen at ${utils.getLocalTime(userStatus.timeInMillis)}"
                    }
                }
            })
    }


    fun storeFCMToken(context: Context){
        FirebaseInstanceId.getInstance()
            .instanceId
            .addOnCompleteListener {
                 if(!it.isSuccessful)
                     return@addOnCompleteListener


                    if(Pref.getStoredToken(context) != it.result!!.token)
                    ref.getFCMTokenRef(FirebaseUtils.getUid()).child(it.result!!.id)
                        .setValue(it.result!!.token)
            }
    }
}