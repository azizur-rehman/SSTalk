package com.aziz.sstalk.utils

import android.content.Context
import android.provider.ContactsContract
import android.widget.Toast
import com.aziz.sstalk.Models.Models
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

object utils {


    object constants {

        val FILE_TYPE_IMAGE = "image"
        val KEY_IMG_PATH = "path"
        val KEY_CAPTION = "caption"

    }


    fun toast(context: Context?, message: CharSequence) =
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

    fun longToast(context: Context?, message: CharSequence) =
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()


    fun getFormattedTenDigitNumber(number:String ) : String {
        var out = Pattern.compile("[^0-9]").matcher(number).replaceAll("")

        if(out.length>10){
            out = number.substring(number.length - 10)
        }

        return out
    }



    fun getContactList(context: Context?) : MutableList<Models.Contact>{

        val numberList:MutableList<Models.Contact> = mutableListOf()
        val cursor = context!!.contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null , null, null , null)

        while(cursor!!.moveToNext()){

            var name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
            var number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))

            var pic = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI))

            number = utils.getFormattedTenDigitNumber(number)

            if(pic == null)
                pic = ""

            if(name == null)
                name = ""



            var isDuplicate = false
            for(item in numberList){
                if(item.number == number ) {
                    isDuplicate = true
                    break

                }

            }

            if(FirebaseUtils.isLoggedIn()){
                isDuplicate = FirebaseAuth.getInstance().currentUser!!.phoneNumber == number;
            }

            if(!isDuplicate)
                numberList.add(Models.Contact(name, number , pic))


        }

        cursor.close()
        return numberList

    }



    fun getLocalTime(timeStamp: Long): String{

        val sdf = SimpleDateFormat("hh:mm aa")
        sdf.timeZone= (Calendar.getInstance().timeZone)

        return sdf.format(Date(timeStamp))
    }

    fun getLocalDateTime(timeStamp: Long): String{

        val sdf = SimpleDateFormat("DD MM YY HH:mm a")
        sdf.timeZone= (Calendar.getInstance().timeZone)

        return sdf.format(Date(timeStamp))
    }

}