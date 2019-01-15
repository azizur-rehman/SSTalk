package com.aziz.sstalk

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import com.aziz.sstalk.utils.utils
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import kotlinx.android.synthetic.main.activity_image_preview.*
import java.lang.Exception

class ImagePreviewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_preview)

        val imgURL = intent.getStringExtra(utils.constants.KEY_IMG_PATH)

        Picasso.get()
            .load(imgURL)
            .into(object: Target{
                override fun onPrepareLoad(placeHolderDrawable: Drawable?) {

                }

                override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                    utils.toast(this@ImagePreviewActivity, e!!.message.toString())
                    finish()
                }

                override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {

                    preview.tag = bitmap

                    preview.setImageBitmap(bitmap)
                    preview.bringToFront()
                    utils.toast(this@ImagePreviewActivity,"Loaded")
                }

            })



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
