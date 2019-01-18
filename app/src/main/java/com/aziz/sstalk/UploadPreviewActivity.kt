package com.aziz.sstalk

import android.app.Activity
import android.app.ProgressDialog
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.view.ViewPager
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import com.aziz.sstalk.adapters.ViewPagerImageAdapter
import com.aziz.sstalk.utils.utils
import com.vincent.filepicker.filter.entity.ImageFile
import kotlinx.android.synthetic.main.activity_upload_preview.*
import me.shaohui.advancedluban.Luban
import me.shaohui.advancedluban.OnCompressListener
import java.io.File

class UploadPreviewActivity : AppCompatActivity() {

    val compressedImagePath:MutableList<String> = ArrayList()
    var compressedImageCaptions:MutableList<String> = ArrayList()
    var lastText:String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload_preview)

        setSupportActionBar(toolbar)
        if(supportActionBar!=null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setHomeButtonEnabled(true)
        }

        val imgPaths = intent.getParcelableArrayListExtra<ImageFile>(utils.constants.KEY_IMG_PATH)



        if(imgPaths.isEmpty()){
            setResult(Activity.RESULT_CANCELED)
            finish()
            utils.longToast(this, "Failed to load image")
        }


        for(item in imgPaths){
            compressedImageCaptions.add("")
        }


        val adapter = ViewPagerImageAdapter(layoutInflater, imgPaths, true)
        viewPager.adapter = adapter





        //preview.setImageBitmap(BitmapFactory.decodeFile(imgPath.toString()))

        sendBtn.setOnClickListener {

            if(imgPaths.isEmpty()){
                setResult(Activity.RESULT_CANCELED, intent)
                finish()
            }
            else {

                compressedImageCaptions = adapter.getImageCaptions()


                var successCount = 0
                val dialog = ProgressDialog.show(this@UploadPreviewActivity, "","Please wait...", true, false)



                for((index, file) in imgPaths.withIndex()){
                    Luban.compress(this@UploadPreviewActivity, File(file.path))
                        .putGear(Luban.THIRD_GEAR)
                        .clearCache()
                        .launch(object : OnCompressListener {
                            override fun onError(e: Throwable?) {
                                dialog.dismiss()
                                Log.d("UploadPreviewActivity", "onError: "+e!!.message)
                                utils.toast(this@UploadPreviewActivity, e.message.toString())
                            }

                            override fun onStart() {
                                Log.d("UploadPreviewActivity", "onStart: ")
                                dialog.show()
                            }

                            override fun onSuccess(file: File?) {



                                dialog.dismiss()
                                compressedImagePath.add(file!!.path)

                                Log.d("UploadPreviewActivity", "onSuccess: success = $successCount, total paths = "+imgPaths.size)


                                if(successCount == imgPaths.size - 1){
                                    setResult(
                                        Activity.RESULT_OK,
                                        intent.putStringArrayListExtra(
                                            utils.constants.KEY_CAPTION,
                                            compressedImageCaptions as java.util.ArrayList<String>?)
                                            .putStringArrayListExtra(utils.constants.KEY_IMG_PATH,
                                                compressedImagePath as java.util.ArrayList<String>?
                                            ))
                                    finish()
                                }

                                successCount++

                            }
                        }
                     )
                }



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
