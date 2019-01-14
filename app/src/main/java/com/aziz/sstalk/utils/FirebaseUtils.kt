package com.aziz.sstalk.utils

import android.app.ProgressDialog
import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.aziz.sstalk.Models.Models
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


object FirebaseUtils {


        val NODE_MESSAGES = "Messages"
        val NODE_USER = "users"
        val KEY_UID = "uid"
        val NODE_LAST_MESSAGE = "LastMessage"
        val KEY_REVERSE_TIMESTAMP = "reverseTimeStamp"
        val KEY_PHONE = "phone"
        val KEY_PROFILE_PIC_URL = "profile_pic_url"
        val KEY_NAME = "name"

        val user_voda = "vHv8TSqbS2YBHZJXS5X5Saz4acC2"
        val user_jio = "LPVjVKbpTzeUDpank04sxkoparE2"


        object ref {

            private fun getRootRef() : DatabaseReference {
                return FirebaseDatabase.getInstance().reference
            }

            fun getChatRef( uid :String, targetUID: String) : DatabaseReference{
                return getRootRef()
                    .child(NODE_MESSAGES)
                    .child(uid)
                    .child(targetUID)
                    .orderByChild(KEY_REVERSE_TIMESTAMP)
                    .ref
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
        }


    fun uploadProfilePic(context: Context, file: File, storageRef: StorageReference,
        dbRef: DatabaseReference,
        toastAfterUploadIfAny: String){


        val dialog = ProgressDialog(context)
        dialog.setMessage("Wait a moment...")
        dialog.setCancelable(false)
        dialog.show()



        Log.d("FirebaseUtils", "uploadImage: File size = "+file.length()/1024)




        val uploadTask = storageRef.putFile(Uri.fromFile(file))

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



                    dbRef.setValue(link)
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


    fun loadProfilePic(uid:String, imageView: ImageView, isLarge: Boolean){


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
                        }
                    }

                    override fun onCancelled(p0: DatabaseError) {

                    }
                })
        }


    fun isLoggedIn() : Boolean = FirebaseAuth.getInstance().currentUser != null


    //todo Remove this else condition when production
    //below is the id for my mobile number(Shanu)
        fun getUid() : String = if (isLoggedIn())  FirebaseAuth.getInstance().uid.toString() else "vHv8TSqbS2YBHZJXS5X5Saz4acC2"

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
            .addValueEventListener(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {

                }

                override fun onDataChange(p0: DataSnapshot) {
                    var messageModel:Models.MessageModel? = null
                    for(post in p0.children){
                        messageModel = post.getValue(Models.MessageModel::class.java)
                    }

                    if(p0.exists()) {
                        textView.text = messageModel!!.message
                        textView.visibility = View.VISIBLE

                        if(messageModel.isFile && messageModel.fileType == utils.constants.FILE_TYPE_IMAGE){
                            textView.text = ("\uD83D\uDDBC Image")
                        }

                    }
                    else {
                        textView.text = ""
                        textView.visibility = View.GONE
                    }
                }
            })
    }




}