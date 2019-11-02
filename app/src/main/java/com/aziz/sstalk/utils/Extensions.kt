@file:Suppress("NOTHING_TO_INLINE")

package com.aziz.sstalk.utils

import android.content.Context
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.View.MeasureSpec.UNSPECIFIED
import org.jetbrains.anko.AlertBuilder
import org.jetbrains.anko.alert
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