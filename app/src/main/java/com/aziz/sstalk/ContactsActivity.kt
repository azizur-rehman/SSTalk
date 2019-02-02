package com.aziz.sstalk

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.aziz.sstalk.models.Models
import com.aziz.sstalk.utils.FirebaseUtils
import com.aziz.sstalk.utils.utils
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.contact_screen.*
import kotlinx.android.synthetic.main.item_contact_list.view.*

class ContactsActivity : AppCompatActivity(){

    //number list has 10 digit formatted number
    var numberList:MutableList<Models.Contact> = mutableListOf()
    var registeredAvailableUser:MutableList<Models.Contact> = mutableListOf()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.contact_screen)

        contacts_list.layoutManager = LinearLayoutManager(this@ContactsActivity)
        contacts_list.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                if(ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
                    requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), 101)
                else
                    loadRegisteredUsers()

                return
            }

        loadRegisteredUsers()

    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {

        when(requestCode){
            101 -> {
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults.isNotEmpty())
                    loadRegisteredUsers()
                else {
                    utils.longToast(this, "Permission not granted exiting")
                    finish()
                }
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


    private fun loadRegisteredUsers(){



        numberList = utils.getContactList(this)

        FirebaseUtils.ref.getAllUserRef()
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(p0: DataSnapshot) {

                    if(!p0.exists()) {
                        utils.toast(this@ContactsActivity, "No registered users")
                        return
                    }

                    registeredAvailableUser.clear()

                    for (post in p0.children){
                        val userModel = post.getValue(Models.User ::class.java)

                        val number = utils.getFormattedTenDigitNumber(userModel!!.phone)
                        val uid = userModel.uid

                        Log.d("ContactsActivity", "onDataChange: number = $number")

                        for((index, item) in numberList.withIndex()) {
                            if (item.number == number) {
                                numberList[index].uid = uid
                                registeredAvailableUser.add(numberList[index])
                            }

                        }

                    }

                    contacts_list.adapter = adapter


                    registeredAvailableUser.add(Models.Contact("Invite Users"))

                    adapter.notifyDataSetChanged()

                    for(item in registeredAvailableUser)
                        Log.d("ContactsActivity", "onDataChange: available user = ${item.name} , ${item.number}")

                }

                override fun onCancelled(p0: DatabaseError) {
                }

            })
    }




    val adapter = object : RecyclerView.Adapter<ViewHolder>() {

        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder
                = ViewHolder(layoutInflater.inflate(R.layout.item_contact_list, p0, false))

        override fun getItemCount(): Int = registeredAvailableUser.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {

            holder.name.text = registeredAvailableUser[position].name
            holder.number.text = registeredAvailableUser[position].number

            holder.pic.borderWidth = holder.pic.borderWidth
            holder.number.visibility = View.VISIBLE

            val uid = registeredAvailableUser.get(index = position).uid

            FirebaseUtils.loadProfilePic(this@ContactsActivity, uid, holder.pic, false)

            if(position == registeredAvailableUser.size - 1) {
                holder.pic.setImageResource(android.R.drawable.ic_menu_share)
                holder.number.visibility = View.GONE
                holder.pic.borderWidth = 0
            }


            holder.itemView.setOnClickListener {
                if(position != registeredAvailableUser.size - 1){


                    utils.longToast(this@ContactsActivity, uid)

                    startActivity(Intent(this@ContactsActivity, MessageActivity::class.java).putExtra(FirebaseUtils.KEY_UID, uid))
                    finish()

                }
            }


        }


    }


            class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
                val name = itemView.name
                val number = itemView.mobile_number
                val pic = itemView.pic
            }
}