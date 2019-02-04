package com.aziz.sstalk.utils

import android.content.Context

object Pref {

    const val KEY_SOUND = "sound"
    const val KEY_VIBRATION = "vibration"
    const val FILE = "settings"
    const val FILE_PROFILE = "profile"

    object Notification{
        fun setSoundEnabled(context: Context, isEnabled:Boolean){
            context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_SOUND, isEnabled)
                .apply()
        }

        fun setVibrationEnabled(context: Context, isEnabled:Boolean){
            context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_VIBRATION, isEnabled)
                .apply()
        }


        fun getSoundEnabled(context: Context):Boolean =
            context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
                .getBoolean(KEY_SOUND, true)

        fun getVibrationEnabled(context: Context):Boolean =
            context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
                .getBoolean(KEY_VIBRATION, true)

    }

    object Profile{
        fun setProfileUrl(context: Context, uid:String, url:String){
            context.getSharedPreferences(FILE_PROFILE, Context.MODE_PRIVATE)
                .edit()
                .putString(uid, url)
                .apply()
        }

        fun isProfileUrlSame(context: Context, uid: String, providedURL:String): Boolean{
           return  context.getSharedPreferences(FILE_PROFILE, Context.MODE_PRIVATE)
                .getString(uid,"") == providedURL

        }
    }
}