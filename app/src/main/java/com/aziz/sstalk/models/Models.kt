package com.aziz.sstalk.models

class Models {

    data class MessageModel(var message:String = "",
                            var from:String = "",
                            var to:String = "",
                            var timeInMillis:Long = System.currentTimeMillis(),
                            var reverseTimeStamp: Long = timeInMillis * -1,
                            var isFile:Boolean = false,
                            var isRead:Boolean = false,
                            var messageType: String = "message",
                            var caption:String = "")

    data class Contact(var name:String = "", var number:String = "", var photoURI:String = "" , var uid: String = "")

    data class User(var name:String = "",
                    var createdOn:Long = System.currentTimeMillis(),
                    var lastModifiedOn: Long = createdOn,
                    var phone:String = "",
                    var profile_pic_url:String = "",
                    var uid:String = "")


    data class LastMessageDetail(var timeInMillis: Long = System.currentTimeMillis(), var reverseTimeStamp: Long = timeInMillis  * -1)

}