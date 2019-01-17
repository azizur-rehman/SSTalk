package com.aziz.sstalk

import android.Manifest
import android.animation.Animator
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
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.aziz.sstalk.Models.Models
import com.aziz.sstalk.utils.FirebaseUtils
import com.aziz.sstalk.utils.LocationHelper
import com.aziz.sstalk.utils.utils
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
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
import kotlinx.android.synthetic.main.bubble_map_left.view.*
import kotlinx.android.synthetic.main.bubble_map_right.view.*
import me.shaohui.advancedluban.Luban
import me.shaohui.advancedluban.OnCompressListener
import java.io.File
import java.lang.Exception
import java.util.*

class MessageActivity : AppCompatActivity() {



    lateinit var mapRight:GoogleMap
    var TYPE_MINE = 0
    var TYPE_TARGET = 1
    var TYPE_MY_MAP = 2
    var TYPE_TARGET_MAP = 3

    val RQ_PICK = 101
    val RQ_PREVIEW = 102
    val RQ_LOCATION = 103

    val RP_STORAGE = 101
    val RP_LOCATION = 102

    var targetUid : String = ""
    var myUID : String = ""

    var imageFile:File? = null

    var user1 = "user---1"
    var user2 = "user---2"

    var isBlockedByMe = false
    var isBlockedByUser = false

    val context = this@MessageActivity
    var loadedPosition:HashMap<Int,Boolean> = HashMap()
    var myLastMessagePosition = 0

    var isUploading = false
    lateinit var adapter:FirebaseRecyclerAdapter<Models.MessageModel, RecyclerView.ViewHolder>

    var locationHelper:LocationHelper? = null

    var shouldHideMenu = true

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



        checkIfBlocked(targetUid)

        setMenuListeners()

        attachment_menu.visibility = View.GONE

        messageInputField.setAttachmentsListener {


        //    val view = layoutInflater.inflate(R.layout.item_date_header, messageInputField,false)

           val cx = (attachment_menu.getLeft() + attachment_menu.getRight()) / 2
            val cy = (attachment_menu.getTop() + attachment_menu.getBottom()) / 2
            val finalRadius = Math.max(attachment_menu.getWidth(), attachment_menu.getHeight())

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                
                val startAnimator =     ViewAnimationUtils.createCircularReveal(attachment_menu,
                   // cx,cy,0F,finalRadius.toFloat()
                    attachment_menu.left,
                    attachment_menu.top,
                    0F,
                    attachment_menu.width.toFloat()
                )

                val closeAnimator =     ViewAnimationUtils.createCircularReveal(attachment_menu,
                    attachment_menu.right,
                    attachment_menu.bottom,
                    0F,
                    attachment_menu.width.toFloat())


                startAnimator.duration = 400
                closeAnimator.duration = 400


                if(attachment_menu.visibility == View.GONE ) {
                    attachment_menu.visibility = View.VISIBLE
                    startAnimator.start()
                }
                else {
                    closeAnimator.start()

                }




                startAnimator.addListener(object : Animator.AnimatorListener{
                    override fun onAnimationRepeat(animation: Animator?) {}

                    override fun onAnimationEnd(animation: Animator?) {
                    }

                    override fun onAnimationCancel(animation: Animator?) {}

                    override fun onAnimationStart(animation: Animator?) {  attachment_menu.visibility = View.VISIBLE
                    }

                })

                    closeAnimator.addListener(object : Animator.AnimatorListener{
                        override fun onAnimationRepeat(animation: Animator?) {}

                        override fun onAnimationEnd(animation: Animator?) {
                            attachment_menu.visibility = View.GONE
                          //  messagesList.alpha = 1f

                            Log.d("MessageActivity", "onAnimationEnd: CLose animaton")

                        }

                        override fun onAnimationCancel(animation: Animator?) {}

                        override fun onAnimationStart(animation: Animator?) {}

                    })


            } else {
                attachment_menu.visibility = if (attachment_menu.visibility == View.VISIBLE) View.GONE else  View.VISIBLE
            }



        }

    }


    private fun setMenuListeners(){


        camera_btn.setOnClickListener {

            attachment_menu.visibility = if (attachment_menu.visibility == View.VISIBLE) View.GONE else  View.VISIBLE

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED)

                    ImagePicker.pickImage(context, RQ_PICK)
                else {
                    requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), RP_STORAGE)
                }

            }
            else{
                ImagePicker.pickImage(context, RQ_PICK)
            }
        }


        gallery_btn.setOnClickListener {
            attachment_menu.visibility = if (attachment_menu.visibility == View.VISIBLE) View.GONE else  View.VISIBLE

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED)

                    ImagePicker.pickImageGalleryOnly(context, RQ_PICK)
                else {
                    requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), RP_STORAGE)
                }

            }
            else{
                ImagePicker.pickImageGalleryOnly(context, RQ_PICK)
            }
        }


        location_btn.setOnClickListener {

            attachment_menu.visibility = if (attachment_menu.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                    startActivityForResult(Intent(context, MapsActivity::class.java), RQ_LOCATION)
                else
                    requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), RP_LOCATION)

            }
            else{
                startActivityForResult(Intent(context, MapsActivity::class.java), RQ_LOCATION)
            }
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {

        when(requestCode){
            RP_STORAGE -> {
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    ImagePicker.pickImage(context,RQ_PICK)
                else
                    utils.toast(context, "Permission denied")
            }

            RP_LOCATION -> {
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    startActivityForResult(Intent(context, MapsActivity::class.java), RQ_LOCATION)
                else
                    utils.toast(context, "Permission denied")
            }
        }

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {



        if(resultCode != Activity.RESULT_OK)
            return


        val messageID = "MSG" +System.currentTimeMillis() + myUID


        if(requestCode == RQ_PICK ){

            val filePath = ImagePicker.getImagePathFromResult(context, requestCode, resultCode, data)

            Luban.compress(context, File(filePath))
                .putGear(Luban.THIRD_GEAR)
                .launch(object : OnCompressListener {
                    override fun onStart() {

                    }

                    override fun onSuccess(file: File?) {

                        imageFile = file!!




                        startActivityForResult(Intent(context, UploadPreviewActivity::class.java)
                            .putExtra(utils.constants.KEY_IMG_PATH, file.path.toString())
                            , RQ_PREVIEW)


                    }

                    override fun onError(e: Throwable?) {
                        Log.d("MessageActivity", "onError: "+e!!.message.toString())
                        utils.longToast(context, e.message.toString())
                    }

                })


         }


        else if(requestCode == RQ_PREVIEW ){
            val caption = data!!.getStringExtra(utils.constants.KEY_CAPTION)

            if (imageFile != null) {

                Log.d("MessageActivity", "onActivityResult: Uploading Image")


                uploadImage(messageID, imageFile!!, caption,null)
                isUploading = true

            }

        }

        else if(requestCode == RQ_LOCATION){

            val latitude = data!!.getDoubleExtra(utils.constants.KEY_LATITUDE,0.0)
            val longitude = data.getDoubleExtra(utils.constants.KEY_LONGITUDE,0.0)
            val address = data.getStringExtra(utils.constants.KEY_ADDRESS)

            if(latitude == 0.0 || longitude == 0.0){
                utils.toast(context, "Failed to fetch location")
                return
            }

            val message = "$latitude,$longitude"

            addMessage(messageID, Models.MessageModel(message,
                myUID,
                targetUid,
                System.currentTimeMillis(),
                isFile = false,
                isRead = false,
                caption = address,
                messageType = utils.constants.FILE_TYPE_LOCATION),
                false)
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
            myUID, targetUid ,isFile = true, caption = caption, messageType = utils.constants.FILE_TYPE_IMAGE)

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
                        myUID, targetUid ,isFile = true, caption = caption, messageType = utils.constants.FILE_TYPE_IMAGE)

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
            .setQuery(FirebaseUtils.ref.getChatQuery(myUID, targetUid)
                ,Models.MessageModel::class.java)
            //.setLifecycleOwner(this)
            .build()


         adapter = object  : FirebaseRecyclerAdapter<Models.MessageModel, RecyclerView.ViewHolder>(options) {

            override fun onCreateViewHolder(p0: ViewGroup, viewType: Int): RecyclerView.ViewHolder {


               return when(viewType) {
                     TYPE_MINE ->
                       myViewHolder(LayoutInflater.from(this@MessageActivity)
                       .inflate(R.layout.bubble_right, p0 , false))

                     TYPE_MY_MAP ->
                         myMapHolder(LayoutInflater.from(this@MessageActivity)
                             .inflate(R.layout.bubble_map_right, p0, false))


                     TYPE_TARGET_MAP ->
                       targetMapHolder(LayoutInflater.from(this@MessageActivity)
                           .inflate(R.layout.bubble_map_left, p0, false))

                   else -> targetViewHolder(LayoutInflater.from(this@MessageActivity)
                           .inflate(R.layout.bubble_left, p0, false))
               }
            }


            override fun onBindViewHolder(
                holder: RecyclerView.ViewHolder,
                position: Int,
                model: Models.MessageModel) {


                messagesList.setBackgroundColor(Color.WHITE)


                var targetHolder:targetViewHolder
                var myHolder: myViewHolder

                var messageImage:ImageView? = null
                var dateHeader:TextView? = null
                var latitude: Double = 0.0
                var longitude: Double = 0.0
                var mapView: MapView? = null


            //    Log.d("MessageActivity", "onBindViewHolder: "+getItemViewType(position) +" at $position")

                if(model.messageType == utils.constants.FILE_TYPE_LOCATION){


                    try {
                    latitude = model.message.split(",")[0].toDouble()
                    longitude = model.message.split(",")[1].toDouble()
                    }
                    catch (e :Exception){}

                }



                if(holder is targetViewHolder){
                     holder.time.text = utils.getLocalTime(model.timeInMillis)
                    holder.message.text = model.message

                    messageImage = holder.imageView
                    dateHeader = holder.headerDateTime


                    if(model.isFile && model.messageType == utils.constants.FILE_TYPE_IMAGE){

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
                    dateHeader = holder.headerDateTime

                    messageImage = holder.imageView


                    if(model.isFile && model.messageType == utils.constants.FILE_TYPE_IMAGE){

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


                else if(holder is myMapHolder ) {


                     mapView = holder.mapView

                    holder.message.text = model.caption

                }

              else if(holder is targetMapHolder){

                    mapView = holder.mapView
                    holder.message.text = model.caption
                }




                //loading message Image listener
               if(messageImage!=null) {
                   messageImage!!.setOnClickListener {
                       if (!model.message.toString().contains("/storage/"))
                           startActivity(
                               Intent(context, ImagePreviewActivity::class.java)
                                   .putExtra(utils.constants.KEY_IMG_PATH, model.message.toString())
                           )


                   }
               }



                //loading a map
                if(mapView != null){
                    mapView.onCreate(null)

                    mapView.getMapAsync(object : OnMapReadyCallback {
                        override fun onMapReady(p0: GoogleMap?) {

                            p0!!.addMarker(MarkerOptions()
                                .position(LatLng(latitude, longitude)).title("")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                                .draggable(false).visible(true))


                            p0.uiSettings.setAllGesturesEnabled(false)
                            p0.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(latitude, longitude),
                                    12F
                                )
                            )

                            Log.d("MessageActivity", "onMapReady: ")
                        }
                    })

                }





                //next time
           //     dateHeader!!.text = utils.getLocalDateTime(model.timeInMillis)



            }





            override fun getItemViewType(position: Int): Int {

                val model: Models.MessageModel = getItem(position)

                val viewType: Int

                viewType = if(model.from == myUID){

                    if(model.messageType == utils.constants.FILE_TYPE_LOCATION.toString()) {
                        TYPE_MY_MAP
                    } else{
                        TYPE_MINE
                    }
                } else{
                    if(model.messageType == utils.constants.FILE_TYPE_LOCATION.toString())
                            TYPE_TARGET_MAP
                        else
                            TYPE_TARGET

                }


              //  Log.d("MessageActivity", "getItemViewType: Last viewtype value = $viewType at position = $position")

                return viewType


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




    private fun addMessage(messageID: String , messageModel: Models.MessageModel, onlyForMe:Boolean){

        //setting my message

        messageModel.isRead = true
        FirebaseUtils.ref.getChatRef(myUID, targetUid)
            .child(messageID)
            .setValue(messageModel)
            .addOnSuccessListener {

                FirebaseUtils.ref.getLastMessageRef(myUID)
                    .child(targetUid)
                    .setValue(Models.LastMessageDetail())

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
                    .setValue(Models.LastMessageDetail())

                print("Message added to mine") }



    }



    override fun onStart() {
        super.onStart()

        if(adapter!=null)
            adapter.startListening()

    }


    override fun onDestroy() {
        super.onDestroy()


        if(adapter!=null)
            adapter.stopListening()
    }


    private fun checkIfBlocked(targetUID:String) {

        //check if i have blocked
        FirebaseUtils.ref.getBlockedUserRef(FirebaseUtils.getUid(), targetUID)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {

                    isBlockedByMe = if (dataSnapshot.exists())
                        dataSnapshot.getValue(Boolean::class.java)!!
                    else
                        false

                    messageInputField.button.isEnabled = isBlockedByMe || isBlockedByUser
                    messageInputField.isEnabled = messageInputField.button.isEnabled

                }

                override fun onCancelled(databaseError: DatabaseError) {

                }
            })

        //check i am blocked my user

        FirebaseUtils.ref.getBlockedUserRef(targetUID, FirebaseUtils.getUid())
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {

                    isBlockedByUser = if (dataSnapshot.exists())
                        dataSnapshot.getValue(Boolean::class.java)!!
                    else
                        false

                    messageInputField.button.isEnabled = isBlockedByMe || isBlockedByUser
                    messageInputField.isEnabled = messageInputField.button.isEnabled


                }

                override fun onCancelled(databaseError: DatabaseError) {

                }
            })


    }




    class targetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val message = itemView.messageText_left!!
        val time = itemView.time_left!!
        val imageView = itemView.imageview_left!!
        val headerDateTime = itemView.header_left!!
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
        val headerDateTime = itemView.header_right!!
    }


    class targetMapHolder(itemView: View):RecyclerView.ViewHolder(itemView){
        val message = itemView.messageText_map_left!!
        val mapView = itemView.mapview_left!!
        val dateHeader = itemView.mapview_left!!
    }

    class myMapHolder(itemView: View):RecyclerView.ViewHolder(itemView){
        val message = itemView.messageText_map_right!!
        val mapView = itemView.mapview_right!!
        val dateHeader = itemView.header_map_right!!



    }
}