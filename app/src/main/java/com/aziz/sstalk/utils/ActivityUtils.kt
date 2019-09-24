@file:Suppress("NOTHING_TO_INLINE")

package com.aziz.sstalk.utils

import android.view.View

inline fun View.hide(){
    visibility = View.GONE
}

inline fun View.show(){
    visibility = View.VISIBLE
}

inline fun View.hideOrShow(){
    if (visibility == View.VISIBLE) hide()
    else show()
}
