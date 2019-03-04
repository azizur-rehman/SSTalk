package com.aziz.sstalk

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import android.support.design.widget.Snackbar
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.aziz.sstalk.models.Models
import com.aziz.sstalk.utils.FirebaseUtils
import com.aziz.sstalk.utils.utils
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.activity_forward.*
import kotlinx.android.synthetic.main.item__forward_contact_list.view.*
import kotlinx.android.synthetic.main.item_contact_layout.view.*
import me.shaohui.advancedluban.Luban
import me.shaohui.advancedluban.OnCompressListener
import org.jetbrains.anko.doAsyncResult
import org.jetbrains.anko.onComplete
import org.jetbrains.anko.uiThread
import java.io.File
import java.lang.Exception
import java.util.concurrent.Future

class ForwardActivity : AppCompatActivity() {

    val selectedUIDs:MutableList<String> = ArrayList()
    val allFrequentUIDs:MutableList<String> = ArrayList()
    //number list has 10 digit formatted number
    var numberList:MutableList<Models.Contact> = mutableListOf()
    var registeredAvailableUser:MutableList<Models.Contact> = mutableListOf()

    var nameOfRecipient :String = ""

    private var allContactAdapter:RecyclerView.Adapter<ViewHolder>? = null

    val context = this@ForwardActivity
    private var myUID: String = ""

    var fwd_snackbar:Snackbar? = null

    var messageModels: MutableList<Models.MessageModel>? = ArrayList()

    var progressDialog:ProgressDialog? = null

    var currentMessageID = ""
    private var asyncLoader: Future<Unit>? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forward)

        setSupportActionBar(toolbar)


        if(supportActionBar!=null)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)

        fwd_snackbar = Snackbar.make(sendBtn, "", Snackbar.LENGTH_INDEFINITE)


        recyclerLayout.visibility = View.GONE
        asyncLoader = doAsyncResult {
            uiThread { setFrequentAdapter() }
            onComplete { uiThread { recyclerLayout.visibility = View.VISIBLE } }

        }

        myUID = FirebaseUtils.getUid()

        try {
            messageModels = intent.getSerializableExtra(utils.constants.KEY_MSG_MODEL) as MutableList<Models.MessageModel>
        }
        catch (e:Exception){ messageModels = ArrayList()}

        if(messageModels!!.isEmpty())
            handleIncomingIntents(intent)


        sendBtn.setOnClickListener {



                if (intent.type!!.startsWith("image/") && intent.action == Intent.ACTION_SEND) {
                    //image is sent via intent

                    if(bitmap!=null){
                        val messageID = "MSG${System.currentTimeMillis()}"
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
                                    progressDialog!!.setMessage("Please wait...")
                                    progressDialog!!.show()
                                }

                            })

                    }
                    else{
                        utils.toast(context, "Image might be corrupted")
                        finish()
                    }

                }

                else if(intent.type!!.startsWith("video/") && intent.action == Intent.ACTION_SEND){

                    if(currentVideoFile!=null){
                        val messageID = "MSG${System.currentTimeMillis()}"
                        progressDialog!!.setMessage("Please wait...")
                        progressDialog!!.show()
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


        selectedUIDs.forEach {

            var messageID = "MSG${System.currentTimeMillis()}"

            if(currentMessageID.isNotEmpty())
                messageID = currentMessageID

            val targetUID = it

        for (model in messageModels!!) {
            model.from = myUID
            model.timeInMillis = System.currentTimeMillis()
            model.reverseTimeStamp = model.timeInMillis * -1
            model.to = targetUID
            model.caption = ""

            val currentModel = model

            //send to my node
            FirebaseUtils.ref.getChatRef(myUID, targetUID)
                .child(messageID)
                .setValue(currentModel)
                .addOnSuccessListener {
                    FirebaseUtils.setMessageStatusToDB(messageID, myUID, targetUID, true, isRead = true)

                    FirebaseUtils.ref.lastMessage(myUID)
                        .child(targetUID)
                        .setValue(Models.LastMessageDetail())

                }

            currentModel.file_local_path = ""

            //send to target node
            FirebaseUtils.ref.getChatRef(targetUID, FirebaseUtils.getUid())
                .child(messageID)
                .setValue(currentModel)
                .addOnSuccessListener {
                    FirebaseUtils.setMessageStatusToDB(messageID, targetUID, myUID, false, isRead = false)

                    FirebaseUtils.ref.lastMessage(targetUID)
                        .child(myUID)
                        .setValue(Models.LastMessageDetail())
                }
        }
    }

        if(selectedUIDs.size == 1)
            startActivities(arrayOf(
                Intent(context, HomeActivity::class.java)
                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                Intent(context, MessageActivity::class.java)
                .putExtra(FirebaseUtils.KEY_UID, selectedUIDs[0]))
            )
        else

            startActivity(Intent(context, HomeActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))

        finish()
    }


    var bitmap:Bitmap? = null
    var currentVideoFile:File? = null

    private fun handleIncomingIntents(intent: Intent){

        progressDialog = ProgressDialog(this)
        progressDialog!!.setCancelable(false)

        if(intent.action == Intent.ACTION_SEND){
            when {
                intent.type == "text/plain" -> {
                    val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                    messageModels!!.add(Models.MessageModel(text ))
                }
                intent.type!!.startsWith( "image/") -> {

                    if(!utils.hasStoragePermission(this)) {
                        utils.toast(this, "App does not have storage permission")
                        return
                    }
                    val imageURI = intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as Uri
                    bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageURI)


                }
                intent.type!!.startsWith("video/") -> {
                    if(!utils.hasStoragePermission(this)){
                        utils.toast(this, "App does not have storage permission")
                        return
                    }

                    val videoUri = intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as Uri
                    val videoFile =  File(utils.getRealPathFromURI(this, videoUri))

                    if(!videoFile.exists()){
                        utils.toast(context, "Something went wrong in video file")
                        finish()
                    }

                    if(videoFile.length() > (16 * 1024 * 1024)){
                        utils.toast(context, "Please choose a file smaller than 16 MB")
                        finish()
                    }

                    currentVideoFile = videoFile
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


        currentMessageID = messageID


       val ref = FirebaseStorage.getInstance().reference
           .child(fileType)
            .child(messageID)

           val uploadTask = ref.putFile(utils.getUriFromFile(context, file))

               uploadTask
                   .addOnProgressListener {
                       val percentage:Double = (100.0 * it.bytesTransferred) / it.totalByteCount
                       val percent = String.format("%.2f",percentage)
                       progressDialog!!.setMessage("Uploading media $percent%")

                   }
                   .continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
                   if (!task.isSuccessful) {
                       task.exception?.let {
                           throw it
                       }
//                       FirebaseUtils.storeFileMetaData(messageID, task.result!!.metadata!!)

                   }
                   return@Continuation ref.downloadUrl
               })

               .addOnCompleteListener { task->
                progressDialog!!.dismiss()

                val model = (Models.MessageModel(task.result.toString(), isFile = true,
                    file_local_path = originalFile.path, file_size_in_bytes = file.length(),
                    messageType = fileType))
                   messageModels!!.add(model)


                   FirebaseUtils.storeFileMetaData(
                       Models.File(messageID,
                           model.timeInMillis, fileType = fileType,
                           fileSizeInBytes = file.length(),
                           bucket_path = ref.bucket,
                           file_url = task.result.toString(),
                        file_extension = utils.getFileExtension(file)
                       ))

                onForwardToSelectedUIDs()
            }



    }

    private fun setFrequentAdapter(){

        val lastMsgQuery = FirebaseUtils.ref.lastMessage(FirebaseUtils.getUid())
                .orderByChild(FirebaseUtils.KEY_REVERSE_TIMESTAMP)

        val options = FirebaseRecyclerOptions.Builder<Models.LastMessageDetail>()
            .setQuery(lastMsgQuery, Models.LastMessageDetail::class.java)
            .setLifecycleOwner(this)
            .build()

        val adapter = object : FirebaseRecyclerAdapter<Models.LastMessageDetail, ViewHolder>(options){
            override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder =
                ViewHolder(layoutInflater.inflate(R.layout.item__forward_contact_list, p0, false))

            override fun onBindViewHolder(holder: ViewHolder, position: Int, model: Models.LastMessageDetail) {

                val uid = super.getRef(position).key.toString()

                bindHolder(holder, uid,"")


            }

        }




        frequentRecyclerView.adapter = adapter
        frequentRecyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        lastMsgQuery.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onDataChange(p0: DataSnapshot) {
                if(p0.exists()){

                    for (item in p0.children) {
                        if (!allFrequentUIDs.contains(item.key))
                            allFrequentUIDs.add(item.key!!)
                    }
                }

                loadRegisteredUsers()
            }

        })

    }

    private fun setAllContactAdapter(){
        allContactAdapter = object : RecyclerView.Adapter<ViewHolder>(){
            override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder =
                ViewHolder(layoutInflater.inflate(R.layout.item__forward_contact_list, p0, false))


            override fun getItemCount(): Int = registeredAvailableUser.size

            override fun onBindViewHolder(holder: ViewHolder, p1: Int) {

                val uid = registeredAvailableUser[p1].uid

                bindHolder(holder, uid, registeredAvailableUser[p1].number )

            }

        }
        allContactRecyclerView.adapter = allContactAdapter



    }


    @SuppressLint("RestrictedApi")
    private fun bindHolder(holder: ViewHolder, uid:String, phone:String){


        if(forward_progressbar.visibility == View.VISIBLE)
            forward_progressbar.visibility = View.GONE

        holder.title.text = uid

        if(phone.isNotEmpty()){
            holder.title.text = utils.getNameFromNumber(context, phone)
        }
        else{
            FirebaseUtils.setUserDetailFromUID(context, holder.title, uid, true)
        }

        FirebaseUtils.loadProfileThumbnail(context, uid, holder.pic)
        holder.title.setTextColor(Color.BLACK)

        //check if user is blocked
        FirebaseUtils.ref.blockedUser(myUID, uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {}

                override fun onDataChange(p0: DataSnapshot) {
                    holder.itemView.isEnabled = true
                    if(p0.exists()){
                        holder.itemView.isEnabled = !p0.value.toString().toBoolean()
                        holder.itemView.isClickable = holder.itemView.isEnabled
                        holder.title.setTextColor(if(holder.itemView.isEnabled) Color.BLACK else Color.LTGRAY)
                    }

                }

            })




        holder.itemView.setOnClickListener {
            holder.checkBox.isChecked = !holder.checkBox.isChecked



            if(holder.checkBox.isChecked) {
                selectedUIDs.add(uid)
                nameOfRecipient = nameOfRecipient + holder.title.text +" "
            }
            else {
                selectedUIDs.remove(uid)
                nameOfRecipient = nameOfRecipient.replace(holder.title.text.toString(),"")
            }

             fwd_snackbar!!.setText(">  ${nameOfRecipient.trim()}")

            sendBtn.visibility = if(selectedUIDs.isEmpty()) View.GONE else View.VISIBLE

            if(selectedUIDs.isEmpty()) {fwd_snackbar!!.dismiss()
            nameOfRecipient = ""
            }
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
                            if (item.number == number) {
                                numberList[index].uid = uid
                                if(!allFrequentUIDs.contains(uid))
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


    class ViewHolder(view:View):RecyclerView.ViewHolder(view){
         val title = view.name!!
         val pic = view.pic!!
         val checkBox = view.checkbox!!


    }


    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        finish()
        return super.onOptionsItemSelected(item)
    }


    override fun onDestroy() {
        asyncLoader?.cancel(true)
        super.onDestroy()
    }

}
