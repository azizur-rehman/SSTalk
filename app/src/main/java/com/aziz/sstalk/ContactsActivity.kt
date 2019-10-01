package com.aziz.sstalk

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.aziz.sstalk.models.Models
import com.aziz.sstalk.utils.FirebaseUtils
import com.aziz.sstalk.utils.hide
import com.aziz.sstalk.utils.show
import com.aziz.sstalk.utils.utils
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.formats.UnifiedNativeAd
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.contact_screen.*
import kotlinx.android.synthetic.main.item_conversation_layout.view.*
import kotlinx.android.synthetic.main.item_conversation_native_ad.view.*
import org.jetbrains.anko.doAsyncResult
import org.jetbrains.anko.onComplete
import org.jetbrains.anko.uiThread
import java.util.concurrent.Future

class ContactsActivity : AppCompatActivity(){

    //number list has 10 digit formatted number
    var numberList:MutableList<Models.Contact> = mutableListOf()
    var registeredAvailableUser:MutableList<Models.Contact> = mutableListOf()

    var isForSelection = false
    private var asyncLoader: Future<Unit>? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.contact_screen)

        MobileAds.initialize(this, getString(R.string.admob_id))

        title = "My Contacts"

        contacts_list.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this@ContactsActivity)

        isForSelection = intent.getBooleanExtra(utils.constants.KEY_IS_FOR_SELECTION, false)

        asyncLoader = doAsyncResult {

            uiThread {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                    if(ActivityCompat.checkSelfPermission(this@ContactsActivity, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
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


    private fun loadRegisteredUsers(){




        numberList = utils.getContactList(this)

        FirebaseUtils.ref.allUser()
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


                        for((index, item) in numberList.withIndex()) {
                            if (item.number == number || item.number.contains(number)) {
                                numberList[index].uid = uid
                                if(uid!=FirebaseUtils.getUid() && !registeredAvailableUser.contains(numberList[index]))
                                registeredAvailableUser.add(numberList[index])
                            }

                        }

                    }

                    registeredAvailableUser.sortBy { it.name }

                    contacts_list.adapter = adapter

                    if(isForSelection)
                        return

                    registeredAvailableUser.add(Models.Contact("Invite Users"))
                    registeredAvailableUser.add(0,Models.Contact("New Contact"))
                    registeredAvailableUser.add(1,Models.Contact("New Group"))

                    adapter.notifyDataSetChanged()
                    contact_progressbar.visibility = View.GONE


                }

                override fun onCancelled(p0: DatabaseError) {
                }

            })
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        finish()
        return super.onOptionsItemSelected(item)
    }


    val adapter = object : RecyclerView.Adapter<ViewHolder>() {

        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder
                = ViewHolder(layoutInflater.inflate(R.layout.item_conversation_layout, p0, false))

        override fun getItemCount(): Int = registeredAvailableUser.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {


            loadNativeAd(holder.itemView, position)

            holder.name.text = registeredAvailableUser[position].name
            holder.number.text = registeredAvailableUser[position].number

            holder.pic.borderWidth = holder.pic.borderWidth
            holder.number.visibility = View.VISIBLE

            val uid = registeredAvailableUser.get(index = position).uid

            FirebaseUtils.loadProfilePic(this@ContactsActivity, uid, holder.pic)
            holder.pic.setPadding(0,0,0,0)

            if(position == registeredAvailableUser.size - 1 || position == 0 || position == 1) {
                holder.number.visibility = View.GONE
                holder.pic.borderWidth = 0
            }

            when(position){
                0 -> {
                    holder.pic.setImageResource(R.drawable.ic_person_add_white_padded_24dp)
                    holder.pic.circleBackgroundColor = ContextCompat.getColor(this@ContactsActivity, R.color.colorPrimary)
                }

                1-> {
                    holder.pic.setImageResource(R.drawable.ic_group_add_white_24dp)
                    holder.pic.circleBackgroundColor = ContextCompat.getColor(this@ContactsActivity, R.color.colorPrimary)
                }
                registeredAvailableUser.lastIndex -> {
                    holder.pic.setPadding(30,30,30,30)
                    holder.pic.circleBackgroundColor = Color.TRANSPARENT
                    holder.pic.setImageResource(android.R.drawable.ic_menu_share)
                }
            }


            holder.itemView.setOnClickListener {

                if(isForSelection){
                    setResult(Activity.RESULT_OK, intent.apply {
                        putExtra(FirebaseUtils.KEY_UID, uid )
                    })
                    finish()
                    return@setOnClickListener
                }



                when (position) {
                    0 -> {
                        // new contact
                        val contactIntent = Intent(Intent.ACTION_INSERT)
                        contactIntent.type = ContactsContract.RawContacts.CONTENT_TYPE
                        startActivityForResult(contactIntent,1024)
                    }
                    1 -> {
                        //new group
                        startActivity(Intent(this@ContactsActivity, CreateGroupActivity::class.java))
                        finish()
                    }
                    registeredAvailableUser.lastIndex -> utils.shareInviteText(this@ContactsActivity)
                    
                    else -> {
                        startActivity(Intent(this@ContactsActivity, MessageActivity::class.java)
                            .putExtra(FirebaseUtils.KEY_UID, uid)
                            .putExtra(utils.constants.KEY_NAME_OR_NUMBER, registeredAvailableUser[position].number)
                            .putExtra(utils.constants.KEY_TARGET_TYPE, FirebaseUtils.KEY_CONVERSATION_SINGLE))
                        finish()
                    }
                }


            }


        }



    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if(resultCode == Activity.RESULT_OK) {
            Log.d("ContactsActivity", "onActivityResult: ")
            //refresh list
            loadRegisteredUsers()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
                val name = itemView.name
                val number = itemView.mobile_number
                val pic = itemView.pic
                private val time = itemView.messageTime!!

                init {
                    time.visibility = View.GONE
                    itemView.delivery_status_last_msg.visibility = View.GONE
                    itemView.conversation_mute_icon.visibility = View.GONE


                }
            }



    private fun loadNativeAd(itemView:View, position:Int){



        with(itemView){

            conversation_native_ad.hide()




            initAd {

                if(position == utils.constants.ads_after_items || position == utils.constants.ads_after_items + utils.constants.ads_after_items)
                    conversation_native_ad.show()
                else{
                    conversation_native_ad.hide()
                    return@initAd
                }
                it?.let {


                    conversation_native_ad.iconView = itemView.pic

                    itemView.ad_name.text = it.headline
                    itemView.ad_side_text.text = it.advertiser
                    itemView.ad_subtitle.text = it.body

                    if(it.icon != null)
                        itemView.ad_pic.setImageDrawable(it.icon.drawable)

                    if(it.starRating != null)
                        itemView.ad_rating.rating = it.starRating.toFloat()
                    else
                        itemView.ad_rating.hide()

                    with(itemView){
                        ad_call_to_action.text = it.callToAction

                        conversation_native_ad.callToActionView = ad_call_to_action
                        conversation_native_ad.bodyView = ad_subtitle
                        conversation_native_ad.headlineView = ad_name
                        conversation_native_ad.advertiserView = ad_side_text
                        conversation_native_ad.iconView = ad_pic
                    }


                    it.enableCustomClickGesture()

                    conversation_native_ad.setNativeAd(it)

                }
                if(it == null)
                    conversation_native_ad.hide()
            }

        }


    }

    private lateinit var adLoader: AdLoader
    private fun initAd(onLoaded: ((unifiedNativeAd: UnifiedNativeAd?) -> Unit)? = null){

        var unifiedNativeAd: UnifiedNativeAd? = null

        adLoader = AdLoader.Builder(this, getString(R.string.native_ad_conversation))
            .forUnifiedNativeAd {
                unifiedNativeAd = it
                onLoaded?.invoke(it)
            }
            .withAdListener(object : AdListener() {

                override fun onAdLoaded() {

                    onLoaded?.invoke(unifiedNativeAd)
                    super.onAdLoaded()
                }

                override fun onAdFailedToLoad(p0: Int) {
                    super.onAdFailedToLoad(p0)
                    Log.d("HomeActivity", "onAdFailedToLoad: code = $p0")
                    onLoaded?.invoke(null)

                }
            })
            .build()

        adLoader.loadAds(AdRequest.Builder().addTestDevice(utils.constants.redmi_note_3_test_device_id).build(), 5)

    }
}