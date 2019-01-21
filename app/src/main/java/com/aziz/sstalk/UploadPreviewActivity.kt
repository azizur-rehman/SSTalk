package com.aziz.sstalk

import android.app.Activity
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import com.aziz.sstalk.adapters.ViewPagerImageAdapter
import com.aziz.sstalk.utils.utils
import com.vincent.filepicker.filter.entity.ImageFile
import kotlinx.android.synthetic.main.activity_upload_preview.*

class UploadPreviewActivity : AppCompatActivity() {

    val imagePaths:MutableList<String> = ArrayList()
    var imageCaptions:MutableList<String> = ArrayList()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload_preview)

        setSupportActionBar(toolbar)
        if(supportActionBar!=null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setHomeButtonEnabled(true)
        }

        val isForSingleFile = intent.getBooleanExtra(utils.constants.IS_FOR_SINGLE_FILE, false)


        if(isForSingleFile){
            val cameraImagePath = intent.getStringExtra(utils.constants.KEY_IMG_PATH)
            imagePaths.add(cameraImagePath)
        }
        else {
            val imgFilePaths = intent.getParcelableArrayListExtra<ImageFile>(utils.constants.KEY_IMG_PATH)


            for (item in imgFilePaths) {
                imageCaptions.add("")
                imagePaths.add(item.path.toString())
            }
        }


        if (imagePaths.isEmpty()) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            utils.longToast(this, "Failed to load image")
        }

        val adapter = ViewPagerImageAdapter(layoutInflater, imagePaths as java.util.ArrayList<String>, true)
        viewPager.adapter = adapter





        //preview.setImageBitmap(BitmapFactory.decodeFile(imgPath.toString()))

        sendBtn.setOnClickListener {

            if(imagePaths.isEmpty()){
                setResult(Activity.RESULT_CANCELED, intent)
                finish()
            }
            else {

                imageCaptions = adapter.getImageCaptions()



               setResult(
                   Activity.RESULT_OK,
                        intent.putStringArrayListExtra(
                             utils.constants.KEY_CAPTION,
                                  imageCaptions as java.util.ArrayList<String>?)
                                       .putStringArrayListExtra(utils.constants.KEY_IMG_PATH,
                                                imagePaths as java.util.ArrayList<String>?
                                            ))
                                    finish()


                }



            }


    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                setResult(Activity.RESULT_CANCELED)
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
