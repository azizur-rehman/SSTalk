package com.aziz.sstalk.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.ContactsContract
import android.widget.Toast
import com.aziz.sstalk.models.Models
import com.google.firebase.auth.FirebaseAuth
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

object utils {


    object constants {

        val FILE_TYPE_IMAGE = "image"
        val FILE_TYPE_LOCATION = "location"
        val KEY_IMG_PATH = "path"
        val KEY_CAPTION = "caption"

        val KEY_LATITUDE = "lat"
        val KEY_LONGITUDE = "lng"
        val KEY_ADDRESS = "address"

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

        val sdf = SimpleDateFormat("dd mm yy hh:mm aa")
        sdf.timeZone= (Calendar.getInstance().timeZone)

        return sdf.format(Date(timeStamp))
    }


    fun getByteArrayFromBitmap(bitmap: Bitmap) : ByteArray {
        val bout = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG,100, bout)
        return bout.toByteArray()
    }

    fun getBitmapFromByteArray(byteArray: ByteArray) : Bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)

}