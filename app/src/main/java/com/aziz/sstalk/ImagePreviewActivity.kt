package com.aziz.sstalk

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.MenuItem
import com.aziz.sstalk.utils.utils
import kotlinx.android.synthetic.main.activity_image_preview.*

class ImagePreviewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_preview)

        setSupportActionBar(toolbar)

        val imgBytes = intent.getByteArrayExtra(utils.constants.KEY_IMG_PATH)

        preview.setImageBitmap(BitmapFactory.decodeByteArray(imgBytes,0,imgBytes.size))

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
