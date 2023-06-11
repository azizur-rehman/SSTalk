package com.aziz.sstalk

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import android.view.View
import com.aziz.sstalk.adapters.ViewPagerImageAdapter
import com.aziz.sstalk.databinding.ActivityImagePreviewBinding
import com.aziz.sstalk.models.Models
import com.aziz.sstalk.utils.FirebaseUtils
import com.aziz.sstalk.utils.utils
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import java.io.File
import java.lang.Exception

class ImagePreviewActivity : AppCompatActivity() {

    lateinit var binding:ActivityImagePreviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImagePreviewBinding.inflate(layoutInflater).apply { setContentView(root) }


        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        window.statusBarColor = Color.BLACK

        title = ""


        var imgURL = intent.getStringExtra(utils.constants.KEY_IMG_PATH)
        var imgLocalPath = intent.getStringExtra(utils.constants.KEY_LOCAL_PATH)

        val messageModel = intent.getSerializableExtra(utils.constants.KEY_MSG_MODEL) as? Models.MessageModel

        messageModel?.let {

            title = if(it.from == FirebaseUtils.getUid())
                "You"
            else
                "Sender"


            binding.toolbar.subtitle = utils.getLocalDate(it.timeInMillis) +" "+utils.getLocalTime(it.timeInMillis)
        }



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
                binding.preview.setImageBitmap(bitmap)
                //utils.toast(this@ImagePreviewActivity, "Loaded")
                binding.progressBar.visibility = View.GONE

            }

        }

        binding.preview.tag = target

        if(File(imgLocalPath).exists()){


            Picasso.get()
                .load(File(imgLocalPath))
                .networkPolicy(NetworkPolicy.OFFLINE)
                .into(target)

        }
        else{

            if(imgURL.isEmpty()){
                utils.toast(this@ImagePreviewActivity, "Failed to load image")
                Log.d("ImagePreviewActivi13ty", "onCreate: path empty")
                finish()
            }
            else
            Picasso.get()
                .load(imgURL.toString())
                .networkPolicy(NetworkPolicy.OFFLINE)
                .into(target)
        }




    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when(item.itemId){
            android.R.id.home ->{
                finish()
            }
        }


        return super.onOptionsItemSelected(item)
    }
}
