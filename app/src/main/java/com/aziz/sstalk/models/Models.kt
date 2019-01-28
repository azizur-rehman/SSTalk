package com.aziz.sstalk.models

import java.io.Serializable

class Models {

    data class MessageModel(var message:String = "",
                            var from:String = "",
                            var to:String = "",
                            var timeInMillis:Long = System.currentTimeMillis(),
                            var reverseTimeStamp: Long = timeInMillis * -1,
                            var isFile:Boolean = false,
                            var messageType: String = "message",
                            var caption:String = "",
                            var file_local_path:String = "",
                            var file_size_in_bytes:Long = 0,
                            var message_deleted:Boolean = false) : Serializable

    data class Contact(var name:String = "", var number:String = "", var photoURI:String = "" , var uid: String = "")

    data class User(var name:String = "",
                    var createdOn:Long = System.currentTimeMillis(),
                    var lastModifiedOn: Long = createdOn,
                    var phone:String = "",
                    var profile_pic_url:String = "",
                    var uid:String = "")


    data class LastMessageDetail(var timeInMillis: Long = System.currentTimeMillis(), var reverseTimeStamp: Long = timeInMillis  * -1)


    data class MessageStatus(var read:Boolean = false, var delivered:Boolean = false, var messageID:String = "")

    data class UserActivity(var status:String)
}