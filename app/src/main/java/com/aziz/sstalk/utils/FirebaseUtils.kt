package com.aziz.sstalk.utils

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
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
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.squareup.picasso.Callback
import java.io.File
import java.lang.Exception


object FirebaseUtils {


        val NODE_MESSAGES = "Messages"
        val NODE_USER = "users"
        val NODE_BLOCKED_LIST = "Block_list"
        val NODE_MESSAGE_STATUS = "Message_Status"
        val NODE_USER_ACTIVITY_STATUS = "User_Status"

        val VAL_ONLINE = "Online"
        val VAL_OFFLINE = "Offline"
        val VAL_TYPING = "Typing..."


        val KEY_STATUS = "status"
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

            fun getMessageStatusRef(uid: String, messageID: String):DatabaseReference = getRootRef()
                .child(NODE_MESSAGE_STATUS)
                .child(messageID)
                .child(uid)

            fun getUserStatusRef(uid: String):DatabaseReference = getRootRef().child(NODE_USER_ACTIVITY_STATUS)
                .child(uid)
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

            Log.d("FirebaseUtils", "onDataChange: loading in prior from local")

            val file= File(utils.getProfilePicPath(context)+uid+".jpg")
            if(file.exists()){
                Picasso.get().load(file).into(imageView)
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

                                Log.d("FirebaseUtils", "onDataChange: loading from local")

                                    val file= File(utils.getProfilePicPath(context)+uid+".jpg")
                                    if(file.exists()){
                                        Picasso.get().load(file).into(imageView)
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
                    .into(imageView)
            }

            Log.d("FirebaseUtils", "loadProfileThumbnail: ${file.path}")
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
                                        .resize(60,60)
                                        .centerCrop()
                                        .into(imageView)
                                }

                            return
                        }
                        else {
                            //download profile pic
                            Log.d("FirebaseUtils", "onDataChange:,  url has changed, loading from web")
                            Picasso.get().load(link)
                                .placeholder(R.drawable.contact_placeholder)
                                .into(imageView, object : Callback {
                                    override fun onSuccess() {
                                        if (utils.hasStoragePermission(context)) {
                                            utils.saveBitmapToProfileFolder(
                                                context,
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

                        phone = utils.getFormattedTenDigitNumber(phone!!)


                        if(shouldQueryFromContacts){

                            val list = utils.getContactList(context)

                            for(item in list){
                                val number = utils.getFormattedTenDigitNumber(item.number)
                                if(number == phone){
                                  textView.text = item.name
                                }
                            }
                        }
                    }

                })
        }


    fun setLastMessage(targetUID: String, textView: TextView){

        textView.text  = ""

        ref.getChatRef(getUid(), targetUID)
            .limitToLast(1)
            .addValueEventListener(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {}

                override fun onDataChange(p0: DataSnapshot) {

                    var messageModel:Models.MessageModel? = null
                    for(item in p0.children)
                      messageModel = item.getValue(Models.MessageModel::class.java)


                    if(p0.exists()) {
                        textView.text = messageModel!!.message//.replace("\n"," ")
                        textView.visibility = View.VISIBLE



                        if(messageModel.messageType == utils.constants.FILE_TYPE_IMAGE){
                            textView.text = ("\uD83D\uDDBC Image")
                        }
                        else if(messageModel.messageType == utils.constants.FILE_TYPE_VIDEO)
                            textView.text = "\uD83C\uDFA5 Video"


                    }
                    else {
                        textView.text = ""
                        textView.visibility = View.GONE
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
        uid: String,
        messageID: String,
        messageStatusImageView: ImageView
    ){
        FirebaseUtils.ref.getMessageStatusRef(uid, messageID)
            .addValueEventListener(object : ValueEventListener{
                override fun onCancelled(p0: DatabaseError) {

                }

                override fun onDataChange(p0: DataSnapshot) {
                    if(p0.exists()){
                        if(p0.getValue(Models.MessageStatus::class.java)!!.read)
                            messageStatusImageView.setImageResource(R.drawable.ic_read_status)
                        else if(p0.getValue(Models.MessageStatus::class.java)!!.delivered)
                            messageStatusImageView.setImageResource(R.drawable.ic_delivered_tick)
                        else
                            messageStatusImageView.setImageResource(R.drawable.ic_tick_sent_grey_24dp)

                    }
                    else{
                        messageStatusImageView.setImageResource(R.drawable.ic_message_pending_gray_24dp)
                    }
                }
            })

    }


    fun setMessageStatusToDB(messageID: String, uid: String, isDelivered:Boolean, isRead:Boolean){
        ref.getMessageStatusRef(uid,messageID)
            .setValue(Models.MessageStatus(isRead, isDelivered, messageID))

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
}