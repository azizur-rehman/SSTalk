package com.aziz.sstalk

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ActionMode
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import com.aziz.sstalk.models.Models
import com.aziz.sstalk.utils.FirebaseUtils
import com.aziz.sstalk.utils.utils
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.app_bar_home.*
import kotlinx.android.synthetic.main.content_home.*
import kotlinx.android.synthetic.main.item_contact_list.view.*
import java.lang.Exception

class HomeActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    val context = this@HomeActivity

    var hasPermission:Boolean = false
    val id = R.drawable.contact_placeholder
    val debugUserID = "user---2"
    lateinit var adapter:FirebaseRecyclerAdapter<Models.LastMessageDetail, ViewHolder>

    val selectItemPosition:MutableList<Int> = ArrayList()

    var isContextToolbarActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        setSupportActionBar(toolbar)

        //storing firebase token, if updated
        FirebaseUtils.storeFCMToken(this)

        val toggle = ActionBarDrawerToggle(
            this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        show_contacts.setOnClickListener {

            startActivity(Intent(context, ContactsActivity::class.java))
        }

        nav_view.setNavigationItemSelectedListener(this)

          hasPermission = utils.hasContactPermission(this)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if(!hasPermission) {
                    requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), 101)
                }
                else
                    setAdapter()
            }
            else
            setAdapter()


        FirebaseUtils.setMeAsOnline()


    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {

        when(requestCode){
            101 -> {
                hasPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults.isNotEmpty()

                if(hasPermission)
                    //reset the adapter
                    setAdapter()
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }



    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }


    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_my_profile -> {

                startActivity(Intent(context, EditProfile::class.java))
            }
            R.id.nav_setting -> {

                startActivity(Intent(context, SettingsActivity::class.java))
            }

            R.id.nav_share -> {

            }

        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }


    private fun setAdapter(){

        val options = FirebaseRecyclerOptions.Builder<Models.LastMessageDetail>()
            .setQuery(FirebaseUtils.ref.getLastMessageRef(FirebaseUtils.getUid())
                    //todo dont forget to change it
                .orderByChild(FirebaseUtils.KEY_REVERSE_TIMESTAMP),Models.LastMessageDetail::class.java)
            .build()

         adapter = object : FirebaseRecyclerAdapter<Models.LastMessageDetail, ViewHolder>(options){
            override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder = ViewHolder(layoutInflater.inflate(R.layout.item_contact_list, p0, false))

            override fun onBindViewHolder(holder: ViewHolder, position: Int, model: Models.LastMessageDetail) {

                val uid = super.getRef(position).key.toString()

                holder.name.text = uid

                FirebaseUtils.loadProfilePic(this@HomeActivity, uid, holder.pic)

                FirebaseUtils.setLastMessage(uid, holder.lastMessage)

                FirebaseUtils.setUserDetailFromUID(this@HomeActivity, holder.name, uid, hasPermission)

                holder.messageInfo.visibility = View.VISIBLE

                holder.time.text = utils.getLocalTime(model.timeInMillis)

                FirebaseUtils.setUnreadCount(uid, holder.unreadCount, holder.name, holder.lastMessage, holder.time)


                holder.itemView.setOnClickListener {

                    if(isContextToolbarActive){
                        holder.checkbox.visibility = View.VISIBLE
                        holder.checkbox.isChecked = true

                        if(!selectItemPosition.contains(position))
                        {
                            selectItemPosition.add(position)
                        }

                        return@setOnClickListener
                    }

                    startActivity(Intent(context, MessageActivity::class.java).putExtra(FirebaseUtils.KEY_UID, uid)
                        .putExtra(utils.constants.KEY_UNREAD,holder.unreadCount.getTextView().text.toString().toInt()))
                }

                if(!isContextToolbarActive){
                    holder.checkbox.visibility = View.INVISIBLE
                    holder.checkbox.isChecked = false
                }

                holder.itemView.setOnLongClickListener {

                    if(isContextToolbarActive)
                        return@setOnLongClickListener false

                    if(!selectItemPosition.contains(position))
                    {
                        selectItemPosition.add(position)
                    }

                    holder.checkbox.visibility = View.VISIBLE
                    holder.checkbox.isChecked = true

                    startSupportActionMode(object : ActionMode.Callback {
                        override fun onActionItemClicked(p0: ActionMode?, p1: MenuItem?): Boolean {

                            when(p1!!.itemId){
                                R.id.action_delete_conversation -> {
                                    utils.toast(context, "Pending")
                                }
                            }

                            p0!!.finish()
                            return true
                        }

                        override fun onCreateActionMode(p0: ActionMode?, p1: Menu?): Boolean {

                            p0!!.menuInflater.inflate(R.menu.converstation_option_menu, p1)
                            isContextToolbarActive = true

                            return true
                        }

                        override fun onPrepareActionMode(p0: ActionMode?, p1: Menu?): Boolean = true

                        override fun onDestroyActionMode(p0: ActionMode?) {
                            isContextToolbarActive = false

                            for(pos in selectItemPosition)
                                adapter.notifyItemChanged(pos)

                            selectItemPosition.clear()

                        }

                    })

                    true
                }




            }

        }




        conversationRecycler.layoutManager = LinearLayoutManager(context)
        conversationRecycler.adapter = adapter
        conversationRecycler.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        adapter.startListening()
        setonDisconnectListener()
    }





    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val name = itemView.name
        val lastMessage = itemView.mobile_number
        val pic = itemView.pic
        val messageInfo = itemView.messageInfoLayout
        val time = itemView.messageTime
        val unreadCount = itemView.unreadCount
        val onlineStatus = itemView.online_status_imageview
        val checkbox = itemView.contact_checkbox

    }

    override fun onDestroy() {
        try {
            adapter.stopListening()
            FirebaseUtils.setMeAsOffline()
        }
        catch (e:Exception) {}

        super.onDestroy()
    }


    private fun setonDisconnectListener(){

        FirebaseUtils.ref.getUserStatusRef(FirebaseUtils.getUid())
            .onDisconnect()
            .setValue(Models.UserActivityStatus(FirebaseUtils.VAL_OFFLINE, System.currentTimeMillis()))
    }


    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {

        menuInflater.inflate(R.menu.converstation_option_menu, menu)
        menu!!.setHeaderTitle("Options")

        super.onCreateContextMenu(menu, v, menuInfo)
    }
}
