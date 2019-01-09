package com.aziz.sstalk.utils

import android.content.Context
import android.text.Html
import android.util.Log
import android.view.View
import android.widget.TextView
import com.aziz.sstalk.Models.Models
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

object FirebaseUtils {


        val NODE_MESSAGES = "Messages"
        val NODE_USER = "users"
        val KEY_UID = "uid"
        val NODE_LAST_MESSAGE = "LastMessage"
        val KEY_REVERSE_TIMESTAMP = "reverseTimeStamp"
        val KEY_PHONE = "phone"

        private fun getRootRef() : DatabaseReference {
            return FirebaseDatabase.getInstance().reference
        }

        fun getChatRef( uid :String, targetUID: String) : DatabaseReference{
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


        fun isLoggedIn() : Boolean = FirebaseAuth.getInstance().currentUser != null

        fun getUid() : String = if (isLoggedIn())  FirebaseAuth.getInstance().uid.toString() else "INVALID_USER"


        fun setUserDetailFromUID(context : Context,
            textView: TextView,
            uid: String,
            shouldQueryFromContacts: Boolean){

            getUserRef(uid)
                .child(KEY_PHONE)
                .addValueEventListener(object : ValueEventListener {
                    override fun onCancelled(p0: DatabaseError) {}

                    override fun onDataChange(snapshot: DataSnapshot) {
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

        getChatRef(getUid(), targetUID)
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