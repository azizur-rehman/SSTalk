@file:Suppress("NOTHING_TO_INLINE")

package com.aziz.sstalk.utils

import android.content.Context
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.view.View
import android.view.View.MeasureSpec.UNSPECIFIED
import android.widget.ImageView
import com.aziz.sstalk.R
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import org.jetbrains.anko.AlertBuilder
import org.jetbrains.anko.alert
import org.jetbrains.anko.dip
import org.jetbrains.anko.okButton


inline fun Context.showInfoDialog(dialogMessage:String, cancelable:Boolean = true, noinline onOkClick: (() -> Unit?)? = null): AlertBuilder<DialogInterface> {
    val alert = alert { message = dialogMessage
//        iconResource = R.drawable.ic_info_outline_black_24dp
//        title = "Info"
        okButton { onOkClick?.let { it1 -> it1() } }
        isCancelable = cancelable
    }
    alert.show()
    return alert
}

inline fun Context.showConfirmDialog(dialogMessage:String, noinline onOkClick: (() -> Unit?)?): AlertBuilder<DialogInterface> {
    val alert = alert { message = dialogMessage
//        iconResource = R.drawable.ic_info_outline_black_24dp
//        title = "Confirm"
        positiveButton("Yes") { onOkClick?.let { it1 -> it1() } }
        negativeButton("No") {  }
    }
    alert.show()
    return alert
}


val View.toBitmap: Bitmap
    get() {
        measure(UNSPECIFIED, UNSPECIFIED)
        val bitmap = Bitmap.createBitmap(
            measuredWidth, measuredHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        layout(0, 0, width, height)
        draw(canvas)
        return bitmap
    }


fun ImageView.loadImage(
    url: Any?,
    placeholderRes: Int = R.drawable.contact_placeholder,
    errorRes: Int = placeholderRes
){


    kotlin.runCatching {
        Glide.with(context)
            .setDefaultRequestOptions(
                RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(placeholderRes)
                   .error(errorRes)
            )
            .load(url)
//            .transform(
//                CenterCrop(),
//                RoundedCornersTransformation(
//                    dip(roundRadiusInDp), 0,
//                    RoundedCornersTransformation.CornerType.ALL
//                )
//            )
            .listener(object : RequestListener<Drawable?> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable?>?,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable?>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    this@loadImage.setImageDrawable(resource)
                    return true
                }

            })
            .transition(DrawableTransitionOptions.withCrossFade())
            .apply(RequestOptions().override(100))
//           .thumbnail(0.7f)
            .into(this)
    }
}
