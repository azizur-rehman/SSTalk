package com.aziz.sstalk

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import com.aziz.sstalk.fragments.FragmentMyProfile
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
import kotlinx.android.synthetic.main.activity_edit_profile.*
import kotlinx.android.synthetic.main.item_contact_layout.*
import kotlinx.android.synthetic.main.layout_profile_image_picker.*
import me.shaohui.advancedluban.Luban
import me.shaohui.advancedluban.OnCompressListener
import org.jetbrains.anko.selector
import org.jetbrains.anko.toast
import java.io.File
import java.lang.Exception

class EditProfile : AppCompatActivity() {

    var isForAccountCreation = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)


        isForAccountCreation = intent.getBooleanExtra(utils.constants.KEY_IS_ON_ACCOUNT_CREATION, false)

        if(supportActionBar!=null && !isForAccountCreation)
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        title = "My Profile"


        supportFragmentManager.beginTransaction()
            .replace(R.id.container_edit_profile,
                FragmentMyProfile().apply { arguments = Bundle().apply { putBoolean(utils.constants.KEY_IS_ON_ACCOUNT_CREATION, isForAccountCreation) } })
            .commit()

    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if(item!!.itemId == android.R.id.home)
            finish()
        return super.onOptionsItemSelected(item)
    }

}
