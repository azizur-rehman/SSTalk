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
import androidx.recyclerview.widget.RecyclerView
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
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
import com.miguelcatalan.materialsearchview.MaterialSearchView
import kotlinx.android.synthetic.main.activity_forward.*
import kotlinx.android.synthetic.main.contact_screen.*
import kotlinx.android.synthetic.main.contact_screen.searchView
import kotlinx.android.synthetic.main.contact_screen.toolbar
import kotlinx.android.synthetic.main.item_conversation_ad.view.*
import kotlinx.android.synthetic.main.item_conversation_layout.view.*
import org.jetbrains.anko.doAsyncResult
import org.jetbrains.anko.onComplete
import org.jetbrains.anko.uiThread
import java.util.*
import java.util.concurrent.Future
import kotlin.collections.LinkedHashMap

class ContactsActivity : AppCompatActivity(){

    //number list has 10 digit formatted number
    var numberList:MutableList<Models.Contact> = mutableListOf()
    var totalAvailableUser:MutableList<Models.Contact> = mutableListOf()

    var isForSelection = false
    private var asyncLoader: Future<Unit>? = null

    val context = this


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.contact_screen)

        MobileAds.initialize(this, getString(R.string.admob_id))

        setSupportActionBar(toolbar)
        title = "My Contacts"

        contacts_list.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)

        isForSelection = intent.getBooleanExtra(utils.constants.KEY_IS_FOR_SELECTION, false)

        asyncLoader = doAsyncResult {

            uiThread {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                    if(ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
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
                        utils.toast(context, "No registered users")
                        return
                    }

                    totalAvailableUser.clear()

                    for (post in p0.children){
                        val userModel = post.getValue(Models.User ::class.java)

                        val number = utils.getFormattedTenDigitNumber(userModel!!.phone)
                        val uid = userModel.uid

//                        numberList.filterIndexed { index, item ->
//                            (item.number == number || item.number.contains(number) &&
//                        }

                        for((index, item) in numberList.withIndex()) {
                            if (item.number == number || item.number.contains(number)) {
                                numberList[index].uid = uid
                                if(uid!=FirebaseUtils.getUid() && !totalAvailableUser.contains(numberList[index]))
                                totalAvailableUser.add(numberList[index])
                            }

                        }

                    }

                    totalAvailableUser.sortBy { it.name }


                    contacts_list.adapter = adapter

                    if(isForSelection)
                        return

                    totalAvailableUser.add(Models.Contact("Invite Users"))
                    totalAvailableUser.add(0,Models.Contact("New Contact"))
                    totalAvailableUser.add(1,Models.Contact("New Group"))

                    adapter.notifyDataSetChanged()
                    contact_progressbar.visibility = View.GONE


                }

                override fun onCancelled(p0: DatabaseError) {
                }

            })
    }


    private var searchQuery = ""
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        menuInflater.inflate(R.menu.menu_search, menu)

        menu?.findItem(R.id.action_search)?.let { searchView.setMenuItem(it) }



        searchView.setOnQueryTextListener(object : MaterialSearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                (adapter as? Filterable)?.filter?.filter(query)
                searchQuery = query?:""
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if(searchQuery != newText)
                    (adapter as? Filterable)?.filter?.filter(newText)

                searchQuery = newText?:""
                return true
            }

        })


        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == android.R.id.home)
            finish()
        return super.onOptionsItemSelected(item)
    }



    val adapter: RecyclerView.Adapter<ViewHolder> = object : RecyclerView.Adapter<ViewHolder>(), Filterable {

        var registeredUser:List<Models.Contact> = totalAvailableUser


        override fun getFilter(): Filter = object : Filter() {

            override fun performFiltering(p0: CharSequence?): FilterResults {
                val query = p0?.toString()?:"".toLowerCase(Locale.getDefault()).trim()
                registeredUser = totalAvailableUser

                registeredUser = registeredUser.filterIndexed { index, it ->
                    it.name.toLowerCase(Locale.getDefault()).contains(query) ||
                        it.number.contains(query) ||
                            (!isForSelection && index in listOf(0,1,registeredUser.lastIndex))
                }



                return FilterResults().apply { values = registeredUser; count = registeredUser.size }

            }

            override fun publishResults(p0: CharSequence?, p1: FilterResults?) {

                if(p1?.values == null)
                    return

                registeredUser = p1.values as MutableList<Models.Contact>
                notifyDataSetChanged()
            }

        }

        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder
                = ViewHolder(layoutInflater.inflate(R.layout.item_conversation_layout, p0, false))

        override fun getItemCount(): Int = registeredUser.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {


            loadNativeAd(holder.itemView, position)
            val user = registeredUser[position]

            holder.name.text = user.name
            holder.number.text = user.number

            holder.pic.borderWidth = holder.pic.borderWidth
            holder.number.visibility = View.VISIBLE

            val uid = user.uid

            FirebaseUtils.loadProfilePic(context, uid, holder.pic)

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
                        startActivity(Intent(context, CreateGroupActivity::class.java))
                        finish()
                    }
                    // invite users
                    registeredUser.lastIndex -> utils.shareInviteText(context)

                    else -> {
                        startActivity(Intent(context, MessageActivity::class.java)
                            .putExtra(FirebaseUtils.KEY_UID, uid)
                            .putExtra(utils.constants.KEY_NAME_OR_NUMBER, user.number)
                            .putExtra(utils.constants.KEY_TARGET_TYPE, FirebaseUtils.KEY_CONVERSATION_SINGLE))
                        finish()
                    }
                }


            }

            if(isForSelection) return

            holder.pic.setPadding(0,0,0,0)

            if(position == registeredUser.lastIndex || position == 0 || position == 1) {
                holder.number.hide()
                holder.pic.borderWidth = 0
            }

            holder.pic.circleBackgroundColor = ContextCompat.getColor(context, R.color.colorPrimary)

            when(position){
                0 -> {
                    holder.pic.setImageResource(R.drawable.ic_person_add_white_padded_24dp)
                }

                1-> {
                    holder.pic.setImageResource(R.drawable.ic_group_add_white_24dp)
                }
                registeredUser.lastIndex -> {
                    holder.pic.setPadding(30,30,30,30)
                    holder.pic.circleBackgroundColor = Color.TRANSPARENT
                    holder.pic.setImageResource(android.R.drawable.ic_menu_share)
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
                    time.hide()
                    itemView.delivery_status_last_msg.hide()
                    itemView.conversation_mute_icon.hide()


                }
            }



    private var adsLoadedOnce = false
    private val ads:MutableMap< Int, UnifiedNativeAd> = LinkedHashMap()
    private fun loadNativeAd(itemView:View, position:Int){



        with(itemView){

            conversation_native_ad.hide()


            if(adsLoadedOnce)
                return



            initAd {



                if(position == utils.constants.ads_after_items || position == utils.constants.ads_after_items * 2)
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

                    ads[position] = it

                    if(position > utils.constants.ads_after_items && !adsLoadedOnce)
                        adsLoadedOnce = true
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
                    Log.d("ContactsActivity", "onAdLoaded: ")
                    onLoaded?.invoke(unifiedNativeAd)
                    super.onAdLoaded()
                }

                override fun onAdFailedToLoad(p0: Int) {
                    super.onAdFailedToLoad(p0)
                    onLoaded?.invoke(null)

                }
            })
            .build()

        adLoader.loadAd(AdRequest.Builder().addTestDevice(utils.constants.redmi_note_3_test_device_id).build())

    }


    override fun onBackPressed() {

        if(searchView.isSearchOpen)
            searchView.closeSearch()
        else
            super.onBackPressed()
    }
}