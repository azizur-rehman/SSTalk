package com.aziz.sstalk.views

import android.support.v7.widget.RecyclerView
import android.view.View
import kotlinx.android.synthetic.main.bubble_image_left.view.*
import kotlinx.android.synthetic.main.bubble_image_right.view.*
import kotlinx.android.synthetic.main.bubble_left.view.*
import kotlinx.android.synthetic.main.bubble_map_left.view.*
import kotlinx.android.synthetic.main.bubble_map_right.view.*
import kotlinx.android.synthetic.main.bubble_right.view.*
import kotlinx.android.synthetic.main.bubble_video_left.view.*
import kotlinx.android.synthetic.main.bubble_video_right.view.*

object holders {

    class TargetTextMsgHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val message = itemView.messageText_left!!
        val time = itemView.time_left!!
        val headerDateTime = itemView.header_left!!
        // val imageLayout = itemView.imageFrameLayout!!
    }
    class MyTextMsgHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val message = itemView.messageText_right!!
        val time = itemView.time_right!!
        val messageStatus = itemView.delivery_status!!
        val headerDateTime = itemView.header_right!!
    }


    class MyImageMsgHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val message = itemView.messageText_image_right!!
        val time = itemView.time_image_right!!
        val imageView = itemView.imageview_image_right!!
        val progressBar = itemView.progress_bar_image_right!!
        val tapToRetry = itemView.tap_retry_image_right!!
        val messageStatus = itemView.delivery_image_status!!
        val headerDateTime = itemView.header_image_right!!


    }

    class TargetImageMsgHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val message = itemView.messageText_image_left!!
        val time = itemView.time_image_left!!
        val imageView = itemView.imageview_image_left!!
        val headerDateTime = itemView.header_image_left!!
        // val imageLayout = itemView.imageFrameLayout!!
    }



    class MyVideoMsgHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val message = itemView.messageText_video_right!!
        val time = itemView.time_video_right!!
        val centerImageView = itemView.imageview_video_right!!
        val progressBar = itemView.progress_bar_video_right!!
        val tapToRetry = itemView.tap_retry_video_right!!
        val messageStatus = itemView.delivery_video_status!!
        val headerDateTime = itemView.header_video_right!!
        val videoLayout = itemView.videoFrameLayoutRight!!
        val thumbnail = itemView.thumbnail_right!!
        val videoLengthText = itemView.video_length_right!!

        val tap_to_download = itemView.tap_retry_download_video_right!!


    }

    class TargetVideoMsgHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val message = itemView.messageText_video_left!!
        val time = itemView.time_video_left!!
        val centerImageView = itemView.imageview_video_left!!
        val headerDateTime = itemView.header_video_left!!
        val videoLayout = itemView.videoFrameLayoutLeft!!
        val thumbnail = itemView.thumbnail_left!!
        val progressBar = itemView.progress_bar_video_left!!
        val videoLengthText = itemView.video_length_left!!
        val tap_to_download = itemView.tap_retry_download_video_left!!

    }




    class TargetMapHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val message = itemView.messageText_map_left!!
        val mapView = itemView.mapview_left!!
        val dateHeader = itemView.mapview_left!!
    }

    class MyMapHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val message = itemView.messageText_map_right!!
        val mapView = itemView.mapview_right!!
        val dateHeader = itemView.header_map_right!!



    }

}