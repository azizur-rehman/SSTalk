package com.aziz.sstalk.views

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.ColorFilter
import android.os.Build
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import com.aziz.sstalk.R
import kotlinx.android.synthetic.main.bubble_audio_left.view.*
import kotlinx.android.synthetic.main.bubble_audio_right.view.*
import kotlinx.android.synthetic.main.bubble_audio_right.view.time_right
import kotlinx.android.synthetic.main.bubble_image_left.view.*
import kotlinx.android.synthetic.main.bubble_image_right.view.*
import kotlinx.android.synthetic.main.bubble_left.view.*
import kotlinx.android.synthetic.main.bubble_left.view.circle_sender_text
import kotlinx.android.synthetic.main.bubble_left.view.container_left
import kotlinx.android.synthetic.main.bubble_left.view.header_left
import kotlinx.android.synthetic.main.bubble_left.view.messageText_sender_left
import kotlinx.android.synthetic.main.bubble_left.view.time_left
import kotlinx.android.synthetic.main.bubble_map_left.view.*
import kotlinx.android.synthetic.main.bubble_map_right.view.*
import kotlinx.android.synthetic.main.bubble_right.view.*
import kotlinx.android.synthetic.main.bubble_right.view.container_right
import kotlinx.android.synthetic.main.bubble_right.view.header_right
import kotlinx.android.synthetic.main.bubble_video_left.view.*
import kotlinx.android.synthetic.main.bubble_video_right.view.*
import kotlinx.android.synthetic.main.layout_item_audio.view.*
import kotlinx.android.synthetic.main.layout_video_bubble.view.*
import kotlinx.android.synthetic.main.text_header.view.*

object Holders {

    class TargetTextMsgHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val message = itemView.messageText_left!!
        val time = itemView.time_left!!
        val headerDateTime = itemView.header_left!!
        // val imageLayout = itemView.imageFrameLayout!!
        val container = itemView.container_left!!
        val messageLayout = itemView.message_layout_text_left!!
        val senderIcon = itemView.circle_sender_text!!
        val senderTitle = itemView.messageText_sender_left!!

    }
    class MyTextMsgHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val message = itemView.messageText_right!!
        val time = itemView.time_right!!
        val messageStatus = itemView.delivery_status!!
        val headerDateTime = itemView.header_right!!
        val container = itemView.container_right!!
        val messageLayout = itemView.message_layout_text_right!!


    }


    class MyImageMsgHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val message = itemView.messageText_image_right!!
        val time = itemView.time_image_right!!
        val imageView = itemView.imageview_image_right!!
        val progressBar = itemView.progress_bar_image_right!!
        val tapToRetry = itemView.tap_retry_image_right!!
        val messageStatus = itemView.delivery_image_status!!
        val headerDateTime = itemView.header_image_right!!
        val container = itemView.container_image_right!!

        val messageLayout = itemView.message_layout_image_right!!

        val cardContainer = itemView.image_container_right_card!!

        val imageUploadControl = itemView.imageview_image_control_right!!
    }

    class TargetImageMsgHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val message = itemView.messageText_image_left!!
        val time = itemView.time_image_left!!
        val imageView = itemView.imageview_image_left!!
        val headerDateTime = itemView.header_image_left!!
        // val imageLayout = itemView.imageFrameLayout!!
        val container = itemView.container_image_left!!

        val messageLayout = itemView.message_layout_image_left!!
        val senderIcon = itemView.circle_sender_image!!

        val cardContainer = itemView.image_container_left_card!!

        val senderTitle = itemView.messageText_sender_image_left!!

    }



    class MyVideoMsgHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val message = itemView.messageText_video_right!!
        val time = itemView.time_video!!
        val centerImageView = itemView.imageview_video!!
        val progressBar = itemView.progress_bar_video!!
        val tapToRetry = itemView.tap_retry_download_video!!
        val messageStatus = itemView.delivery_video_status!!
        val headerDateTime = itemView.header_video_right!!

        val thumbnail = itemView.video_thumbnail!!
        val videoLengthText = itemView.video_length!!

        val tap_to_download = itemView.tap_retry_download_video!!
        val container = itemView.container_video_right!!

        val messageLayout = itemView.message_layout_video_right!!

        val cardContainer = itemView.video_container_right_card!!



    }

    class TargetVideoMsgHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val message = itemView.messageText_video_left!!
        val time = itemView.time_video!!
        val centerImageView = itemView.imageview_video!!
        val headerDateTime = itemView.header_video_left!!
        val videoLayout = itemView.videoFrameLayout!!
        val thumbnail = itemView.video_thumbnail!!
        val progressBar = itemView.progress_bar_video!!
        val videoLengthText = itemView.video_length!!
        val tap_to_download = itemView.tap_retry_download_video!!
        val container = itemView.container_video_left!!

        val messageLayout = itemView.message_layout_video_left!!
        val senderIcon = itemView.circle_sender_video!!

        val cardContainer = itemView.video_container_left_card!!
        val senderTitle = itemView.messageText_sender_video_left!!

    }




    class TargetMapHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val message = itemView.messageText_map_left!!
        val mapView = itemView.mapview_left!!
        val dateHeader = itemView.header_map_left!!
        val time = itemView.time_map_left

        val messageLayout = itemView.message_layout_map_left!!
        val senderIcon = itemView.circle_sender_map!!

        val container = itemView.container_map_left!!
        val senderTitle = itemView.messageText_map_sender_left!!

    }

    class MyMapHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val message = itemView.messageText_map_right!!
        val mapView = itemView.mapview_right!!
        val dateHeader = itemView.header_map_right!!
        val messageStatus = itemView.delivery_status_map_right!!
        val messageLayout = itemView.message_layout_map_right!!
        val time = itemView.time_map_right!!


        val container = itemView.container_map_right!!


    }


    class TextHeaderHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val text = itemView.header_textView!!
        val dateTextView = itemView.header_date_text!!
    }

    class MyAudioHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val title = itemView.item_audio_title!!
        val dateTextView = itemView.header_right!!
        val time = itemView.time_right
        val lengthOrSize = itemView.item_audio_length_size!!
        val audioProgressBar = itemView.audio_progress_bar
        val messageLayout = itemView.message_layout_audio_right
        val audioIcon = itemView.item_audio_icon

        init {
            itemView.item_audio_container.setBackgroundResource(R.drawable.shape_bubble_rounded_right)
            val color = Color.WHITE
            title.setTextColor(color)
            lengthOrSize.setTextColor(color)
            audioProgressBar.color = Color.WHITE
            audioIcon.setColorFilter(color)
        }
    }

    class TargetAudioHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val title = itemView.item_audio_title!!
        val dateTextView = itemView.header_left!!
        val time = itemView.time_left
        val lengthOrSize = itemView.item_audio_length_size!!
        val audioProgressBar = itemView.audio_progress_bar
        val messageLayout = itemView.message_layout_audio_left
        val audioIcon = itemView.item_audio_icon

        init {
            itemView.item_audio_container.setBackgroundResource(R.drawable.shape_bubble_rounded_left)
            val color = Color.BLACK
            title.setTextColor(color)
            lengthOrSize.setTextColor(color)
            audioIcon.setColorFilter(color)
        }
    }

}