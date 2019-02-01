package com.aziz.sstalk

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        setSupportActionBar(toolbar)


        val toggle = ActionBarDrawerToggle(
            this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        show_contacts.setOnClickListener {

            startActivity(Intent(context, ContactsActivity::class.java))
        }

        nav_view.setNavigationItemSelectedListener(this)

          hasPermission = (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if(!hasPermission) {
                    requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), 101)
                }
                else
                    setAdapter()
            }
            else
            setAdapter()



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

            }
            R.id.nav_slideshow -> {

            }
            R.id.nav_manage -> {

            }
            R.id.nav_share -> {

            }
            R.id.nav_send -> {

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

                FirebaseUtils.loadProfilePic(this@HomeActivity, uid, holder.pic, false)

                FirebaseUtils.setLastMessage(uid, holder.lastMessage)

                FirebaseUtils.setUserDetailFromUID(this@HomeActivity, holder.name, uid, hasPermission)

                holder.messageInfo.visibility = View.VISIBLE

                holder.time.text = utils.getLocalTime(model.timeInMillis)


                holder.itemView.setOnClickListener {
                    startActivity(Intent(context, MessageActivity::class.java).putExtra(FirebaseUtils.KEY_UID, uid))
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

    }

    override fun onDestroy() {
        try {
            adapter.stopListening()
            FirebaseUtils.ref.getUserStatusRef(FirebaseUtils.getUid())
                .setValue(Models.UserActivityStatus("Offline", System.currentTimeMillis()))
        }
        catch (e:Exception) {}

        super.onDestroy()
    }


    override fun onResume() {

        FirebaseUtils.ref.getUserStatusRef(FirebaseUtils.getUid())
            .setValue(Models.UserActivityStatus("Online", System.currentTimeMillis()))
        super.onResume()
    }

    private fun setonDisconnectListener(){

        FirebaseUtils.ref.getUserStatusRef(FirebaseUtils.getUid())
            .onDisconnect()
            .setValue(Models.UserActivityStatus("Offline", System.currentTimeMillis()))
    }
}
