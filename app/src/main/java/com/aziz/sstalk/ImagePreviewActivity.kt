package com.aziz.sstalk

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import android.view.View
import com.aziz.sstalk.utils.utils
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import kotlinx.android.synthetic.main.activity_image_preview.*
import java.io.File
import java.lang.Exception

class ImagePreviewActivity : AppCompatActivity() {



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_preview)

        var imgURL = intent.getStringExtra(utils.constants.KEY_IMG_PATH)
        var imgLocalPath = intent.getStringExtra(utils.constants.KEY_LOCAL_PATH)

        if(imgURL == null)
            imgURL = ""

        if(imgLocalPath == null)
            imgLocalPath = ""


        if(imgURL.isEmpty() && imgLocalPath.isEmpty()){
            utils.toast(this@ImagePreviewActivity, "Failed to load image")
            finish()
        }


         val target:Target = object : Target{
            override fun onPrepareLoad(placeHolderDrawable: Drawable?) {

                //utils.toast(this@ImagePreviewActivity, "Preparing to load")
            }

            override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                utils.toast(this@ImagePreviewActivity, "Failed to load : Image might be deleted")
                Log.d("ImagePreviewActivity", "onBitmapFailed: ${e!!.message}")
                finish()

            }

            override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                preview.setImageBitmap(bitmap)
                //utils.toast(this@ImagePreviewActivity, "Loaded")
                progress_bar.visibility = View.GONE

            }

        }

        preview.tag = target

        if(File(imgLocalPath).exists()){


            Picasso.get()
                .load(File(imgLocalPath))
                .networkPolicy(NetworkPolicy.OFFLINE)
                .into(target)

        }
        else{

            if(imgURL.isEmpty()){
                utils.toast(this@ImagePreviewActivity, "Failed to load image")
                Log.d("ImagePreviewActivity", "onCreate: path empty")
                finish()
            }
            else
            Picasso.get()
                .load(imgURL.toString())
                .networkPolicy(NetworkPolicy.OFFLINE)
                .into(target)
        }



        setSupportActionBar(toolbar)
        if(supportActionBar!=null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setHomeButtonEnabled(true)
        }
    }


    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        when(item!!.itemId){
            android.R.id.home ->{
                finish()
            }
        }


        return super.onOptionsItemSelected(item)
    }
}
