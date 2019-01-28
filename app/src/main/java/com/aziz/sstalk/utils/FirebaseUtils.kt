package com.aziz.sstalk.utils

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.v4.content.FileProvider
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
import java.io.File
import java.lang.Exception


object FirebaseUtils {


        val NODE_MESSAGES = "Messages"
        val NODE_USER = "users"
        val NODE_BLOCKED_LIST = "Block_list"
        val NODE_MESSAGE_STATUS = "Message_Status"
        val NODE_USER_ACTIVITY_STATUS = "User_Activity_Status"


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


            fun getBlockedUserRef(uid: String, targetUID: String): DatabaseReference = getRootRef()
                .child(NODE_BLOCKED_LIST)
                .child(uid)
                .child(targetUID)
                .child(KEY_BLOCKED)

            fun getMessageStatusRef(uid: String, messageID: String):DatabaseReference = getRootRef()
                .child(NODE_MESSAGE_STATUS)
                .child(messageID)
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


    fun loadProfilePic(context: Context, uid:String, imageView: ImageView, isLarge: Boolean){


            if(uid.isEmpty())
                return
            ref.getUserRef(uid)
                .child(KEY_PROFILE_PIC_URL)
                .addValueEventListener(object : ValueEventListener {

                    override fun onDataChange(p0: DataSnapshot) {
                        if (p0.exists()) {
                            val link: String? = p0.getValue(String::class.java)
                            if(isLarge)
                            Picasso.get().load(link)
                                .placeholder(R.drawable.contact_placeholder)
                                .into(imageView)
                            else{
                                Picasso.get().load(link)
                                    .resize(120,120)
                                    .centerCrop()
                                    .placeholder(R.drawable.contact_placeholder)
                                    .into(imageView)
                            }

                            imageView.setOnClickListener {
                                context.startActivity(Intent(context, ImagePreviewActivity::class.java)
                                    .putExtra(utils.constants.KEY_IMG_PATH, link))
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
}