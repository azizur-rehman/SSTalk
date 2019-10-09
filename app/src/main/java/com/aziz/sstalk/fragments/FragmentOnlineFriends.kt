package com.aziz.sstalk.fragments

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.aziz.sstalk.HomeActivity
import com.aziz.sstalk.MessageActivity
import com.aziz.sstalk.R
import com.aziz.sstalk.models.Models
import com.aziz.sstalk.utils.FirebaseUtils
import com.aziz.sstalk.utils.loadAvailableUsers
import com.aziz.sstalk.utils.utils
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.item_contact_layout.view.*
import kotlinx.android.synthetic.main.layout_recycler_view.*
import kotlinx.android.synthetic.main.layout_recycler_view.view.*
import org.jetbrains.anko.collections.forEachWithIndex
import java.lang.Exception

class FragmentOnlineFriends : Fragment() {


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.layout_recycler_view,container, false)
        loadRegisteredUsers(view)
        return view


    }


    private fun loadRegisteredUsers(view: View){

        if(!utils.hasContactPermission(context!!))
            return


        context?.loadAvailableUsers { registeredAvailableUsers ->


            val onlineUsers = mutableListOf<Models.Contact>()


            registeredAvailableUsers.forEachWithIndex { _, it ->
                FirebaseUtils.ref.userStatus(it.uid)
                    .addValueEventListener(object : ValueEventListener {
                        override fun onCancelled(p0: DatabaseError) {
                        }

                        override fun onDataChange(p0: DataSnapshot) {

                            if(!p0.exists())
                                return

                            val onlineStatus = p0.getValue(Models.UserActivityStatus::class.java)

                            if(onlineStatus?.status == FirebaseUtils.VAL_ONLINE ||
                                onlineStatus?.status!!.startsWith(FirebaseUtils.VAL_TYPING)) {
                                if (!onlineUsers.contains(it)) {
                                    onlineUsers.add(it)
                                }
                            }
                            else
                                onlineUsers.remove(it)

                            try {
                                setOnlineAdapter(onlineUsers, view)
                            }
                            catch (e:Exception){ }
                        }

                    })}

        }


    }

    private fun setOnlineAdapter(onlineUsers:MutableList<Models.Contact>, view: View){


        recycler_back_message.visibility =if(onlineUsers.isNotEmpty()) View.GONE
        else View.VISIBLE

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
            val name = itemView.name
            val pic = itemView.pic

        }

       try { (context as HomeActivity).setOnlineCount(onlineUsers.size) }
       catch (e:Exception){}

        view.recyclerView.adapter = object : RecyclerView.Adapter<ViewHolder>() {
            override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder {
                return ViewHolder(layoutInflater.inflate(R.layout.item_layout_online,
                    p0, false))
            }

            override fun getItemCount(): Int = onlineUsers.size

            override fun onBindViewHolder(p0: ViewHolder, p1: Int) {
                p0.name.text = utils.getNameFromNumber(context!!, onlineUsers[p1].number)
                FirebaseUtils.loadProfileThumbnail(context!!, onlineUsers[p1].uid, p0.pic)

                p0.itemView.setOnClickListener {
                    startActivity(
                        Intent(context, MessageActivity::class.java)
                            .apply {
                                putExtra(FirebaseUtils.KEY_UID, onlineUsers[p1].uid)
                                putExtra(utils.constants.KEY_TARGET_TYPE, FirebaseUtils.KEY_CONVERSATION_SINGLE)
                                putExtra(utils.constants.KEY_NAME_OR_NUMBER, onlineUsers[p1].number)
                            })
                }
            }

        }


    }
}