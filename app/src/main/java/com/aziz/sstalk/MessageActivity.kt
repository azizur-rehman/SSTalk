package com.aziz.sstalk

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import com.aziz.sstalk.Models.Models
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
import com.mvc.imagepicker.ImagePicker
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_message.*
import kotlinx.android.synthetic.main.bubble_right.view.*
import kotlinx.android.synthetic.main.bubble_left.view.*
import me.shaohui.advancedluban.Luban
import me.shaohui.advancedluban.OnCompressListener
import java.io.File
import java.lang.Exception

class MessageActivity : AppCompatActivity() {

    var TYPE_MINE = 0
    var TYPE_TARGET = 1

    val RQ_PICK = 123
    val RQ_PREVIEW = 321

    var targetUid : String = ""
    var myUID : String = ""

    var imageFile:File? = null

    var user1 = "user --- 1"
    var user2 = "user --- 2"

    val context = this@MessageActivity
    var loadedPosition:HashMap<Int,Boolean> = HashMap()
    var myLastMessagePosition = 0

    var isUploading = false
    lateinit var adapter:FirebaseRecyclerAdapter<Models.MessageModel, RecyclerView.ViewHolder>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_message)


      //  targetUid = intent.getStringExtra(FirebaseUtils.KEY_UID)

        myUID = FirebaseUtils.getUid()

      //  myUID = FirebaseUtils.user_voda
      //  targetUid = FirebaseUtils.user_jio



        myUID = user2
        targetUid = user1


//        //todo remove this in production
//        if(myUID.equals(FirebaseUtils.getUid())) {
//            myUID = user1
//            targetUid = user2
//        }
//        else{
//            myUID = user2
//            targetUid = user1
//        }


        Log.d("MessageActivity", "onCreate: myUID = "+myUID)
        Log.d("MessageActivity", "onCreate: target UID = "+targetUid)

        setSendMessageListener()

        setRecyclerAdapter()

        setReyclerScroll()


        messageInputField.setAttachmentsListener {

            if(ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                ImagePicker.pickImage(context, RQ_PICK)
            else{
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 101)
                }
                else{
                    ImagePicker.pickImage(context, RQ_PICK)
                }
            }
        }

    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {

        when(requestCode){
            101 -> {
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    ImagePicker.pickImage(context,RQ_PICK)
                else
                    utils.toast(context, "Permission denied")
            }
        }

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {


        if(requestCode == RQ_PICK && resultCode == Activity.RESULT_OK){

            val filePath = ImagePicker.getImagePathFromResult(context, requestCode, resultCode, data)

            Luban.compress(context, File(filePath))
                .putGear(Luban.THIRD_GEAR)
                .launch(object : OnCompressListener {
                    override fun onStart() {

                    }

                    override fun onSuccess(file: File?) {

                        imageFile = file!!




                        startActivityForResult(Intent(context, ImagePreviewActivity::class.java)
                            .putExtra(utils.constants.KEY_IMG_PATH, file.path.toString())
                            , RQ_PREVIEW)


                    }

                    override fun onError(e: Throwable?) {
                        Log.d("MessageActivity", "onError: "+e!!.message.toString())
                        utils.longToast(context, e.message.toString())
                    }

                })


         }


        if(requestCode == RQ_PREVIEW && resultCode == Activity.RESULT_OK){
            val caption = data!!.getStringExtra(utils.constants.KEY_CAPTION)

            if (imageFile != null) {
               // utils.toast(context, "Write code for Uploading... with caption = $caption")

                Log.d("MessageActivity", "onActivityResult: Uploading Image")

                val messageID = "MSG" +System.currentTimeMillis() + myUID

                uploadImage(messageID, imageFile!!, caption,null)
                isUploading = true

              //  FirebaseUtils.uploadPic(context,imageFile,)
            }

        }

        super.onActivityResult(requestCode, resultCode, data)
    }


    private fun uploadImage(messageID: String, file: File, caption: String, progressBar: ProgressBar?){
        val dialog = ProgressDialog(context)
        dialog.setMessage("Uploading...")
        dialog.setCancelable(false)
       // dialog.show()

        Log.d("MessageActivity", "uploadImage: dir = "+file.path)


        //Initial node
        val messageModel= Models.MessageModel(file.path,
            myUID, targetUid ,isFile = true, caption = caption, fileType = utils.constants.FILE_TYPE_IMAGE)

        addMessage(messageID, messageModel, true)

        isUploading = true


        val ref =  FirebaseStorage.getInstance()
            .reference.child("images").child(messageID)


        val uploadTask = ref.putFile(Uri.fromFile(file))

        uploadTask.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    throw it
                }
            }
            return@Continuation ref.downloadUrl
        })
            .addOnCompleteListener { task ->
                dialog.dismiss()
                isUploading = false
                if(task.isSuccessful) {
                    val link = task.result
                    val messageModel= Models.MessageModel(link.toString() ,
                        myUID, targetUid ,isFile = true, caption = caption, fileType = utils.constants.FILE_TYPE_IMAGE)

                    if(BuildConfig.DEBUG)
                    utils.toast(context, "Uploaded")

                    if (progressBar != null) {
                        progressBar.visibility = View.GONE
                    }

                    addMessage(messageID, messageModel, false)

                }
                else
                    utils.toast(context, task.exception!!.message.toString())
             }

            .addOnSuccessListener {
            dialog.dismiss()
            isUploading = false

            }
            .addOnFailureListener{
                dialog.dismiss()
                isUploading = false
            }



    }

    private fun setRecyclerAdapter(){

        val linearLayoutManager = LinearLayoutManager(this)

        linearLayoutManager.stackFromEnd = true

        messagesList.layoutManager = linearLayoutManager


        val options = FirebaseRecyclerOptions.Builder<Models.MessageModel>()
            .setQuery(FirebaseUtils.ref.getChatRef(myUID, targetUid)
                ,Models.MessageModel::class.java)
            //.setLifecycleOwner(this)
            .build()


         adapter = object  : FirebaseRecyclerAdapter<Models.MessageModel, RecyclerView.ViewHolder>(options) {

            override fun onCreateViewHolder(p0: ViewGroup, p1: Int): RecyclerView.ViewHolder {


                Log.d("MessageActivity", "onCreateViewHolder: viewtype ==== "+getItemViewType(p1))

               return if(getItemViewType(p1) == TYPE_MINE)
                     myViewHolder(LayoutInflater.from(this@MessageActivity)
                            .inflate(R.layout.bubble_right, p0 , false))
                    else
                        targetViewHolder(LayoutInflater.from(this@MessageActivity)
                            .inflate(R.layout.bubble_left, p0, false))
            }


            override fun onBindViewHolder(
                holder: RecyclerView.ViewHolder,
                position: Int,
                model: Models.MessageModel) {


                messagesList.setBackgroundColor(Color.WHITE)


                var targetHolder:targetViewHolder
                var myHolder: myViewHolder


                if(holder is targetViewHolder){
                     holder.time.text = utils.getLocalTime(model.timeInMillis)
                    holder.message.text = model.message


                    if(model.isFile && model.fileType == utils.constants.FILE_TYPE_IMAGE){

                        holder.imageView.visibility = View.VISIBLE


                        if(model.message.isNotEmpty() ) {
                            Picasso.get()
                                .load(model.message.toString()).tag(model.message.toString())
                                .centerCrop().resize(600,400)
                                .error(R.drawable.error_placeholder2)
                                .placeholder(R.drawable.placeholder_image)
                                .into(holder.imageView)

                            loadedPosition[position] = true

                            holder.message.text = model.caption
                        }
                    }


                    //when there is no image
                    else{
                        holder.imageView.visibility = View.GONE

                    }




                }

                else if (holder is myViewHolder){
                    holder.time.text = utils.getLocalTime(model.timeInMillis)
                    holder.message.text = model.message



                    if(model.isFile && model.fileType == utils.constants.FILE_TYPE_IMAGE){

                        holder.imageLayout.visibility = View.VISIBLE
                        holder.progressBar.visibility = View.VISIBLE
                        holder.progressBar.bringToFront()



                        if(model.message.isNotEmpty() ) {

                            if(model.message.contains("/storage/")){

                                //when image is not uploaded


                                holder.tapToRetry.setOnClickListener {
                                    holder.progressBar.visibility = View.VISIBLE



                                    if(model.message.contains("/storage/"))
                                    uploadImage(super.getRef(position).key!!, File(model.message.toString()),
                                        model.caption, holder.progressBar)
                                }

                                Picasso.get()
                                    .load(File(model.message.toString()))
                                    .resize(600,500)
                                    .centerCrop()
                                    .error(R.drawable.error_placeholder2)
                                    .placeholder(R.drawable.placeholder_image)
                                    .tag(model.message.toString())
                                    .into(holder.imageView, object: Callback{

                                        override fun onSuccess() {

                                            Log.d("MessageActivity", "onSuccess: is uploading = $isUploading")

                                            if(isUploading) {
                                                holder.progressBar.visibility = View.VISIBLE
                                                holder.tapToRetry.visibility = View.GONE
                                            }else {
                                                holder.tapToRetry.visibility = View.VISIBLE
                                                holder.progressBar.visibility = View.GONE

                                            }

                                        }

                                        override fun onError(e: Exception?) {

                                            holder.tapToRetry.visibility = View.GONE
                                            holder.progressBar.visibility = View.GONE


                                        }

                                    })
                                loadedPosition[position] = true
                            }

                            else {
                                holder.tapToRetry.visibility = View.GONE

                                Picasso.get()
                                    .load(model.message.toString())
                                    //.networkPolicy(NetworkPolicy.OFFLINE)
                                    .centerCrop()
                                    .resize(600,400)
                                    .error(R.drawable.error_placeholder2)
                                    .placeholder(R.drawable.placeholder_image)
                                    .tag(model.message.toString())
                                    .into(holder.imageView, object: Callback{
                                        override fun onSuccess() {

                                            holder.tapToRetry.visibility = View.GONE
                                            holder.progressBar.visibility = View.GONE


                                        }

                                        override fun onError(e: Exception?) {

                                            holder.tapToRetry.visibility = View.VISIBLE
                                            holder.progressBar.visibility = View.GONE

                                            holder.tapToRetry.setOnClickListener {
                                                it.visibility = View.GONE
                                                utils.longToast(context, "Image might be deleted.")
                                            }


                                        }

                                    })



                                loadedPosition[position] = true
                            }
                        }
                        else {
                            if (imageFile != null) {
                                holder.progressBar.bringToFront()
                                holder.imageView.setImageBitmap(BitmapFactory.decodeFile(imageFile!!.path.toString()))
                                myLastMessagePosition = position
                            }
                        }



                        holder.message.text = model.caption

                    }
                    else{
                        holder.imageLayout.visibility = View.GONE

                    }



                    FirebaseUtils.ref.getChatRef(targetUid, myUID)
                        .child(super.getRef(position).key!!)
                        .addValueEventListener(object : ValueEventListener{
                            override fun onCancelled(p0: DatabaseError) {

                            }

                            override fun onDataChange(p0: DataSnapshot) {
                                if(p0.exists()){
                                    holder.messageStatus.setImageResource(R.drawable.ic_delivered_tick)
                                    if(p0.getValue(Models.MessageModel::class.java)!!.isRead)
                                        holder.messageStatus.setImageResource(R.drawable.ic_read_status)
                                }
                                else{
                                    holder.messageStatus.setImageDrawable(null)
                                }
                            }
                        })


                    //end of my holder

                }





            }



            override fun getItemViewType(position: Int): Int {

                Log.d("MessageActivity", "getItemViewType: position $position , from = "+super.getSnapshots()[position].from)

                var model: Models.MessageModel = super.getItem(position)


                return if (model.from == myUID)
                    TYPE_MINE
                else
                    TYPE_TARGET
            }

        }

        messagesList.adapter = adapter


        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {

                if(adapter.getItem(positionStart).from == myUID)
                    messagesList.scrollToPosition(adapter.itemCount - 1)

                super.onItemRangeInserted(positionStart, itemCount)
            }
        })

    }


    private fun setSendMessageListener(){


        messageInputField.setInputListener {

            val message = messageInputField.inputEditText.text.toString()

            val messageModel= Models.MessageModel(message ,
                myUID, targetUid ,isFile = false)

            val messageID = "MSG" +System.currentTimeMillis() + myUID


            addMessage(messageID, messageModel, false)
            loadedPosition[messagesList.adapter!!.itemCount ]



            true
        }

    }


    fun sendViewToBack(child: View) {
        val parent = child.parent as ViewGroup
        if (null != parent) {
            parent.removeView(child)
            parent.addView(child, 0)
        }
    }


    private fun addMessage(messageID: String , messageModel: Models.MessageModel, onlyForMe:Boolean){




        //setting my message

        messageModel.isRead = true
        FirebaseUtils.ref.getChatRef(myUID, targetUid)
            .child(messageID)
            .setValue(messageModel)
            .addOnSuccessListener {

                FirebaseUtils.ref.getLastMessageRef(myUID)
                    .child(targetUid)
                    .setValue(Models.lastMessageDetail())

                print("Message sent to $targetUid") }


        if(onlyForMe)
            return


        //setting  message to target
        messageModel.isRead = false
        FirebaseUtils.ref.getChatRef(targetUid, myUID)
            .child(messageID)
            .setValue(messageModel)
            .addOnSuccessListener {

                FirebaseUtils.ref.getLastMessageRef(targetUid)
                    .child(myUID)
                    .setValue(Models.lastMessageDetail())

                print("Message added to mine") }



    }


    class targetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val message = itemView.messageText_left!!
        val time = itemView.time_left!!
        val imageView = itemView.imageview_left!!
       // val imageLayout = itemView.imageFrameLayout!!
    }
    class myViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val message = itemView.messageText_right!!
        val time = itemView.time_right!!
        val imageView = itemView.imageview_right!!
        val imageLayout = itemView.imageFrameLayoutRight!!
        val progressBar = itemView.progress_bar_right!!
        val tapToRetry = itemView.tap_retry_right!!
        val messageStatus = itemView.delivery_status!!
    }


    override fun onStart() {
        super.onStart()

        if(adapter!=null)
            adapter.startListening()
    }

    override fun onStop() {
        super.onStop()


        if(adapter!=null)
            adapter.stopListening()
    }


    private fun setReyclerScroll(){

        val viewGroup:ViewGroup  = window.decorView.findViewById(android.R.id.content)

        val textView = TextView(this)
        textView.layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT)

        textView.text = "Sample text"

      //  messagesList.layoutManager!!.addView(textView, 0)

    }
}