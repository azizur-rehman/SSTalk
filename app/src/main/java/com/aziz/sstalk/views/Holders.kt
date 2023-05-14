package com.aziz.sstalk.views

import android.graphics.Color
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aziz.sstalk.R
import com.aziz.sstalk.databinding.BubbleAudioLeftBinding
import com.aziz.sstalk.databinding.BubbleAudioRightBinding
import com.aziz.sstalk.databinding.BubbleImageLeftBinding
import com.aziz.sstalk.databinding.BubbleImageRightBinding
import com.aziz.sstalk.databinding.BubbleLeftBinding
import com.aziz.sstalk.databinding.BubbleMapLeftBinding
import com.aziz.sstalk.databinding.BubbleMapRightBinding
import com.aziz.sstalk.databinding.BubbleRightBinding
import com.aziz.sstalk.databinding.BubbleVideoLeftBinding
import com.aziz.sstalk.databinding.BubbleVideoRightBinding
import com.aziz.sstalk.utils.visible

object Holders {

    class TargetTextMsgHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val binding = BubbleLeftBinding.bind(itemView)
        val message = binding.messageTextLeft
        val time = binding.timeLeft
        val headerDateTime = binding.headerLeft
        // val imageLayout = binding.imageFrameLayout!!
        val container = binding.containerLeft!!
        val messageLayout = binding.messageLayoutTextLeft!!
        val senderIcon = binding.senderIcon!!
        val senderTitle = binding.messageTextSenderLeft!!

    }
    class MyTextMsgHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val binding = BubbleRightBinding.bind(itemView)
        val message = binding.messageTextRight!!
        val time = binding.timeRight!!
        val messageStatus = binding.deliveryStatus!!
        val headerDateTime = binding.headerRight!!
        val container = binding.containerRight!!
        val messageLayout = binding.messageLayoutTextRight!!


    }


    class MyImageMsgHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val binding = BubbleImageRightBinding.bind(itemView)
        val message = binding.messageTextImageRight!!
        val time = binding.timeImageRight!!
        val imageView = binding.imageviewImageRight!!
        val progressBar = binding.progressBarImageRight!!
        val tapToRetry = binding.tapRetryImageRight!!
        val messageStatus = binding.deliveryImageStatus!!
        val headerDateTime = binding.headerImageRight!!
        val container = binding.containerImageRight!!

        val messageLayout = binding.messageTextImageRight!!

        val cardContainer = binding.imageContainerRightCard!!

        val imageUploadControl = binding.imageviewImageControlRight!!
    }

    class TargetImageMsgHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val binding = BubbleImageLeftBinding.bind(itemView)

        val message = binding.messageTextImageLeft!!
        val time = binding.timeImageLeft!!
        val imageView = binding.imageviewImageLeft!!
        val headerDateTime = binding.headerImageLeft!!
        // val imageLayout = binding.imageFrameLayout!!
        val container = binding.containerImageLeft!!

        val messageLayout = binding.messageLayoutImageLeft!!
        val senderIcon = binding.circleSenderImage!!

        val cardContainer = binding.imageContainerLeftCard!!

        val senderTitle = binding.messageTextSenderImageLeft!!

    }



    class MyVideoMsgHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val itemBinding = BubbleVideoRightBinding.bind(itemView)
        val binding = itemBinding.includeVideoBubble


        val message = itemBinding.messageTextVideoRight!!
        val time = binding.timeVideo!!
        val centerImageView = binding.imageviewVideo!!
        val progressBar = binding.progressBarVideo!!
        val tapToRetry = binding.tapRetryDownloadVideo!!
        val messageStatus = binding.deliveryVideoStatus!!
        val headerDateTime = itemBinding.headerVideoRight!!

        val thumbnail = binding.videoThumbnail!!
        val videoLengthText = binding.videoLength!!

        val tap_to_download = binding.tapRetryDownloadVideo!!
        val container = itemBinding.containerVideoRight!!

        val messageLayout = itemBinding.messageLayoutVideoRight!!

        val cardContainer = itemBinding.videoContainerRightCard!!



    }

    class TargetVideoMsgHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val itemBinding = BubbleVideoLeftBinding.bind(itemView)
        val binding = itemBinding.includeVideoBubble


        val message = itemBinding.messageTextVideoLeft!!
        val time = binding.timeVideo!!
        val centerImageView = binding.imageviewVideo!!
        val progressBar = binding.progressBarVideo!!
        val tapToRetry = binding.tapRetryDownloadVideo!!
        val messageStatus = binding.deliveryVideoStatus!!
        val headerDateTime = itemBinding.headerVideoLeft!!

        val thumbnail = binding.videoThumbnail!!
        val videoLengthText = binding.videoLength!!

        val tap_to_download = binding.tapRetryDownloadVideo!!
        val container = itemBinding.containerVideoLeft!!

        val messageLayout = itemBinding.messageLayoutVideoLeft!!

        val cardContainer = itemBinding.videoContainerLeftCard!!

        val senderIcon = itemBinding.circleSenderVideo
        val senderTitle = itemBinding.messageTextSenderVideoLeft

    }




    class TargetMapHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val binding = BubbleMapLeftBinding.bind(itemView)

        val message = binding.messageTextMapLeft!!
        val mapView = binding.mapviewLeft!!
        val dateHeader = binding.headerMapLeft!!
        val time = binding.timeMapLeft

        val messageLayout = binding.messageTextMapLeft!!
        val senderIcon = binding.circleSenderMap!!

        val container = binding.containerMapLeft!!
        val senderTitle = binding.messageTextMapSenderLeft!!

    }

    class MyMapHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val binding = BubbleMapRightBinding.bind(itemView)
        val message = binding.messageTextMapRight!!
        val mapView = binding.mapviewRight!!
        val dateHeader = binding.headerMapRight!!
        val messageStatus = binding.deliveryStatusMapRight!!
        val messageLayout = binding.messageLayoutMapRight!!
        val time = binding.timeMapRight!!


        val container = binding.containerMapRight!!


    }


    class TextHeaderHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val text = itemView.findViewById<TextView>(R.id.header_textView)
        val dateTextView = itemView.findViewById<TextView>(R.id.header_date_text!!)
    }

    class MyAudioHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val itemBinding = BubbleAudioRightBinding.bind(itemView)
        val binding = itemBinding.includeAudioBubble

        val title = binding.itemAudioTitle!!
        val dateTextView = itemBinding.headerRight!!
        val time = itemBinding.timeRight!!
        val lengthOrSize = binding.itemAudioLengthSize!!
        val audioProgressBar = itemBinding.audioProgressBar!!
        val messageLayout = itemBinding.messageLayoutAudioRight!!
        val audioIcon = binding.itemAudioIcon!!
        val messageStatus = binding.deliveryStatus!!

        init {
            binding.itemAudioContainer.setBackgroundResource(R.drawable.shape_bubble_right)
            val color = Color.WHITE
            title.setTextColor(color)
            lengthOrSize.setTextColor(color)
            audioIcon.setColorFilter(color)
            audioProgressBar.progress = 0f
        }
    }

    class TargetAudioHolder(itemView: View): RecyclerView.ViewHolder(itemView){

        val itemBinding = BubbleAudioLeftBinding.bind(itemView)
        val binding = itemBinding.includeAudioBubble

        val title = binding.itemAudioTitle!!
        val dateTextView = itemBinding.headerLeft!!
        val time = itemBinding.timeLeft!!
        val lengthOrSize = binding.itemAudioLengthSize!!
        val audioProgressBar = itemBinding.audioProgressBar!!
        val messageLayout = itemBinding.messageLayoutAudioLeft!!
        val audioIcon = binding.itemAudioIcon!!
        val messageStatus = binding.deliveryStatus!!

        val senderTitle = itemBinding.sender!!
        val senderIcon = itemBinding.senderIcon!!

        init {
            binding.itemAudioContainer.setBackgroundResource(R.drawable.shape_bubble_left)
            val color = Color.BLACK
            title.setTextColor(color)
            lengthOrSize.setTextColor(color)
            audioIcon.setColorFilter(color)
            binding.deliveryStatus.visible = false
            audioProgressBar.progress = 0f
        }
    }



}