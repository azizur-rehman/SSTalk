package com.aziz.sstalk.fragments

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aziz.sstalk.HomeActivity
import com.aziz.sstalk.R
import com.aziz.sstalk.utils.FirebaseUtils
import com.aziz.sstalk.utils.utils
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import kotlinx.android.synthetic.main.content_my_profile.*
import kotlinx.android.synthetic.main.layout_profile_image_picker.*
import me.shaohui.advancedluban.Luban
import me.shaohui.advancedluban.OnCompressListener
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.selector
import org.jetbrains.anko.toast
import java.io.File
import java.lang.Exception

class FragmentMyProfile : Fragment() {


    var myUID = FirebaseUtils.getUid()
    var isProfileChanged = false
    lateinit var bitmap: Bitmap
    lateinit var imageFile:File

    var isOnAccountCreated = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.content_my_profile, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        isOnAccountCreated = arguments?.getBoolean(utils.constants.KEY_IS_ON_ACCOUNT_CREATION, false)?:false

        with(view) {

            myUID = FirebaseUtils.getUid()

            FirebaseUtils.loadProfilePic(context, myUID, profile_circleimageview)

            profile_pick_btn.setOnClickListener {

                context?.selector(
                    "Edit profile picture",
                    listOf("Change picture", "Remove picture")
                ) { _, pos ->

                    if (pos == 0) {
                        CropImage.activity()
                            .setGuidelines(CropImageView.Guidelines.ON)
                            .setCropShape(CropImageView.CropShape.RECTANGLE)
                            .setAspectRatio(1, 1)
                            .start(context, this@FragmentMyProfile)
                    } else {
                        //delete pic
                        FirebaseUtils.ref.user(myUID)
                            .child(FirebaseUtils.KEY_PROFILE_PIC_URL).setValue("")
                            .addOnSuccessListener { view.snackbar("Profile pic removed") }
                    }

                }


            }

            if (FirebaseUtils.isLoggedIn()) {
                FirebaseUtils.ref.user(myUID)
                    .child("name").addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onCancelled(p0: DatabaseError) {}

                        override fun onDataChange(p0: DataSnapshot) {
                            if (p0.exists()) {
                                val name = p0.value.toString()
                                profile_name.setText(name)
                            }
                        }
                    })
            }

            updateProfileBtn.setOnClickListener {

                if (profile_name.text.isEmpty()) {
                    profile_name.error = "Cannot be empty"
                    return@setOnClickListener
                }


                if (isProfileChanged) {

                    val storageRef = FirebaseUtils.ref.profilePicStorageRef(myUID)

                    val dbRef = FirebaseUtils.ref.user(myUID)
                        .child(FirebaseUtils.KEY_PROFILE_PIC_URL)

                    uploadProfilePic(context, imageFile, storageRef, dbRef, "Profile updated")

                    // uploadImage(imageFile)
                    isProfileChanged = false
                }





                FirebaseUtils.ref.user(myUID)
                    .child(FirebaseUtils.KEY_NAME)
                    .setValue(profile_name.text.toString())

                if (profile_name.text.isNotEmpty()) {
                    if (FirebaseUtils.isLoggedIn()) {
                        FirebaseAuth.getInstance().currentUser!!.updateProfile(
                            UserProfileChangeRequest.Builder()
                                .setDisplayName(profile_name.text.toString().trim()).build()
                        )
                    }
                }


                if (isOnAccountCreated
                    and !isProfileChanged
                ) {
                    startActivity(Intent(context, HomeActivity::class.java))
                    activity?.finish()
                }


            }

            //load profile name
            FirebaseUtils.ref.user(FirebaseUtils.getUid())
                .child(FirebaseUtils.KEY_NAME)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onCancelled(p0: DatabaseError) {
                    }

                    override fun onDataChange(p0: DataSnapshot) {
                        profile_name.setText(p0.value.toString().trim())
                    }
                })
        }

    }


    private fun uploadProfilePic(context: Context, file: File, storageRef: StorageReference,
                                 dbRef: DatabaseReference,
                                 toastAfterUploadIfAny: String){


        val dialog = ProgressDialog(context)
        dialog.setMessage("Wait a moment...")
        dialog.setCancelable(false)
        dialog.show()



        Log.d("EditProfile", "uploadImage: File size = "+file.length()/1024)




        val uploadTask = storageRef.putFile(utils.getUriFromFile(context, file))

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

                try {
                    dialog.dismiss()
                }catch (e: Exception){}
                if(task.isSuccessful) {
                    val link = task.result

                    dbRef.setValue(link.toString())
                        .addOnSuccessListener {
                            //   isProfileChanged = false
                            if(toastAfterUploadIfAny.isNotEmpty())
                                utils.toast(context, toastAfterUploadIfAny) }


                    if(FirebaseUtils.isLoggedIn()) {
                        FirebaseAuth.getInstance().currentUser!!
                            .updateProfile(
                                UserProfileChangeRequest.Builder()
                                    .setPhotoUri(link).build()
                            )
                    }

                    if(isOnAccountCreated){
                        startActivity(Intent(context, HomeActivity::class.java))
                        activity?.finish()
                    }




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


                utils.printIntentKeyValues(data!!)

                val result = CropImage.getActivityResult(data)
                val filePath = result.uri.path

                Log.d("EditProfile", "onActivityResult: $filePath")
                //utils.getRealPathFromURI(context, result.uri)
                //ImagePicker.getImagePathFromResult(context, requestCode, resultCode, data)

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

                            context?.toast( e?.message.toString())
                        }

                    })


            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }




}