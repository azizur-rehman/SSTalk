package com.aziz.sstalk

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.util.Log
import android.view.*
import com.aziz.sstalk.models.Models
import com.aziz.sstalk.utils.FirebaseUtils
import com.aziz.sstalk.utils.utils
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.UploadTask
import com.squareup.picasso.Picasso
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import com.vincent.filepicker.DividerGridItemDecoration
import com.yarolegovich.lovelydialog.LovelyInfoDialog
import com.yarolegovich.lovelydialog.LovelyTextInputDialog
import kotlinx.android.synthetic.main.activity_user_profile.*
import kotlinx.android.synthetic.main.content_user_profile.*
import kotlinx.android.synthetic.main.item_group_member_layout.view.*
import kotlinx.android.synthetic.main.item_image.view.*
import kotlinx.android.synthetic.main.item_video.view.*
import kotlinx.android.synthetic.main.layout_profile_image_picker.*
import me.shaohui.advancedluban.Luban
import me.shaohui.advancedluban.OnCompressListener
import org.jetbrains.anko.*
import org.jetbrains.anko.collections.forEachWithIndex
import java.io.File
import java.util.ArrayList
import java.util.concurrent.Future

class UserProfileActivity : AppCompatActivity() {

    val  messageModels:MutableList<Models.MessageModel> = ArrayList()
    var myUID = ""
    var targetUID = ""
    var isBlockedByMe = false
    var isPhoneLoaded = false
    var name = ""
    var isGroup = false

    private var asyncLoader: Future<Boolean>? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)
        setSupportActionBar(toolbar)

        if(supportActionBar!=null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setHomeButtonEnabled(true)
        }

        FirebaseUtils.setonDisconnectListener()

        contentView?.setBackgroundColor(Color.TRANSPARENT)

        myUID = FirebaseUtils.getUid()

        targetUID = intent.getStringExtra(FirebaseUtils.KEY_UID)
        name = intent.getStringExtra(FirebaseUtils.KEY_NAME)

        isGroup = intent.getBooleanExtra(utils.constants.KEY_IS_GROUP, false)

        title = if(isGroup) name else utils.getNameFromNumber(this, name)

        utils.printIntentKeyValues(intent)
        add_group_member_btn.visibility = View.GONE

        if(!isGroup) {
            phone_textview.text = name
            isPhoneLoaded = true
        }else {
            phone_textview.visibility = View.GONE
            loadGroupMembers()
        }

        if(phone_textview.text.isEmpty() && !isGroup) {
            // if phone number is not available
            FirebaseUtils.ref.allUser()
                .child(targetUID)
                .child(FirebaseUtils.KEY_PHONE)
                .addValueEventListener(object : ValueEventListener {
                    override fun onCancelled(p0: DatabaseError) {
                    }

                    override fun onDataChange(p0: DataSnapshot) {
                        phone_textview.text = p0.getValue(String::class.java)
                        isPhoneLoaded = true
                        invalidateOptionsMenu()
                    }

                })

        }

        val layoutManager = androidx.recyclerview.widget.GridLayoutManager(this, 4)
        mediaRecyclerView.addItemDecoration(DividerGridItemDecoration(this))
        mediaRecyclerView.isNestedScrollingEnabled = true

        mediaRecyclerView.layoutManager = layoutManager



        asyncLoader = doAsyncResult {

            uiThread {

                if(!isGroup)
                    FirebaseUtils.loadProfilePic(this@UserProfileActivity, targetUID, user_profile_imageview)
                else
                    FirebaseUtils.loadGroupPic(context, targetUID, user_profile_imageview)

                //loading media recyclerview
                FirebaseUtils.ref.getChatRef(myUID,targetUID)
                    .orderByChild(FirebaseUtils.KEY_REVERSE_TIMESTAMP)
                    .addValueEventListener(object:ValueEventListener {
                        override fun onCancelled(p0: DatabaseError) {
                        }

                        override fun onDataChange(p0: DataSnapshot) {


                            messageModels.clear()

                            if(!p0.exists())
                                return

                            p0.children.forEach {
                                val model = it.getValue(Models.MessageModel ::class.java)

                                if(model!!.file_local_path.isNotEmpty() && File(model.file_local_path).exists())
                                    messageModels.add(it.getValue(Models.MessageModel ::class.java)!!)
                            }


                            if(messageModels.isEmpty())
                                return

                            if(mediaRecyclerView.adapter != null)
                                mediaRecyclerView.adapter!!.notifyDataSetChanged()
                            else {

                                mediaRecyclerView.adapter = object : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
                                    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): androidx.recyclerview.widget.RecyclerView.ViewHolder {
                                        //0 for image
                                        //1 for video right now

                                        return if (p1 == 0) imageHolder(layoutInflater.inflate(R.layout.item_image, p0, false))
                                        else videoHolder(layoutInflater.inflate(R.layout.item_video, p0, false))


                                    }

                                    override fun getItemCount(): Int = messageModels.size

                                    override fun getItemViewType(position: Int): Int {
                                        return if (messageModels[position].messageType == utils.constants.FILE_TYPE_IMAGE) 0 else 1
                                    }

                                    override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, p1: Int) {

                                        if (holder is imageHolder) {
                                            Picasso.get().load(File(messageModels[p1].file_local_path))
                                                .fit()
                                                .centerCrop()
                                                .into(holder.imageView)
                                            //  holder.imageView.setImageBitmap(BitmapFactory.decodeFile(messageModels[p1].file_local_path))

                                            holder.imageView.setOnClickListener {
                                                startActivity(
                                                    Intent(this@UserProfileActivity, ImagePreviewActivity::class.java)
                                                        .putExtra(
                                                            utils.constants.KEY_LOCAL_PATH,
                                                            messageModels[p1].file_local_path
                                                        )
                                                )
                                            }
                                        } else if (holder is videoHolder) {

                                            utils.loadVideoThumbnailFromLocalAsync(this@UserProfileActivity, holder.imageView, messageModels[p1].file_local_path)
                                            holder.length.text = utils.getVideoLength(
                                                this@UserProfileActivity,
                                                messageModels[p1].file_local_path
                                            )

                                            holder.imageView.setOnClickListener {
                                                utils.startVideoIntent(
                                                    this@UserProfileActivity,
                                                    messageModels[p1].file_local_path
                                                )
                                            }
                                        }
                                    }

                                }

                            }
                        }

                    })

            }

        }

        phone_textview.setOnClickListener {
            if(isPhoneLoaded && phone_textview.text.isNotEmpty())
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("tel:${phone_textview.text}")))
        }


        block_user.setOnClickListener {

            if(isGroup){

                alert { message = "Exit from this group? You will no longer be a part of this conversation."
                positiveButton("Yes, Go on!"){

                    FirebaseUtils.ref.groupMember(targetUID , myUID)
                        .child("removed").setValue(true).addOnSuccessListener {
                            this.ctx.toast("Group left")
                            groupMembers.forEach {
                                // only to notify others
                                FirebaseUtils.removeMember(it.uid, targetUID,
                                    FirebaseUtils.getPhoneNumber(), name, false)
                            }

                            // notify myself
                            FirebaseUtils.removeMember(FirebaseUtils.getUid(),
                                targetUID, FirebaseUtils.getPhoneNumber(),
                                name, false)


                            if(!groupMembers.any{ it.admin }){
                                if(groupMembers.isNotEmpty()){
                                    FirebaseUtils.ref.groupMember(targetUID, groupMembers[0].uid)
                                        .child("admin")
                                        .setValue(true)
                                }
                            }

                            finish()
                        }


                }
                    negativeButton("No, Don't"){

                    }
                }.show()

                return@setOnClickListener
            }

            AlertDialog.Builder(this@UserProfileActivity).setMessage("${if (isBlockedByMe) "Unblock" else "Block"} this user")
                .setPositiveButton("Yes") { _, _ ->
                    FirebaseUtils.ref.blockedUser(myUID, targetUID)
                        .setValue(!isBlockedByMe)
                }
                .setNegativeButton("No", null)
                .show()
        }

        if(!isGroup)
        checkIfBlocked()
        else {
            block_user.text = "Exit from group"
            block_user.setCompoundDrawablesWithIntrinsicBounds(
                ContextCompat.getDrawable(this, R.drawable.ic_logout_red)
                ,null,null, null)
        }


        //set notification switch enable/disable
        notification_switch.setOnCheckedChangeListener { _, isChecked ->
            FirebaseUtils.ref.notificationMute(targetUID)
                .setValue(isChecked)
        }

        //set switch initial value
        FirebaseUtils.ref.notificationMute(targetUID)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                }

                override fun onDataChange(p0: DataSnapshot) {
                    if(!p0.exists()) {
                        notification_switch.isChecked = false
                        return
                    }
                        notification_switch.isChecked = p0.getValue(Boolean::class.java)!!
                }
            })
    }


    override fun onDestroy() {

        asyncLoader?.cancel(true)

        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        if(isGroup) menuInflater.inflate(R.menu.edit_profile_menu, menu)
        else if(phone_textview.text.toString() == supportActionBar?.title ) {
             menuInflater.inflate(R.menu.user_profile_menu, menu)
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        when(item!!.itemId){
            android.R.id.home -> finish()
            R.id.action_contact -> {
                val contactIntent = Intent(Intent.ACTION_INSERT)
                contactIntent.putExtra(ContactsContract.Intents.Insert.PHONE, phone_textview.text)
                contactIntent.type = ContactsContract.RawContacts.CONTENT_TYPE
                startActivityForResult(contactIntent, 111)
            }

            R.id.action_edit -> {

                if(!groupMembers.any { it.uid == myUID }){
                    toast("You are no longer a part of this group")
                    return false
                }

                selector("Edit Group", listOf("Edit name", "Change Group picture", "Remove picture")){_,i ->
                    when(i){
                        0 -> {
                            //show group name edit dialog
                            showGroupNameEditDialog()
                        }

                        1 -> {
                            //show group profile change
                            CropImage.activity()
                                .setGuidelines(CropImageView.Guidelines.ON)
                                .setCropShape(CropImageView.CropShape.RECTANGLE)
                                .setAspectRatio(1,1)
                                .start(this)
                        }

                        2 -> {
                            alert { message = "Remove profile picture?"
                            yesButton { updateProfileUrl(targetUID, "")}
                                noButton {  }
                            }.show()
                        }
                    }
                }


            }
        }

        return super.onOptionsItemSelected(item)

    }

    private fun checkIfBlocked(){
        //check if i have blocked
        FirebaseUtils.ref.blockedUser(myUID, targetUID)
            .addValueEventListener(object : ValueEventListener {
                @SuppressLint("SetTextI18n")
                override fun onDataChange(dataSnapshot: DataSnapshot) {

                    isBlockedByMe = if (dataSnapshot.exists())
                        dataSnapshot.getValue(Boolean::class.java)!!
                    else
                        false

                    block_user.text = "${if(isBlockedByMe) "Unblock" else "Block" } this user"

                }

                override fun onCancelled(databaseError: DatabaseError) {

                }
            })
    }




    var groupMembers:MutableList<Models.GroupMember> = ArrayList()
    private fun loadGroupMembers(){
        if(!isGroup)
            return

        profile_heading.text = "Group Participants"



        //load created by
        FirebaseUtils.ref.groupInfo(targetUID)
//            .child("createdBy")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) { }

                override fun onDataChange(p0: DataSnapshot) {
                    if(!p0.exists())
                        return

                    val group = p0.getValue(Models.Group::class.java)

                    val uid = group?.createdBy

                    FirebaseUtils.ref.user(uid!!)
                        .child("phone")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onCancelled(p0: DatabaseError) { }

                            override fun onDataChange(p0: DataSnapshot) {
                                val phone = p0.getValue(String::class.java)
                                val subtitle = "Created by ${utils.getNameFromNumber(this@UserProfileActivity,phone!!)}" +
                                        " on ${utils.getHeaderFormattedDate(group.createdOn)}"

                                Log.d("UserProfileActivity", "onDataChange: $subtitle")
                                supportActionBar?.subtitle = subtitle
                                toolbar_subtitle_textView.text = subtitle
                            }
                        })

                }
            })


        FirebaseUtils.ref.groupMembers(targetUID)
            .orderByChild("addedOn")
            .addValueEventListener(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {                }

                override fun onDataChange(p0: DataSnapshot) {

                    groupMembers.clear()

                    for(post in p0.children){
                        val member = post.getValue(Models.GroupMember::class.java)!!
                        if(!member.removed)
                        groupMembers.add(member)

                    }
                    setMemberAdapter(groupMembers)
                }
            })
    }

    private fun setMemberAdapter(groupMembers:MutableList<Models.GroupMember>){

        val excludedUIDs:MutableList<String> = ArrayList()
        val isAdmin = groupMembers.any { it.uid == FirebaseUtils.getUid() && it.admin }

        // keep track of latest value just in case its changed and user is engaged with the screen
        FirebaseUtils.ref.groupInfo(targetUID)
            .child(utils.constants.KEY_NAME)
            .addValueEventListener(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                }

                override fun onDataChange(p0: DataSnapshot) {
                    name = p0.value.toString()
                    Log.d("UserProfileActivity", "onDataChange: value has changed name = $name")
                    title = name
                }
            })


        if(!groupMembers.any { it.uid == myUID }){
            group_member_recycler_view.visibility = View.GONE
            block_user.visibility = View.GONE
            return
        }

        groupMembers.forEach { excludedUIDs.add(it.uid) }

        if(isAdmin)
            add_group_member_btn.visibility = View.VISIBLE

        add_group_member_btn.setOnClickListener {
            startActivityForResult(Intent(this, MultiContactChooserActivity::class.java)
                .apply { putStringArrayListExtra(utils.constants.KEY_EXCLUDED_LIST, excludedUIDs as ArrayList<String>) }, 101)
        }

        class memberHolder(itemView: View): androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView){
            var name = itemView.name!!
            var profilePic = itemView.pic!!
            var admin = itemView.admin_textview!!

        }

        group_member_recycler_view.adapter = object : androidx.recyclerview.widget.RecyclerView.Adapter<memberHolder>() {
            override fun onCreateViewHolder(p0: ViewGroup, p1: Int): memberHolder {
                return memberHolder(layoutInflater.inflate(R.layout.item_group_member_layout, p0, false))
            }

            override fun getItemCount(): Int = groupMembers.size

            override fun onBindViewHolder(p0: memberHolder, p1: Int) {

                FirebaseUtils.loadProfileThumbnail(this@UserProfileActivity, groupMembers[p1].uid,
                    p0.profilePic)
                p0.name.text = utils.getNameFromNumber(this@UserProfileActivity, groupMembers[p1].phoneNumber)

                p0.admin.visibility =  if(groupMembers[p1].admin)  View.VISIBLE else View.GONE

                val groupMember =  groupMembers[p0.adapterPosition]

                p0.itemView.setOnClickListener {
                    Log.d("UserProfileActivity", "onBindViewHolder: uid = $groupMember")
                    if(groupMember.uid == myUID)
                        return@setOnClickListener


                    FirebaseUtils.showTargetOptionMenuFromProfile(this@UserProfileActivity,
                        groupMember.uid, targetUID, groupMember.phoneNumber,groupMember.admin, isAdmin ,
                        groupMembers, name)


                }
            }

        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if(requestCode == 101 && resultCode == Activity.RESULT_OK){
            utils.printIntentKeyValues(intent)
            //Now add members
            val selectedUsers = data?.getParcelableArrayListExtra<Models.Contact>(utils.constants.KEY_SELECTED)
                    as java.util.ArrayList<Models.Contact>?

            val progressDialog = ProgressDialog.show(this, "", "Please wait...", true, false)

            selectedUsers?.forEachWithIndex {index, it ->
                val groupMember = Models.GroupMember(
                    it.uid,
                    FirebaseUtils.getUid(), FirebaseUtils.getPhoneNumber(),
                    it.number, false, false, System.currentTimeMillis()
                )

                FirebaseUtils.ref.groupMember(targetUID, it.uid)
                    .setValue(groupMember)

                // add create event in message node
                FirebaseUtils.createdGroupEvent(it.uid, targetUID, it.number)

                // add member event in message node
                FirebaseUtils.addedMemberEvent(it.uid, targetUID, it.number)

                FirebaseUtils.ref.lastMessage(it.uid)
                    .child(targetUID)
                    .setValue(Models.LastMessageDetail(type = FirebaseUtils.KEY_CONVERSATION_GROUP,
                        nameOrNumber = name))
                    .addOnSuccessListener { if(index == selectedUsers.lastIndex) {
                        progressDialog.dismiss()
                        this.toast("New member added")
                    }}

                //add event to other members
                groupMembers.forEach {member ->
                    // add member event in message node

                    if(member.uid != it.uid) {

                        FirebaseUtils.addedMemberEvent(member.uid, targetUID, it.number)

                        FirebaseUtils.ref.lastMessage(member.uid)
                            .child(targetUID)
                            .setValue(
                                Models.LastMessageDetail(
                                    type = FirebaseUtils.KEY_CONVERSATION_GROUP,
                                    nameOrNumber = name
                                )
                            )
                    }

                }

            }




            }

        else if(requestCode == 111 && resultCode == Activity.RESULT_OK){
            supportActionBar?.title = utils.getNameFromNumber(this, name)
        }

        else  if(resultCode == Activity.RESULT_OK && requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){

            utils.printIntentKeyValues(data!!)

            val result = CropImage.getActivityResult(data)
            val filePath = result.uri.path

            Luban.compress(this, File(filePath))
                .putGear(Luban.THIRD_GEAR)
                .launch(object : OnCompressListener {
                    override fun onStart() {
                    }

                    override fun onSuccess(file: File?) {
                        uploadGroupProfilePic(targetUID, file!!)
                    }

                    override fun onError(e: Throwable?) {
                        uploadGroupProfilePic(targetUID, File(filePath))
                    }

                })

        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    class imageHolder(itemView:View): androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView){
        val imageView = itemView.iv_thumbnail_image

    }

    class videoHolder(itemView:View): androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView){
        val imageView = itemView.iv_thumbnail_video
        val length = itemView.txt_duration

    }



    val context = this@UserProfileActivity


    //for uploading group profile pic
    private fun uploadGroupProfilePic(groupID: String , imageFile:File){


        if(!isGroup)
            return

        val dialog = ProgressDialog(context)
        dialog.setMessage("Wait a moment...")
        dialog.setCancelable(false)
        dialog.show()

        val storageRef = FirebaseUtils.ref.profilePicStorageRef(groupID)

        val uploadTask = storageRef.putFile(utils.getUriFromFile(context, imageFile))

        Log.d("UserProfileActivity", "uploadGroupProfilePic: path = ${imageFile.path}")

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
                    updateProfileUrl(groupID, link.toString())
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


    private fun updateProfileUrl(groupID: String, url:String){
        FirebaseUtils.ref.groupInfo(groupID)
            .child(FirebaseUtils.KEY_PROFILE_PIC_URL)
            .setValue(url)
            .addOnSuccessListener {
                if(url.isNotEmpty()) toast("Profile pic updated")
                else toast("Picture removed")
            }
    }


    private fun showGroupNameEditDialog(){

      LovelyTextInputDialog(this)
      .setTopColorRes(R.color.colorAccent)
          .setTopTitleColor(Color.WHITE)
          .setTopTitle("New Group name")
      .setTitle("Edit the group name")
      .setInitialInput(name)
          .setInputFilter("Invalid Group name") {
              return@setInputFilter it.isNotBlank() && it.length > 3
          }
          .setConfirmButton("Confirm") {

              val newName = it

          FirebaseUtils.ref.groupInfo(targetUID)
              .child(FirebaseUtils.KEY_NAME)
              .setValue(newName)
              .addOnSuccessListener {

                  groupMembers.forEach { member -> FirebaseUtils.ref.lastMessage(member.uid)
                      .child(targetUID).child(FirebaseUtils.KEY_NAME_OR_NUMBER).setValue(newName) }

                  startActivity(Intent(context, HomeActivity::class.java)
                      .apply {
                          flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                      })
                  finish()
                  toast("Group name has been changed")
              }



      }

          .setNegativeButton("No" ){}
      .show()

    }
}
