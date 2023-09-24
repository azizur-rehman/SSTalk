package com.aziz.sstalk

import android.Manifest
import android.animation.LayoutTransition
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationItem
import com.aziz.sstalk.databinding.ActivityHomeBinding
import com.aziz.sstalk.databinding.AppBarHomeBinding
import com.aziz.sstalk.databinding.ItemConversationLayoutBinding
import com.aziz.sstalk.databinding.ItemPhoneContactLayoutBinding
import com.aziz.sstalk.fragments.FragmentMyProfile
import com.aziz.sstalk.fragments.FragmentOnlineFriends
import com.aziz.sstalk.fragments.FragmentSearch
import com.aziz.sstalk.fragments.OnlineVM
import com.aziz.sstalk.models.Models
import com.aziz.sstalk.utils.*
import com.aziz.sstalk.utils.utils.STORAGE_PERMISSION_REQUEST_CODE
import com.aziz.sstalk.utils.utils.requestStoragePermission
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.android.gms.ads.*
import com.google.android.gms.ads.formats.UnifiedNativeAd
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.miguelcatalan.materialsearchview.MaterialSearchView
import de.hdodenhof.circleimageview.CircleImageView
import org.jetbrains.anko.*
import org.jetbrains.anko.design.indefiniteSnackbar
import org.jetbrains.anko.design.snackbar
import java.util.Arrays
import java.util.concurrent.Executors

class HomeActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    val context = this@HomeActivity

    var hasPermission:Boolean = false
    val id = R.drawable.contact_placeholder
    var isAnyMuted = false
    var unreadConversation = 0
    
    var rewardedAd: RewardedAd? = null

    lateinit var adapter:FirebaseRecyclerAdapter<Models.LastMessageDetail, ViewHolder>

    val selectedItemPosition:MutableList<Int> = ArrayList()
    val selectedRecipients:MutableList<String> = ArrayList()

    var actionMode:ActionMode? = null

    var isContextToolbarActive = false
    private var isOnlineFragmentLoaded = false

    lateinit var binding:ActivityHomeBinding
    lateinit var appBarBinding:AppBarHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater).apply { setContentView(root) }
        appBarBinding = binding.includeAppBar

        val testDeviceIds = listOf("69BC59B0B1C3FBEB9CB57015F2831F1D")
        val configuration = RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build()
        MobileAds.setRequestConfiguration(configuration)

        setSupportActionBar(appBarBinding.toolbar)


        if(!FirebaseUtils.isLoggedIn()){
            startActivity(Intent(context, SplashActivity::class.java))
            finish()
            return
        }


        try {
            (findViewById<ViewGroup>(R.id.app_bar_layout)).layoutTransition
                .enableTransitionType(LayoutTransition.CHANGING)
        }
        catch (e:Exception){e.printStackTrace()}


        MobileAds.initialize(this)
        loadRewardedAd()

        //storing firebase token, if updated
        FirebaseUtils.updateFCMToken()

        //check for update
        FirebaseUtils.checkForUpdate(context, false)

        val toggle = ActionBarDrawerToggle(
            this, binding.drawerLayout, appBarBinding.toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        appBarBinding.showContacts.setOnClickListener {
            startActivity(Intent(context, ContactsActivity::class.java))
        }


        appBarBinding.includeContentHome.conversationProgressbar.visibility = View.VISIBLE
        initComponents()


//        if (true && BuildConfig.DEBUG) {
//            reportFullyDrawn()
//        }
    }

    private fun initComponents(){
        appBarBinding.includeContentHome.conversationProgressbar.visibility = View.GONE
        binding.navView.setNavigationItemSelectedListener(this )

        setBottomNavigationView()

        hasPermission = utils.hasContactPermission(this) && utils.hasStoragePermission(context)

        if(!utils.hasContactPermission(this)) {
            requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), 101)
        }
        else if(!utils.hasStoragePermission(this))
            requestStoragePermission()
        else
            setAdapter()


        //setting update navigation drawer
        if(FirebaseUtils.isLoggedIn()) {

            (binding.navView.getHeaderView(0).findViewById(R.id.nav_header_title) as TextView).text = FirebaseAuth.getInstance().currentUser!!.displayName
            (binding.navView.getHeaderView(0).findViewById(R.id.nav_header_subtitle) as TextView).text = FirebaseAuth.getInstance().currentUser!!.phoneNumber
            FirebaseUtils.loadProfileThumbnail(this, FirebaseUtils.getUid(),
                binding.navView.getHeaderView(0).findViewById<CircleImageView>(R.id.drawer_profile_image_view))
        }

    }


    override fun onResume() {
        Pref.setCurrentTargetUID(this, "")
        FirebaseUtils.setMeAsOnline()
        super.onResume()
    }

    override fun onPause() {

        FirebaseUtils.setMeAsOffline()
        super.onPause()
    }


    override fun onRequestPermissionsResult(requestCode:
                                            Int, permissions: Array<out String>, grantResults: IntArray) {

        when(requestCode){
            101, STORAGE_PERMISSION_REQUEST_CODE -> {
                hasPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults.isNotEmpty()

                if(hasPermission && utils.hasStoragePermission(context))
                    //reset the adapter
                    setAdapter()
                else{
                    appBarBinding.showContacts.indefiniteSnackbar("Permission not granted. Please grant necessary permissions to continue", "Grant"){
                        requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE), 101)
                    }
                }
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }



    override fun onBackPressed() {
        when {
            binding.drawerLayout.isDrawerOpen(GravityCompat.START) -> binding.drawerLayout.closeDrawer(GravityCompat.START)

            supportFragmentManager.backStackEntryCount>0 -> {
                repeat(supportFragmentManager.backStackEntryCount) {
                    supportFragmentManager.popBackStackImmediate()
                }
                appBarBinding.bottomNavigationHome.setCurrentItem(0, false)
                isOnlineFragmentLoaded = false
                title = "Recent"

            }

            appBarBinding.searchView.isSearchOpen -> appBarBinding.searchView.closeSearch()
            else -> finish()
        }
    }


    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {

            R.id.nav_create_group -> {
                startActivity(Intent(context, CreateGroupActivity::class.java))
            }

            R.id.nav_setting -> {

                startActivity(Intent(context, SettingsActivity::class.java))
            }

            R.id.nav_share -> {
                utils.shareInviteText(context)
            }

            R.id.nav_about -> {
                startActivity(Intent(this, AboutTheDeveloperActivity::class.java))
            }

            R.id.nav_support -> {

                if(rewardedAd == null)
                {
                    appBarBinding.bottomNavigationHome.snackbar("Ad not available at the moment")
                    loadRewardedAd()
                    return false
                }

                showConfirmDialog("Support us by watching a short video"){
                    rewardedAd?.show(this) {

                    }
                }
            }

        }

        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }


    private fun setAdapter(){

        loadOnlineUsers()

        appBarBinding.includeContentHome.conversationProgressbar.visibility = View.VISIBLE

//        with(appBarBinding.includeContentHome.conversationRecycler){
//            setHasFixedSize(true)
//            setItemViewCacheSize(20)
//            setDrawingCacheEnabled(true)
//            setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH)
//        }


        val reference = FirebaseUtils.ref.lastMessage(FirebaseUtils.getUid())
            .orderByChild(FirebaseUtils.KEY_REVERSE_TIMESTAMP)
        val options = FirebaseRecyclerOptions.Builder<Models.LastMessageDetail>()
            .setQuery(reference ,Models.LastMessageDetail::class.java)
            .build()

         adapter = object : FirebaseRecyclerAdapter<Models.LastMessageDetail, ViewHolder>(options){
            override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder = ViewHolder(
                ItemConversationLayoutBinding.bind(
                layoutInflater.inflate(R.layout.item_conversation_layout, p0, false)
                )

            )

            override fun onBindViewHolder(holder: ViewHolder, position: Int, model: Models.LastMessageDetail) {

                val uid = super.getRef(position).key.toString()

                if(model.type == FirebaseUtils.KEY_CONVERSATION_GROUP) {
                    holder.name.text = model.nameOrNumber.trim()


                    FirebaseUtils.loadGroupPicThumbnail(context, uid, holder.pic)


                    if(holder.name.text.isEmpty() || utils.isGroupID(holder.name.text.toString()))
                        FirebaseUtils.setGroupName(uid, holder.name)

                    holder.onlineStatus.visibility = View.GONE
                }
                else {
                    holder.name.text = utils.getNameFromNumber(context, model.nameOrNumber)
                    FirebaseUtils.loadProfileThumbnail(this@HomeActivity, uid, holder.pic)
                    FirebaseUtils.setUserOnlineStatus(uid, holder.onlineStatus)

                }

                loadNativeAd(holder.itemView, position)


                FirebaseUtils.setMuteImageIcon(uid, holder.muteIcon)

                FirebaseUtils.setLastMessage(uid, holder.lastMessage, holder.deliveryTick)



                holder.time.visibility = View.VISIBLE

                //modifying date according to time

                holder.time.text = utils.getHeaderFormattedDate(model.timeInMillis)

                Executors.newSingleThreadExecutor().submit { FirebaseUtils.setUnreadCount(uid, holder.unreadCount, holder.name, holder.lastMessage, holder.time) }


                if(!isContextToolbarActive){
                    holder.checkbox.visibility = View.INVISIBLE
                    holder.checkbox.isChecked = false
                }

                holder.itemView.findViewById<View>(R.id.item_conversation_layout).setOnClickListener {

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

                    val unreadCount = try { holder.unreadCount.textView.text.toString().toInt() }
                    catch (e:Exception){ 0 }


                    startChat(uid, model.type, model.nameOrNumber, unreadCount)


                }



                holder.itemView.findViewById<View>(R.id.item_conversation_layout).setOnLongClickListener {

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
                        override fun onActionItemClicked(p0: ActionMode?, p1: MenuItem): Boolean {

                            when(p1.itemId){
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

                            selectedItemPosition.forEach { pos ->
                                val vh = appBarBinding.includeContentHome.conversationRecycler.findViewHolderForAdapterPosition(pos) as? ViewHolder
                                vh?.checkbox?.visibility = View.INVISIBLE
                                vh?.checkbox?.setChecked(false, true)
                            }

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

        appBarBinding.includeContentHome.recyclerBackMessage.setOnClickListener { startActivity(intentFor<ContactsActivity>()) }

        reference.addValueEventListener(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                }

                override fun onDataChange(p0: DataSnapshot) {

                    appBarBinding.includeContentHome.conversationProgressbar.visibility = View.GONE

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

                                    appBarBinding.bottomNavigationHome.setNotification(unreadConversations.size.toString().takeIf { unreadConversations.size > 0 }?:"", 0)

                                }

                                override fun onCancelled(p0: DatabaseError) {}
                            })
                    }

                    if(p0.exists()){
                        appBarBinding.includeContentHome.recyclerBackMessage.visibility = View.GONE
                    }
                    else
                        appBarBinding.includeContentHome.recyclerBackMessage.visibility = View.VISIBLE


                }
            })


        appBarBinding.includeContentHome.conversationRecycler.layoutManager = LinearLayoutManager(context)
        appBarBinding.includeContentHome.conversationRecycler.adapter = adapter
//        appBarBinding.includeContentHome.conversationRecycler.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        adapter.startListening()

        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()

                if(adapter.itemCount > 0){
                    appBarBinding.includeContentHome.conversationRecycler.scrollToPosition(0)
                }
            }
        })

        FirebaseUtils.setonDisconnectListener()
    }


    class ViewHolder(itemView: ItemConversationLayoutBinding) : RecyclerView.ViewHolder(itemView.root){

        val name = itemView.name!!
        val lastMessage = itemView.mobileNumber!!
        val pic = itemView.pic!!
        val time = itemView.messageTime!!
        val unreadCount = itemView.unreadCount!!
        val onlineStatus = itemView.onlineStatusImageview!!
        val checkbox = itemView.contactCheckbox!!
        val deliveryTick = itemView.deliveryStatusLastMsg!!
        val muteIcon = itemView.conversationMuteIcon!!

        init {
            onlineStatus.visibility = View.GONE
            checkbox.isEnabled = false
            muteIcon.visibility = View.GONE
        }

    }

    override fun onDestroy() {

        try {

            adapter.stopListening()
            FirebaseUtils.setMeAsOffline()

            ads.values.forEach { it.destroy() }
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

        supportFragmentManager.addOnBackStackChangedListener {
            if(supportFragmentManager.backStackEntryCount == 0) {
                appBarBinding.showContacts.show()
            }
            else appBarBinding.showContacts.hide()
        }

        val fragmentOnline = FragmentOnlineFriends()


        with(appBarBinding.bottomNavigationHome){
            addItem(AHBottomNavigationItem("Recent", R.drawable.ic_chat))
            addItem(AHBottomNavigationItem("Online", R.drawable.ic_online_small))
            addItem(AHBottomNavigationItem("My Profile", R.drawable.ic_person_outlined))

            accentColor = ContextCompat.getColor(context, R.color.colorPrimary)

            setUseElevation(true)
            setOnTabSelectedListener { position, _ ->

                repeat(supportFragmentManager.backStackEntryCount){supportFragmentManager.popBackStack()}


                when(position){
                    0 ->  {
                        title = "Recent"
                        repeat(supportFragmentManager.backStackEntryCount) {
                            supportFragmentManager.popBackStackImmediate()
                        }
                    }

                    1 -> {
                        supportFragmentManager.beginTransaction()
                            .replace(binding.includeAppBar.includeContentHome.homeLayoutContainer.id, fragmentOnline)
                            .addToBackStack(null)
                            .commit()
                        title = "Online contacts"
                        //set online users
                        if(onOnlineUsersLoaded == null)
                        onOnlineUsersLoaded = fragmentOnline.setOnlineListener()

                    }

                    2 -> {
                        supportFragmentManager.beginTransaction()
                            .replace(binding.includeAppBar.includeContentHome.homeLayoutContainer.id, FragmentMyProfile())
                            .addToBackStack(null).commit()
                        title = "My Profile"
                    }
                }

                return@setOnTabSelectedListener true
            }

        }




    }


    private fun setOnlineCount(count:Int) {
        try {
            appBarBinding.bottomNavigationHome.setNotification(count.toString().takeIf { count > 0 }?:"", 1)
        }
        catch (e:Exception){}
    }

    private val ads:MutableMap< Int, NativeAd> = LinkedHashMap()
    private fun loadNativeAd(itemView:View, position:Int){


        val itemBinding = ItemConversationLayoutBinding.bind(itemView)
        val adLayoutBinding = itemBinding.adLayout

        adLayoutBinding.adLayout.hide()

        if(position == utils.constants.ads_after_items || position == utils.constants.ads_after_items * 2) {
            //show ad
        }
        else{
            adLayoutBinding.adLayout.hide()
            return
        }


        if(ads.containsKey(position) && ads[position] != null){
            utils.populateNativeAdView(ads[position]!!, adLayoutBinding.adLayout)
            return
        }


        val adLoader = AdLoader.Builder(this, getString(R.string.native_ad_conversation))
            .forNativeAd { nativeAd ->
                ads[position] = nativeAd
                utils.populateNativeAdView(nativeAd, adLayoutBinding.adLayout)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(p0: LoadAdError) {
                    // Handle ad load failure
                    Log.d("HomeActivity", "onAdFailedToLoad: Native Ad "+p0.message)
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()

        adLoader.loadAd(AdRequest.Builder().build())

    }


    private lateinit var adLoader:AdLoader


    private fun loadRewardedAd(){

//        if(rewardedAd != null)
//            return

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(this,getString(R.string.rewarded_ad_unit), adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d("HomeActivity", "onAdFailedToLoad: "+adError.message)
                rewardedAd = null
            }

            override fun onAdLoaded(ad: RewardedAd) {
                Log.d("HomeActivity", "Ad was loaded.")
                rewardedAd = ad
                rewardedAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
                    override fun onAdClicked() {
                        // Called when a click is recorded for an ad.
                        Log.d("HomeActivity", "onAdClicked: ")
                    }

                    override fun onAdDismissedFullScreenContent() {
                        // Called when ad is dismissed.
                        // Set the ad reference to null so you don't show the ad a second time.
                        Log.d("HomeActivity", "onAdDismissedFullScreenContent: ")
                        rewardedAd = null
                        loadRewardedAd()
                    }

                    override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                        // Called when ad fails to show.
                        Log.d("HomeActivity", "onAdFailedToShowFullScreenContent: ")
                        rewardedAd = null
                        loadRewardedAd()
                    }

                    override fun onAdImpression() {
                        // Called when an impression is recorded for an ad.
                        Log.d("HomeActivity", "onAdImpression: ")
                    }

                    override fun onAdShowedFullScreenContent() {
                        // Called when ad is shown.
                        Log.d("HomeActivity", "onAdShowedFullScreenContent: ")
                    }
                }
            }
        })


    }




    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_search, menu)

        appBarBinding.searchView.setMenuItem(menu?.findItem(R.id.action_search))

        val searchFragment = FragmentSearch()
        var onSearched:OnSearched? = null

        searchFragment.setOnItemClickedListener(object : FragmentSearch.OnItemClicked {
            override fun onClicked() {
                Handler().postDelayed({ appBarBinding.searchView.closeSearch()},500)
            }
        }
        )



        appBarBinding.searchView.setOnSearchViewListener(object : MaterialSearchView.SearchViewListener{
            override fun onSearchViewClosed() {
                // detach search fragment
                repeat(supportFragmentManager.backStackEntryCount)
                {
                    supportFragmentManager.popBackStackImmediate()
                }
                appBarBinding.bottomNavigationHome.show()
                appBarBinding.showContacts.show()

            }

            override fun onSearchViewShown() {
                // attach search fragment
                supportFragmentManager.beginTransaction().replace(binding.includeAppBar.includeContentHome.homeLayoutContainer.id, searchFragment).addToBackStack(null).commit()
                onSearched = searchFragment.setSearchAdapter()
                appBarBinding.bottomNavigationHome.hide()
                appBarBinding.showContacts.hide()

            }

        })

        appBarBinding.searchView.setOnQueryTextListener(object : MaterialSearchView.OnQueryTextListener{
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

    var onlineUsers:List<Models.Contact> = listOf()
    private fun loadOnlineUsers(){

        ViewModelProviders.of(this)[OnlineVM::class.java]
            .getOnlineUsers(context)
            .observe(this, Observer { onlineUsers ->

               setOnlineCount(onlineUsers.size)
                this.onlineUsers = onlineUsers
                onOnlineUsersLoaded?.onLoaded(onlineUsers)

            })

    }


    var onOnlineUsersLoaded:OnlineUsersLoaded? = null
    interface OnlineUsersLoaded{
        fun onLoaded(users:List<Models.Contact>)
    }

    interface OnSearched{
        fun onSubmit(query:String)
        fun onQueryChanged(newQuery:String)
    }
}
