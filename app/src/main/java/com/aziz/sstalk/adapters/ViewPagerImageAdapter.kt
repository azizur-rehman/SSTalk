package com.aziz.sstalk.adapters

import android.content.Intent
import android.media.ThumbnailUtils
import android.provider.MediaStore
import androidx.viewpager.widget.PagerAdapter
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.aziz.sstalk.R
import com.aziz.sstalk.utils.utils
import kotlinx.android.synthetic.main.layout_upload_image_preview.view.*
import java.io.File
import java.lang.Exception
import java.util.ArrayList


class ViewPagerImageAdapter(
    private val inflater: LayoutInflater,
    private val paths: ArrayList<String>,
    private val type: String
) : PagerAdapter() {

    var captions:MutableList<String>  = ArrayList()

    init {
        for(index in paths.indices){
            captions.add(index,"")
        }
    }



    override fun instantiateItem(container: ViewGroup, position: Int): Any {

        val itemView = inflater.inflate(R.layout.layout_upload_image_preview, container, false)

        val imgView = itemView.image_preview

        if(type == utils.constants.FILE_TYPE_IMAGE) {

            imgView.setImageURI(utils.getUriFromFile(container.context, File(paths[position])))
        }
        else if(type == utils.constants.FILE_TYPE_VIDEO){
            itemView.image_preview.visibility = View.GONE

            val thumb = ThumbnailUtils.createVideoThumbnail((paths[position]), MediaStore.Video.Thumbnails.MINI_KIND)
            itemView.video_preview.setImageBitmap(thumb)


            itemView.video_preview.setOnClickListener {
                try {
                    val videoIntent = Intent(Intent.ACTION_VIEW)
                    val uri =utils.getUriFromFile(container.context,  File(paths[position]))
                    videoIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    videoIntent.setDataAndType(uri, "video/*")
                    container.context.startActivity(videoIntent)
                }
                catch (e: Exception){
                    utils.toast(container.context, e.message.toString())
                }
            }
        }



        itemView.captionEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                captions.add(position, s.toString().trim())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

        })

        container.addView(itemView)

        return itemView
    }


    override fun isViewFromObject(p0: View, p1: Any): Boolean = p0 === p1 as LinearLayout

    override fun getCount(): Int = paths.size

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View?)
    }


    fun getImageCaptions():MutableList<String> = captions



}