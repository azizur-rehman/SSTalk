package com.aziz.sstalk

import android.annotation.SuppressLint
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.aziz.sstalk.utils.FirebaseUtils
import com.aziz.sstalk.utils.utils
import com.firebase.ui.database.FirebaseListAdapter
import com.firebase.ui.database.FirebaseListOptions
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.google.firebase.database.DatabaseReference
import kotlinx.android.synthetic.main.activity_block_list.*
import kotlinx.android.synthetic.main.item_contact_layout.view.*

class BlockListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_block_list)

        title = "Block List"
        if(supportActionBar!=null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setHomeButtonEnabled(true)
        }


        val query = FirebaseUtils.ref.getBlockedUserListQuery(FirebaseUtils.getUid())

        val options = FirebaseListOptions.Builder<Any>()
            .setQuery(query, Any::class.java)
            .setLifecycleOwner(this)
            .setLayout(R.layout.item_contact_layout)
            .build()


        val adapter = object : FirebaseListAdapter<Any>(options){
            override fun populateView(v: View, model: Any, position: Int) {

                val title = v.name
                val pic = v.pic
                val uid =  getRef(position).key.toString()


                FirebaseUtils.setUserDetailFromUID(this@BlockListActivity,title, uid , true)

                FirebaseUtils.loadProfileThumbnail(this@BlockListActivity, uid, pic)
            }



        }


        block_listview.setOnItemClickListener { _, _, position, _ ->
            val uid = adapter.getRef(position).key.toString()

            AlertDialog.Builder(this@BlockListActivity).setMessage("Unblock this user")
                .setPositiveButton("Yes") { _, _ ->
                    FirebaseUtils.ref.getBlockedUserRef(FirebaseUtils.getUid(), uid)
                        .setValue(false)
                }
                .setNegativeButton("No", null)
                .show()

        }

        block_listview.adapter = adapter
    }


    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        finish()
        return super.onOptionsItemSelected(item)
    }
}
