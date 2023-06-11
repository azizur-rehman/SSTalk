package com.aziz.sstalk

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.aziz.sstalk.databinding.ActivityCreateGroupBinding
import com.aziz.sstalk.models.Models
import com.aziz.sstalk.utils.FirebaseUtils
import com.aziz.sstalk.utils.utils
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.storage.UploadTask
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import de.hdodenhof.circleimageview.CircleImageView
import me.shaohui.advancedluban.Luban
import me.shaohui.advancedluban.OnCompressListener
import org.jetbrains.anko.toast
import java.io.File

class CreateGroupActivity : AppCompatActivity() {

    var participantList:MutableList<Models.Contact> = ArrayList()

    var isProfileChanged = false
    lateinit var bitmap: Bitmap
    var profileURL = ""
    lateinit var imageFile:File
    val context = this@CreateGroupActivity

    lateinit var binding: ActivityCreateGroupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateGroupBinding.inflate(layoutInflater).apply {  setContentView(root) }

        title = "Create new Group"

        binding.includeImagePicker.profileCircleimageview.setImageResource(R.drawable.ic_group_white_24dp)
        binding.includeImagePicker.profileCircleimageview.circleBackgroundColor = ContextCompat.getColor(this, R.color.colorPrimary)

        binding.addParticipantBtn.setOnClickListener {

            val excludedUIDs:MutableList<String> = ArrayList()
             participantList.forEach { excludedUIDs.add(it.uid) }
                startActivityForResult(Intent(this, MultiContactChooserActivity::class.java).apply {
                putStringArrayListExtra(utils.constants.KEY_EXCLUDED_LIST, excludedUIDs as java.util.ArrayList<String>?)
            },101)
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.includeImagePicker.profilePickBtn.setOnClickListener {

            CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setCropShape(CropImageView.CropShape.RECTANGLE)
                .setAspectRatio(1,1)
                .start(this)
//            ImagePicker.pickImage(this, 123)

        }


    }


    private fun setGridAdapter(selectedUsers:ArrayList<Models.Contact>?) {

        binding.participantRecyclerview.layoutManager = androidx.recyclerview.widget.GridLayoutManager(this, 4)
        binding.participantRecyclerview.setHasFixedSize(true)

        val horizontalAdapter = object : RecyclerView.Adapter<ParticipantHolder>() {
            override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ParticipantHolder {
                return ParticipantHolder(
                    layoutInflater.inflate(
                        R.layout.item_grid_contact_layout,
                        p0,
                        false
                    )
                )
            }

            override fun getItemCount(): Int = selectedUsers!!.size

            override fun onBindViewHolder(p0: ParticipantHolder, p1: Int) {


                p0.name.text = utils.getNameFromNumber(
                    this@CreateGroupActivity,
                    selectedUsers?.get(p1)!!.number
                )

                FirebaseUtils.loadProfileThumbnail(
                    this@CreateGroupActivity, selectedUsers[p1].uid,
                    p0.pic
                )

                p0.cancelBtn.setOnClickListener {
                    selectedUsers.removeAt(p0.adapterPosition)
                    notifyItemRemoved(p0.adapterPosition)
                }
            }

        }

        binding.participantRecyclerview.adapter = horizontalAdapter
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if(resultCode == Activity.RESULT_OK && requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){

            utils.printIntentKeyValues(data!!)

            val result = CropImage.getActivityResult(data)
            val filePath = result.uri.path

            Log.d("CreateGroup", "onActivityResult: path = $filePath")
            imageFile = File(filePath)

            Luban.compress(this, File(filePath))
                .putGear(Luban.THIRD_GEAR)
                .launch(object : OnCompressListener {
                    override fun onStart() {
                    }

                    override fun onSuccess(file: File?) {

                        imageFile = file!!
                        bitmap = BitmapFactory.decodeFile(file.path)
                        binding.includeImagePicker.profileCircleimageview.setImageBitmap(bitmap)
                        isProfileChanged = true

                    }

                    override fun onError(e: Throwable?) {
                        bitmap = BitmapFactory.decodeFile(filePath)
                        binding.includeImagePicker.profileCircleimageview.setImageBitmap(bitmap)
                        isProfileChanged = true

                    }

                })

        }
        else if(resultCode == Activity.RESULT_OK){
            // for receiving participant list
            val selectedUsers = data?.getParcelableArrayListExtra<Models.Contact>(utils.constants.KEY_SELECTED)
                    as java.util.ArrayList<Models.Contact>

            if(participantList.isEmpty()){
                participantList = selectedUsers
            }
            else{
                participantList.addAll(selectedUsers)
            }

            Log.d("CreateGroupActivity", "onActivityResult: $participantList")


            setGridAdapter(participantList as ArrayList<Models.Contact>)
        }

        super.onActivityResult(requestCode, resultCode, data)
    }


    class ParticipantHolder(itemView: View): RecyclerView.ViewHolder(itemView){

        val name = itemView.findViewById<TextView>(R.id.grid_name)
        val pic = itemView.findViewById<CircleImageView>(R.id.grid_pic)
        val cancelBtn = itemView.findViewById<View>(R.id.grid_cancel_btn)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_tick, menu)
        return super.onCreateOptionsMenu(menu)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if(item.itemId == android.R.id.home){
            finish()
        }
        else{
            //create group

            if(binding.groupNameEdittext.text.isEmpty()){
                binding.groupNameEdittext.error = "Group name cannot be empty"
                return false
            }

            if(binding.groupNameEdittext.text.length <=3){
                binding.groupNameEdittext.error = "Too short for a group name"
                return false
            }


            val groupID = "GRP${System.currentTimeMillis()}"
            if(isProfileChanged)
                uploadGroupProfilePicAndCreateGroup(groupID)
            else
                createGroup(groupID)
        }


        return super.onOptionsItemSelected(item)
    }




    private fun createGroup(groupID: String){


        val groupName = binding.groupNameEdittext.text.toString().trim()

        val groupInfo = Models.Group(groupName,
            createdBy = FirebaseUtils.getUid(),
            groupID = groupID)




        FirebaseUtils.ref.groupInfo(groupID)
            .setValue(groupInfo)
            .addOnSuccessListener {
                context.toast("Group created successfully")

                //Now add members
                participantList.forEach {
                    val groupMember = Models.GroupMember(it.uid,
                        FirebaseUtils.getUid(), FirebaseUtils.getPhoneNumber(),
                        it.number,false, false,System.currentTimeMillis())

                    FirebaseUtils.ref.groupMember(groupID, it.uid)
                        .setValue(groupMember)

                    // add create event in message node
                    FirebaseUtils.createdGroupEvent(it.uid, groupID, it.number)

                    // add member event in message node
                    FirebaseUtils.addedMemberEvent(it.uid, groupID, it.number)

                    FirebaseUtils.ref.lastMessage(it.uid)
                        .child(groupID)
                        .setValue(Models.LastMessageDetail(type = FirebaseUtils.KEY_CONVERSATION_GROUP,
                            nameOrNumber = groupName))
                }


                //add myself
                val groupMember = Models.GroupMember(FirebaseUtils.getUid(),
                    FirebaseUtils.getUid(), FirebaseUtils.getPhoneNumber(),
                    FirebaseUtils.getPhoneNumber()
                    ,true,false,  System.currentTimeMillis())

                //add in member
                FirebaseUtils.ref.groupMember(groupID, FirebaseUtils.getUid())
                    .setValue(groupMember)

                // add create event in message node
                FirebaseUtils.createdGroupEvent(FirebaseUtils.getUid(), groupID, FirebaseUtils.getPhoneNumber())

                participantList.forEach {
                    // add member event in message node
                    FirebaseUtils.addedMemberEvent(FirebaseUtils.getUid(), groupID, it.number)
                }

                //update last message
                FirebaseUtils.ref.lastMessage(FirebaseUtils.getUid())
                    .child(groupID)
                    .setValue(Models.LastMessageDetail(type = FirebaseUtils.KEY_CONVERSATION_GROUP,
                        nameOrNumber = groupName))
                    .addOnSuccessListener {

                        if(profileURL.isEmpty())
                            finish()
                        else
                            updateProfileUrl(groupID, profileURL)
                    }



            }





    }


    private fun uploadGroupProfilePicAndCreateGroup(groupID: String){


        val dialog = ProgressDialog(context)
        dialog.setMessage("Wait a moment...")
        dialog.setCancelable(false)
        dialog.show()

        val storageRef = FirebaseUtils.ref.profilePicStorageRef(groupID)

        val uploadTask = storageRef.putFile(utils.getUriFromFile(context, imageFile))

        Log.d("CreateGroupActivity", "uploadGroupProfilePicAndCreateGroup: path = ${imageFile.path}")

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

                    profileURL = link?.toString()!!

                    Log.d("CreateGroupActivity", "uploadGroupProfilePicAndCreateGroup: profile updated ")

                    createGroup(groupID)

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
                finish()
            }
    }

}
