package com.aziz.sstalk

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import com.google.android.material.navigation.NavigationView
import androidx.core.view.GravityCompat
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.util.Log
import android.view.*
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationItem
import com.aziz.sstalk.fragments.FragmentOnlineFriends
import com.aziz.sstalk.fragments.FragmentSearch
import com.aziz.sstalk.models.Models
import com.aziz.sstalk.utils.*
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.formats.UnifiedNativeAd
import com.google.android.gms.ads.reward.RewardItem
import com.google.android.gms.ads.reward.RewardedVideoAd
import com.google.android.gms.ads.reward.RewardedVideoAdListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.miguelcatalan.materialsearchview.MaterialSearchView
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.app_bar_home.*
import kotlinx.android.synthetic.main.content_home.*
import kotlinx.android.synthetic.main.content_home.recycler_back_message
import kotlinx.android.synthetic.main.item_conversation_ad.view.*
import kotlinx.android.synthetic.main.item_conversation_layout.view.*
import kotlinx.android.synthetic.main.layout_recycler_view.*
import org.jetbrains.anko.*
import org.jetbrains.anko.design.indefiniteSnackbar
import org.jetbrains.anko.design.snackbar
import java.lang.Exception
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.collections.ArrayList

class HomeActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    val context = this@HomeActivity

    var hasPermission:Boolean = false
    val id = R.drawable.contact_placeholder
    var isAnyMuted = false
    var unreadConversation = 0
    
    lateinit var rewardedVideoAd:RewardedVideoAd

    lateinit var adapter:FirebaseRecyclerAdapter<Models.LastMessageDetail, ViewHolder>

    val selectedItemPosition:MutableList<Int> = ArrayList()
    val selectedRecipients:MutableList<String> = ArrayList()

    var actionMode:ActionMode? = null

    var isContextToolbarActive = false
    private var asyncLoader: Future<Boolean>? = null
    private var isOnlineFragmentLoaded = false
    private val fragmentOnline = FragmentOnlineFriends()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        setSupportActionBar(toolbar)


        if(!FirebaseUtils.isLoggedIn()){
            startActivity(Intent(context, SplashActivity::class.java))
            finish()
            return
        }

        MobileAds.initialize(this, getString(R.string.admob_id))
        loadRewardedAd()

        //storing firebase token, if updated
        FirebaseUtils.updateFCMToken()

        //check for update
        FirebaseUtils.checkForUpdate(context, false)

        val toggle = ActionBarDrawerToggle(
            this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        show_contacts.setOnClickListener {

            startActivity(Intent(context, ContactsActivity::class.java))
        }


        conversation_progressbar.visibility = View.VISIBLE
        initComponents()

    }

    private fun initComponents(){
        conversation_progressbar.visibility = View.GONE
        asyncLoader = doAsyncResult {

            onComplete {  }

            activityUiThread {
                nav_view.setNavigationItemSelectedListener(this@HomeActivity )

                setBottomNavigationView()

                hasPermission = utils.hasContactPermission(this@HomeActivity) && utils.hasStoragePermission(context)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if(!hasPermission) {
                        requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE), 101)
                    }
                    else
                        setAdapter()
                }
                else
                    setAdapter()



                //setting update navigation drawer
                if(FirebaseUtils.isLoggedIn()) {

                    (nav_view.getHeaderView(0).findViewById(R.id.nav_header_title) as TextView).text = FirebaseAuth.getInstance().currentUser!!.displayName
                    (nav_view.getHeaderView(0).findViewById(R.id.nav_header_subtitle) as TextView).text = FirebaseAuth.getInstance().currentUser!!.phoneNumber
                    FirebaseUtils.loadProfileThumbnail(this@HomeActivity, FirebaseUtils.getUid(),
                        nav_view.getHeaderView(0).findViewById<CircleImageView>(R.id.drawer_profile_image_view))
                }
            }
        }
    }


    override fun onResume() {
        Pref.setCurrentTargetUID(this, "")
        FirebaseUtils.setMeAsOnline()
        super.onResume()
    }

    override fun onPause() {

        if(utils.isAppIsInBackground(this))
            FirebaseUtils.setMeAsOffline()
        super.onPause()
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {

        when(requestCode){
            101 -> {
                hasPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults.isNotEmpty()

                if(hasPermission && utils.hasStoragePermission(context))
                    //reset the adapter
                    setAdapter()
                else{
                    show_contacts.indefiniteSnackbar("Permission not granted. Please grant necessary permissions to continue", "Grant"){
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE), 101)
                        }
                    }
                }
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }



    override fun onBackPressed() {
        when {
            drawer_layout.isDrawerOpen(GravityCompat.START) -> drawer_layout.closeDrawer(GravityCompat.START)
            isOnlineFragmentLoaded -> {
                supportFragmentManager.beginTransaction()
                    .remove(fragmentOnline)
                    .commit()
                bottom_navigation_home.setCurrentItem(0, false)
                isOnlineFragmentLoaded = false
                title = "Recent"
            }
            searchView.isSearchOpen -> searchView.closeSearch()
            else -> finish()
        }
    }


    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {

            R.id.nav_create_group -> {
                startActivity(Intent(context, CreateGroupActivity::class.java))
            }

            R.id.nav_my_profile -> {

                startActivity(Intent(context, EditProfile::class.java))
            }
            R.id.nav_setting -> {

                startActivity(Intent(context, SettingsActivity::class.java))
            }

            R.id.nav_share -> {
                utils.shareInviteText(context)
            }

            R.id.nav_about -> {
                startActivity(Intent(this@HomeActivity, AboutTheDeveloperActivity::class.java))
            }

            R.id.nav_support -> {

                if(!rewardedVideoAd.isLoaded)
                {
                    bottom_navigation_home.snackbar("Ad not available at the moment","Try again"){
                        loadRewardedAd()
                    }
                    return false
                }

                showConfirmDialog("Support us by watching a short video"){
                    rewardedVideoAd.show()
                }
            }

        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }


    private fun setAdapter(){

        conversation_progressbar.visibility = View.VISIBLE

        with(conversationRecycler){
            setHasFixedSize(true)
            setItemViewCacheSize(20)
            setDrawingCacheEnabled(true)
            setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH)
        }


        val reference = FirebaseUtils.ref.lastMessage(FirebaseUtils.getUid())
            .orderByChild(FirebaseUtils.KEY_REVERSE_TIMESTAMP)
        val options = FirebaseRecyclerOptions.Builder<Models.LastMessageDetail>()
            .setQuery(reference ,Models.LastMessageDetail::class.java)
            .build()

         adapter = object : FirebaseRecyclerAdapter<Models.LastMessageDetail, ViewHolder>(options){
            override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder = ViewHolder(layoutInflater.inflate(R.layout.item_conversation_layout, p0, false))

            override fun onBindViewHolder(holder: ViewHolder, position: Int, model: Models.LastMessageDetail) {

                val uid = super.getRef(position).key.toString()

                if(model.type == FirebaseUtils.KEY_CONVERSATION_GROUP) {
                    holder.name.text = model.nameOrNumber.trim()


                    FirebaseUtils.loadGroupPic(context, uid, holder.pic)


                    if(holder.name.text.isEmpty() || utils.isGroupID(holder.name.text.toString()))
                        FirebaseUtils.setGroupName(uid, holder.name)

                    holder.onlineStatus.visibility = View.GONE
                }
                else {
                    holder.name.text = utils.getNameFromNumber(context, model.nameOrNumber)
                    FirebaseUtils.loadProfilePic(this@HomeActivity, uid, holder.pic)
                    FirebaseUtils.setUserOnlineStatus(uid, holder.onlineStatus)

                }

                loadNativeAd(holder.itemView, position)


                Executors.newSingleThreadExecutor().submit { FirebaseUtils.setMuteImageIcon(uid, holder.muteIcon) }

                Executors.newSingleThreadExecutor().submit { FirebaseUtils.setLastMessage(uid, holder.lastMessage, holder.deliveryTick) }



                holder.time.visibility = View.VISIBLE

                //modifying date according to time

                holder.time.text = utils.getHeaderFormattedDate(model.timeInMillis)

                Executors.newSingleThreadExecutor().submit { FirebaseUtils.setUnreadCount(uid, holder.unreadCount, holder.name, holder.lastMessage, holder.time) }


                if(!isContextToolbarActive){
                    holder.checkbox.visibility = View.INVISIBLE
                    holder.checkbox.isChecked = false
                }

                holder.itemView.item_conversation_layout.setOnClickListener {

                    if(isContextToolbarActive){

                        if(!selectedItemPosition.contains(position))
                        {
                            holder.checkbox.visibility = View.VISIBLE
                            holder.checkbox.isChecked = true
                            selectedItemPosition.add(position)
                            selectedRecipients.add(uid)
                        }
                        else{
                            holder.checkbox.visibility = View.INVISIBLE
                            holder.checkbox.isChecked = false
                            selectedItemPosition.remove(position)
                            selectedRecipients.remove(uid)
                        }

                        if(selectedItemPosition.size==2)
                            actionMode?.invalidate()

                        actionMode?.title = selectedItemPosition.size.toString()
                        if(selectedItemPosition.isEmpty() && actionMode!=null)
                            actionMode?.finish()

                        return@setOnClickListener
                    }

                    val unreadCount = try { holder.unreadCount.getTextView().text.toString().toInt() }
                    catch (e:Exception){ 0 }


                    startChat(uid, model.type, model.nameOrNumber, unreadCount)


                }



                holder.itemView.item_conversation_layout.setOnLongClickListener {

                    if(isContextToolbarActive)
                        return@setOnLongClickListener false

                    if(!selectedItemPosition.contains(position))
                    {
                        selectedItemPosition.add(position)
                        selectedRecipients.add(uid)
                    }


                    checkIfAnyMuted(adapter.getRef(position).key!!)

                    holder.checkbox.visibility = View.VISIBLE
                    holder.checkbox.isChecked = true

                    actionMode = startSupportActionMode(object : ActionMode.Callback {
                        override fun onActionItemClicked(p0: ActionMode?, p1: MenuItem?): Boolean {

                            when(p1?.itemId){
                                R.id.action_delete_conversation -> {
                                    Log.d("HomeActivity", "onActionItemClicked: deleting pos = $selectedItemPosition")
                                    deleteSelectedConversations(selectedItemPosition.toMutableList())
                                }

                                R.id.action_mute -> {
                                    muteSelectedConversation()
                                }

                                R.id.action_mark_as_read -> {
                                    markAllAsRead(selectedItemPosition.toMutableList())

                                }
                            }

                            p0?.finish()
                            return true
                        }

                        override fun onCreateActionMode(p0: ActionMode?, p1: Menu?): Boolean {

                            p0?.menuInflater?.inflate(R.menu.converstation_option_menu, p1)
                            isContextToolbarActive = true


                            return true
                        }

                        override fun onPrepareActionMode(p0: ActionMode?, p1: Menu?): Boolean {
                             p1?.findItem(R.id.action_mute)?.isVisible = selectedItemPosition.size == 1
                            p0?.title = selectedItemPosition.size.toString()

                            return true
                        }

                        override fun onDestroyActionMode(p0: ActionMode?) {
                            isContextToolbarActive = false

                            Log.d("HomeActivity", "onDestroyActionMode: $selectedItemPosition")

                            for(pos in selectedItemPosition)
                                adapter.notifyItemChanged(pos)

                            selectedItemPosition.clear()
                            selectedRecipients.clear()
                            isAnyMuted = false

                        }

                    })
                    actionMode?.title = selectedItemPosition.size.toString()

                    true
                }




            }

        }



        val unreadConversations:MutableList<String> = ArrayList()

        recycler_back_message.setOnClickListener { startActivity(intentFor<ContactsActivity>()) }

        reference.addValueEventListener(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                }

                override fun onDataChange(p0: DataSnapshot) {

                    conversation_progressbar.visibility = View.GONE

                    p0.children.forEach {
                        val uid = it.key?:""

                        FirebaseUtils.ref.allMessageStatus(FirebaseUtils.getUid(), uid)
                            .orderByChild("read").equalTo(false)
                            .addValueEventListener(object : ValueEventListener{
                                override fun onDataChange(p0: DataSnapshot) {
                                    if(p0.childrenCount > 0 ) {
                                        if(!unreadConversations.contains(uid) && uid.isNotEmpty())
                                            unreadConversations.add(uid)
                                    }
                                    else unreadConversations.remove(uid)

                                    bottom_navigation_home.setNotification(unreadConversations.size.toString().takeIf { unreadConversations.size > 0 }?:"", 0)

                                }

                                override fun onCancelled(p0: DatabaseError) {}
                            })
                    }

                    if(p0.exists()){
                        recycler_back_message.visibility = View.GONE
                    }
                    else
                        recycler_back_message.visibility = View.VISIBLE


                }
            })


        conversationRecycler.layoutManager = LinearLayoutManager(context)
        conversationRecycler.adapter = adapter
//        conversationRecycler.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        adapter.startListening()

        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()

                if(adapter.itemCount > 0){
                    recyclerView.scrollToPosition(0)
                }
            }
        })

        FirebaseUtils.setonDisconnectListener()
    }





    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val name = itemView.name!!
        val lastMessage = itemView.mobile_number!!
        val pic = itemView.pic!!
        val time = itemView.messageTime!!
        val unreadCount = itemView.unreadCount!!
        val onlineStatus = itemView.online_status_imageview!!
        val checkbox = itemView.contact_checkbox!!
        val deliveryTick = itemView.delivery_status_last_msg!!
        val muteIcon = itemView.conversation_mute_icon!!

        init {
            onlineStatus.visibility = View.GONE
            checkbox.isEnabled = false
            muteIcon.visibility = View.GONE
        }

    }

    override fun onDestroy() {

        try {
            if(asyncLoader?.isDone!!)
                asyncLoader?.cancel(true)


            adapter.stopListening()
            FirebaseUtils.setMeAsOffline()
        }
        catch (e:Exception) {}

        super.onDestroy()
    }




    private fun deleteSelectedConversations(itemPositions:MutableList<Int>) {


        AlertDialog.Builder(context)
            .setMessage("Delete these conversation(s)?")
            .setPositiveButton("Yes") { _, _ ->

                itemPositions.forEachIndexed { index, i ->
                    val conversationRef = adapter.getRef(i)
                    val targetUID = conversationRef.key
                    //delete conversation from reference
                     conversationRef.removeValue().addOnSuccessListener {

                         //delete messages after successful conversation deletion
                         FirebaseUtils.ref.getChatRef(FirebaseUtils.getUid(),
                             targetUID!!)
                             .removeValue()


                         if(index == itemPositions.lastIndex)
                             utils.toast(context, "${itemPositions.size} Conversation(s) deleted")

                     } }  }
            .setNegativeButton("No", null)
            .show()

    }


    private fun muteSelectedConversation(){
        selectedItemPosition.forEach {
            FirebaseUtils.ref.notificationMute(adapter.getRef(it).key!!)
                .setValue(true)
        }
    }


    private fun markAllAsRead(itemPositions:MutableList<Int>){
        itemPositions.forEach {
            FirebaseUtils.ref.allMessageStatus( FirebaseUtils.getUid(),
                adapter.getRef(it).key!!)
                .orderByChild("read").equalTo(false)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onCancelled(p0: DatabaseError) {
                    }

                    override fun onDataChange(p0: DataSnapshot) {
                        if(!p0.exists())
                            return

                        for(snapshot in p0.children){
                            snapshot.ref.child("read").setValue(true)
                        }
                    }
                })
        }
    }


    private fun checkIfAnyMuted(targetUID:String){

        //set switch initial value
        FirebaseUtils.ref.notificationMute(targetUID)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                }

                override fun onDataChange(p0: DataSnapshot) {
                    if(!p0.exists()) {
                        isAnyMuted = false
                        return
                    }
                    isAnyMuted = p0.getValue(Boolean::class.java)!!
                }
            })
    }




    private fun setBottomNavigationView(){



        title = "Recent"


        with(bottom_navigation_home){
            addItem(AHBottomNavigationItem("Recent", R.drawable.ic_chat))
            addItem(AHBottomNavigationItem("Online", R.drawable.ic_person_outlined))
            accentColor = ContextCompat.getColor(context, R.color.colorPrimary)

            setUseElevation(true)
            setOnTabSelectedListener { position, wasSelected ->
                when(position){
                    0 ->  {
                        title = "Recent"
                        supportFragmentManager.beginTransaction()
                            .remove( fragmentOnline)
                            .commit()
                        isOnlineFragmentLoaded = false
                    }

                    1 -> {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.homeLayoutContainer, fragmentOnline)
                            .commit()
                        isOnlineFragmentLoaded = true
                        title = "Online contacts"
                    }
                }

                return@setOnTabSelectedListener true
            }

        }




    }


    fun setOnlineCount(count:Int) {
        try {
            bottom_navigation_home.setNotification(count.toString().takeIf { count > 0 }?:"", 1)
        }
        catch (e:Exception){}
    }

    private fun loadNativeAd(itemView:View, position:Int){



        with(itemView){

            conversation_native_ad.hide()


            initAd {

                
                it?.let {

                    when (position) {
                        utils.constants.ads_after_items, utils.constants.ads_after_items * 2 -> {

                        }
                        else -> {
                            conversation_native_ad.hide()
                            return@initAd
                        }
                    }


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
                    conversation_native_ad.show()

                conversation_native_ad.setNativeAd(it)

                }
        }

    }


}

    private lateinit var adLoader:AdLoader
    private fun initAd(onLoaded: ((unifiedNativeAd:UnifiedNativeAd?) -> Unit)? = null){

        var unifiedNativeAd:UnifiedNativeAd? = null

        adLoader = AdLoader.Builder(this, getString(R.string.native_ad_conversation))
            .forUnifiedNativeAd {
                unifiedNativeAd = it
                onLoaded?.invoke(it)
            }
            .withAdListener(object : AdListener() {

                override fun onAdLoaded() {
                    Log.d("HomeActivity", "onAdLoaded: ")
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
    

    private fun loadRewardedAd(){

        rewardedVideoAd = MobileAds.getRewardedVideoAdInstance(this)
        rewardedVideoAd.rewardedVideoAdListener = object : RewardedVideoAdListener{
            override fun onRewardedVideoAdClosed() {}

            override fun onRewardedVideoAdLeftApplication() {
            }

            override fun onRewardedVideoAdLoaded() {
            }

            override fun onRewardedVideoAdOpened() {
            }

            override fun onRewardedVideoCompleted() {
            }

            override fun onRewarded(p0: RewardItem?) {
                bottom_navigation_home.snackbar("Thank you for your support")
            }

            override fun onRewardedVideoStarted() {
            }

            override fun onRewardedVideoAdFailedToLoad(p0: Int) {
            }

        }
        rewardedVideoAd.loadAd(getString(R.string.rewarded_ad_unit), AdRequest.Builder()
            .addTestDevice(utils.constants.redmi_note_3_test_device_id).build())
    }




    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_search, menu)

        searchView.setMenuItem(menu?.findItem(R.id.action_search))

        val searchFragment = FragmentSearch()
        var onSearched:OnSearched? = null

        searchFragment.setOnItemClickedListener(object : FragmentSearch.OnItemClicked {
            override fun onClicked() {
                Handler().postDelayed({ searchView.closeSearch()},500)
            }
        }
        )



        searchView.setOnSearchViewListener(object : MaterialSearchView.SearchViewListener{
            override fun onSearchViewClosed() {
                // detach search fragment
                repeat(supportFragmentManager.backStackEntryCount)
                {
                    supportFragmentManager.popBackStackImmediate()
                }
                bottom_navigation_home.show()
                show_contacts.show()

            }

            override fun onSearchViewShown() {
                // attach search fragment
                supportFragmentManager.beginTransaction().replace(R.id.homeLayoutContainer, searchFragment).addToBackStack(null).commit()
                onSearched = searchFragment.setSearchAdapter()
                bottom_navigation_home.hide()
                show_contacts.hide()

            }

        })

        searchView.setOnQueryTextListener(object : MaterialSearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                onSearched?.onSubmit(query.orEmpty())
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                onSearched?.onQueryChanged(newText.orEmpty())
                return newText.isNullOrEmpty()
            }

        })

        return super.onCreateOptionsMenu(menu)
    }

    interface OnSearched{
        fun onSubmit(query:String)
        fun onQueryChanged(newQuery:String)
    }
}
