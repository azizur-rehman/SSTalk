package com.aziz.sstalk.adapters

import android.net.Uri
import android.support.v4.content.FileProvider
import android.support.v4.view.PagerAdapter
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import com.aziz.sstalk.R
import com.aziz.sstalk.utils.utils
import com.squareup.picasso.Picasso
import com.vincent.filepicker.filter.entity.ImageFile
import kotlinx.android.synthetic.main.layout_upload_image_preview.view.*
import java.io.File
import java.util.ArrayList


class ViewPagerImageAdapter(
    private val inflater: LayoutInflater,
    private val imagePaths: ArrayList<String>,
    private val shouldShowEditText: Boolean
) : PagerAdapter() {

    var captions:MutableList<String>  = ArrayList()

    init {
        for(index in imagePaths.indices){
            captions.add(index,"")
        }
    }



    override fun instantiateItem(container: ViewGroup, position: Int): Any {

        val itemView = inflater.inflate(R.layout.layout_upload_image_preview, container, false)

        val imgView = itemView.image_preview

        if(shouldShowEditText){
            itemView.captionEditText.addTextChangedListener(object :TextWatcher{
                override fun afterTextChanged(s: Editable?) {
                    captions.add(position, s.toString())
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                }

            })



        }
        else{
            itemView.upload_caption_edittext_layout.visibility = View.GONE
        }


        imgView.setImageURI(FileProvider.getUriForFile(container.context,utils.constants.URI_AUTHORITY,File(imagePaths[position])))

//        Picasso.get()
//            .load(File(imagePaths[position].path))
//            .placeholder(R.drawable.placeholder_image)
//            .error(R.drawable.error_placeholder)
//            .into(imgView)

        container.addView(itemView)

        return itemView
    }


    override fun isViewFromObject(p0: View, p1: Any): Boolean = p0 === p1 as LinearLayout

    override fun getCount(): Int = imagePaths.size

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View?)
    }


    fun getImageCaptions():MutableList<String> = captions



}