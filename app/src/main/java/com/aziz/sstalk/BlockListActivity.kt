package com.aziz.sstalk

import android.app.Activity
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.aziz.sstalk.utils.FirebaseUtils
import com.aziz.sstalk.utils.utils
import com.firebase.ui.database.FirebaseListAdapter
import com.firebase.ui.database.FirebaseListOptions
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
                    FirebaseUtils.ref.blockedUser(FirebaseUtils.getUid(), uid)
                        .setValue(false)
                }
                .setNegativeButton("No", null)
                .show()

        }

        block_listview.adapter = adapter
    }


    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if(item!!.itemId == android.R.id.home)
        finish()
        else{
            startActivityForResult(Intent(this, ContactsActivity::class.java).apply {
                putExtra(utils.constants.KEY_IS_FOR_SELECTION, true)
            },111)
        }
        return super.onOptionsItemSelected(item)
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
            menuInflater.inflate(R.menu.user_profile_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if(requestCode == 111 && resultCode == Activity.RESULT_OK){

            val uid = data!!.getStringExtra(FirebaseUtils.KEY_UID)
            Log.d("BlockListActivity", "onActivityResult: blocking -> $uid")
            FirebaseUtils.ref.blockedUser(FirebaseUtils.getUid(), uid)
                .setValue(true)
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

}
