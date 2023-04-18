package com.aziz.sstalk

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import com.google.android.material.snackbar.Snackbar
import androidx.recyclerview.widget.RecyclerView
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.work.*
import com.aziz.sstalk.firebase.ForwardWorker
import com.aziz.sstalk.firebase.UploadWorker
import com.aziz.sstalk.models.Models
import com.aziz.sstalk.utils.*
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.miguelcatalan.materialsearchview.MaterialSearchView
import kotlinx.android.synthetic.main.activity_forward.*
import kotlinx.android.synthetic.main.item_contact_layout.view.*
import me.shaohui.advancedluban.Luban
import me.shaohui.advancedluban.OnCompressListener
import org.jetbrains.anko.*
import org.jetbrains.anko.collections.forEachWithIndex
import java.io.File
import java.lang.Exception
import java.util.*
import java.util.concurrent.Future
import kotlin.collections.ArrayList

class ForwardActivity : AppCompatActivity() {

    private val selectedUIDs:MutableList<String> = ArrayList()
    private val selectedTitles:MutableList<String> = ArrayList()
    private val selectedNumbers:MutableList<String> = ArrayList()

    val allFrequentUIDs:MutableList<String> = ArrayList()

    var isImageFromIntent = false
    var isVideoFromIntent = false
    var isTextFromIntent = false

    val allFrequentConverstation:MutableList<Models.LastMessageDetail> = ArrayList()

    //number list has 10 digit formatted number
    var numberList:MutableList<Models.Contact> = mutableListOf()
    var registeredAvailableUser:MutableList<Models.Contact> = mutableListOf()


    private lateinit var allAvailableUsers:MutableList<Models.Contact>


    var nameOfRecipient :String = ""

    private var allContactAdapter: RecyclerView.Adapter<ViewHolder>? = null

    val context = this@ForwardActivity
    private var myUID: String = ""

    var fwd_snackbar: Snackbar? = null

    var messageModels: MutableList<Models.MessageModel> = mutableListOf()

    var progressDialog:ProgressDialog? = null

    var currentMessageID = ""
    private var asyncLoader: Future<Unit>? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forward)

        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if(!FirebaseUtils.isLoggedIn()){
            startActivity(packageManager.getLaunchIntentForPackage(packageName))
            finish()
            return
        }


        fwd_snackbar = Snackbar.make(sendBtn, "", Snackbar.LENGTH_INDEFINITE)

        caption_layout.visibility = View.GONE

        recyclerLayout.visibility = View.GONE

        setFrequentAdapter()
        recyclerLayout.visibility = View.VISIBLE

        myUID = FirebaseUtils.getUid()

        messageModels = intent.getSerializableExtra(utils.constants.KEY_MSG_MODEL) as? MutableList<Models.MessageModel>?: mutableListOf()


        if(messageModels.isNullOrEmpty())
        {
            messageModels = mutableListOf()
            handleIncomingIntents(intent)
        }


        sendBtn?.setOnClickListener {



                if (isImageFromIntent) {
                    //image is sent via intent

                    if(bitmap!=null){
                        val messageID = "IMG_${System.currentTimeMillis()}"
                        val currentFile = utils.saveBitmapToSent(context, bitmap!!, messageID)
                        Luban.compress(context, File(currentFile))
                            .putGear(Luban.THIRD_GEAR)
                            .launch(object : OnCompressListener{
                                override fun onSuccess(file: File?) {
                                    uploadAndForward(messageID, file!!, File(currentFile),
                                        utils.constants.FILE_TYPE_IMAGE)
                                }

                                override fun onError(e: Throwable?) {
                                    uploadAndForward(messageID, File(currentFile), File(currentFile),
                                        utils.constants.FILE_TYPE_IMAGE)
                                    Log.d(
                                        "ForwardActivity",
                                        "onError: failed to compress original image, uploading original image"
                                    )
                                }

                                override fun onStart() {
                                    progressDialog?.setMessage("Please wait...")
                                    progressDialog?.show()
                                }

                            })

                    }
                    else{
                        utils.toast(context, "Image might be corrupted")
                        finish()
                    }

                }

                else if(isVideoFromIntent){

                    if(currentVideoFile!=null){
                        val messageID = "VID_${System.currentTimeMillis()}"
                        progressDialog?.setMessage("Please wait...")
                        progressDialog?.show()
                        uploadAndForward(messageID, currentVideoFile!!, currentVideoFile!!,
                            utils.constants.FILE_TYPE_VIDEO)
                    }

                }

                else{
                    onForwardToSelectedUIDs()
                }


        }
    }


    private fun onForwardToSelectedUIDs() {



        val forwardRequests:MutableList<OneTimeWorkRequest> = mutableListOf()



        selectedUIDs.forEachWithIndex { i, targetUID->

            var messageID: String


            messageModels.forEach {model ->

                messageID = "MSG${System.currentTimeMillis()}"

            val replacement = when(model.messageType){
                utils.constants.FILE_TYPE_IMAGE -> "IMG_"
                utils.constants.FILE_TYPE_VIDEO -> "VID_"
                utils.constants.FILE_TYPE_AUDIO -> "AUD_"

                utils.constants.FILE_TYPE_LOCATION -> "LOC_"
                else -> "MSG"

            }

            messageID = messageID.replace("MSG", replacement)

            model.from = myUID
            model.timeInMillis = System.currentTimeMillis()
            model.reverseTimeStamp = model.timeInMillis * -1
            model.to = targetUID
            model.caption = captionEditText.text.toString()

            if(caption_layout.visibility == View.VISIBLE && captionEditText.text.isNotEmpty())
                model.caption = captionEditText.text.toString()


            val inputData = workDataOf(
                msg_id to messageID,
                msg_model to model.convertToJsonString(),
                target_uid to targetUID,
                key_nameOrNumber to selectedNumbers.getOrNull(i)
            )
            val request = OneTimeWorkRequestBuilder<ForwardWorker>()
                .setInputData(inputData)
                .build()

            forwardRequests.add(request)

                Log.d("ForwardActivity", "onForwardToSelectedUIDs: ${model.message} $messageID to $targetUID")

        }
    }



        WorkManager.getInstance().enqueue(forwardRequests)

            startActivity(Intent(context, HomeActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))

        finish()
    }


    var bitmap:Bitmap? = null
    var currentVideoFile:File? = null

    private fun handleIncomingIntents(intent: Intent){

        progressDialog = ProgressDialog(this)
        progressDialog?.setCancelable(false)

        caption_layout.visibility = View.VISIBLE

        if(intent.action == Intent.ACTION_SEND){
            when {
                intent.type == "text/plain" -> {
                    val text = intent.getStringExtra(Intent.EXTRA_TEXT)?:return
                    messageModels.add(Models.MessageModel(text ))
                    isTextFromIntent = true
                    caption_layout?.visibility = View.GONE
                }
                intent.type?.startsWith( "image/")?:false -> {

                    if(!utils.hasStoragePermission(this)) {
                        utils.toast(this, "App does not have storage permission")
                        finish()
                        return
                    }
                    val imageURI = intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as Uri
                    bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageURI)
                    isImageFromIntent = true
                    caption_layout.visibility = View.VISIBLE

                    preview.setImageBitmap(bitmap)

                }
                intent.type?.startsWith("video/")?:false -> {
                    if(!utils.hasStoragePermission(this)){
                        utils.toast(this, "App does not have storage permission")
                        finish()
                        return
                    }

                    play_icon.visibility = View.VISIBLE
                    videoLength.visibility = View.VISIBLE

                    try{
                         val videoUri = intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as Uri
                    Log.d("ForwardActivity", "handleIncomingIntents: initial video path = ${videoUri.path}")
                    val videoFile =  File(utils.getRealPathFromURI(this, videoUri))
                    Log.d("ForwardActivity", "handleIncomingIntents: real video path = ${videoFile.path}")


                    if(!videoFile.exists()){
                        utils.toast(context, "Something went wrong in video file")
                        finish()
                    }

                        videoLength.text = utils.getAudioVideoLength(context, videoFile.path)

                     utils.setVideoThumbnailFromWebAsync(context, videoFile.path, preview)

                    if(videoFile.length() > max_file_size){
                        utils.toast(context, "Please choose a file smaller than 16 MB")
                        finish()
                    }

                    currentVideoFile = videoFile
                    isVideoFromIntent = true

                    }
                    catch (e:Exception){
                        longToast("Failed to load video")
                        finish()
                    }
                    caption_layout.visibility = View.VISIBLE

                }
            }
        }
    }


    private fun uploadAndForward(
        messageID:String,
        file: File,
        originalFile: File,
        fileType: String
    ){

        val model = (Models.MessageModel("", isFile = true,
            file_local_path = originalFile.path, file_size_in_bytes = file.length(),
            messageType = fileType, caption = captionEditText.text.toString()))

        val uploadRequest = OneTimeWorkRequestBuilder<UploadWorker>()
            .setInputData(workDataOf(
                msg_id to messageID,
                msg_model to model.convertToJsonString(),
                selected_uids to selectedUIDs.joinToString(","),
                key_nameOrNumber to selectedNumbers.joinToString(",")
                ))
            .addTag(messageID)
            .build()


        WorkManager.getInstance().enqueueUniqueWork(messageID, ExistingWorkPolicy.KEEP, uploadRequest)

        WorkManager.getInstance().getWorkInfosForUniqueWorkLiveData("forward")
            .observe(this, androidx.lifecycle.Observer {
                it.forEach {
                    Log.d("ForwardActivity", "uploadAndForward: ${it.outputData.keyValueMap}")
                }
            })

        startActivity(Intent(context, HomeActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))

        finish()


    }

    var adapter: FirebaseRecyclerAdapter<Models.LastMessageDetail, ViewHolder>? = null

    private fun setFrequentAdapter(){

        val lastMsgQuery = FirebaseUtils.ref.lastMessage(FirebaseUtils.getUid())
                .orderByChild(FirebaseUtils.KEY_REVERSE_TIMESTAMP)

        val options = FirebaseRecyclerOptions.Builder<Models.LastMessageDetail>()
            .setQuery(lastMsgQuery, Models.LastMessageDetail::class.java)
            .build()

        adapter = object : FirebaseRecyclerAdapter<Models.LastMessageDetail, ViewHolder>(options){
            override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder =
                ViewHolder(layoutInflater.inflate(R.layout.item_contact_layout, p0, false))

            override fun onBindViewHolder(holder: ViewHolder, position: Int, model: Models.LastMessageDetail) {

                val uid = super.getRef(position).key.toString()

                val type = model.type
                holder.lastMessageTime.text = utils.getHeaderFormattedDate(model.timeInMillis)

                bindHolder(holder, uid,model.nameOrNumber, type)

            }

        }

        frequentRecyclerView.adapter = adapter

        adapter?.startListening()

        lastMsgQuery.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onDataChange(p0: DataSnapshot) {
                if(p0.exists()){

                    for (item in p0.children) {
                        if (!allFrequentUIDs.contains(item.key)) {
                            allFrequentUIDs.add(item.key!!)
                            allFrequentConverstation.add(item.getValue(Models.LastMessageDetail::class.java)!!)
                        }
                    }
                }

                loadRegisteredUsers()
            }

        })

    }


    val recyclerFilter = object : Filter(){
        override fun performFiltering(p0: CharSequence?): FilterResults {
            val query = p0.toString().toLowerCase(Locale.getDefault()).trim()

            registeredAvailableUser = allAvailableUsers

            registeredAvailableUser = registeredAvailableUser.filter { it.name.toLowerCase().contains(query)
                    ||  it.number.toLowerCase().contains(query)}.toMutableList()

            return FilterResults().apply { values = registeredAvailableUser; count = registeredAvailableUser.size }

        }

        override fun publishResults(p0: CharSequence?, p1: FilterResults?) {
            p1?.let {
                allContactAdapter?.notifyDataSetChanged()
            }
        }

    }

    private fun setAllContactAdapter(){

        registeredAvailableUser.sortBy { it.name }
        allAvailableUsers = registeredAvailableUser


        allContactAdapter = object : RecyclerView.Adapter<ViewHolder>(), Filterable{

            override fun getFilter(): Filter {
                return recyclerFilter
            }

            override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder =
                ViewHolder(layoutInflater.inflate(R.layout.item_contact_layout, p0, false))


            override fun getItemCount(): Int = registeredAvailableUser.size

            override fun onBindViewHolder(holder: ViewHolder, p1: Int) {

                val uid = registeredAvailableUser[p1].uid

                bindHolder(holder, uid, registeredAvailableUser[p1].number , FirebaseUtils.KEY_CONVERSATION_SINGLE)

            }

        }
        allContactRecyclerView.adapter = allContactAdapter



    }


    @SuppressLint("RestrictedApi")
    private fun bindHolder(holder: ViewHolder, uid:String, phone:String, type:String){

        val isGroup = type == FirebaseUtils.KEY_CONVERSATION_GROUP

        if(forward_progressbar.visibility == View.VISIBLE)
            forward_progressbar.visibility = View.GONE

        holder.title.text = phone



        if(isGroup) {
            FirebaseUtils.loadGroupPicThumbnail(context, uid, holder.pic)
            if(phone.isEmpty())
                FirebaseUtils.setGroupName(uid, holder.title)
        }
        else {
            FirebaseUtils.loadProfileThumbnail(context, uid, holder.pic)
            if(phone.isNotEmpty()){
                holder.title.text = utils.getNameFromNumber(context, phone)
            }
            else{
                FirebaseUtils.setUserDetailFromUID(context, holder.title, uid, true)
            }
        }

        holder.title.setTextColor(Color.BLACK)

        if(!isGroup) {
            //check if user is blocked
            checkIfBlocked(uid, holder)
        }
        else{
            //check if not in group
            checkIfInGroup(uid, holder)
        }

        holder.checkBox.isChecked = selectedUIDs.contains(uid)
        holder.checkBox.invisible = !holder.checkBox.isChecked


        holder.itemView.setOnClickListener {
            holder.checkBox.isChecked = !holder.checkBox.isChecked

            holder.checkBox.invisible = !holder.checkBox.isChecked

            val nameOrNumber = if (isGroup) holder.title.text else phone

            if(holder.checkBox.isChecked) {
                selectedUIDs.add(uid)
                selectedTitles.add(holder.title.text.toString())
                selectedNumbers.add(nameOrNumber.toString())
            }
            else {
                selectedUIDs.remove(uid)
                selectedTitles.remove(holder.title.text.toString())
                selectedNumbers.remove(nameOrNumber.toString())
            }

            nameOfRecipient  = selectedTitles.joinToString(", ")

             fwd_snackbar?.setText(">  ${nameOfRecipient.trim()}")

            sendBtn.visible = selectedUIDs.isNotEmpty()

            if(selectedUIDs.isEmpty()) { fwd_snackbar!!.dismiss(); nameOfRecipient = "" }
            else { if(!fwd_snackbar!!.isShown) fwd_snackbar!!.show() }

        }
    }

    private fun loadRegisteredUsers(){

        numberList = utils.getContactList(this)

        FirebaseUtils.ref.allUser()
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(p0: DataSnapshot) {

                    if(!p0.exists()) {
                      //  utils.toast(context, "No registered users")
                        return
                    }

                    registeredAvailableUser.clear()

                    for (post in p0.children){
                        val userModel = post.getValue(Models.User ::class.java)

                        val number = utils.getFormattedTenDigitNumber(userModel!!.phone)
                        val uid = userModel.uid

                        if(uid == FirebaseUtils.getUid())
                            continue

                        for((index, item) in numberList.withIndex()) {
                            if (item.number == number || item.number.contains(number)) {
                                numberList[index].uid = uid
                                if(!allFrequentUIDs.any { it == uid } && !registeredAvailableUser.any{ it == numberList[index] }
                                    && !registeredAvailableUser.any { it.number == number }
                                )
                                registeredAvailableUser.add(numberList[index])
                            }

                        }

                    }

                    setAllContactAdapter()


                }

                override fun onCancelled(p0: DatabaseError) {
                }

            })
    }


    //for group members
    private fun addMessageToGroupMembers(messageID: String , messageModel: Models.MessageModel, groupId:String
    , groupName:String) {

        FirebaseUtils.ref.groupMembers(groupId)
            .orderByChild("removed").equalTo(false)
            .addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onDataChange(p0: DataSnapshot) {
                val groupMembers:MutableList<Models.GroupMember> = ArrayList()
                for(post in p0.children){
                    groupMembers.add(post.getValue(Models.GroupMember::class.java)!!)

                    groupMembers.forEach {
                        val memberID = it.uid

                        //setting  message to target
                        if(memberID != myUID) {

                            Log.d("MessageActivity", "addMessageToGroupMembers: targets -> $memberID")

                            FirebaseUtils.ref.getChatRef(memberID, groupId)  // must be (participant, groupID)
                                .child(messageID)
                                .setValue(messageModel)
                                .addOnSuccessListener {

                                    FirebaseUtils.setMessageStatusToDB(messageID, memberID, groupId, false, false,
                                        groupName)

                                    FirebaseUtils.ref.lastMessage(memberID)
                                        .child(groupId)
                                        .setValue(Models.LastMessageDetail(type = FirebaseUtils.KEY_CONVERSATION_GROUP
                                            , nameOrNumber = groupName
                                        ))

                                }
                        }
                    }
                }
            }
        })





    }



    class ViewHolder(view:View): RecyclerView.ViewHolder(view){
         val title = view.name!!
         val pic = view.pic!!
         val checkBox = view.checkbox!!
         val lastMessageTime:TextView = view.messageTime

        init {
            checkBox.isEnabled = false
        }
    }



    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == android.R.id.home)
        finish()
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        menuInflater.inflate(R.menu.menu_search, menu)
         menu?.findItem(R.id.action_search)?.let { searchView.setMenuItem(it) }
        searchView.setOnQueryTextListener(object : MaterialSearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {

                (allContactAdapter as? Filterable)?.filter?.filter(query)

                return query.isNullOrEmpty()
            }

            override fun onQueryTextChange(newText: String?): Boolean {

                (allContactAdapter as? Filterable)?.filter?.filter(newText)
                return newText.isNullOrEmpty()
            }

        })

        return super.onCreateOptionsMenu(menu)
    }


    override fun onDestroy() {
        adapter?.stopListening()
        super.onDestroy()
    }


    private fun checkIfBlocked(uid: String, holder: ViewHolder){
        FirebaseUtils.ref.blockedUser(myUID, uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {}

                override fun onDataChange(p0: DataSnapshot) {
                    holder.itemView.isEnabled = true
                    if (p0.exists()) {
                        holder.itemView.isEnabled = !p0.value.toString().toBoolean()
                    }
                    holder.itemView.isClickable = holder.itemView.isEnabled
                    holder.title.setTextColor(if (holder.itemView.isEnabled) Color.BLACK else Color.LTGRAY)


                }

            })

        FirebaseUtils.ref.blockedUser(uid, myUID)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {}

                override fun onDataChange(p0: DataSnapshot) {
                    holder.itemView.isEnabled = true
                    if (p0.exists()) {
                        holder.itemView.isEnabled = !p0.value.toString().toBoolean()
                    }
                    holder.itemView.isClickable = holder.itemView.isEnabled
                    holder.title.setTextColor(if (holder.itemView.isEnabled) Color.BLACK else Color.LTGRAY)


                }

            })

    }


     private fun checkIfInGroup(selectedGroupID:String, holder: ViewHolder){

         holder.itemView.isEnabled = true
         holder.itemView.isClickable = true
         holder.title.setTextColor(if(holder.itemView.isEnabled) Color.BLACK else Color.LTGRAY)



         FirebaseUtils.ref.groupMembers(selectedGroupID)
            .orderByChild("removed").equalTo(false)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                }

                override fun onDataChange(p0: DataSnapshot) {
                    //only available members will be returned
                    var isMeRemoved = false

                    isMeRemoved = if(!p0.exists())
                        true
                    else
                        !p0.children.any {
                            it.getValue(Models.GroupMember::class.java)?.uid == myUID }


                    try {

                        holder.itemView.isEnabled = !isMeRemoved
                        holder.itemView.isClickable = holder.itemView.isEnabled
                        holder.title.setTextColor(if(holder.itemView.isEnabled) Color.BLACK else Color.LTGRAY)

                    }
                    catch (e:Exception){}

                }
            })
    }


    override fun onBackPressed() {

        if(searchView.isSearchOpen)
            searchView.closeSearch()
        else
            super.onBackPressed()
    }

}
