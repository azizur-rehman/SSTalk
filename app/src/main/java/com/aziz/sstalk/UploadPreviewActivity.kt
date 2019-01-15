package com.aziz.sstalk

import android.app.Activity
import android.graphics.BitmapFactory
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import com.aziz.sstalk.utils.utils
import kotlinx.android.synthetic.main.activity_upload_preview.*

class UploadPreviewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload_preview)

        setSupportActionBar(toolbar)

        val imgPath = intent.getStringExtra(utils.constants.KEY_IMG_PATH)

        Log.d("UploadPreviewActivity", "onCreate: path = $imgPath")

        if(imgPath.isEmpty()){
            setResult(Activity.RESULT_CANCELED)
            finish()
            utils.longToast(this, "Failed to load image")
        }

        preview.setImageBitmap(BitmapFactory.decodeFile(imgPath.toString()))

        sendBtn.setOnClickListener {
            setResult(Activity.RESULT_OK, intent.putExtra(utils.constants.KEY_CAPTION, captionEditText.text.toString()))
            finish()

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
