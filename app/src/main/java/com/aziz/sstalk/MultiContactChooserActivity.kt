package com.aziz.sstalk

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.aziz.sstalk.models.Models
import com.aziz.sstalk.utils.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.miguelcatalan.materialsearchview.MaterialSearchView
import kotlinx.android.synthetic.main.activity_multi_contact_chooser.*
import kotlinx.android.synthetic.main.activity_multi_contact_chooser.searchView
import kotlinx.android.synthetic.main.item_contact_layout.view.*
import kotlinx.android.synthetic.main.item_grid_contact_layout.view.*
import org.jetbrains.anko.*
import java.util.concurrent.Future

class MultiContactChooserActivity : AppCompatActivity(){

    //number list has 10 digit formatted number
    var numberList:MutableList<Models.Contact> = mutableListOf()
    var registeredAvailableUser:MutableList<Models.Contact> = mutableListOf()

    var allUsers = registeredAvailableUser

    var excludedUIDs:MutableList<String> = ArrayList()

    var selectedUsers:MutableList<Models.Contact> = ArrayList()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_multi_contact_chooser)
        title = "Choose from contacts"
        setSupportActionBar(toolbar)

        excludedUIDs = intent.getStringArrayListExtra(utils.constants.KEY_EXCLUDED_LIST)?:ArrayList()


        contacts_list.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this@MultiContactChooserActivity)
        participant_recyclerview.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(
            this@MultiContactChooserActivity,
            androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false
        )


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                    if(ActivityCompat.checkSelfPermission(this@MultiContactChooserActivity, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
                        requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), 101)
                    else
                        loadRegisteredUsers()

                }
        else loadRegisteredUsers()




        supportActionBar?.setDisplayHomeAsUpEnabled(true)




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

        val item = menu?.findItem(R.id.action_search)
        item?.isVisible = true

        searchView.setMenuItem(item)
        searchView.setOnQueryTextListener(object : MaterialSearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {

                (adapter as? Filterable)?.filter?.filter(newText)
                return true
            }

        })

        return super.onCreateOptionsMenu(menu)
    }



    private fun loadRegisteredUsers(){


        numberList = utils.getContactList(this)

        loadAvailableUsers {

            registeredAvailableUser = it

            registeredAvailableUser.removeAll { excludedUIDs.contains(it.uid) }



            contacts_list.adapter = adapter
            participant_recyclerview.adapter = horizontalAdapter

            allUsers = registeredAvailableUser

            if(registeredAvailableUser.isEmpty())
                utils.longToast(this@MultiContactChooserActivity, "No contacts available")

        }


    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if(item.itemId == R.id.action_confirm)
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
        else if(item.itemId == android.R.id.home)
            finish()
        return super.onOptionsItemSelected(item)
    }


    val adapter: RecyclerView.Adapter<ViewHolder> = object : RecyclerView.Adapter<ViewHolder>(), Filterable {
        override fun getFilter(): Filter {
            return object : Filter(){
                override fun performFiltering(p0: CharSequence?): FilterResults {

                    val query = p0?.toString().orEmpty()
                    registeredAvailableUser = allUsers.filter { it.name.contains(query, true) || it.number.contains(query)}.toMutableList()
                    return FilterResults().apply { values = registeredAvailableUser }
                }

                override fun publishResults(p0: CharSequence?, p1: FilterResults?) {
                    registeredAvailableUser = p1?.values as MutableList<Models.Contact>
                    notifyDataSetChanged()
                }

            }
        }

        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder
                = ViewHolder(layoutInflater.inflate(R.layout.item_contact_layout, p0, false))

        override fun getItemCount(): Int = registeredAvailableUser.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {

            holder.name.text = registeredAvailableUser[position].name


            val user = registeredAvailableUser[holder.adapterPosition]
            val uid = user.uid

            FirebaseUtils.loadProfilePic(this@MultiContactChooserActivity, uid, holder.pic)

            holder.checkBox.setChecked( selectedUsers.contains(user), false)

            holder.itemView.setOnClickListener {


                holder.checkBox.setChecked(!holder.checkBox.isChecked, true)
                holder.checkBox.hideOrShow()

                if(holder.checkBox.isChecked) {
                    selectedUsers.add(user)
                    horizontalAdapter.notifyItemInserted(selectedUsers.lastIndex)

                }else {
                    val index = selectedUsers.indexOf(user)
                    selectedUsers.remove(user)
                    horizontalAdapter.notifyItemRemoved(index)

                }


                if(selectedUsers.isNotEmpty())
                {
                    if(!participant_recyclerview.visible) participant_recyclerview.show()
                    participant_recyclerview.smoothScrollToPosition(selectedUsers.lastIndex)
                }



            }



        }



    }

    val horizontalAdapter = object : RecyclerView.Adapter<ParticipantHolder>() {
        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ParticipantHolder {
            return ParticipantHolder(layoutInflater.inflate(R.layout.item_grid_contact_layout, p0, false))
        }

        override fun getItemCount(): Int = selectedUsers.size

        override fun onBindViewHolder(p0: ParticipantHolder, p1: Int) {

            val user = selectedUsers[p0.adapterPosition]

            p0.name.text = utils.getNameFromNumber(this@MultiContactChooserActivity,
                user.number).trim().split(" ")[0]

            Log.d("MultiContactChooserActivity", "onBindViewHolder: $user")

            FirebaseUtils.loadProfileThumbnail(this@MultiContactChooserActivity, user.uid,
                p0.pic)

        }

    }




    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
                val name = itemView.name
                val pic = itemView.pic
                val checkBox = itemView.checkbox

                init {
                    checkBox.isEnabled = false
                }

            }

    class ParticipantHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val name = itemView.grid_name!!
        val pic = itemView.grid_pic!!

        init {
            itemView.grid_cancel_btn.visibility = View.GONE
        }
    }
}