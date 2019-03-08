package com.aziz.sstalk

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.aziz.sstalk.models.Models
import com.aziz.sstalk.utils.FirebaseUtils
import com.aziz.sstalk.utils.utils
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.UploadTask
import com.mvc.imagepicker.ImagePicker
import kotlinx.android.synthetic.main.activity_create_group.*
import kotlinx.android.synthetic.main.item_grid_contact_layout.view.*
import kotlinx.android.synthetic.main.layout_profile_image_picker.*
import me.shaohui.advancedluban.Luban
import me.shaohui.advancedluban.OnCompressListener
import org.jetbrains.anko.toast
import java.io.File

class CreateGroupActivity : AppCompatActivity() {

    var participantList:MutableList<Models.Contact> = ArrayList()

    var isProfileChanged = false
    lateinit var bitmap: Bitmap
    lateinit var imageFile:File
    val context = this@CreateGroupActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_group)


        add_participant_btn.setOnClickListener {
            startActivityForResult(Intent(this, MultiContactChooserActivity::class.java),101)
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        profile_pick_btn.setOnClickListener { ImagePicker.pickImage(this, 123) }


    }


    private fun setGridAdapter(selectedUsers:ArrayList<Models.Contact>?) {

        participant_recyclerview.layoutManager = GridLayoutManager(this, 4)
        participant_recyclerview.setHasFixedSize(true)

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
            }

        }

        participant_recyclerview.adapter = horizontalAdapter
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if(resultCode == Activity.RESULT_OK && requestCode == 123){

            val filePath = ImagePicker.getImagePathFromResult(this, requestCode, resultCode, data)

            imageFile = File(filePath)

            Luban.compress(this, File(filePath))
                .putGear(Luban.THIRD_GEAR)
                .launch(object : OnCompressListener {
                    override fun onStart() {
                    }

                    override fun onSuccess(file: File?) {

                        imageFile = file!!
                        bitmap = BitmapFactory.decodeFile(file.path)
                        profile_circleimageview.setImageBitmap(bitmap)
                        isProfileChanged = true

                    }

                    override fun onError(e: Throwable?) {
                        bitmap = BitmapFactory.decodeFile(filePath)
                        profile_circleimageview.setImageBitmap(bitmap)
                        isProfileChanged = true

                    }

                })

        }
        else if(resultCode == Activity.RESULT_OK){
            val selectedUsers = data?.getParcelableArrayListExtra<Models.Contact>(utils.constants.KEY_SELECTED)
                    as java.util.ArrayList<Models.Contact>
            participantList = selectedUsers
            setGridAdapter(selectedUsers)
        }

        super.onActivityResult(requestCode, resultCode, data)
    }


    class ParticipantHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val name = itemView.grid_name!!
        val pic = itemView.grid_pic!!
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_tick, menu)
        return super.onCreateOptionsMenu(menu)
    }


    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        if(item!!.itemId == android.R.id.home){
            finish()
        }
        else{
            //create group

            if(group_name_edittext.text.isEmpty()){
                group_name_edittext.error = "Group name cannot be empty"
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


        val groupInfo = Models.Group(group_name_edittext.text.toString(),
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
                        it.number,false, System.currentTimeMillis())

                    FirebaseUtils.ref.groupMember(groupID, it.uid)
                        .setValue(groupMember)
                    FirebaseUtils.ref.lastMessage(it.uid)
                        .child(groupID)
                        .setValue(Models.LastMessageDetail(type = FirebaseUtils.KEY_CONVERSATION_GROUP))
                }


                //add myself
                val groupMember = Models.GroupMember(FirebaseUtils.getUid(),
                    FirebaseUtils.getUid(), FirebaseUtils.getPhoneNumber(),
                    FirebaseUtils.getPhoneNumber()
                    ,true, System.currentTimeMillis())

                FirebaseUtils.ref.groupMember(groupID, FirebaseUtils.getUid())
                    .setValue(groupMember)

                FirebaseUtils.ref.lastMessage(FirebaseUtils.getUid())
                    .child(groupID)
                    .setValue(Models.LastMessageDetail(type = FirebaseUtils.KEY_CONVERSATION_GROUP))
                    .addOnSuccessListener {
                        finish()
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

                    FirebaseUtils.ref.groupInfo(groupID)
                        .child(FirebaseUtils.KEY_PROFILE_PIC_URL)
                        .setValue(link)

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

}
