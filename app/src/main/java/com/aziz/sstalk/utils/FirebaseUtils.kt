package com.aziz.sstalk.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import androidx.core.content.ContextCompat
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.aziz.sstalk.ImagePreviewActivity
import com.aziz.sstalk.MessageActivity
import com.aziz.sstalk.models.Models
import com.aziz.sstalk.utils.FirebaseUtils.ref.user
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import com.aziz.sstalk.R
import com.aziz.sstalk.UserProfileActivity
import com.aziz.sstalk.utils.FirebaseUtils.ref.allMessageStatus
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.nex3z.notificationbadge.NotificationBadge
import com.squareup.picasso.Callback
import com.squareup.picasso.MemoryPolicy
import org.jetbrains.anko.*
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
        val NODE_GROUP_INFO = "GroupInfo"
        val NODE_GROUP_MEMBER = "GroupMember"

        val EVENT_TYPE_ADDED = "added"
        val EVENT_TYPE_REMOVED = "removed"
        val EVENT_TYPE_LEFT = "left"
        val EVENT_TYPE_CREATED = "created"


        val VAL_ONLINE = "Online"
        val VAL_OFFLINE = "Offline"
        val VAL_TYPING = "Typing..."

        val NODE_FILE = "Files"

        val NODE_FEEDBACK = "Feedbacks"


        val KEY_STATUS = "status"
        val KEY_ENABLED = "enabled"
        val KEY_UID = "uid"
        val NODE_LAST_MESSAGE = "LastMessage"
        val KEY_REVERSE_TIMESTAMP = "reverseTimeStamp"
        val KEY_TIME_IN_MILLIS = "timeInMillis"
        val KEY_PHONE = "phone"
        val KEY_PROFILE_PIC_URL = "profile_pic_url"
        val KEY_NAME = "name"
        val KEY_NAME_OR_NUMBER = "nameOrNumber"

        val KEY_CONVERSATION_SINGLE = "single"
        val KEY_CONVERSATION_GROUP = "group"


    val KEY_BLOCKED = "blocked"

        val KEY_FILE_LOCAL_PATH = "file_local_path"


        val user_voda = "vHv8TSqbS2YBHZJXS5X5Saz4acC2"
        val user_jio = "LPVjVKbpTzeUDpank04sxkoparE2"


        object ref {

            private fun root() : DatabaseReference {

                return FirebaseDatabase.getInstance().reference
            }

            fun fileRef(): DatabaseReference = root().child(NODE_FILE)

            fun getChatRoot(uid: String) = root().child(NODE_MESSAGES).child(uid)

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

            fun lastMessage(uid :String, targetUID: String) : DatabaseReference{
                return lastMessage(uid).child(targetUID)
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
                        .child(getUid())
                    //.child("vHv8TSqbS2YBHZJXS5X5Saz4acC2")

            fun messageStatus(uid: String, targetUID: String, messageID: String):DatabaseReference =
                allMessageStatus(uid, targetUID)
                .child(messageID)

            fun userStatusRoot():DatabaseReference = root().child(NODE_USER_ACTIVITY_STATUS)

            fun userStatus(uid: String):DatabaseReference = root().child(NODE_USER_ACTIVITY_STATUS)
                .child(uid)

            //this will return a boolean snapshot
            fun notificationMute(uid: String):DatabaseReference =
                    root().child(NODE_INDIVIDUAL_NOTIFICATION_SETTING)
                        .child(getUid())
                        .child(uid)
                        .child(KEY_ENABLED)

            //this will return a snapshot array
            fun getNotificationMuteRootRef():DatabaseReference =
                root().child(NODE_INDIVIDUAL_NOTIFICATION_SETTING)
                    .child(getUid())

            fun groupInfo(groupID:String):DatabaseReference =
                    root().child(NODE_GROUP_INFO).child(groupID)

            fun groupMembers(groupID:String):DatabaseReference =
                root().child(NODE_GROUP_MEMBER).child(groupID)

            fun groupMember(groupID:String, uid: String):DatabaseReference =
                root().child(NODE_GROUP_MEMBER).child(groupID).child(uid)

            fun feedback():DatabaseReference = root().child(NODE_FEEDBACK)
        }

    private fun loadContactPic(context: Context, id: String, imageView: ImageView, isGroup:Boolean = false){


        val placeHolder = if(isGroup) R.drawable.ic_group_white_24dp else R.drawable.contact_placeholder

        imageView.setImageResource(placeHolder)

        if(id.isEmpty())
            return

        var fileExists = false


        if(utils.hasStoragePermission(context)){

            val file= File(utils.profilePicPath +id+".jpg")
//            val transitionBundle =  ActivityOptions.makeThumbnailScaleUpAnimation(imageView,
//                BitmapFactory.decodeFile(file.path),0,0).toBundle()

            if(file.exists()){
                fileExists = true
                Log.d("FirebaseUtils", "loadProfilePic: exists of $id")
                imageView.loadImage(file, placeHolder)

                imageView.setOnClickListener {
                    context.startActivity(Intent(context, ImagePreviewActivity::class.java)
                        .putExtra(utils.constants.KEY_LOCAL_PATH, file.path))
                }
            }

        }

        (if(isGroup) ref.groupInfo(id) else user(id))
            .child(KEY_PROFILE_PIC_URL)
            .addValueEventListener(object : ValueEventListener {

                override fun onDataChange(p0: DataSnapshot) {

                    if(!p0.exists()) {
                        Log.d("FirebaseUtils", "onDataChange: profile pic not exists for $id")

                        imageView.setImageResource(placeHolder)

                        if(utils.hasStoragePermission(context)){
                            File(utils.profilePicPath +id+".jpg")
                                .delete()
                        }
                        return
                    }

                    if (p0.exists()) {
                        val link: String? = p0.getValue(String::class.java)

                        if(link!!.isEmpty()) {
                            Log.d("FirebaseUtils", "onDataChange: profile pic not exists for $id")

                            imageView.loadImage(placeHolder)

                            if(utils.hasStoragePermission(context)){
                                File(utils.profilePicPath +id+".jpg")
                                    .delete()
                            }
                            return
                        }

                        Log.d("FirebaseUtils", "loadProfilePic: loading for  = $id")

                        if(Pref.Profile.isProfileUrlSame(context, id, link.toString())
                            && fileExists){

                            Log.d("FirebaseUtils", "onDataChange: profile exists and is same for $id")

                            val file= File(utils.profilePicPath +id+".jpg")
                            if(file.exists()){

                                imageView.loadImage(file, placeHolder)


                                imageView.setOnClickListener {
                                    context.startActivity(Intent(context, ImagePreviewActivity::class.java)
                                        .putExtra(utils.constants.KEY_LOCAL_PATH, file.path))
                                }
                                return
                            }

                        }
                        else {




                            Log.d("FirebaseUtils", "onDataChange: profile doesn't exist or changed for $id")


                            Glide.with(context)
                                .load(link)
                                .apply(RequestOptions()
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    .placeholder(placeHolder)
                                    .error(placeHolder)
                                )
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .addListener(object : RequestListener<Drawable>{
                                    override fun onLoadFailed(
                                        e: GlideException?,
                                        model: Any?,
                                        target: Target<Drawable>?,
                                        isFirstResource: Boolean
                                    ): Boolean {

                                        Log.d("FirebaseUtils", "onError: error loading image for $id = ${e?.message}")
                                        return false
                                    }

                                    override fun onResourceReady(
                                        resource: Drawable?,
                                        model: Any?,
                                        target: Target<Drawable>?,
                                        dataSource: DataSource?,
                                        isFirstResource: Boolean
                                    ): Boolean {

                                        if (utils.hasStoragePermission(context)) {
                                            Log.d("FirebaseUtils", "onSuccess: saving profile pic")
                                            utils.saveBitmapToProfileFolder(
                                                (resource as? BitmapDrawable?)?.bitmap,
                                                id
                                            )
                                            Pref.Profile.setProfileUrl(context, id, link.toString())
                                        }
                                        return true
                                    }

                                })
                                .into(imageView)



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
    fun loadProfilePic(context: Context, uid: String, imageView: ImageView){
        loadContactPic(context, uid, imageView)
    }


    //for group
    fun loadGroupPic(context: Context, groupId: String, imageView: ImageView){

        loadContactPic(context, groupId, imageView, isGroup = true)

    }

    fun loadProfileThumbnail(context: Context, uid:String, imageView: ImageView){

       try{
           imageView.setImageResource(R.drawable.contact_placeholder)
       }catch (e:Exception){}


        if(uid.isEmpty())
            return


        var fileExists = false

        if(utils.hasStoragePermission(context)){
            val file= File(utils.profilePicPath +uid+".jpg")
            if(file.exists()){
                Log.d("FirebaseUtils", "loadProfileThumbnail: profile exists")
                fileExists = true

                Glide.with(context).load(file).into(imageView)

//                Picasso.get().load(file)
//                    .resize(80,80)
//                    .centerCrop()
//                    .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
//                    .into(imageView)
            }

        }




        user(uid)
            .child(KEY_PROFILE_PIC_URL)
            .addValueEventListener(object : ValueEventListener {

                override fun onDataChange(p0: DataSnapshot) {


                    if(!p0.exists()) {
                        Log.d("FirebaseUtils", "onDataChange: profile thumbnail not exists for $uid")
                        imageView.setImageResource(R.drawable.contact_placeholder)
                        if(utils.hasStoragePermission(context)){
                            File(utils.profilePicPath +uid+".jpg")
                                .delete()
                        }
                        return
                    }

                    if (p0.exists()) {
                        val link = p0.getValue(String::class.java)?:""

                        if(link.isEmpty()) {
                            Log.d("FirebaseUtils", "onDataChange: profile pic not exists for $uid")
                            Picasso.get().load(R.drawable.contact_placeholder).into(imageView)
                            if(utils.hasStoragePermission(context)){
                                File(utils.profilePicPath +uid+".jpg")
                                    .delete()
                            }
                            return
                        }


                        if(Pref.Profile.isProfileUrlSame(context, uid, link)
                            && fileExists){
                                val file= File(utils.profilePicPath +uid+".jpg")
                                if(file.exists()){
                                    Glide.with(context).load(file).into(imageView)
//                                    Picasso.get().load(file)
//                                        .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
//                                        .resize(80,80)
//                                        .centerCrop()
//                                        .into(imageView)
                                }

                            return
                        }
                        else {
                            //download profile pic
                            Log.d("FirebaseUtils", "onDataChange:,  profile url has changed, loading from web")


                            Glide.with(context).asBitmap().addListener(object : RequestListener<Bitmap?>{
                                override fun onLoadFailed(
                                    e: GlideException?,
                                    model: Any?,
                                    target: Target<Bitmap?>?,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    Log.d("FirebaseUtils", "onError: error loading image for $uid = ${e?.message}")

                                    return false
                                }

                                override fun onResourceReady(
                                    resource: Bitmap?,
                                    model: Any?,
                                    target: Target<Bitmap?>?,
                                    dataSource: DataSource?,
                                    isFirstResource: Boolean
                                ): Boolean {

                                    if (utils.hasStoragePermission(context)) {
                                        utils.saveBitmapToProfileFolder(
                                            resource,
                                            uid
                                        )
                                        Pref.Profile.setProfileUrl(context, uid, link.toString())
                                    }

                                    return true
                                }

                            }).load(link).into(imageView)

//                            Picasso.get().load(link)
//                                .placeholder(R.drawable.contact_placeholder)
//                                .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
//                                .into(imageView, object : Callback {
//                                    override fun onSuccess() {
//
//                                    }
//
//                                    override fun onError(e: Exception?) {
//                                     }
//
//                                })
                        }
                    }
                }

                override fun onCancelled(p0: DatabaseError) {

                }
            })
    }


    fun loadGroupPicThumbnail(context: Context, groupId:String, imageView: ImageView){

        try{
            imageView.setImageResource(R.drawable.ic_group_white_24dp)
        }catch (e:Exception){}


        if(groupId.isEmpty())
            return

        var fileExists = false

        if(utils.hasStoragePermission(context)){
            val file= File(utils.profilePicPath +groupId+".jpg")
            if(file.exists()){
                fileExists = true

                Glide.with(context).load(file).into(imageView)

//                Picasso.get().load(file)
//                    .resize(80,80)
//                    .centerCrop()
//                    .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
//                    .into(imageView)
            }

        }




        ref.groupInfo(groupId)
            .child(KEY_PROFILE_PIC_URL)
            .addValueEventListener(object : ValueEventListener {

                override fun onDataChange(p0: DataSnapshot) {

                    if(!p0.exists()) {
                        Log.d("FirebaseUtils", "onDataChange: group(snapshot) thumbnail not exists for $groupId")
                        imageView.setImageResource(R.drawable.ic_group_white_24dp)

                        if(utils.hasStoragePermission(context)){
                           File(utils.profilePicPath +groupId+".jpg")
                               .delete()
                        }

                        return
                    }

                    if (p0.exists()) {
                        val link: String? = p0.getValue(String::class.java)

                        if(link!!.isEmpty()) {
                            Log.d("FirebaseUtils", "onDataChange: profile pic not exists for $groupId")
                            imageView.setImageResource(R.drawable.ic_group_white_24dp)
                            if(utils.hasStoragePermission(context)){
                                File(utils.profilePicPath +groupId+".jpg")
                                    .delete()
                            }
                            return
                        }

                        if(Pref.Profile.isProfileUrlSame(context, groupId, link.toString())
                            && fileExists){
                            val file= File(utils.profilePicPath +groupId+".jpg")
                            if(file.exists()){


                                Glide.with(context).load(file).into(imageView)

//                                Picasso.get().load(file)
//                                    .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
//                                    .resize(80,80)
//                                    .centerCrop()
//                                    .into(imageView)
                            }

                            return
                        }
                        else {
                            //download profile pic
                            Log.d("FirebaseUtils", "onDataChange:,  profile url has changed, loading from web")

                            Glide.with(context).asBitmap().listener(object : RequestListener<Bitmap?>{
                                override fun onLoadFailed(
                                    e: GlideException?,
                                    model: Any?,
                                    target: Target<Bitmap?>?,
                                    isFirstResource: Boolean
                                ): Boolean  = false

                                override fun onResourceReady(
                                    resource: Bitmap?,
                                    model: Any?,
                                    target: Target<Bitmap?>?,
                                    dataSource: DataSource?,
                                    isFirstResource: Boolean
                                ): Boolean {

                                    if (utils.hasStoragePermission(context)) {
                                        utils.saveBitmapToProfileFolder(
                                            (imageView.drawable as BitmapDrawable).bitmap,
                                            groupId
                                        )
                                        Pref.Profile.setProfileUrl(context, groupId, link.toString())
                                    }

                                    return true
                                }

                            }).into(imageView)

//                            Picasso.get().load(link)
//                                .placeholder(R.drawable.ic_group_white_24dp)
//                                .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
//                                .into(imageView, object : Callback {
//                                    override fun onSuccess() {
//
//                                    }
//
//                                    override fun onError(e: Exception?) {
//
//                                    }
//
//                                })
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

    fun getPhoneNumber() : String = if(isLoggedIn()) FirebaseAuth.getInstance().currentUser!!.phoneNumber!! else "1234567890"


    fun setUserDetailFromUID(context : Context,
            textView: TextView,
            uid: String,
            shouldQueryFromContacts: Boolean, onLoaded: ((number: String) -> Unit?)? = null){

            user(uid)
                .child(KEY_PHONE)
                .addValueEventListener(object : ValueEventListener {
                    override fun onCancelled(p0: DatabaseError) {}

                    override fun onDataChange(snapshot: DataSnapshot) {

                        if(!snapshot.exists()){
                            textView.text = ""
                            return
                        }


                        val phone = snapshot.getValue(String::class.java)


                        textView.text = phone

                        onLoaded?.invoke(phone.orEmpty())

                        if(shouldQueryFromContacts){

                            textView.text = utils.getNameFromNumber(context, phone!!)


                        }
                    }

                })
        }


    fun setLastMessage(targetUID: String, textView: TextView, messageStatusImageView:ImageView){

        textView.text  = ""

        messageStatusImageView.visibility = View.GONE

        ref.getChatQuery(getUid(), targetUID)
            .limitToLast(1)
            .addValueEventListener(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {}

                override fun onDataChange(p0: DataSnapshot) {


                    var messageModel:Models.MessageModel? = null
                    var messageID = ""

                    if(!p0.exists()) {
                        textView.text = ""
                        textView.visibility = View.GONE
                        return
                    }

                    for(item in p0.children) {
                        messageModel = item.getValue(Models.MessageModel::class.java)
                        messageID = item.key!!
                    }



                    //.replace("\n"," ")
                    when {
                        messageModel?.messageType == EVENT_TYPE_LEFT -> textView.text = "❗ A member left"
                        messageModel?.messageType == EVENT_TYPE_ADDED -> textView.text = "❗ A new member was added"
                        messageModel?.messageType == EVENT_TYPE_CREATED -> textView.text = "❗ Group was created"
                        messageModel?.messageType == EVENT_TYPE_REMOVED -> textView.text = "❗ A member was removed"

                    }

                    if(textView.text.isNotEmpty())
                        return


                    if(p0.exists()) {
                        textView.text = messageModel?.message//.replace("\n"," ")
                        textView.visibility = View.VISIBLE


                        if(messageModel?.from == getUid()){
                            messageStatusImageView.visibility = View.VISIBLE
                            setDeliveryStatusTick(targetUID, messageID, messageStatusImageView)
                        }
                        else{
                            messageStatusImageView.visibility = View.GONE
                        }

                        when {
                            messageModel?.messageType == utils.constants.FILE_TYPE_IMAGE -> textView.text = ("\uD83D\uDDBC Image")
                            messageModel?.messageType == utils.constants.FILE_TYPE_VIDEO -> textView.text = ("\uD83C\uDFA5 Video")
                            messageModel?.messageType == utils.constants.FILE_TYPE_AUDIO -> textView.text = ("\uD83C\uDFB5 Audio")
                            messageModel?.messageType == utils.constants.FILE_TYPE_LOCATION -> textView.text = ("\uD83D\uDCCC ${if(messageModel.caption.isNullOrEmpty()) " Location" else messageModel.caption}")
                        }

                    }

                }
            })
    }

    fun setUnreadCount(targetUID: String, notificationBadge: NotificationBadge, vararg boldTextViews: TextView){

        var initialTypeface:Typeface? = null
        notificationBadge.visibility = View.GONE

        if(boldTextViews.isNotEmpty())
            initialTypeface = boldTextViews[0].typeface


        allMessageStatus(getUid(), targetUID)
            .orderByChild("delivered")
            .equalTo(false)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                }

                override fun onDataChange(p0: DataSnapshot) {
                    p0.children.forEach {
                        it.ref.child("delivered").setValue(true)
                    }
                }
            })

        allMessageStatus(getUid(), targetUID)
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
        ref.userStatus(getUid())
            .setValue(Models.UserActivityStatus(VAL_ONLINE, System.currentTimeMillis()))
    }

    fun setMeAsOffline(){
        ref.userStatus(getUid())
            .setValue(Models.UserActivityStatus(VAL_OFFLINE, System.currentTimeMillis()))
    }

    fun setMeAsTyping(targetUID:String){
        ref.userStatus(getUid())
            .setValue(Models.UserActivityStatus(VAL_TYPING +" - $targetUID", System.currentTimeMillis()))
    }

    fun setDeliveryStatusTick(
        targetUID: String, messageID: String,
        messageStatusImageView: ImageView
    ){

        messageStatusImageView.alpha = 0.8f

        if(utils.isGroupID(targetUID)){
            messageStatusImageView.visibility = View.GONE
            return
        }


        ref.messageStatus(targetUID,getUid(), messageID)
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


    fun setMessageStatusToDB(messageID: String, uid: String,targetUID: String, isDelivered:Boolean, isRead:Boolean, groupNameIf:String){
        Log.d(
            "FirebaseUtils",
            "setMessageStatusToDB: setting values to $uid -> $targetUID as $isDelivered, $isRead on $messageID"
        )

        Log.d("FirebaseUtils", "setMessageStatusToDB: group name = $groupNameIf")

        ref.messageStatus(uid,targetUID,messageID)
            .setValue(Models.MessageStatus(getUid(), isRead, isDelivered, messageID,
                if(isLoggedIn()) FirebaseAuth.getInstance().currentUser!!.phoneNumber!! else "1234567890",
                if(isLoggedIn()) FirebaseAuth.getInstance().currentUser!!.photoUrl.toString() else "",
                groupNameIf))
    }


    fun setReadStatusToMessage(messageID: String, targetUID: String){


        try {

          Handler().postDelayed({
              Log.d(
                  "FirebaseUtils",
                  "setReadStatusToMessage: setting read status to  -> $targetUID as  $messageID " +
                          "after 1 sec delay"
              )
              ref.messageStatus(getUid(), targetUID, messageID)
                  .child("read")
                  .setValue(true)

              ref.messageStatus(getUid(), targetUID, messageID)
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

                    when {
                        userStatus.status == VAL_ONLINE -> {
                            textView.text = userStatus.status
                            textView.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(context, R.drawable.shape_bubble_online),
                                null, null,null)
                        }
                        userStatus.status.startsWith(VAL_TYPING) -> {
                            if(userStatus.status.endsWith(uid))
                                textView.text = VAL_TYPING
                            else
                                textView.text = VAL_ONLINE

                            textView.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(context, R.drawable.shape_bubble_online),
                                null, null,null)

                        }
                        else -> {
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

                    if(userStatus.status == VAL_ONLINE || userStatus.status.startsWith(VAL_TYPING)){
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

                    ref.FCMToken(getUid()).child(it.result!!.id)
                        .setValue(it.result!!.token)
            }
    }


    fun deleteCurrentToken(){
        FirebaseInstanceId.getInstance()
            .instanceId
            .addOnCompleteListener {
                if(!it.isSuccessful)
                    return@addOnCompleteListener

                ref.FCMToken(getUid()).child(it.result!!.id)
                    .removeValue().addOnSuccessListener {
                        Log.d("FirebaseUtils", "deleteCurrentToken: token removed")
                    }
            }
    }


    fun storeFileMetaData(file:Models.File){
        ref.fileRef()
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
                            imageView.show()
                            return
                        }
                    }
                    imageView.visibility = View.GONE

                }
            })
    }



    fun checkForUpdate(context: Context, shouldShowToast:Boolean){
        val key_app_code = "App_Version_Code"

        //this will return an int
//        FirebaseDatabase.getInstance().getReference(key_app_code)
//            .addListenerForSingleValueEvent(object : ValueEventListener {
//                override fun onCancelled(p0: DatabaseError) {
//                }
//
//                override fun onDataChange(p0: DataSnapshot) {
//
//                    val versionCode = p0.getValue(Int::class.java)?:BuildConfig.VERSION_CODE
//                    Log.d("FirebaseUtils", "onDataChange: current version = ${com.aziz.sstalk.BuildConfig.VERSION_CODE}")
//                    Log.d("FirebaseUtils", "onDataChange: available version = $versionCode")
//
//                    if(versionCode > com.aziz.sstalk.BuildConfig.VERSION_CODE){
//                        //show update dialog
//                        context.alert {
//                            positiveButton("Go to PlayStore"){
//                                context.browse(utils.constants.APP_LINK)
//                            }
//                            negativeButton("Cancel"){
//                            }
//                            title = "Update available"
//                            message = "A New update has been available for SS Talk"
//                            isCancelable = false
//                        }
//                            .show()
//                    }
//                    else if(shouldShowToast){
//                        context.toast("No update available")
//                    }
//
//                }
//            })

    }


    fun addedMemberEvent(uid:String, groupID: String, addingMemberPhoneNumber:String){

        Log.d(
            "FirebaseUtils",
            "addedMemberEvent: adding $addingMemberPhoneNumber to group $groupID and showing to $uid"
        )

        ref.getChatRef(uid, groupID)
            .child("MSG${System.currentTimeMillis()}")
            .setValue(Models.MessageModel(addingMemberPhoneNumber,  //"message" will contain number of the one who is added
                getPhoneNumber(), // "from"  will be phone number of adder
                messageType = EVENT_TYPE_ADDED
                ))
    }


    fun createdGroupEvent(uid:String, groupID: String, addingMemberPhoneNumber:String){

        Log.d(
            "FirebaseUtils",
            "createdGroupEvent: adding $addingMemberPhoneNumber to group $groupID and showing to $uid"
        )

        ref.getChatRef(uid, groupID)
            .child("MSG${System.currentTimeMillis()}")
            .setValue(Models.MessageModel(addingMemberPhoneNumber,  //"message" will contain number of the one who is added
                getPhoneNumber(), // "from"  will be phone number of adder
                messageType = EVENT_TYPE_CREATED
            ))
    }


    fun removedMemberEvent(uid:String, groupID: String, removedMemberPhoneNumber:String){

        ref.getChatRef(uid, groupID)
            .child("MSG${System.currentTimeMillis()}")
            .setValue(Models.MessageModel(removedMemberPhoneNumber,
                getPhoneNumber(),// this will use as remover
                messageType = EVENT_TYPE_REMOVED
            ))
    }


    private fun leftMemberEvent(uid: String, groupID: String){

        ref.getChatRef(uid, groupID)
            .child("MSG${System.currentTimeMillis()}")
            .setValue(Models.MessageModel(
                getPhoneNumber(),
                getPhoneNumber(),// this will use as remover
                messageType = EVENT_TYPE_LEFT
            ))
    }

    fun setGroupName(groupID: String, textView: TextView, onLoaded: ((name:String) -> Unit?)?=null){


        ref.groupInfo(groupID)
            .child(utils.constants.KEY_NAME)
            .addValueEventListener(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                }

                override fun onDataChange(p0: DataSnapshot) {
                    if(p0.exists())
                    {
                        textView.text = p0.value.toString()
                        onLoaded?.invoke(textView.text.toString())
                    }
                    else
                        textView.text = "Unknown Group"
                }
            })
    }


    fun setTargetOptionMenu(context: Context, uid: String, phoneNumber:String, view: View){

        view.setOnClickListener {
         context.selector("", listOf("View Profile", "Message", "Make a call")) { _, i ->

            when (i) {
                1 -> {
                    context.startActivity(Intent(context, MessageActivity::class.java).apply {
                        putExtra(KEY_UID, uid)
                        putExtra(utils.constants.KEY_NAME_OR_NUMBER, phoneNumber)
                        putExtra(utils.constants.KEY_TARGET_TYPE, KEY_CONVERSATION_SINGLE)
                        addFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    })
                }

                0 -> {
                    context.startActivity(Intent(context, UserProfileActivity::class.java).apply {
                        putExtra(KEY_UID, uid)
                        putExtra(KEY_NAME, phoneNumber)
                        putExtra(utils.constants.KEY_IS_GROUP, false )
                    })
                }

                2 -> {
                    if(utils.hasCallPermission(context)){
                        context.alert { yesButton{ context.makeCall(phoneNumber) }
                            noButton{}
                            message = "Call ${utils.getNameFromNumber(context, phoneNumber)}"
                        }.show()
                    }
                    else
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            (context as Activity).requestPermissions(arrayOf(android.Manifest.permission.CALL_PHONE), 132)
                        }

                }
            }
        }
        }
    }


    //this method will add events only, it won't remove member
    //you have to use it in callback of member removal
    fun removeMember(uid: String, groupID: String, phoneNumber: String, groupName: String,
                     isRemovedByOther:Boolean){
        if(isRemovedByOther)
            removedMemberEvent(uid, groupID, phoneNumber)
        else
            leftMemberEvent(uid, groupID)
        //update last message node
        ref.lastMessage(uid)
            .child(groupID)
            .setValue(Models.LastMessageDetail(type = KEY_CONVERSATION_GROUP,
                nameOrNumber = groupName))

    }

    fun showTargetOptionMenuFromProfile(
        context: Context,
        uid: String,
        groupID: String,
        phoneNumber: String,
        isAdmin: Boolean,
        isMeAdmin: Boolean,
        groupMembers : MutableList<Models.GroupMember>,
        groupName:String
    ){


            context.selector("", listOf("${if(isAdmin) "Dismiss" else "Make"} Admin",
                "Remove this member",
                "View Profile", "Message", "Make a call")) { _, i ->

                when (i) {

                    0 -> {
                        if(!isMeAdmin)
                            context.toast("You have to be an admin to remove someone")
                        else{
                            context.alert {
                                message = if(isAdmin) "Dismiss ${utils.getNameFromNumber(context, phoneNumber)} from admin?"
                                else "Make ${utils.getNameFromNumber(context, phoneNumber)} as Admin?"
                                yesButton{
                                    ref.groupMember(groupID, uid)
                                        .child("admin").setValue(!isAdmin)
                                }
                                noButton{}
                            }.show()
                        }
                    }

                    1-> {

                        // make or dismiss admin
                        if(!isMeAdmin) {
                            context.toast("You have to be an admin to remove someone")
                            return@selector
                        }

                        context.alert { yesButton {
                            ref.groupMember(groupID, uid)
                                .child("removed").setValue(true).addOnSuccessListener {
                                    this.ctx.toast("Member removed")
                                    repeat(groupMembers.size) {

                                        removeMember(uid, groupID, phoneNumber, groupName, true)
                                    }

                                    // add event to myself
                                    removedMemberEvent(getUid(), groupID, phoneNumber)
                                }
                        }
                            noButton{}
                            message = "Remove ${utils.getNameFromNumber(context, phoneNumber)} from this group?"
                        }.show()
                    }


                    3 -> {
                        //show message activity
                        context.startActivity(Intent(context, MessageActivity::class.java).apply {
                            putExtra(KEY_UID, uid)
                            putExtra(utils.constants.KEY_NAME_OR_NUMBER, phoneNumber)
                            putExtra(utils.constants.KEY_TARGET_TYPE, KEY_CONVERSATION_SINGLE)
                            addFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        })
                    }

                    2 -> {
                        //show profile activity
                        context.startActivity(Intent(context, UserProfileActivity::class.java).apply {
                            putExtra(KEY_UID, uid)
                            putExtra(KEY_NAME, phoneNumber)
                            putExtra(utils.constants.KEY_IS_GROUP, false )
                        })
                    }

                    4 -> {
                        //make call
                        if(utils.hasCallPermission(context)) {
                            context.alert { yesButton{
                                context.makeCall(phoneNumber)
                            }
                                noButton{}
                                message = "Call ${utils.getNameFromNumber(context, phoneNumber)}?"
                            }.show()
                        }
                        else
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                (context as Activity).requestPermissions(arrayOf(android.Manifest.permission.CALL_PHONE), 132)
                            }

                    }
                }

        }
    }



     fun setonDisconnectListener(){

        ref.userStatus(getUid())
            .onDisconnect()
            .setValue(Models.UserActivityStatus(VAL_OFFLINE, System.currentTimeMillis()))
    }
}