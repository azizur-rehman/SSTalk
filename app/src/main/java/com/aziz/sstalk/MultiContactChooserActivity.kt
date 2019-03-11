package com.aziz.sstalk

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.aziz.sstalk.models.Models
import com.aziz.sstalk.utils.FirebaseUtils
import com.aziz.sstalk.utils.utils
import com.aziz.sstalk.views.holders
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_multi_contact_chooser.*
import kotlinx.android.synthetic.main.contact_screen.*
import kotlinx.android.synthetic.main.item__forward_contact_list.view.*
import kotlinx.android.synthetic.main.item_conversation_layout.view.*
import kotlinx.android.synthetic.main.item_grid_contact_layout.view.*
import org.jetbrains.anko.*
import java.io.Serializable
import java.util.concurrent.Future

class MultiContactChooserActivity : AppCompatActivity(){

    //number list has 10 digit formatted number
    var numberList:MutableList<Models.Contact> = mutableListOf()
    var registeredAvailableUser:MutableList<Models.Contact> = mutableListOf()

    var excludedUIDs:MutableList<String> = ArrayList()

    var selectedUsers:MutableList<Models.Contact> = ArrayList()

    private var asyncLoader: Future<Unit>? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_multi_contact_chooser)

        excludedUIDs = intent.getStringArrayListExtra(utils.constants.KEY_EXCLUDED_LIST)


        contacts_list.layoutManager = LinearLayoutManager(this@MultiContactChooserActivity)
        participant_recyclerview.layoutManager = LinearLayoutManager(this@MultiContactChooserActivity,
            LinearLayoutManager.HORIZONTAL, false)

        asyncLoader = doAsyncResult {

            uiThread {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                    if(ActivityCompat.checkSelfPermission(this@MultiContactChooserActivity, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
                        requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), 101)
                    else
                        loadRegisteredUsers()

                }
                else
                    loadRegisteredUsers()
            }

            onComplete { contact_progressbar.visibility = View.GONE  }
        }


        supportActionBar?.setDisplayHomeAsUpEnabled(true)




    }


    override fun onDestroy() {
        asyncLoader?.cancel(true)
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {

        when(requestCode){
            101 -> {
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults.isNotEmpty())
                    loadRegisteredUsers()
                else {
                    utils.longToast(this, "Permission not granted, exiting...")
                    finish()
                }
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_tick, menu)
        return super.onCreateOptionsMenu(menu)
    }



    private fun loadRegisteredUsers(){




        numberList = utils.getContactList(this)

        FirebaseUtils.ref.allUser()
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(p0: DataSnapshot) {

                    if(!p0.exists()) {
                        utils.toast(this@MultiContactChooserActivity, "No registered users")
                        return
                    }

                    registeredAvailableUser.clear()

                    for (post in p0.children){
                        val userModel = post.getValue(Models.User ::class.java)

                        val number = utils.getFormattedTenDigitNumber(userModel!!.phone)
                        val uid = userModel.uid


                        for((index, item) in numberList.withIndex()) {
                            if (item.number == number || item.number.contains(number)) {
                                numberList[index].uid = uid
                                if(uid!=FirebaseUtils.getUid() && !registeredAvailableUser.contains(numberList[index])) {
                                    if(excludedUIDs.isEmpty() || !excludedUIDs.contains(uid))
                                    registeredAvailableUser.add(numberList[index])
                                }
                            }

                        }

                    }


                    contacts_list.adapter = adapter
                    participant_recyclerview.adapter = horizontalAdapter

                    if(registeredAvailableUser.isEmpty())
                        utils.longToast(this@MultiContactChooserActivity, "No contacts available")

                    contact_progressbar.visibility = View.GONE


                }

                override fun onCancelled(p0: DatabaseError) {
                }

            })
    }


    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        if(item?.itemId == R.id.action_confirm)
        {
            if(selectedUsers.isEmpty()){
                setResult(Activity.RESULT_CANCELED)
                finish()
                return false
            }

            val selectedUIDs:MutableList<String> = ArrayList()
            selectedUsers.forEach { selectedUIDs.add(it.uid) }

            setResult(Activity.RESULT_OK, intent.putParcelableArrayListExtra(utils.constants.KEY_SELECTED,
                selectedUsers as java.util.ArrayList<out Parcelable>))
            finish()
        }
        else
        finish()
        return super.onOptionsItemSelected(item)
    }


    val adapter = object : RecyclerView.Adapter<ViewHolder>() {

        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder
                = ViewHolder(layoutInflater.inflate(R.layout.item__forward_contact_list, p0, false))

        override fun getItemCount(): Int = registeredAvailableUser.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {

            holder.name.text = registeredAvailableUser[position].name

            holder.pic.borderWidth = holder.pic.borderWidth

            val uid = registeredAvailableUser.get(index = position).uid

            FirebaseUtils.loadProfilePic(this@MultiContactChooserActivity, uid, holder.pic)

            holder.itemView.setOnClickListener {

                holder.checkBox.isChecked = !holder.checkBox.isChecked

                if(holder.checkBox.isChecked) {
                    selectedUsers.add(registeredAvailableUser[holder.adapterPosition])
                    horizontalAdapter.notifyItemInserted(selectedUsers.lastIndex)

                }else {
                    selectedUsers.remove(registeredAvailableUser[holder.adapterPosition])
                    horizontalAdapter.notifyItemRemoved(selectedUsers.lastIndex)

                }

                if(selectedUsers.isNotEmpty())
                participant_recyclerview.smoothScrollToPosition(selectedUsers.lastIndex)

            }



        }



    }

    val horizontalAdapter = object : RecyclerView.Adapter<ParticipantHolder>() {
        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ParticipantHolder {
            return ParticipantHolder(layoutInflater.inflate(R.layout.item_grid_contact_layout, p0, false))
        }

        override fun getItemCount(): Int = selectedUsers.size

        override fun onBindViewHolder(p0: ParticipantHolder, p1: Int) {
            p0.name.text = utils.getNameFromNumber(this@MultiContactChooserActivity,
                selectedUsers[p1].number)

            FirebaseUtils.loadProfileThumbnail(this@MultiContactChooserActivity, selectedUsers[p1].uid,
                p0.pic)
        }

    }




    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
                val name = itemView.name
                val pic = itemView.pic
                val checkBox = itemView.checkbox
            }

    class ParticipantHolder(itemView: View):RecyclerView.ViewHolder(itemView){
        val name = itemView.grid_name!!
        val pic = itemView.grid_pic!!
    }
}