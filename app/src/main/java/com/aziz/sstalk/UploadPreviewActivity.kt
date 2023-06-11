package com.aziz.sstalk

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import com.aziz.sstalk.adapters.ViewPagerImageAdapter
import com.aziz.sstalk.databinding.ActivityUploadPreviewBinding
import com.aziz.sstalk.utils.utils
import com.vincent.filepicker.filter.entity.ImageFile
import com.vincent.filepicker.filter.entity.VideoFile

class UploadPreviewActivity : AppCompatActivity() {

    val imagePaths:MutableList<String> = ArrayList()
    var imageCaptions:MutableList<String> = ArrayList()

    lateinit var binding:ActivityUploadPreviewBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        if(supportActionBar!=null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setHomeButtonEnabled(true)
        }

        val isForSingleFile = intent.getBooleanExtra(utils.constants.IS_FOR_SINGLE_FILE, false)


        if(isForSingleFile){
            val cameraImagePath = intent.getStringExtra(utils.constants.KEY_IMG_PATH)
            if (cameraImagePath != null) {
                imagePaths.add(cameraImagePath)
            }
        }
        else {


            if (intent.getStringExtra(utils.constants.KEY_FILE_TYPE) == utils.constants.FILE_TYPE_VIDEO) {
                val videoPaths = intent.getParcelableArrayListExtra<VideoFile>(utils.constants.KEY_IMG_PATH)

                if (videoPaths != null) {
                    for (item in videoPaths) {
                        imageCaptions.add("")
                        imagePaths.add(item.path.toString())
                    }
                }
            } else {
                val imgFilePaths = intent.getParcelableArrayListExtra<ImageFile>(utils.constants.KEY_IMG_PATH)

                if (imgFilePaths != null) {
                    for (item in imgFilePaths) {
                        imageCaptions.add("")
                        imagePaths.add(item.path.toString())
                    }
                }
            }
        }

        val fileTypes = ArrayList<String>()
        for (i in 0 until imagePaths.size)
        {
            intent.getStringExtra(utils.constants.KEY_FILE_TYPE)?.let { fileTypes.add(it) }
        }

        if (imagePaths.isEmpty()) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            utils.longToast(this, "Failed to load image")
        }

        val adapter = ViewPagerImageAdapter(layoutInflater, imagePaths as java.util.ArrayList<String>, fileTypes)
        binding.viewPager.adapter = adapter





        //preview.setImageBitmap(BitmapFactory.decodeFile(imgPath.toString()))

        binding.sendBtn.setOnClickListener {

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
                                            )
                            .putExtra(utils.constants.KEY_FILE_TYPE, intent.getStringExtra(utils.constants.KEY_FILE_TYPE))
                                        )

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
