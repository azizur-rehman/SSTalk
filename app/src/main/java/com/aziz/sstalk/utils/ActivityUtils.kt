@file:Suppress("NOTHING_TO_INLINE")

package com.aziz.sstalk.utils

import android.content.Context
import android.view.View
import com.aziz.sstalk.models.Models
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import org.jetbrains.anko.collections.forEachWithIndex
import java.lang.Exception

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


fun Context.loadAvailableUsers(action:(registeredAvailableUsers:MutableList<Models.Contact>)->Unit){

    val context = this


    if(!utils.hasContactPermission(context))
        return



    FirebaseUtils.ref.allUser()
        .addValueEventListener(object : ValueEventListener {
            override fun onDataChange(p0: DataSnapshot) {


                val numberList: MutableList<Models.Contact> = utils.getContactList(context)
                val registeredAvailableUser:MutableList<Models.Contact> = mutableListOf()


                registeredAvailableUser.clear()

                for (post in p0.children){
                    val userModel = post.getValue(Models.User ::class.java)

                    val number = utils.getFormattedTenDigitNumber(userModel!!.phone)
                    val uid = userModel.uid


                    for((index, item) in numberList.withIndex()) {
                        if (item.number == number || item.number.contains(number)) {
                            numberList[index].uid = uid
                            if(uid!=FirebaseUtils.getUid() && !registeredAvailableUser.contains(numberList[index]))
                                registeredAvailableUser.add(numberList[index])
                        }

                    }

                }

                action.invoke(registeredAvailableUser)





            }

            override fun onCancelled(p0: DatabaseError) {
            }

        })
}