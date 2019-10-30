@file:Suppress("NOTHING_TO_INLINE")

package com.aziz.sstalk.utils

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.drawable.InsetDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.aziz.sstalk.MessageActivity
import com.aziz.sstalk.models.Models
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener

import com.aziz.sstalk.R

inline fun View.hide(){
    visibility = View.GONE
}

inline fun View.show(){
    visibility = View.VISIBLE
}

inline fun View.hideOrShow() = if (visibility == View.VISIBLE) hide() else show()

var View.visible:Boolean
    get() { return visibility == View.VISIBLE }
    set(value) = if (value) show() else hide()


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
                registeredAvailableUser.sortBy { it.name }
                action.invoke(registeredAvailableUser)





            }

            override fun onCancelled(p0: DatabaseError) {
            }

        })
}


fun <T> RecyclerView.setCustomAdapter(context: Context, items: Collection<T>, resID: Int,
                                        onBindViewHolder: (itemView: View, position: Int, item: T )->Unit) : RecyclerView.Adapter<RecyclerView.ViewHolder> {


    val adapter =  object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

            val view = LayoutInflater.from(context).inflate(resID, parent, false)
            return object : RecyclerView.ViewHolder(view){}
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

            onBindViewHolder.invoke(holder.itemView, position, items.elementAt(position))
        }

    }
    this.adapter = adapter
    return adapter

}



fun Context.startChat(uid:String, type:String, nameOrNumber:String, unreadCount:Int = 0, message:Models.MessageModel? = null){
    startActivity(
        Intent(this, MessageActivity::class.java)
            .apply {
                putExtra(FirebaseUtils.KEY_UID, uid)
                putExtra(utils.constants.KEY_TARGET_TYPE, type)
                putExtra(utils.constants.KEY_NAME_OR_NUMBER, nameOrNumber)
                putExtra(utils.constants.KEY_MSG_MODEL, message)
                putExtra(utils.constants.KEY_UNREAD, unreadCount) //optional
            }
    )
}


inline fun <reified T> Query.onSingleEvent(crossinline onLoaded:(snapshot: T?) -> Unit){
    this.addListenerForSingleValueEvent(object : ValueEventListener{
        override fun onCancelled(p0: DatabaseError) {
        }

        override fun onDataChange(p0: DataSnapshot) {
            onLoaded.invoke(p0.getValue(T::class.java))
        }

    })
}


inline fun <reified T> Query.onSingleListEvent(crossinline onLoaded:(list: List<T?>, snapshot:DataSnapshot) -> Unit){
    this.addListenerForSingleValueEvent(object : ValueEventListener{
        override fun onCancelled(p0: DatabaseError) {
        }

        override fun onDataChange(p0: DataSnapshot) {
            val list : MutableList<T?> = ArrayList()
            for (post in p0.children)
                list.add(post.getValue(T::class.java))

            onLoaded.invoke(list, p0)
        }

    })
}


inline fun <reified T> Query.onRealTimeListEvent(crossinline onLoaded:(snapshot: List<T?>) -> Unit){
    this.addValueEventListener(object : ValueEventListener{
        override fun onCancelled(p0: DatabaseError) {
        }

        override fun onDataChange(p0: DataSnapshot) {
            val list : MutableList<T?> = ArrayList()
            for (post in p0.children)
                list.add(post.getValue(T::class.java))

            onLoaded.invoke(list)
        }

    })
}


inline fun <reified T> Query.onRealtimeEvent(crossinline onLoaded:(snapshot:T?) -> Unit){
    this.addValueEventListener(object : ValueEventListener{
        override fun onCancelled(p0: DatabaseError) {
        }

        override fun onDataChange(p0: DataSnapshot) {
            onLoaded.invoke(p0.getValue(T::class.java))
        }

    })
}


const val max_file_size:Long = 16 * 1024 * 1024

inline fun Dialog.makeRound(width:Int = WindowManager.LayoutParams.MATCH_PARENT, height:Int = WindowManager.LayoutParams.WRAP_CONTENT) {
    val insetDrawable = InsetDrawable(ContextCompat.getDrawable(this.context, R.drawable.rounded_white_background), 30)
    window?.setLayout(width,height)
    window?.setBackgroundDrawable(insetDrawable)
}