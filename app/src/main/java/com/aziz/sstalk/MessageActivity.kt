package com.aziz.sstalk

import android.Manifest
import android.animation.*
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityOptions
import android.app.NotificationManager
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import android.os.Environment.DIRECTORY_DCIM
import android.provider.MediaStore
import com.google.android.material.snackbar.Snackbar
import androidx.emoji.text.EmojiCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.util.Log
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.*
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import android.widget.*
import cafe.adriel.androidaudiorecorder.AndroidAudioRecorder
import cafe.adriel.androidaudiorecorder.model.AudioChannel
import cafe.adriel.androidaudiorecorder.model.AudioSampleRate
import cafe.adriel.androidaudiorecorder.model.AudioSource
import com.aziz.sstalk.firebase.MessagingService
import com.aziz.sstalk.models.Models
import com.aziz.sstalk.utils.*
import com.aziz.sstalk.views.ColorGenerator
import com.aziz.sstalk.views.Holders
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage
import com.google.firebase.ml.naturallanguage.smartreply.FirebaseTextMessage
import com.google.firebase.ml.naturallanguage.smartreply.SmartReplySuggestionResult
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateModelManager
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslator
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslatorOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.mikhaellopez.circularprogressbar.CircularProgressBar
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import com.stfalcon.chatkit.messages.MessageInput
import com.vincent.filepicker.Constant
import com.vincent.filepicker.activity.AudioPickActivity
import com.vincent.filepicker.activity.ImagePickActivity
import com.vincent.filepicker.activity.VideoPickActivity
import com.vincent.filepicker.filter.entity.AudioFile
import com.vincent.filepicker.filter.entity.ImageFile
import com.vincent.filepicker.filter.entity.VideoFile
import kotlinx.android.synthetic.main.activity_message.*
import kotlinx.android.synthetic.main.bubble_left.view.*
import kotlinx.android.synthetic.main.bubble_right.view.*
import kotlinx.android.synthetic.main.item_smart_reply.view.*
import kotlinx.android.synthetic.main.layout_attachment_menu.*
import kotlinx.android.synthetic.main.layout_include_message_activity_toolbar.*
import kotlinx.android.synthetic.main.layout_item_audio.view.*
import kotlinx.android.synthetic.main.layout_video_bubble.view.*
import kotlinx.android.synthetic.main.text_header.view.*
import me.shaohui.advancedluban.Luban
import me.shaohui.advancedluban.OnCompressListener
import org.jetbrains.anko.*
import java.io.File
import java.io.Serializable
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Future
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class MessageActivity : AppCompatActivity() {


    var unreadHeaderPosition = 0
    var unreadMessageCount = 0
    var unreadFirstMessageID = ""
    var TYPE_MINE = 0
    var TYPE_TARGET = 1
    var TYPE_MY_MAP = 2
    var TYPE_TARGET_MAP = 3
    var TYPE_MY_IMAGE = 4
    var TYPE_TARGET_IMAGE = 5
    val TYPE_MY_VIDEO = 6
    val TYPE_TARGET_VIDEO = 7
    val TYPE_MY_AUDIO = 8
    val TYPE_TARGET_AUDIO = 9

    val TYPE_EVENT = 10


    val RQ_CAMERA = 100
    val RQ_GALLERY = 101
    val RQ_PREVIEW_IMAGE = 102
    val RQ_LOCATION = 103
    val RQ_VIDEO = 104
    val RQ_RECORDING = 105
    val RQ_AUDIO = 106

    val RP_STORAGE_GALLERY = 101
    val RP_LOCATION = 102
    val RP_STORAGE_CAMERA = 103
    val RP_STORAGE_VIDEO = 104

    val RP_INITAL_STORAGE_PERMISSION = 105
    val RP_RECORDING = 106

    var targetUid : String = ""
    var targetType:String = FirebaseUtils.KEY_CONVERSATION_SINGLE
    var myUID : String = ""
    var isGroup = false
    var nameOrNumber = ""

     var cameraImagePath  = ""
     var cameraImageUri: Uri? = null


    var isBlockedByMe = false
    var isBlockedByUser = false

    val context = this@MessageActivity
    var loadedPosition:HashMap<Int,Boolean> = HashMap()

    var selectedMessageModel:MutableList<Models.MessageModel> = ArrayList()
    var selectedMessageIDs:MutableList<String> = ArrayList()
    val selectedItemPosition:MutableList<Int> = ArrayList()

    var searchFilterItemPosition:MutableList<Int> = ArrayList()
    var groupMembers:MutableList<Models.GroupMember> = ArrayList()
    var hasLastMessageSelected = false

    private var asyncLoader: Future<Boolean>? = null


    val isUploading:HashMap<String,Boolean> = HashMap()
    private val CircularProgressBarsAt:HashMap<String,CircularProgressBar> = HashMap()
    private val mediaControlImageViewAt:HashMap<String,ImageView> = HashMap()

    lateinit var adapter:FirebaseRecyclerAdapter<Models.MessageModel, RecyclerView.ViewHolder>

    private var audioFile:File? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_message)


        FirebaseUtils.setonDisconnectListener()


        setSupportActionBar(toolbar)



        targetUid = intent.getStringExtra(FirebaseUtils.KEY_UID).orEmpty()
        val type:String? = intent.getStringExtra(utils.constants.KEY_TARGET_TYPE)


        nameOrNumber = intent.getStringExtra(utils.constants.KEY_NAME_OR_NUMBER).orEmpty()

        targetType = if(type.isNullOrEmpty()) FirebaseUtils.KEY_CONVERSATION_SINGLE
        else type

        Log.d("MessageActivity", "onCreate: type = $targetType")
        Log.d("MessageActivity", "onCreate: name or number = $nameOrNumber")

        isGroup = targetType == FirebaseUtils.KEY_CONVERSATION_GROUP

        myUID = FirebaseUtils.getUid()

        if(targetUid.isEmpty()){
            toast("Something went wrong")
            finish()
            return
        }


        loadGroupMembers()

        blockedSnackbar =   Snackbar.make(messageInputField, "You cannot reply to this conversation anymore", Snackbar.LENGTH_INDEFINITE)




        asyncLoader= doAsyncResult {
            uiThread {
                message_progressbar.visibility = View.VISIBLE
                initComponents()
                
                onComplete {
                    message_progressbar.visibility = View.GONE
                }
            }


        }


        
        
    }


    private fun initComponents(){

        with(messagesList){
            setHasFixedSize(true)
            setItemViewCacheSize(20)
            setDrawingCacheEnabled(true)
            setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH)
        }


        if(isGroup) {
            target_name_textview.text = nameOrNumber

            if(utils.isGroupID(nameOrNumber) || nameOrNumber.isEmpty()){
                FirebaseUtils.ref.groupInfo(targetUid).child(utils.constants.KEY_NAME)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onCancelled(p0: DatabaseError) { }
                        override fun onDataChange(p0: DataSnapshot) {
                            if(p0.exists()) {
                                nameOrNumber = p0.getValue(String::class.java)!!
                                target_name_textview.text = nameOrNumber
                            }
                        }
                    })
//                FirebaseUtils.setGroupName(nameOrNumber, target_name_textview)
            }

            FirebaseUtils.loadGroupPicThumbnail(context, targetUid, profile_circleimageview)
            if(nameOrNumber.isEmpty())
                FirebaseUtils.setGroupName(targetUid, target_name_textview)

            monitorGroupNameChanges()
        }
        else {
            target_name_textview.text = (utils.getNameFromNumber(context, nameOrNumber))
            FirebaseUtils.loadProfileThumbnail(context, targetUid, profile_circleimageview)
            FirebaseUtils.setUserOnlineStatus(this, targetUid, user_online_status)
        }

        val emojiConfig = androidx.emoji.bundled.BundledEmojiCompatConfig(this)
        EmojiCompat.init(emojiConfig)
            .registerInitCallback(object:EmojiCompat.InitCallback() {
                override fun onInitialized() {
                    setRecyclerAdapter()
                    super.onInitialized()
                }
            })


        messageInputField.setOnFocusChangeListener { v, hasFocus -> dateStickyHeader.visibility = View.GONE }




        layout_toolbar_title.setOnClickListener {
            val thumbnailTransition = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ActivityOptions.makeThumbnailScaleUpAnimation(it, it.toBitmap, 0,0).toBundle()
            } else null

            val sceneTransition = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ActivityOptions.makeSceneTransitionAnimation(this).toBundle()
            } else null

            val scaleTransition = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ActivityOptions.makeScaleUpAnimation(it, 0,0, it.width, it.height).toBundle()
            } else null

            startActivity(Intent(this, UserProfileActivity::class.java)
                .putExtra(FirebaseUtils.KEY_UID, targetUid)
                .putExtra(FirebaseUtils.KEY_NAME, nameOrNumber)
                .putExtra(utils.constants.KEY_IS_GROUP, isGroup )
            )
        }

        back_layout_toolbar_message.setOnClickListener {
            if(intent.getBooleanExtra(utils.constants.KEY_IS_ONCE, false))
                startActivity(Intent(context, HomeActivity::class.java))
            finish()
        }


        messageInputField.setTypingListener(object : MessageInput.TypingListener {
            override fun onStartTyping() {
                FirebaseUtils.setMeAsTyping(targetUid)
                dateStickyHeader.visibility = View.GONE
            }

            override fun onStopTyping() { FirebaseUtils.setMeAsOnline() }

        })


        Log.d("MessageActivity", "onCreate: myUID = $myUID")
        Log.d("MessageActivity", "onCreate: target UID = $targetUid")

        setSendMessageListener()




        smart_reply_layout.visibility = View.GONE
        checkIfBlocked(targetUid){

            if(isBlockedByUser || isBlockedByMe) {
                messageInputField.visibility = View.INVISIBLE
                smart_reply_layout.visibility = View.GONE
                blockedSnackbar?.show()

            }
            else {
                messageInputField.visibility = View.VISIBLE
                smart_reply_layout.visibility = View.VISIBLE
                blockedSnackbar?.dismiss()
                bindSmartReply()

            }
            invalidateOptionsMenu()

        }

        setMenuListeners()

        attachment_menu.visibility = View.INVISIBLE

        messageInputField.setAttachmentsListener {


            if(isBlockedByUser || isBlockedByMe)
                return@setAttachmentsListener


            if(attachment_menu.visibility != View.VISIBLE) {
                utils.setEnterRevealEffect(this, attachment_menu)
                
            }
            else {
                utils.setExitRevealEffect(attachment_menu)
                
            }



        }


    }


    private fun monitorGroupNameChanges(){
        // keep track of latest value just in case its changed and user is engaged with the screen
        FirebaseUtils.ref.groupInfo(targetUid)
            .child(utils.constants.KEY_NAME)
            .addValueEventListener(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                }

                override fun onDataChange(p0: DataSnapshot) {
                    nameOrNumber = p0.value.toString()
                    target_name_textview.text = nameOrNumber
                }
            })

    }


    private fun setMenuListeners(){

        val galleryIntent = Intent(context, ImagePickActivity::class.java)
        galleryIntent.putExtra(ImagePickActivity.IS_NEED_CAMERA, true)
        galleryIntent.putExtra(ImagePickActivity.IS_NEED_FOLDER_LIST, true)
        galleryIntent.putExtra(Constant.MAX_NUMBER,5)

        val videoIntent = Intent(context, VideoPickActivity::class.java)
        videoIntent.putExtra(VideoPickActivity.IS_NEED_CAMERA, true)
        videoIntent.putExtra(VideoPickActivity.IS_NEED_FOLDER_LIST, true)
        videoIntent.putExtra(Constant.MAX_NUMBER, 5)


        val audioIntent = intentFor<AudioPickActivity>().apply {
            putExtra(Constant.MAX_NUMBER, 5)
            putExtra(AudioPickActivity.IS_NEED_RECORDER, true)
        }

        camera_btn.setOnClickListener {

            if(attachment_menu.visibility != View.VISIBLE) utils.setEnterRevealEffect(this, attachment_menu) else utils.setExitRevealEffect(attachment_menu)


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED  && ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED) {

                    startCamera()

                }
                else {
                    requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), RP_STORAGE_CAMERA)
                }

            }
            else{
                startCamera()
            }

            
        }


        gallery_btn.setOnClickListener {

            if(!attachment_menu.visible) utils.setEnterRevealEffect(this, attachment_menu) else utils.setExitRevealEffect(attachment_menu)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED){

                    startActivityForResult(galleryIntent, RQ_GALLERY)

                }

                else {
                    requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), RP_STORAGE_GALLERY)
                }

            }
            else{
                startActivityForResult(galleryIntent, RQ_GALLERY)
            }
            
        }


        location_btn.setOnClickListener {

            if(attachment_menu.visibility != View.VISIBLE) utils.setEnterRevealEffect(this, attachment_menu) else utils.setExitRevealEffect(attachment_menu)

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


        video_pick_btn.setOnClickListener {
            if(attachment_menu.visibility != View.VISIBLE) utils.setEnterRevealEffect(this, attachment_menu)
            else utils.setExitRevealEffect(attachment_menu)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED){

                    startActivityForResult(videoIntent, RQ_VIDEO)

                }

                else {
                    requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), RP_STORAGE_GALLERY)
                }

            }
            else{
                startActivityForResult(videoIntent, RQ_VIDEO)
            }
            

        }

        audio_btn.setOnClickListener {
            if(!attachment_menu.visible) utils.setEnterRevealEffect(this, attachment_menu) else utils.setExitRevealEffect(attachment_menu)

            startActivityForResult(audioIntent, RQ_AUDIO)

        }

        recording_btn.setOnClickListener {
            if(!attachment_menu.visible) utils.setEnterRevealEffect(this, attachment_menu) else utils.setExitRevealEffect(attachment_menu)
            if(!utils.hasRecordingPermission(this)){
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), RP_RECORDING)
                }
                else
                    startAudioRecording()
            }
            else
                startAudioRecording()

        }
    }

    private fun startCamera(){
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
        val file = File(
            Environment.getExternalStoragePublicDirectory(DIRECTORY_DCIM).absolutePath
                    + "/IMG_" + timeStamp + ".jpg"
        )
        val mImagePath = file.absolutePath
        cameraImagePath = mImagePath

        val contentValues = ContentValues(1)
        contentValues.put(MediaStore.Images.Media.DATA, mImagePath)
        val mImageUri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        cameraImageUri = mImageUri

        intent.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri)

        startActivityForResult(intent, RQ_CAMERA)
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {


        if(grantResults.isEmpty())
            toast("Permission not granted")

        when(requestCode){
            RP_STORAGE_GALLERY -> {
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    val galleryIntent = Intent(context, ImagePickActivity::class.java)
                    galleryIntent.putExtra(ImagePickActivity.IS_NEED_CAMERA, true)
                    galleryIntent.putExtra(Constant.MAX_NUMBER,5)
                    startActivityForResult(galleryIntent, RQ_GALLERY)
                }
                else
                    utils.toast(context, "Permission denied")
            }
            RP_STORAGE_CAMERA -> {
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    startCamera()
                }
                else
                    utils.toast(context, "Permission denied")
            }
            RP_LOCATION -> {
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    startActivityForResult(Intent(context, MapsActivity::class.java), RQ_LOCATION)
                else
                    utils.toast(context, "Permission denied")
            }


            RP_INITAL_STORAGE_PERMISSION -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    setRecyclerAdapter()
                } else {
                    utils.toast(context, "Permission denied")
                    finish()
                }
            }

            RP_RECORDING -> {
                startAudioRecording()
            }
        }

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {


        if(resultCode == Activity.RESULT_CANCELED && requestCode == RQ_CAMERA){
            cameraImageUri?.let { contentResolver.delete(it, null,null) }
        }


        if(resultCode != Activity.RESULT_OK)
            return


        var messageID = "MSG" +System.currentTimeMillis()


        when (requestCode) {
            RQ_GALLERY -> {


                val filePaths = data!!.getParcelableArrayListExtra<ImageFile>(Constant.RESULT_PICK_IMAGE)



                if(filePaths.isNullOrEmpty())
                    return


                startActivityForResult(Intent(context, UploadPreviewActivity::class.java)
                    .putParcelableArrayListExtra(utils.constants.KEY_IMG_PATH, filePaths)
                    .putExtra(utils.constants.IS_FOR_SINGLE_FILE, false)
                    .putExtra(utils.constants.KEY_FILE_TYPE, utils.constants.FILE_TYPE_IMAGE)
                    ,
                    RQ_PREVIEW_IMAGE)


            }
            RQ_LOCATION -> {

                val latitude = data!!.getDoubleExtra(utils.constants.KEY_LATITUDE,0.0)
                val longitude = data.getDoubleExtra(utils.constants.KEY_LONGITUDE,0.0)
                val address = data.getStringExtra(utils.constants.KEY_ADDRESS)?:""

                if(latitude == 0.0 || longitude == 0.0){
                    utils.toast(context, "Failed to fetch location")
                    return
                }

                val message = "$latitude,$longitude"

                addMessageToBoth(messageID, Models.MessageModel(message,
                    myUID,
                    targetUid,
                    System.currentTimeMillis(),
                    isFile = false,
                    caption = address,
                    messageType = utils.constants.FILE_TYPE_LOCATION))
            }

            RQ_CAMERA -> {
                val file = File(cameraImagePath)
                Log.d("MessageActivity", "onActivityResult: "+file.path)

                if(file.path.isEmpty()){
                    utils.toast(context, "Failed to capture image")
                    return
                }


                startActivityForResult(Intent(context, UploadPreviewActivity::class.java)
                    .putExtra(utils.constants.KEY_IMG_PATH, file.path)
                    .putExtra(utils.constants.IS_FOR_SINGLE_FILE, true)
                    .putExtra(utils.constants.KEY_FILE_TYPE, utils.constants.FILE_TYPE_IMAGE)
                    ,
                    RQ_PREVIEW_IMAGE)

            }

            RQ_VIDEO -> {
                val videoPaths = data!!.getParcelableArrayListExtra<VideoFile>(Constant.RESULT_PICK_VIDEO)


                startActivityForResult(Intent(context, UploadPreviewActivity::class.java)
                    .putParcelableArrayListExtra(utils.constants.KEY_IMG_PATH, videoPaths)
                    .putExtra(utils.constants.IS_FOR_SINGLE_FILE, false)
                    .putExtra(utils.constants.KEY_FILE_TYPE, utils.constants.FILE_TYPE_VIDEO)
                    ,
                    RQ_PREVIEW_IMAGE)

            }


            // after returning from preview
            RQ_PREVIEW_IMAGE -> {
                val caption = data!!.getStringArrayListExtra(utils.constants.KEY_CAPTION)
                val imgPaths = data.getStringArrayListExtra(utils.constants.KEY_IMG_PATH)

                if (!imgPaths.isNullOrEmpty()) {

                    Log.d("MessageActivity", "onActivityResult: Uploading Image")


                    for((index, path) in imgPaths.withIndex()) {
                        messageID = "MSG" +System.currentTimeMillis()

                        uploadFile(
                            messageID, File(path.toString()),
                            caption[index],
                            data.getStringExtra(utils.constants.KEY_FILE_TYPE),
                            true
                        )
                    }

                }

            }
            RQ_AUDIO -> {
                val audioPaths = data?.getParcelableArrayListExtra<AudioFile>(Constant.RESULT_PICK_AUDIO)

                if (!audioPaths.isNullOrEmpty()) {



                    for(file in audioPaths) {
                        messageID = "MSG" +System.currentTimeMillis()


                        uploadFile(
                            messageID, File(file.path),
                            "",
                            utils.constants.FILE_TYPE_AUDIO,
                            true
                        )
                    }

                }
            }
            RQ_RECORDING -> // on recording
                audioFile?.let {
                    uploadFile("MSG${System.currentTimeMillis()}", it, "", utils.constants.FILE_TYPE_AUDIO, false )
                }
        }

        super.onActivityResult(requestCode, resultCode, data)

    }



   private fun setRecyclerAdapter(){


       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
           if (ActivityCompat.checkSelfPermission(
                   context,
                   Manifest.permission.READ_EXTERNAL_STORAGE
               ) == PackageManager.PERMISSION_GRANTED  && ActivityCompat.checkSelfPermission(
                   context,
                   Manifest.permission.WRITE_EXTERNAL_STORAGE
               ) == PackageManager.PERMISSION_GRANTED) {

           }
           else {
               requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), RP_INITAL_STORAGE_PERMISSION)
                return
           }

       }

       unreadMessageCount = intent.getIntExtra(utils.constants.KEY_UNREAD, 0)

       messagesList.setHasFixedSize(true)
       messagesList.setItemViewCacheSize(20)
       messagesList.isDrawingCacheEnabled = true
       messagesList.drawingCacheQuality = View.DRAWING_CACHE_QUALITY_HIGH

       val linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.stackFromEnd = true
        messagesList.layoutManager = linearLayoutManager

       setScrollingListener()


        val options = FirebaseRecyclerOptions.Builder<Models.MessageModel>()
            .setQuery(FirebaseUtils.ref.getChatRef(myUID, targetUid)

               // .limitToLast(20)
                ,Models.MessageModel::class.java)
            .build()


         adapter = object  : FirebaseRecyclerAdapter<Models.MessageModel, RecyclerView.ViewHolder>(options) {

            override fun onCreateViewHolder(p0: ViewGroup, viewType: Int): RecyclerView.ViewHolder {


               return when(viewType) {
                     TYPE_MINE ->
                       Holders.MyTextMsgHolder(LayoutInflater.from(this@MessageActivity)
                       .inflate(R.layout.bubble_right, p0 , false))

                     TYPE_MY_MAP ->
                         Holders.MyMapHolder(LayoutInflater.from(this@MessageActivity)
                             .inflate(R.layout.bubble_map_right, p0, false))


                     TYPE_TARGET_MAP ->
                       Holders.TargetMapHolder(LayoutInflater.from(this@MessageActivity)
                           .inflate(R.layout.bubble_map_left, p0, false))

                     TYPE_MY_IMAGE ->
                         Holders.MyImageMsgHolder(LayoutInflater.from(this@MessageActivity)
                             .inflate(R.layout.bubble_image_right, p0, false))

                   TYPE_TARGET_IMAGE ->
                       Holders.TargetImageMsgHolder(LayoutInflater.from(this@MessageActivity)
                           .inflate(R.layout.bubble_image_left, p0, false))

                   TYPE_MY_VIDEO ->
                       Holders.MyVideoMsgHolder(LayoutInflater.from(this@MessageActivity)
                           .inflate(R.layout.bubble_video_right, p0, false))

                   TYPE_TARGET_VIDEO ->
                       Holders.TargetVideoMsgHolder(LayoutInflater.from(this@MessageActivity)
                           .inflate(R.layout.bubble_video_left, p0, false))

                   TYPE_EVENT ->
                       Holders.TextHeaderHolder(LayoutInflater.from(context).inflate(R.layout.text_header, p0, false))

                   TYPE_MY_AUDIO ->
                       Holders.MyAudioHolder(LayoutInflater.from(context).inflate(R.layout.bubble_audio_right,p0, false))

                   TYPE_TARGET_AUDIO ->
                       Holders.TargetAudioHolder(LayoutInflater.from(context).inflate(R.layout.bubble_audio_left,p0, false))


                   else -> Holders.TargetTextMsgHolder(LayoutInflater.from(this@MessageActivity)
                           .inflate(R.layout.bubble_left, p0, false))
               }
            }


             override fun getItemCount(): Int {

                 message_progressbar.visibility = View.GONE

                 return super.getItemCount()
             }

             override fun onDataChanged() {

                 super.onDataChanged()
                 val focusedMessage = intent.getSerializableExtra(utils.constants.KEY_MSG_MODEL) as? Models.MessageModel
                     ?: return

                 selectedPosition = snapshots.indexOf(focusedMessage)
                 messagesList.scrollToPosition(selectedPosition)
                 searchFilterItemPosition.add(selectedPosition)
                 notifyItemChanged(selectedPosition)

             }


            @SuppressLint("ObjectAnimatorBinding", "SetTextI18n")
            override fun onBindViewHolder(
                holder: RecyclerView.ViewHolder,
                position: Int,
                model: Models.MessageModel) {



                if(getItemViewType(position) == TYPE_EVENT){

                    val textHolder = holder as Holders.TextHeaderHolder

                    when(model.messageType) {
                         FirebaseUtils.EVENT_TYPE_ADDED -> {
                            textHolder.text.text = utils.getNameFromNumber(context, model.from) +" added " +utils.getNameFromNumber(context, model.message)
                        }
                        FirebaseUtils.EVENT_TYPE_REMOVED -> {
                            textHolder.text.text = utils.getNameFromNumber(context, model.from)  +" removed " +utils.getNameFromNumber(context, model.message)

                        }
                        FirebaseUtils.EVENT_TYPE_LEFT -> {
                            textHolder.text.text = utils.getNameFromNumber(context, model.message) +" left"
                        }
                        FirebaseUtils.EVENT_TYPE_CREATED -> {
                            textHolder.text.text = utils.getNameFromNumber(context, model.from) +" created this group"
                        }
                    }

                    //setting header if event
                    textHolder.dateTextView.text = utils.getHeaderFormattedDate(model.timeInMillis)
                    if(position>0){

                        val previousDate = Date(snapshots[position - 1].timeInMillis)

                        textHolder.dateTextView.visibility =  if(!DateFormatter.isSameDay(Date(model.timeInMillis) ,  previousDate)){ View.VISIBLE }
                        else{ View.GONE }

                    }
                    else{

                        textHolder.dateTextView.visibility = View.VISIBLE

                    }


                    return
                }


                setObserver()


                var messageImage:ImageView? = null
                var targetSenderIcon:ImageView? = null
                var targetSenderTitle:TextView? = null
                var dateHeader:TextView? = null
                var latitude = 0.0
                var longitude = 0.0
                val messageID = super.getRef(position).key!!
                var thumbnail:ImageView? = null
                var videoLengthTextView:TextView? = null

                var container:LinearLayout? = null

                var tapToDownload:TextView? = null
                var messageTextView:TextView? = null

                var messageLayout:View? = null


                val date = Date(model.timeInMillis)


                 try {


                    if(model.from != FirebaseUtils.getUid())
                        FirebaseUtils.setReadStatusToMessage(messageID, targetUid)

                }
                catch (e:Exception){
                    Log.d("MessageActivity", "onBindViewHolder: ${e.message}")}

                if(model.messageType == utils.constants.FILE_TYPE_LOCATION){


                    try {
                    latitude = model.message.split(",")[0].toDouble()
                    longitude = model.message.split(",")[1].toDouble()
                    }
                    catch (e :Exception){}

                }



                when (holder) {
                    is Holders.TargetTextMsgHolder -> {
                        holder.time.text = utils.getLocalTime(model.timeInMillis)
                        holder.message.text = model.message
                        container = holder.container
                        messageTextView = holder.message
                        messageLayout = holder.messageLayout
                        dateHeader = holder.headerDateTime

                        holder.itemView.bubble_left_translation.visibility = View.GONE
                        targetSenderIcon = holder.senderIcon

                        targetSenderTitle = holder.senderTitle

                    }
                    is Holders.MyTextMsgHolder -> {
                        holder.time.text = utils.getLocalTime(model.timeInMillis)
                        holder.message.text = model.message
                        dateHeader = holder.headerDateTime
                        container = holder.container
                        FirebaseUtils.setDeliveryStatusTick(targetUid, messageID, holder.messageStatus)
                        messageTextView = holder.message
                        messageLayout = holder.messageLayout
                        holder.itemView.bubble_right_translation.visibility = View.GONE
                        //end of my holder

                    }
                    is Holders.MyImageMsgHolder -> {

                        holder.time.text = utils.getLocalTime(model.timeInMillis)
                        messageImage = holder.imageView
                        container = holder.container
                        dateHeader = holder.headerDateTime

                        FirebaseUtils.setDeliveryStatusTick(targetUid, messageID, holder.messageStatus)


                        messageTextView = holder.message
                        messageLayout = holder.messageLayout


                        //setting holder config
                        setMyImageHolder(holder, model, messageID)

                    }
                    is Holders.TargetImageMsgHolder -> {
                        messageImage = holder.imageView
                        dateHeader = holder.headerDateTime
                        container = holder.container
                        holder.time.text = utils.getLocalTime(model.timeInMillis)
                        messageTextView = holder.message
                        messageLayout = holder.messageLayout

                        targetSenderIcon = holder.senderIcon
                        targetSenderTitle = holder.senderTitle

                        //setting holder setting
                        setTargetImageHolder(holder, model, messageID)


                    }
                    is Holders.MyVideoMsgHolder -> {

                        thumbnail = holder.thumbnail
                        videoLengthTextView = holder.videoLengthText
                        holder.time.text = utils.getLocalTime(model.timeInMillis)
                        tapToDownload = holder.tap_to_download
                        dateHeader = holder.headerDateTime
                        container = holder.container


                        FirebaseUtils.setDeliveryStatusTick(targetUid, messageID, holder.messageStatus)
                        messageTextView = holder.message
                        messageLayout = holder.messageLayout

                        //setting holder config
                        setMyVideoHolder(holder, model, messageID)


                    }
                    is Holders.TargetVideoMsgHolder -> {

                        tapToDownload = holder.tap_to_download
                        holder.time.text = utils.getLocalTime(model.timeInMillis)
                        thumbnail = holder.thumbnail
                        videoLengthTextView = holder.videoLengthText
                        dateHeader = holder.headerDateTime
                        container = holder.container

                        targetSenderIcon = holder.senderIcon
                        targetSenderTitle = holder.senderTitle

                        messageTextView = holder.message
                        messageLayout = holder.messageLayout


                        //setting holder config
                        setTargetVideoHolder(holder, model, messageID)




                    }

                    is Holders.MyMapHolder -> {
                        holder.message.text = model.caption
                        holder.message.visibility =  if(model.caption.isEmpty()) View.GONE else View.VISIBLE
                        holder.time.text = utils.getLocalTime(model.timeInMillis)
                        messageLayout = holder.messageLayout
                        dateHeader = holder.dateHeader
                        messageTextView = holder.message
                        container = holder.container

                        loadMap(holder.mapView, LatLng(latitude,longitude))
                        FirebaseUtils.setDeliveryStatusTick(targetUid, messageID, holder.messageStatus)


                    }

                    is Holders.TargetMapHolder -> {

                        holder.message.text = model.caption
                        dateHeader = holder.dateHeader
                        container = holder.container
                        holder.time.text = utils.getLocalTime(model.timeInMillis)
                        messageLayout = holder.messageLayout
                        messageTextView = holder.message

                        targetSenderIcon = holder.senderIcon
                        targetSenderTitle = holder.senderTitle


                        loadMap(holder.mapView, LatLng(latitude,longitude))


                        holder.message.visibility =  if(model.caption.isEmpty()) View.GONE else View.VISIBLE




                    }

                    is Holders.MyAudioHolder -> {
                        messageLayout = holder.messageLayout
                        container = holder.itemView.container_right
                        dateHeader = holder.dateTextView
                        holder.time.text = utils.getLocalTime(model.timeInMillis)

                        holder.itemView.item_audio_container.setOnClickListener {
                            if(File(model.file_local_path).exists()) playAudio(model.file_local_path) else downloadAudio(model, messageID)
                        }
                        CircularProgressBarsAt[messageID] = holder.audioProgressBar

                        val audioFile = File(model.file_local_path)
                         holder.lengthOrSize.text = if(audioFile.exists()) utils.getAudioVideoLength(context, audioFile.path)
                        else utils.getFileSize(model.file_size_in_bytes)

                        if(model.message.isEmpty()){
                            //show retry
                            holder.audioIcon.setImageResource(R.drawable.ic_file_upload_white_24dp)
                            holder.audioIcon.setOnClickListener {
                                fileUpload(messageID, File(model.file_local_path), model.file_local_path,"", utils.constants.FILE_TYPE_AUDIO)
                            }

                         }

                        if(!audioFile.exists())
                            holder.audioIcon.setImageResource(R.drawable.ic_file_download_white_24dp)

                        downloadAudio(model, messageID)

                    }

                    is Holders.TargetAudioHolder -> {
                        messageLayout = holder.messageLayout
                        container = holder.itemView.container_left
                        dateHeader = holder.dateTextView
                        holder.time.text = utils.getLocalTime(model.timeInMillis)

                        holder.itemView.item_audio_container.setOnClickListener {
                            if(File(model.file_local_path).exists()) playAudio(model.file_local_path) else downloadAudio(model, messageID)
                        }
                        CircularProgressBarsAt[messageID] = holder.audioProgressBar

                        val audioFile = File(model.file_local_path)
                        holder.lengthOrSize.text = if(audioFile.exists()) utils.getAudioVideoLength(context, audioFile.path)
                        else utils.getFileSize(model.file_size_in_bytes)

                        downloadAudio(model, messageID)

                    }
                }




                //setting sender icon
                targetSenderIcon?.let {
                    if(position==0){
                        FirebaseUtils.loadProfileThumbnail(context, model.from, it)
                        it.visibility = View.VISIBLE }
                    else{
                        if(model.from == snapshots[position -1 ].from && DateFormatter.isSameDay(Date(model.timeInMillis), Date(snapshots[position-1].timeInMillis))) it.visibility = View.INVISIBLE
                        else {
                            it.visibility = View.VISIBLE
                            FirebaseUtils.loadProfileThumbnail(context, model.from, it)
                        }
                    }
                }

                //setting sender title
                targetSenderTitle?.let {
                    if(isGroup) {

                        it.visibility = if(targetSenderIcon?.visibility == View.VISIBLE) View.VISIBLE
                        else View.GONE

                        if (groupMembers.isNotEmpty())
                            try {
                                it.text =
                                    utils.getNameFromNumber(
                                        context,
                                        groupMembers.filter { it.uid == model.from }[0].phoneNumber
                                    )
                                FirebaseUtils.setTargetOptionMenu(context,model.from,
                                    groupMembers.filter { it.uid == model.from}[0].phoneNumber,
                                    it)
                            }
                            catch (e:Exception){
                                it.text = "Removed Member"
                            }
                        it.setTextColor(ColorGenerator.MATERIAL
                            .getColor(it.text.toString()))
                    }
                }


                if(container!=null) {
                    // add unread header
                    val unreadView = (layoutInflater.inflate(R.layout.text_header, container, false))
                    container.removeView(unreadView)
                    if (unreadMessageCount > 0 && (adapter.itemCount - unreadMessageCount - 1) == position) {
                        unreadView.header_textView.text = "$unreadMessageCount unread messages"
                        unreadHeaderPosition = position
                        unreadMessageCount = 0
                    }
                }


                //loading message Image listener
                messageImage?.setOnClickListener {
                    if(!isContextMenuActive)
                        startActivity(
                            Intent(context, ImagePreviewActivity::class.java)
                                .putExtra(utils.constants.KEY_MSG_MODEL, model)
                                .putExtra(utils.constants.KEY_IMG_PATH, model.message.toString())
                                .putExtra(utils.constants.KEY_LOCAL_PATH, model.file_local_path.toString())
                        )
                }



                //setting video intent
                if(thumbnail != null){

                    if(File(model.file_local_path).exists()){
                        videoLengthTextView?.text = utils.getAudioVideoLength(context, model.file_local_path)

                        utils.loadVideoThumbnailFromLocalAsync(context, thumbnail, model.file_local_path)
                        tapToDownload?.hide()
                    }
                    else{

                        utils.setVideoThumbnailFromWebAsync(context, model.message, thumbnail)
                        videoLengthTextView?.text = utils.getFileSize(model.file_size_in_bytes)

                        Log.d("MessageActivity", "onBindViewHolder: $messageID file not found")

                            tapToDownload?.show()
                            tapToDownload?.setOnClickListener {

                                if(isContextMenuActive)
                                    return@setOnClickListener

                                downloadVideo(model , messageID)
                                it.visibility = View.GONE
                            }

                    }

                    thumbnail.setOnClickListener {


                        if(isContextMenuActive)
                            return@setOnClickListener

                        if(model.file_local_path.isNotEmpty() && File(model.file_local_path).exists()) {

                            utils.startVideoIntent(context, model.file_local_path)
                        }
                        else {
                         //   downloadVideo(messageID, holder.progressBar)
                            utils.toast(context, "File not found on the device")
                        }

                    }

                }



                messageTextView?.let { it.setLinkTextColor(Color.BLUE)
                    val emojiProcessed = EmojiCompat.get().process(messageTextView.text)
                    messageTextView.text = emojiProcessed
                    messageTextView.setLinkTextColor(Color.RED)
                }


                //set date Header

                dateHeader?.text = utils.getHeaderFormattedDate(model.timeInMillis)
                if(position>0){
                    val previousDate = Date(snapshots[position - 1].timeInMillis)
                    dateHeader?.visible =  !DateFormatter.isSameDay(date ,  previousDate)
                }
                else{
                    dateHeader?.show()
                }
                dateHeader?.setPadding(20,80,20,80)



                //setting contextual toolbar
                setContextualToolbarOnViewHolder(messageLayout as View, messageID, model, position)



                if(searchFilterItemPosition.contains(position) ){
                    messageTextView?.let { utils.highlightTextView(it, searchQuery, Color.parseColor("#51C1EE")) }

                    if(selectedPosition == position) {
                        val fadeAnim = ObjectAnimator.ofObject(messageLayout, "backgroundColor",
                            ArgbEvaluator(),Color.parseColor("#51C1EE"), Color.TRANSPARENT)
                        fadeAnim.duration = 2000
                        fadeAnim.start()
                        selectedPosition = -1
                    }
                }
                else{

                    messageTextView?.text = if(model.isFile || model.messageType == utils.constants.FILE_TYPE_LOCATION)
                        model.caption else model.message
                }




               // holder.itemView.setPadding(0,10,0,10)


            }


            override fun getItemViewType(position: Int): Int {

                val model: Models.MessageModel = getItem(position)

                val viewType: Int

                if(model.messageType == FirebaseUtils.EVENT_TYPE_REMOVED ||
                        model.messageType == FirebaseUtils.EVENT_TYPE_LEFT ||
                        model.messageType == FirebaseUtils.EVENT_TYPE_ADDED ||
                    model.messageType == FirebaseUtils.EVENT_TYPE_CREATED)
                    return TYPE_EVENT


                viewType = if(model.from == myUID){

                    when {
                        model.messageType == utils.constants.FILE_TYPE_LOCATION -> TYPE_MY_MAP
                        model.messageType == utils.constants.FILE_TYPE_IMAGE -> TYPE_MY_IMAGE
                        model.messageType == utils.constants.FILE_TYPE_VIDEO -> TYPE_MY_VIDEO
                        model.messageType == utils.constants.FILE_TYPE_AUDIO -> TYPE_MY_AUDIO
                        else -> TYPE_MINE
                    }
                } else{
                    when {
                        model.messageType == utils.constants.FILE_TYPE_LOCATION -> TYPE_TARGET_MAP
                        model.messageType == utils.constants.FILE_TYPE_IMAGE -> TYPE_TARGET_IMAGE
                        model.messageType == utils.constants.FILE_TYPE_VIDEO -> TYPE_TARGET_VIDEO
                        model.messageType == utils.constants.FILE_TYPE_AUDIO -> TYPE_TARGET_AUDIO
                        else -> TYPE_TARGET
                    }

                }


                return viewType


            }

        }

        messagesList.adapter = adapter




       adapter.startListening()




    }


    private fun <T:RecyclerView.ViewHolder> bindAudioHolder(){

    }

    private fun setObserver(){
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {


                val model = adapter.snapshots[positionStart]

                val layoutManager = messagesList.layoutManager as LinearLayoutManager
                val lastVisiblePosition = layoutManager.findLastCompletelyVisibleItemPosition()

                // If the recycler view is initially being loaded or the
                // user is at the bottom of the list, scroll to the bottom
                // of the list to show the newly added message.

                if (lastVisiblePosition == -1 ||
                    (positionStart >= (adapter.itemCount - 1) &&
                            lastVisiblePosition == (positionStart - 1))) {
                        messagesList.scrollToPosition(adapter.itemCount - 1 - unreadMessageCount)
                }

                if(model.from == myUID)
                    messagesList.scrollToPosition(adapter.itemCount - 1 - unreadMessageCount)



                super.onItemRangeInserted(positionStart, itemCount)
            }


        })

    }



    private fun setSendMessageListener(){



        messageInputField.setInputListener {

            if(isBlockedByMe || isBlockedByUser) {
                return@setInputListener true
            }

            val message = messageInputField.inputEditText.text.toString()

            val messageModel= Models.MessageModel(message.trim() ,
                myUID, targetUid ,isFile = false)

            val messageID = "MSG" +System.currentTimeMillis()

            adapter.notifyItemChanged(0)

            addMessageToBoth(messageID, messageModel)
            loadedPosition[messagesList.adapter!!.itemCount ]



            false
        }

    }


    private fun setTapToRetryBtn(
        tapToRetry:View, progressBar: CircularProgressBar, filePath:String, messageID: String,
        caption: String, fileType:String){



        tapToRetry.visibility = View.GONE

        FirebaseUtils.ref.getChatRef(myUID, targetUid)
            .child(messageID)
            .child("message")
            .addValueEventListener(object :ValueEventListener{
                override fun onDataChange(p0: DataSnapshot) {



                    if(p0.exists())
                    tapToRetry.visibility = if(p0.getValue(String::class.java)!!.isEmpty()) View.VISIBLE else View.GONE

                    if(isUploading[messageID] == true)
                        tapToRetry.visibility = View.GONE


                }

                override fun onCancelled(p0: DatabaseError) {
                }
            })

        tapToRetry.setOnClickListener {
          progressBar.visibility = View.VISIBLE
            it.visibility = View.GONE

            if(File(filePath).exists())
                uploadFile(
                    messageID, File(filePath.toString()),
                    caption, fileType, false
                )
            else{
                utils.toast(context, "File does not exists on this device")
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun loadMap(mapView: MapView, latLng: LatLng){
        //loading a map

        try {
            mapView.run {
                onCreate(null)

                getMapAsync { googleMap ->


                    googleMap?.addMarker(
                        MarkerOptions()
                            .position(latLng).title("")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                            .draggable(false).visible(true)
                    )


                    googleMap?.uiSettings?.setAllGesturesEnabled(false)
                    googleMap?.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            latLng,
                            12F
                        )
                    )

                    Log.d("MessageActivity", "onMapReady: ")
                }
            }
        }
        catch (e:Exception){}

    }



    private fun addMessageToBoth(messageID: String , messageModel: Models.MessageModel){

        //setting  message to both


        addMessageToMyNode(messageID, messageModel)

        if(isGroup) addMessageToGroupMembers(messageID, messageModel)
        else addMessageToTargetNode(messageID, messageModel)


    }


    private fun addMessageToMyNode(messageID: String , messageModel: Models.MessageModel){

        //setting my message
        unreadMessageCount = 0

        unreadFirstMessageID = ""

        FirebaseUtils.ref.getChatRef(myUID, targetUid) // if it is group then targetUid is group id
            .child(messageID)
            .setValue(messageModel)
            .addOnSuccessListener {

                FirebaseUtils.setMessageStatusToDB(messageID, myUID, targetUid, true, true,
                    nameOrNumber)

                FirebaseUtils.ref.lastMessage(myUID)
                    .child(targetUid)
                    .setValue(Models.LastMessageDetail(type = targetType, nameOrNumber = nameOrNumber))


                // for a rare case
                if(nameOrNumber.isEmpty()){
                    FirebaseUtils.ref.user(targetUid).child(FirebaseUtils.KEY_PHONE)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onCancelled(p0: DatabaseError) {}

                            override fun onDataChange(p0: DataSnapshot) {
                                val phoneNumber = p0.getValue(String::class.java)
                                if(p0.exists())
                                FirebaseUtils.ref.lastMessage(myUID)
                                    .child(targetUid).child("nameOrNumber").setValue(phoneNumber)
                            }
                        })
                }



                print("Message sent to $targetUid") }

    }



    private fun addMessageToTargetNode(messageID: String , messageModel: Models.MessageModel) {

        //setting  message to target
        FirebaseUtils.ref.getChatRef(targetUid, myUID)  // must be (participant, groupID)
            .child(messageID)
            .setValue(messageModel)
            .addOnSuccessListener {

                FirebaseUtils.setMessageStatusToDB(messageID, targetUid, myUID,false, false, nameOrNumber)

                FirebaseUtils.ref.lastMessage(targetUid)
                    .child(myUID)
                    .setValue(Models.LastMessageDetail(nameOrNumber = FirebaseUtils.getPhoneNumber()))

                print("Message added to mine") }

    }


    //for group members
    private fun addMessageToGroupMembers(messageID: String , messageModel: Models.MessageModel
                                       ) {

        groupMembers.forEach {
            val memberID = it.uid

            //setting  message to target
            if(memberID != myUID) {

                Log.d("MessageActivity", "addMessageToGroupMembers: targets -> $memberID")

                FirebaseUtils.ref.getChatRef(memberID, targetUid)  // must be (participant, groupID)
                    .child(messageID)
                    .setValue(messageModel)
                    .addOnSuccessListener {

                        FirebaseUtils.setMessageStatusToDB(messageID, memberID, targetUid, false, false, nameOrNumber)

                        FirebaseUtils.ref.lastMessage(memberID)
                            .child(targetUid)
                            .setValue(Models.LastMessageDetail(type = FirebaseUtils.KEY_CONVERSATION_GROUP,
                                nameOrNumber = if(nameOrNumber.isNotEmpty()) nameOrNumber else target_name_textview.text.toString() ))

                    }
            }
        }




    }


    //upload
    private fun uploadFile(
        messageID: String,
        file: File,
        caption: String,
        messageType: String,
        isNewIDRequired:Boolean
    ){


        Log.d("MessageActivity", "fileUpload: dir = "+file.path)

        val fileSizeInMB = (file.length()/(1024* 1024))

        Log.d("MessageActivity", "uploadFile: file size = $fileSizeInMB")

        if(fileSizeInMB > 16){
            utils.longToast(context, "File size exceeded by 16 MB, Please choose a smaller file")
            return
        }

        val originalPath = file.path



        //Initial node
        var messageModel= Models.MessageModel(
            "",
            myUID, targetUid ,isFile = true, caption = caption, messageType = messageType, file_local_path = originalPath,
            file_size_in_bytes = file.length())


        isUploading[messageID] = true





        when(messageType) {

            utils.constants.FILE_TYPE_IMAGE -> {

                //image compressor
                Luban.compress(context, File(file.path))
                    .putGear(Luban.THIRD_GEAR)
                    .clearCache()
                    .launch(object : OnCompressListener {
                        override fun onError(e: Throwable?) {
                            //setting up node for original image
                            messageModel= Models.MessageModel(
                                "",
                                myUID, targetUid ,isFile = true,
                                caption = caption, messageType = messageType,
                                file_local_path = originalPath,
                                file_size_in_bytes = file.length())


                            //uploading original image
                            //this happens in a rare case when cache is bad
                            val newID = if(isNewIDRequired) "MSG" +System.currentTimeMillis() else messageID

                            isUploading[newID] = true
                            fileUpload(newID, file, originalPath, caption, messageType)

                            addMessageToMyNode(newID, messageModel)

                            utils.toast(context, "Failed to compress, uploading original image")
                        }

                        override fun onStart() {

                        }

                        override fun onSuccess(file: File?) {

                            //setting up node for compressed image
                            messageModel= Models.MessageModel(
                                "",
                                myUID, targetUid ,isFile = true,
                                caption = caption, messageType = messageType,
                                file_local_path = originalPath,
                                file_size_in_bytes = file!!.length())

                            val fileSizeInMB = (file.length()/(1024* 1024))


                            val newID = if(isNewIDRequired) "MSG" +System.currentTimeMillis() else messageID

                            isUploading[newID] = true


                            Log.d("MessageActivity", "uploadFile: file size = $fileSizeInMB")

                            if(fileSizeInMB > 16){
                                utils.toast(context, "File size exceeded by 16 MB, Please choose a smaller file")
                                return
                            }

                            fileUpload(newID, file, originalPath, caption, messageType)

                            addMessageToMyNode(newID, messageModel)

                        }
                    })
            }



            //for video upload
            utils.constants.FILE_TYPE_VIDEO -> {

                fileUpload(messageID, file, originalPath, caption, messageType)

                addMessageToMyNode(messageID, messageModel)


            }

            // for audio Upload
            utils.constants.FILE_TYPE_AUDIO -> {
                fileUpload(messageID, file, originalPath, caption, messageType)
                addMessageToMyNode(messageID, messageModel)
            }


        }




    }



    private fun fileUpload(
        messageID: String,
        file: File,
        originalFinalPath: String,
        caption: String,
        messageType: String
    ) {


        Log.d("MessageActivity", "fileUpload: path = ${file.path}")

        val ref =  FirebaseStorage.getInstance()
            .reference.child(messageType).child(messageID)

        val uploadTask = ref.putFile(utils.getUriFromFile(context, file))


        val progressBar = try { CircularProgressBarsAt[messageID] } catch (e:Exception){ null }
        val mediaControl = try { mediaControlImageViewAt[messageID] } catch (e:Exception){ null }
        //setting initial value

        progressBar?.progress = 0f
        progressBar?.enableIndeterminateMode(true)


        val position = adapter.snapshots.indexOfFirst { it.file_local_path == file.path }


        uploadTask.addOnProgressListener { taskSnapshot ->



                val percentage:Double = (100.0 * taskSnapshot.bytesTransferred) / taskSnapshot.totalByteCount


                if(CircularProgressBarsAt.containsKey(messageID)){

                        if(percentage.toInt() < 5){
                            CircularProgressBarsAt[messageID]?.enableIndeterminateMode(true)
                        }
                        else{
                            CircularProgressBarsAt[messageID]?.enableIndeterminateMode(false)
                        }

                        CircularProgressBarsAt[messageID]?.progress = percentage.toFloat()


                }


            //setting cancel button value
            mediaControl?.show()
            mediaControl?.setOnClickListener {


                if(percentage >= 100)
                    return@setOnClickListener



                if(BuildConfig.DEBUG)
                    utils.toast(context, "Upload cancelled")


                uploadTask.cancel()
                mediaControl.setImageResource(R.drawable.ic_play_white)

                try { adapter.notifyItemChanged(position) }
                catch (e:Exception){ e.printStackTrace(); adapter.notifyDataSetChanged() }
            }


             }
            .continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
                if (!task.isSuccessful) {
                    task.exception?.let {
                        throw it
                    }
                }

                return@Continuation ref.downloadUrl
            })
            .addOnCanceledListener {
                isUploading[messageID] = false
                    progressBar?.hide()
                if(position in 0 until adapter.itemCount)
                    adapter.notifyItemChanged(position)

                Log.d("MessageActivity", "fileUpload: upload cancelled")

            }
            .addOnCompleteListener { task ->

                isUploading[messageID] = false

                mediaControl?.hide()


                if (task.isSuccessful) {
                    val link = task.result
                    val time = System.currentTimeMillis()
                    val targetModel = Models.MessageModel(
                        link.toString(),
                        myUID, targetUid,
                        isFile = true, caption = caption, messageType = messageType,
                        file_size_in_bytes = file.length(),
                        timeInMillis = time,
                        reverseTimeStamp = time * -1
                    )

                    if (BuildConfig.DEBUG)
                        utils.toast(context, "Uploaded")


                    if(isGroup) addMessageToGroupMembers(messageID, targetModel)
                    else addMessageToTargetNode(messageID, targetModel)


                    val myModel = Models.MessageModel(
                        link.toString(),
                        myUID, targetUid,
                        isFile = true, caption = caption, messageType = messageType,
                        file_local_path = originalFinalPath,
                        file_size_in_bytes = file.length(),
                        timeInMillis = time,
                        reverseTimeStamp = time * -1
                    )

                    addMessageToMyNode(messageID, myModel)

                    FirebaseUtils.storeFileMetaData(
                        Models.File(messageID,
                        time, fileType = messageType,
                            fileSizeInBytes = file.length(),
                            bucket_path = ref.bucket,
                            file_url = link.toString(),
                        file_extension = utils.getFileExtension(file)
                    ))


                } else {
                    toast("Failed to upload. File might not be found.")
                    task.exception?.printStackTrace()
                }
            }




    }


    //downloading video and saving to file in the form of file
    private fun downloadVideo(model: Models.MessageModel, messageID: String){

        val itemHolder = messagesList.findViewHolderForAdapterPosition(adapter.snapshots.indexOf(model))

        val myHolder = itemHolder  as? Holders.MyVideoMsgHolder
        val targetHolder = itemHolder as? Holders.TargetVideoMsgHolder

        val progressBar = myHolder?.progressBar?:targetHolder?.progressBar

        progressBar?.show()
        progressBar?.progress =0f


        val storageRef =
            FirebaseStorage.getInstance().getReferenceFromUrl(model.message)

            val videoFile = utils.getVideoFile(messageID, model.from == myUID)

        Log.d("MessageActivity", "downloadVideo: downloading video to location = ${videoFile.path}")

            storageRef.getFile(videoFile)
                .addOnProgressListener {
                    val percentage:Double = (100.0 * it.bytesTransferred) / it.totalByteCount
                    progressBar?.progress = percentage.toFloat()

                }
                .addOnCompleteListener{
                    progressBar?.hide()
                }
                .addOnCanceledListener {
                    progressBar?.hide()
                }
                .addOnSuccessListener {

                    utils.addMediaToMediaStore(context, messageID, videoFile)

                    val children = mapOf(FirebaseUtils.KEY_FILE_LOCAL_PATH to videoFile.path)

                    FirebaseUtils.ref.getChatRef(myUID,targetUid)
                        .child(messageID)
                        .updateChildren(children)

                }





    }


    //downloading audio and saving to file in the form of file
    private fun downloadAudio(model: Models.MessageModel, messageID: String){

        if(model.message.isEmpty()) return

        val itemHolder = messagesList.findViewHolderForAdapterPosition(adapter.snapshots.indexOf(model))

        val myHolder = itemHolder  as? Holders.MyAudioHolder
        val targetHolder = itemHolder as? Holders.TargetAudioHolder

        val progressBar = myHolder?.audioProgressBar?:targetHolder?.audioProgressBar

        progressBar?.show()
        progressBar?.progress =0f


        val storageRef =
            FirebaseStorage.getInstance().getReferenceFromUrl(model.message)

        val audioFile = utils.getAudioFile(messageID, model.from == myUID)

        Log.d("MessageActivity", "downloadVideo: downloading audio to location = ${audioFile.path}")

        storageRef.getFile(audioFile)
            .addOnProgressListener {
                val percentage:Double = (100.0 * it.bytesTransferred) / it.totalByteCount
                Log.d("MessageActivity", "downloadAudio: progress = $percentage")
                progressBar?.progress = percentage.toFloat()

            }
            .addOnCompleteListener{
                progressBar?.hide()
            }
            .addOnCanceledListener {
                progressBar?.hide()
            }
            .addOnSuccessListener {

                utils.addMediaToMediaStore(context, messageID, audioFile, "audio/mp3")

                val children = mapOf(FirebaseUtils.KEY_FILE_LOCAL_PATH to audioFile.path)

                FirebaseUtils.ref.getChatRef(myUID,targetUid)
                    .child(messageID)
                    .updateChildren(children)

            }





    }


    override fun onStart() {
        super.onStart()


        try {

            adapter.startListening()

        } catch (e:Exception){}
    }


    override fun onDestroy() {
        super.onDestroy()

        Pref.setCurrentTargetUID(context, "")

        if(!asyncLoader?.isDone!!)
            asyncLoader?.cancel(true)

        try {

            adapter.stopListening()


    } catch (e:Exception){}

}


    var blockedSnackbar: Snackbar? = null

    private fun checkIfBlocked(targetUID:String, onChecked:()->Unit) {



        //check if i have blocked
        FirebaseUtils.ref.blockedUser(myUID, targetUID)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {

                    isBlockedByMe = if (dataSnapshot.exists())
                        dataSnapshot.getValue(Boolean::class.java)!!
                    else
                        false

                    onChecked.invoke()


                }

                override fun onCancelled(databaseError: DatabaseError) {

                }
            })
        //check i am blocked by user

        FirebaseUtils.ref.blockedUser(targetUID, myUID)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {

                    isBlockedByUser = if (dataSnapshot.exists())
                        dataSnapshot.getValue(Boolean::class.java)!!
                    else
                        false

                    onChecked.invoke()


                }

                override fun onCancelled(databaseError: DatabaseError) {

                }
            })


    }


    override fun onResume() {
        invalidateOptionsMenu()
        FirebaseUtils.setMeAsOnline()
        //cancel any notification, if any
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(MessagingService.NotificationDetail.SINGLE_ID)
        notificationManager.cancel(MessagingService.NotificationDetail.MUlTIPLE_ID)

        //setting current target for notification
        Pref.setCurrentTargetUID(context, targetUid)

        super.onResume()
    }


    override fun onPause() {


        if(utils.isAppIsInBackground(this)) {
            FirebaseUtils.setMeAsOffline()
            Pref.setCurrentTargetUID(context,"")
        }

        super.onPause()
    }

    //setting my Holders
    //setting my holder config
    private fun setMyImageHolder(holder: Holders.MyImageMsgHolder, model: Models.MessageModel, messageID: String){
        holder.tapToRetry.visibility = View.GONE

        holder.progressBar.visibility = View.VISIBLE
        CircularProgressBarsAt[messageID] = holder.progressBar
        mediaControlImageViewAt[messageID] = holder.imageUploadControl

        holder.imageUploadControl.hide()

        holder.cardContainer.setCornerEnabled(true,true, model.caption.isEmpty(), false)



        holder.message.visibility =  if(model.caption.isEmpty()) View.GONE else View.VISIBLE


        setTapToRetryBtn(holder.tapToRetry,holder.progressBar,model.file_local_path, messageID,model.caption, model.messageType)


        if(model.file_local_path.isNotEmpty() && File(model.file_local_path).exists()){

            //when image is not uploaded

            Picasso.get()
                .load(File(model.file_local_path.toString()))
                //  .resize(600,500)
                .fit()
                .centerCrop()
                .error(R.drawable.error_placeholder2)
                .placeholder(R.drawable.placeholder_image)
                .tag(model.message.toString())
                .into(holder.imageView, object: Callback{

                    override fun onSuccess() {
                        holder.progressBar.visible = (isUploading[messageID] == true)

                    }

                    override fun onError(e: Exception?) {

                        holder.progressBar.hide()

                        Log.d("MessageActivity", "onError: img file failed to load : " + e?.message)
                    }

                })
        }

        else  {

            if(model.message.isNotEmpty())
                Picasso.get()
                    .load(model.message.toString())
                    .fit()
                    .centerCrop()
                    .error(R.drawable.error_placeholder2)
                    .placeholder(R.drawable.placeholder_image)
                    .tag(model.message.toString())
                    .into(holder.imageView, object: Callback{
                        override fun onSuccess() {

                            holder.progressBar.visibility = View.GONE

                            saveBitmapFromPicasso(model.message.toString(), messageID, true)


                        }

                        override fun onError(e: Exception?) {

                            holder.progressBar.visibility = if(isUploading[messageID] == false) View.GONE else View.VISIBLE

                            Log.d("MessageActivity", "onError: img url failed to load")

                            holder.tapToRetry.setOnClickListener {

                                if(isContextMenuActive)
                                    return@setOnClickListener

                                it.visibility = View.GONE
                                utils.longToast(context, "Image might be deleted.")
                            }


                        }

                    })



        }

        holder.message.text = model.caption
    }

    //setting my video holder
    private fun setMyVideoHolder(holder:Holders.MyVideoMsgHolder, model: Models.MessageModel, messageID: String){

        CircularProgressBarsAt[messageID] = holder.progressBar
        mediaControlImageViewAt[messageID] = holder.centerImageView


        holder.cardContainer.setCornerEnabled(true,true, model.caption.isEmpty(), false)

        setTapToRetryBtn(holder.tapToRetry,holder.progressBar,model.file_local_path, messageID,model.caption, model.messageType)


        holder.tapToRetry.visibility = View.GONE
        holder.progressBar.visibility = if(isUploading[messageID] == true) View.VISIBLE else View.GONE
        holder.message.visibility =  if(model.caption.isEmpty()) View.GONE else View.VISIBLE


        if(holder.progressBar.visible)
            holder.centerImageView.setImageResource(R.drawable.ic_clear_white_24dp)
        else
            holder.centerImageView.setImageResource(R.drawable.ic_play_white)

    }

    //setting my audio holder
    private fun SetMyAudioHolder(){
    }


    //setting target Holders
    //setting target image holder
    private fun setTargetImageHolder(holder: Holders.TargetImageMsgHolder, model:Models.MessageModel, messageID: String){
        holder.message.visibility =  if(model.caption.isEmpty()) View.GONE else View.VISIBLE


        holder.message.text = model.caption
        holder.cardContainer.setCornerEnabled(false,true, model.caption.isEmpty(), model.caption.isEmpty())


        if(model.file_local_path.isNotEmpty() && File(model.file_local_path).exists()) {
            Picasso.get()
                .load(File(model.file_local_path.toString()))
                .tag(model.message.toString())
                .fit()
                .centerCrop()
                //.resize(600,400)
                .error(R.drawable.error_placeholder2)
                .placeholder(R.drawable.placeholder_image)
                .into(holder.imageView)



        }

        else{
            Picasso.get()
                .load(model.message.toString())
                .centerCrop()
                .resize(600,400)
                .error(R.drawable.error_placeholder2)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_placeholder2)
                .placeholder(R.drawable.placeholder_image)
                .tag(model.message.toString())
                .into(holder.imageView, object: Callback {
                    override fun onSuccess() {
                        saveBitmapFromPicasso(model.message.toString(), messageID, false)

                    }

                    override fun onError(e: Exception?) {
                    }
                }
                )
        }
    }


    //setting target video holder
    private fun setTargetVideoHolder(holder: Holders.TargetVideoMsgHolder, model: Models.MessageModel, messageID: String){

        holder.message.visibility =  if(model.caption.isEmpty()) View.GONE else View.VISIBLE
        holder.message.text = model.caption
        holder.cardContainer.setCornerEnabled(false,true, model.caption.isEmpty(), model.caption.isEmpty())
        holder.tap_to_download.hide()

        holder.itemView.delivery_video_status.hide()


        CircularProgressBarsAt[messageID] = holder.progressBar
        mediaControlImageViewAt[messageID] = holder.centerImageView

        //lets hide progressbar for now
        holder.progressBar.visibility = View.GONE




        if(holder.progressBar.visibility == View.VISIBLE)
            holder.centerImageView.setImageResource(R.drawable.ic_clear_white_24dp)
        else
            holder.centerImageView.setImageResource(R.drawable.ic_play_white)


        if(model.file_local_path.isEmpty()){
            downloadVideo(model, messageID)
        }
        else if(!File(model.file_local_path).exists()){
            holder.tap_to_download.show()
        }
    }






    var selectedDrawable:Drawable? = null
    var unselectedDrawable:Drawable? = null

    var isTranslatorPressed = false

    //setting contextual toolbar on viewHolder

    var actionMode: ActionMode? = null
    private fun setContextualToolbarOnViewHolder(itemView: View, messageID: String, model:Models.MessageModel, position:Int){

        selectedDrawable = ColorDrawable(ContextCompat.getColor(context, R.color.transparent_green))
        unselectedDrawable = ColorDrawable(Color.TRANSPARENT)


        if(selectedMessageIDs.contains(messageID))
            itemView.background = selectedDrawable
        else
            itemView.background = unselectedDrawable



        itemView.setOnLongClickListener {



            if(!isContextMenuActive) {

                if (!selectedMessageIDs.contains(messageID)) {
                    selectedItemPosition.add(position)
                    selectedMessageModel.add(model)
                    selectedMessageIDs.add(messageID)
                }


                actionMode = startSupportActionMode(object : ActionMode.Callback {
                        override fun onActionItemClicked(p0: ActionMode?, p1: MenuItem?): Boolean {
                            when (p1?.itemId) {

                                R.id.action_delete -> {

                                   deleteSelectedMessages(p0)

                                }

                                R.id.action_copy -> {

                                    var messages = ""
                                    for (itemPosition in selectedItemPosition) {
                                        val message = adapter.getItem(itemPosition)
                                        messages = if (message.isFile) messages + message.caption + "\n"
                                        else messages + message.message + "\n"
                                    }
                                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

                                    clipboard.setPrimaryClip (ClipData.newPlainText("Messages ", messages.trim()))
                                    utils.toast(context, "Messages copied")
                                }

                                R.id.action_forward -> {

                                    selectedMessageModel.clear()
                                    for (itemPosition in selectedItemPosition)
                                        selectedMessageModel.add(adapter.getItem(itemPosition))

                                    startActivity(
                                        Intent(context, ForwardActivity::class.java)
                                            .putExtra(
                                                utils.constants.KEY_MSG_MODEL,
                                                selectedMessageModel as Serializable
                                            )
                                    )
                                }

                                R.id.action_translate -> {

                                    Log.d("MessageActivity", "onActionItemClicked: ${Pref.getSettingFile(context).all}")

                                    if(Pref.getSettingFile(context).contains(Pref.KEY_DEFAULT_TRANSLATION_LANG))
                                        translateMessage(itemView, model)
                                    else{
                                        alert { message = "You have not set any default language. Go to Menu -> Setting to set it."
                                        positiveButton("Go to setting"){ startActivity(intentFor<SettingsActivity>())}
                                            negativeButton("Cancel"){}
                                        }.show()
                                    }
                                }

                            }

                            if (p1?.itemId != R.id.action_delete)
                                p0?.finish()

                            return true

                        }

                        override fun onCreateActionMode(p0: ActionMode?, p1: Menu?): Boolean {
                            p0?.menuInflater?.inflate(R.menu.chat_actions_menu, p1!!)
                            p1?.findItem(R.id.action_translate)?.isVisible = (selectedMessageModel.size == 1 && !model.isFile)
                            isContextMenuActive = true
                            return true
                        }

                        override fun onPrepareActionMode(p0: ActionMode?, p1: Menu?): Boolean {
                            val models = selectedMessageModel
                            val isContainsFile = models.any {
                                it.isFile
                            }

                            p1?.findItem(R.id.action_translate)?.isVisible = (models.size == 1 && model.messageType == "message")

                            p1?.findItem(R.id.action_copy)?.isVisible = !isContainsFile
                            p0?.title = selectedItemPosition.size.toString()

                            return true
                        }

                        override fun onDestroyActionMode(p0: ActionMode?) {
                            for (pos in selectedItemPosition)
                            {
                                if(!isTranslatorPressed)
                                    adapter.notifyItemChanged(pos)
                            }

                            selectedItemPosition.clear()
                            selectedMessageIDs.clear()
                            selectedMessageModel.clear()
                            isContextMenuActive = false
                            isTranslatorPressed = false
                            hasLastMessageSelected = false
                        }

                    })

                actionMode?.title = selectedItemPosition.size.toString()


                itemView.background = selectedDrawable
            }


            true
        }


        itemView.setOnClickListener {


            if(isContextMenuActive) {

                if(selectedMessageIDs.contains(messageID)){
                    itemView.background = unselectedDrawable
                    selectedItemPosition.remove(position)
                    selectedMessageModel.remove(model)
                    selectedMessageIDs.remove(messageID)

                }
                else{
                    itemView.background = selectedDrawable
                    selectedItemPosition.add(position)
                    selectedMessageModel.add(model)
                    selectedMessageIDs.add(messageID)
                }

                actionMode?.title = selectedItemPosition.size.toString()
                actionMode?.invalidate()

                if(selectedItemPosition.isEmpty()){
                        actionMode?.finish()
                }


            }

        }
    }

    private fun translateMessage(itemView: View, model: Models.MessageModel):Unit {

        isTranslatorPressed = true
        itemView.background = ColorDrawable(Color.TRANSPARENT)

        var translationHeading = "Identifying Language"

        if(model.from == myUID)
        {
            itemView.bubble_right_translation.text = translationHeading
            itemView.bubble_right_translation.visibility = View.VISIBLE

        }
        else
        {
            itemView.bubble_left_translation.text = translationHeading
            itemView.bubble_left_translation.visibility = View.VISIBLE

        }

        //identify language
        FirebaseNaturalLanguage.getInstance().languageIdentification
            .identifyLanguage(model.message)
            .addOnSuccessListener { languageCode ->
                Log.d("MessageActivity", "translateMessage: language = $languageCode")

                val translatorOptions = FirebaseTranslatorOptions.Builder()
                    .setSourceLanguage(FirebaseTranslateLanguage.languageForLanguageCode(languageCode)?:FirebaseTranslateLanguage.EN)
                    .setTargetLanguage(Pref.getDefaultLanguage(context))
                    .build()

                val languageName = Locale(languageCode).displayLanguage

                val firebaseTranslator =  FirebaseNaturalLanguage.getInstance().getTranslator(translatorOptions)

                FirebaseApp.initializeApp(this)?.let { firebaseApp ->
                    FirebaseTranslateModelManager.getInstance().getAvailableModels(firebaseApp).addOnSuccessListener {


                        if (it.any { remoteModel -> remoteModel.language == Pref.getDefaultLanguage(context) }) {
                            // model is available to translate
                            Log.d("MessageActivity", "translateMessage: Translator Model is available")
                            onTranslatorDownloaded(itemView, model, firebaseTranslator, languageName)
                        }

                        else {
                            // download translator, and then translate
                            translationHeading = "Downloading files for $languageName"

                            when (myUID) {
                                model.from -> itemView.bubble_right_translation.text = translationHeading
                                else -> itemView.bubble_left_translation.text = translationHeading
                            }

                            firebaseTranslator.downloadModelIfNeeded()
                                .addOnSuccessListener {
                                    Log.d("MessageActivity", "translateMessage: translator downloaded")
                                    onTranslatorDownloaded(itemView, model, firebaseTranslator, languageName)
                                }
                                .addOnFailureListener {
                                    toast(it.localizedMessage)
                                }

                        }
                    }
                }



            }

    }


    private fun onTranslatorDownloaded(itemView: View, model: Models.MessageModel, firebaseTranslator:FirebaseTranslator, languageName:String){


        var translationHeading = "Translating from $languageName"


        when (myUID) {
            model.from -> itemView.bubble_right_translation.text = translationHeading
            else -> itemView.bubble_left_translation.text = translationHeading
        }

        firebaseTranslator.translate(model.message)
            .addOnSuccessListener { translatedText ->
                Log.d("MessageActivity", "translateMessage: $translatedText")
                translationHeading = "Translated to ${Locale(FirebaseTranslateLanguage.languageCodeForLanguage(Pref.getDefaultLanguage(this))).displayName} from $languageName"
                when (myUID) {
                    model.from -> {
                        itemView.messageText_right.text = translatedText
                        itemView.bubble_right_translation.text = translationHeading
                    }
                    else -> {
                        itemView.messageText_left.text = translatedText
                        itemView.bubble_left_translation.text = translationHeading
                    }
                }
            }
            .addOnFailureListener {
                Log.d("MessageActivity", "translateMessage: error =  ${it.message}")
                translationHeading = "Failed to Translate"
                when (myUID) {
                    model.from -> itemView.bubble_right_translation.text = translationHeading
                    else -> itemView.bubble_left_translation.text = translationHeading
                }
            }
    }

    var isContextMenuActive = false


    override fun onBackPressed() {

        if(attachment_menu.visibility == View.VISIBLE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                utils.setExitRevealEffect(attachment_menu)
            }
            else
                attachment_menu.visibility = View.GONE
        else {
            if(intent.getBooleanExtra(utils.constants.KEY_IS_ONCE, false))
                startActivity(Intent(context, HomeActivity::class.java))


            finish()

        }
    }

    private var blockItem: MenuItem? = null
    private var selectedPosition = -1
    private var searchQuery = ""

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.message_activity_menu, menu)

        val searchView = menu!!.findItem(R.id.app_bar_search).actionView as SearchView


        blockItem = menu.findItem(R.id.menu_action_block)
        blockItem?.title = if(isBlockedByMe) "Unblock" else "Block"


        setSearchView(searchView)

        if(isGroup){
            blockItem?.isVisible = false
        }



        return super.onCreateOptionsMenu(menu)
    }


    var searchPosition = 0

    private fun setSearchView(searchView: SearchView){



        searchView.maxWidth = Integer.MAX_VALUE
        searchView.isIconified = true


        val searchLayout = searchView.getChildAt(0) as LinearLayout
        val upBtn = ImageButton(context)
        val downBtn = ImageButton(context)


        upBtn.setImageResource(R.drawable.ic_up_white_24dp)
        upBtn.background = null
        upBtn.scaleType= ImageView.ScaleType.FIT_XY

        downBtn.setImageResource(R.drawable.ic__down_white_24dp)
        downBtn.background = null
        downBtn.scaleType= ImageView.ScaleType.FIT_XY

        upBtn.hide()
        downBtn.hide()

        searchLayout.addView(upBtn)
        searchLayout.addView(downBtn)


        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {


                if(query!!.isEmpty())
                    return true

                var resultCount = 0
                searchFilterItemPosition.clear()
                searchPosition = 0
                selectedPosition = -1
                searchQuery = query.toString().trim().toLowerCase()

                for((index,model) in adapter.snapshots.withIndex().reversed()){


                    if(model.isFile){
                        if (model.caption.contains(searchQuery, true)) {
                            searchFilterItemPosition.add((index))
                            resultCount++
                        }
                    }
                    else {
                        if (model.message.contains(query.toString(), true)) {
                            searchFilterItemPosition.add(index)
                            resultCount++
                        }
                    }
                }


                if(resultCount>0) {
                    utils.toast(context, "$resultCount results found")
                    upBtn.visibility = View.VISIBLE
                    downBtn.visibility = View.VISIBLE
                    messagesList.scrollToPosition(searchFilterItemPosition[0])
                    selectedPosition = searchFilterItemPosition[0]
                }
                else{
                    utils.toast(context, "No result")
                    upBtn.visibility = View.GONE
                    downBtn.visibility = View.GONE

                }


                utils.hideSoftKeyboard(this@MessageActivity)

                for(pos in searchFilterItemPosition)
                adapter.notifyItemChanged(pos)

                return true
            }

            override fun onQueryTextChange(p0: String?): Boolean = true

        })


        searchView.setOnCloseListener {

            upBtn.visibility = View.GONE
            downBtn.visibility = View.GONE

            selectedPosition = -1
            searchQuery = ""
            searchView.onActionViewCollapsed()
            searchFilterItemPosition.clear()

            adapter.notifyDataSetChanged()

            true
        }

        upBtn.setOnClickListener {

            if(searchFilterItemPosition.isEmpty())
                return@setOnClickListener

            if(searchPosition>=searchFilterItemPosition.size){
                searchPosition = 0
            }


            if(searchPosition>=0 && searchPosition<searchFilterItemPosition.count()) {
                selectedPosition = searchFilterItemPosition[searchPosition]
                messagesList.scrollToPosition(searchFilterItemPosition[searchPosition])

            }

            adapter.notifyDataSetChanged()

            searchPosition++
            utils.hideSoftKeyboard(this@MessageActivity)


        }

        downBtn.setOnClickListener {


            if(searchFilterItemPosition.isEmpty())
                return@setOnClickListener

            if(searchPosition<0){
                searchPosition = searchFilterItemPosition.size - 1
            }


            if(searchPosition>=0 && searchPosition<searchFilterItemPosition.count()) {
                selectedPosition = searchFilterItemPosition[searchPosition]
                messagesList.scrollToPosition(searchFilterItemPosition[searchPosition])

            }
            adapter.notifyDataSetChanged()

            searchPosition--
            utils.hideSoftKeyboard(this@MessageActivity)

        }

    }


    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        when(item!!.itemId){
            R.id.menu_action_block -> {

                AlertDialog.Builder(context).setMessage("${if (isBlockedByMe) "Unblock" else "Block"} this user")
                    .setPositiveButton("Yes") { _, _ ->
                        FirebaseUtils.ref.blockedUser(myUID, targetUid)
                            .setValue(!isBlockedByMe)
                    }
                    .setNegativeButton("No", null)
                    .show()

            }


            R.id.menu_action_clear -> {
                AlertDialog.Builder(context).setMessage("Clear all the messages from this user?")
                    .setPositiveButton("Yes, Please!") { _, _ ->
                        FirebaseUtils.ref.getChatRef(myUID, targetUid)
                            .removeValue()
                            .addOnCompleteListener {
                                utils.toast(context, if (it.isSuccessful) "Messages cleared" else "Failed to clear messages")
                            }
                    }
                    .setNegativeButton("No, Don't", null)
                    .show()
            }
        }


        return super.onOptionsItemSelected(item)
    }





    private fun saveBitmapFromPicasso(url:String, messageID: String, isSent:Boolean){


        Picasso.get().load(url)
            .into(object : Target {
                override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
                }

                override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                }

                override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                    if(bitmap!=null){
                        val path: String = if(isSent) utils.saveBitmapToSent(context, bitmap, messageID)
                        else utils.saveBitmapToReceived(context, bitmap, messageID)

                        FirebaseUtils.ref.getChatRef(myUID,targetUid)
                            .child(messageID)
                            .child(FirebaseUtils.KEY_FILE_LOCAL_PATH)
                            .setValue(path)
                    }
                }
            })



    }



    private fun setScrollingListener(){

        bottomScrollButton.hide()
        dateStickyHeader.visibility = View.INVISIBLE

        if(unreadMessageCount == 0)
        unreadCount.visibility = View.GONE
        Handler().postDelayed({
            FirebaseUtils.setUnreadCount(targetUid, unreadCount)
        },2000)


        val layoutManager = messagesList.layoutManager as LinearLayoutManager

        val textView = TextView(this)
        textView.text = "Sample text"

        val handler = Handler()
        var isRunning = false

        bottomScrollButton.setOnClickListener {
            unreadCount.visibility = View.INVISIBLE
            messagesList.scrollToPosition(adapter.itemCount - 1)
        }

        messagesList.setOnScrollListener(object : RecyclerView.OnScrollListener() {


            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {


                if(newState == RecyclerView.SCROLL_STATE_IDLE || newState == RecyclerView.SCROLL_STATE_SETTLING) {

                    if(dateStickyHeader.visibility == View.VISIBLE && !isRunning) {
                        isRunning = true
                        handler.postDelayed({
                            runOnUiThread {
                                if (layoutManager.findLastVisibleItemPosition() < adapter.itemCount - 1)
                                    dateStickyHeader.visibility = View.GONE
                                isRunning = false
                            }
                        }, 1500)
                    }
                }

                if(newState == RecyclerView.SCROLL_STATE_DRAGGING){
                    if(layoutManager.findLastVisibleItemPosition() < adapter.itemCount - 1)
                        dateStickyHeader.visibility = View.VISIBLE
                }

                super.onScrollStateChanged(recyclerView, newState)
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {



                if(layoutManager.findLastVisibleItemPosition() == adapter.itemCount - 1 )
                {
                    bottomScrollButton.hide()
                    smart_reply_root_layout.animate().translationY(0f).withStartAction { smart_reply_root_layout.show()}
                }
                else if(adapter.itemCount > 5)
                {
                    bottomScrollButton.show()
                    smart_reply_root_layout.animate().translationY(smart_reply_root_layout.height.toFloat())
                        .withEndAction { smart_reply_root_layout.hide() }

                }




                if(layoutManager.findFirstVisibleItemPosition() <= 1) {
                    dateStickyHeader.visibility = View.GONE
                    return
                }

                dateStickyHeader.text = utils.getHeaderFormattedDate(adapter.getItem(layoutManager.findFirstVisibleItemPosition())
                    .timeInMillis)




                super.onScrolled(recyclerView, dx, dy)
            }

        })
    }


    private fun deleteSelectedMessages(actionMode: ActionMode?){


        AlertDialog.Builder(context)
            .setMessage("Delete selected messages?")
            .setPositiveButton("Yes") { _, _ ->


                for ((index, messageID) in selectedMessageIDs.withIndex()) {
                    FirebaseUtils.ref.getChatRef(myUID, targetUid)
                        .child(messageID)
                        .removeValue()
                        .addOnCompleteListener {
                            if (index == selectedMessageIDs.lastIndex) {
                                toast("Message deleted")

                                //if last message was deleted
                                if(selectedItemPosition.contains(adapter.itemCount-1)){
                                    updateLastMessage()
                                }
                            }
                        }
                }

                actionMode?.finish()
            }
            .setNegativeButton("No", null)
            .show()


    }


    private fun updateLastMessage(){
        FirebaseUtils.ref.getChatQuery(myUID, targetUid)
            .limitToLast(1)
            .onSingleListEvent<Models.MessageModel> { list, _ ->
                list.forEach {
                    if(it != null)
                    FirebaseUtils.ref.lastMessage(myUID, targetUid)
                        .updateChildren(mapOf("timeInMillis" to it.timeInMillis, "reverseTimeStamp" to it.reverseTimeStamp))
                }
            }

    }

    //load members if group
    private fun loadGroupMembers(){
        Log.d("MessageActivity", "loadGroupMembers: is group = $isGroup")
        if(!isGroup)
            return


        FirebaseUtils.ref.groupMembers(targetUid)
            .addValueEventListener(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                }

                override fun onDataChange(p0: DataSnapshot) {
                    groupMembers.clear()
                    var isMeRemoved = false
                    var members = ""


                    for(post in p0.children){
                        val member = post.getValue(Models.GroupMember::class.java)!!
                        groupMembers.add(member)
                        members += utils.getNameFromNumber(context, member.phoneNumber) +", "

                        if(member.uid == myUID) {
                            Log.d("MessageActivity", "onDataChange: ${post.value}")
                            isMeRemoved = member.removed
                        }
                    }

                    if(!p0.exists())
                        isMeRemoved = true

                    try {
                        if (!isMeRemoved) {
                            members = members.trim().substring(0, members.lastIndex - 1)
                            user_online_status.text = members
                            Log.d("MessageActivity", "onDataChange: member name = $members")
                        }
                        else user_online_status.visibility = View.GONE
                    }
                    catch (e:Exception){}
                    val snackbar = Snackbar.make(messageInputField, "You cannot reply to this conversation anymore", Snackbar.LENGTH_INDEFINITE)

                    if(isMeRemoved)
                        snackbar.show()
                    else
                        snackbar.dismiss()
                }
            })
    }



    private fun bindSmartReply(){


        smart_reply_layout.visibility = View.GONE

        FirebaseUtils.ref.getChatRef(myUID, targetUid)
            .addValueEventListener(object : ValueEventListener{
                override fun onCancelled(p0: DatabaseError) {}

                override fun onDataChange(p0: DataSnapshot) {

                    val conversation:MutableList<FirebaseTextMessage> = ArrayList()
                    var isLastMessageMine = false
                    p0.children.forEachIndexed { index, dataSnapshot ->
                        val message = dataSnapshot.getValue(Models.MessageModel::class.java)
                        message?.let {
                            val textMessage = message.message.takeIf { message.messageType == "message" }?:message.messageType

                            if(it.from == targetUid)
                            conversation.add(FirebaseTextMessage.createForRemoteUser(textMessage,
                                System.currentTimeMillis(), targetUid))
                            else
                                conversation.add(FirebaseTextMessage.createForLocalUser(textMessage,
                                System.currentTimeMillis()))


                            if(index == p0.childrenCount.toInt() - 1){
                                isLastMessageMine = it.from == myUID
                            }
                        }
                    }

                    //generate smart reply
                    try {


                        if(conversation.isEmpty()) return

                        FirebaseNaturalLanguage.getInstance().smartReply
                            .suggestReplies(conversation)
                            .addOnSuccessListener {
                                when(it.status){
                                    SmartReplySuggestionResult.STATUS_SUCCESS -> {


                                    }
                                    SmartReplySuggestionResult.STATUS_NO_REPLY -> {
                                        Log.d("MessageActivity", "ML Translation: no reply")
                                    }
                                    SmartReplySuggestionResult.STATUS_NOT_SUPPORTED_LANGUAGE -> {
                                        Log.d("MessageActivity", "ML Translation: language not supported ")
                                    }
                                }

                                smart_reply_layout.visible = (it.suggestions.isNotEmpty())

                                smart_reply_recycler.setCustomAdapter(context, it.suggestions, R.layout.item_smart_reply){itemView, _,item ->
                                    val suggestion  = item.text
                                    itemView.item_text.text = suggestion

                                    itemView.item_text.setOnClickListener {
                                        messageInputField.inputEditText.setText(suggestion)
                                        if(Pref.isTapToReply(context))
                                            messageInputField.button.callOnClick()
                                    }

                                    itemView.setOnClickListener {
                                        messageInputField.inputEditText.setText(suggestion)
                                        if(Pref.isTapToReply(context))
                                            messageInputField.button.callOnClick()
                                    }
                                }


                            }
                    } catch (e: Exception) {
                        Log.e("MessageActivity", "onDataChange: error on smart suggestion : ",e)
                    }
                }
            })


        smart_reply_setting.setOnClickListener {

            val smartReplyCheckbox = CheckBox(this)
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            params.setMargins(30, 30, 30, 30)
            smartReplyCheckbox.layoutParams = params

            val layout = LinearLayout(this)
            layout.setPadding(20,20,20,20)
            layout.layoutParams = params

            smartReplyCheckbox.text = "Tap to send"
            smartReplyCheckbox.setTextSize( TypedValue.COMPLEX_UNIT_SP, 18f)

            layout.addView(smartReplyCheckbox)

            smartReplyCheckbox.isChecked = Pref.isTapToReply(this)
            smartReplyCheckbox.setOnCheckedChangeListener { _, isChecked ->
                Pref.isTapToReply(context, isChecked)
            }
            val settingDialog = alert {
                customView = layout
                positiveButton("Ok"){}
            }

            settingDialog.show()
        }

    }



    private fun startAudioRecording(){

        //  val filePath = utils.sentAudioPath+"/MSG${System.currentTimeMillis()}.wav"
        val filePath = utils.appFolder+"/recorded_audio.wav"

        audioFile = File(filePath)

        AndroidAudioRecorder.with(this)
            .setFilePath(filePath)
            .setAutoStart(true)
            .setKeepDisplayOn(true)
            .setSource(AudioSource.MIC)
            .setChannel(AudioChannel.STEREO)
            .setSampleRate(AudioSampleRate.HZ_16000)
            .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
            .setRequestCode(RQ_RECORDING)
            .record()

    }


    private val mediaPlayer = MediaPlayer()
    private fun playAudio(path:String){

        val playIntent = Intent(Intent.ACTION_VIEW)
        playIntent.setDataAndType(utils.getUriFromFile(context, File(path)), "audio/*")
        startActivity(playIntent)

        if(true)
            return

        try {

            with(mediaPlayer) {
                setDataSource(path)
                setAudioStreamType(AudioManager.STREAM_MUSIC)
                setOnTimedTextListener { mediaPlayer, timedText ->
                    Log.d(
                        "MessageActivity",
                        "playAudio: $timedText"
                    )
                }
                if (!isPlaying) {
                    prepareAsync()
                    start()
                }
            }

        }
        catch (e:Exception){
            e.printStackTrace()
        }
    }


}