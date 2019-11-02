package com.aziz.sstalk.fragments

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.aziz.sstalk.models.Models
import com.aziz.sstalk.utils.FirebaseUtils
import com.aziz.sstalk.utils.loadAvailableUsers
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import org.jetbrains.anko.collections.forEachWithIndex

class OnlineVM : ViewModel() {

    private val onlineUsers:MutableLiveData<MutableList<Models.Contact>> = MutableLiveData()

    fun getOnlineUsers(context: Context?): MutableLiveData<MutableList<Models.Contact>> {

        if(onlineUsers.value == null){
            loadUsers(context)
        }

        return onlineUsers
    }


    private fun loadUsers(context: Context?){


        context?.loadAvailableUsers { registeredAvailableUsers ->

            Log.d("OnlineVM", "loadUsers: loaded")

            val users = mutableListOf<Models.Contact>()


            registeredAvailableUsers.forEachWithIndex { _, user ->
                FirebaseUtils.ref.userStatus(user.uid)
                    .addValueEventListener(object : ValueEventListener {
                        override fun onCancelled(p0: DatabaseError) {
                        }

                        override fun onDataChange(p0: DataSnapshot) {

                            if(!p0.exists())
                                return

                            val onlineStatus = p0.getValue(Models.UserActivityStatus::class.java)

                            onlineStatus?.let {

                                if(onlineStatus.status == FirebaseUtils.VAL_ONLINE ||
                                    onlineStatus.status.startsWith(FirebaseUtils.VAL_TYPING)) {
                                    if (!users.contains(user)) {
                                        users.add(user)
                                    }
                                } else
                                    users.remove(user)

                                onlineUsers.value = users

                            }
                        }

                    })}

        }

    }
}