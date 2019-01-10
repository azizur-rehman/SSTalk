package com.aziz.sstalk

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.aziz.sstalk.utils.FirebaseUtils
import com.aziz.sstalk.utils.utils
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.mvc.imagepicker.ImagePicker
import kotlinx.android.synthetic.main.activity_edit_profile.*
import kotlinx.android.synthetic.main.layout_profile_image_picker.*

class EditProfile : AppCompatActivity() {

    val myUID = FirebaseUtils.getUid()
    val context = this
    var isProfileChanged = false
    lateinit var bitmap:Bitmap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        profile_pick_btn.setOnClickListener { ImagePicker.pickImage(context) }

        updateProfileBtn.setOnClickListener {
            if(isProfileChanged)
            uploadImage(utils.getByteArrayFromBitmap(bitmap))

            FirebaseUtils.getUserRef(myUID)
                .child(FirebaseUtils.KEY_NAME)
                .setValue(profile_name.text)

        }

    }



    private fun uploadImage(bytes: ByteArray){
        val dialog = ProgressDialog(context)
        dialog.setMessage("Uploading...")
        dialog.setCancelable(false)
        dialog.show()



        val ref =  FirebaseStorage.getInstance()
            .reference.child("profile_pics").child(myUID)

        val uploadTask = ref.putBytes(bytes)

        uploadTask.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    throw it
                }
            }
            return@Continuation ref.downloadUrl
        })
            .addOnCompleteListener { task ->
                dialog.dismiss()
                if(task.isSuccessful) {
                    val link = task.result

                    Log.d("EditProfile", "uploadImage: ")

                    FirebaseUtils.getUserRef(myUID)
                        .child(FirebaseUtils.KEY_PROFILE_PIC_URL)
                        .setValue(link)
                        .addOnSuccessListener { 
                            isProfileChanged = false
                            utils.toast(context, "Profile Pic updated") }




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


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        when(resultCode){
            Activity.RESULT_OK -> {

                 bitmap = ImagePicker.getImageFromResult(context, requestCode, resultCode, data!!)!!
                profile_circleimageview.setImageBitmap(bitmap)
                isProfileChanged = true
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

}
