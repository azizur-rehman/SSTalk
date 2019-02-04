package com.aziz.sstalk

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.aziz.sstalk.utils.FirebaseUtils
import com.aziz.sstalk.utils.utils
import com.mvc.imagepicker.ImagePicker
import kotlinx.android.synthetic.main.activity_edit_profile.*
import kotlinx.android.synthetic.main.layout_profile_image_picker.*
import me.shaohui.advancedluban.Luban
import me.shaohui.advancedluban.OnCompressListener
import java.io.File

class EditProfile : AppCompatActivity() {

    var myUID = FirebaseUtils.getUid()
    val context = this
    var isProfileChanged = false
    lateinit var bitmap:Bitmap
    lateinit var imageFile:File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        myUID = utils.constants.debugUserID

        FirebaseUtils.loadProfilePic(this, myUID, profile_circleimageview)

        profile_pick_btn.setOnClickListener { ImagePicker.pickImage(context) }

        updateProfileBtn.setOnClickListener {
            if(isProfileChanged) {

                val storageRef = FirebaseUtils.ref.getProfilePicStorageRef(myUID)

                val dbRef = FirebaseUtils.ref.getUserRef(myUID)
                    .child(FirebaseUtils.KEY_PROFILE_PIC_URL)

                FirebaseUtils.uploadProfilePic(context, imageFile, storageRef, dbRef, "Profile updated")

               // uploadImage(imageFile)
                isProfileChanged = false
            }

            FirebaseUtils.ref.getUserRef(myUID)
                .child(FirebaseUtils.KEY_NAME)
                .setValue(profile_name.text.toString())

        }

    }




    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        when(resultCode){
            Activity.RESULT_OK -> {

                 val filePath = ImagePicker.getImagePathFromResult(context, requestCode, resultCode, data)

                Luban.compress(context, File(filePath))
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
                            utils.toast(context, e!!.message.toString())
                        }

                    })


            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

}
