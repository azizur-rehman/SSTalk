package com.aziz.sstalk

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.view.View
import com.aziz.sstalk.utils.utils
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import kotlinx.android.synthetic.main.activity_image_preview.*
import java.lang.Exception

class ImagePreviewActivity : AppCompatActivity() {



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_preview)

        val imgURL = intent.getStringExtra(utils.constants.KEY_IMG_PATH)


         val target:Target = object : Target{
            override fun onPrepareLoad(placeHolderDrawable: Drawable?) {

                //utils.toast(this@ImagePreviewActivity, "Preparing to load")
            }

            override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                utils.toast(this@ImagePreviewActivity, "Failed to load image")
                finish()

            }

            override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                preview.setImageBitmap(bitmap)
                //utils.toast(this@ImagePreviewActivity, "Loaded")
                progress_bar.visibility = View.GONE

            }

        }


        Picasso.get()
            .load(imgURL.toString())
            .into(target)

        preview.tag = target





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
