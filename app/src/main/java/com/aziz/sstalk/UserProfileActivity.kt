package com.aziz.sstalk

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.aziz.sstalk.models.Models
import com.aziz.sstalk.utils.FirebaseUtils
import com.aziz.sstalk.utils.utils
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import com.vincent.filepicker.DividerGridItemDecoration
import kotlinx.android.synthetic.main.activity_user_profile.*
import kotlinx.android.synthetic.main.content_user_profile.*
import kotlinx.android.synthetic.main.item_image.view.*
import kotlinx.android.synthetic.main.item_video.view.*
import org.jetbrains.anko.doAsyncResult
import org.jetbrains.anko.onComplete
import org.jetbrains.anko.uiThread
import java.io.File
import java.util.ArrayList
import java.util.concurrent.Future

class UserProfileActivity : AppCompatActivity() {

    val  messageModels:MutableList<Models.MessageModel> = ArrayList()
    var myUID = ""
    var targetUID = ""
    var isBlockedByMe = false
    var isPhoneLoaded = false
    var name = ""
    var isGroup = false

    private var asyncLoader: Future<Boolean>? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)
        setSupportActionBar(toolbar)

        if(supportActionBar!=null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setHomeButtonEnabled(true)
        }


        myUID = FirebaseUtils.getUid()

        targetUID = intent.getStringExtra(FirebaseUtils.KEY_UID)
        name = intent.getStringExtra(FirebaseUtils.KEY_NAME)

        isGroup = intent.getBooleanExtra(utils.constants.KEY_IS_GROUP, false)

        title = if(isGroup) name else utils.getNameFromNumber(this, name)

        utils.printIntentKeyValues(intent)

        if(!isGroup) phone_textview.text = name else phone_textview.visibility = View.GONE

        if(phone_textview.text.isEmpty() && !isGroup) {
            FirebaseUtils.ref.allUser()
                .child(targetUID)
                .child(FirebaseUtils.KEY_PHONE)
                .addValueEventListener(object : ValueEventListener {
                    override fun onCancelled(p0: DatabaseError) {
                    }

                    override fun onDataChange(p0: DataSnapshot) {
                        phone_textview.text = p0.getValue(String::class.java)
                        isPhoneLoaded = true
                        invalidateOptionsMenu()
                    }

                })

        }

        val layoutManager = GridLayoutManager(this, 4)
        mediaRecyclerView.addItemDecoration(DividerGridItemDecoration(this))
        mediaRecyclerView.isNestedScrollingEnabled = true

        mediaRecyclerView.layoutManager = layoutManager as RecyclerView.LayoutManager?



        asyncLoader = doAsyncResult {

            uiThread {

                FirebaseUtils.loadProfilePic(this@UserProfileActivity, targetUID, user_profile_imageview)

                FirebaseUtils.ref.getChatRef(myUID,targetUID)
                    .orderByChild(FirebaseUtils.KEY_REVERSE_TIMESTAMP)
                    .addValueEventListener(object:ValueEventListener {
                        override fun onCancelled(p0: DatabaseError) {
                        }

                        override fun onDataChange(p0: DataSnapshot) {


                            messageModels.clear()

                            if(!p0.exists())
                                return

                            p0.children.forEach {
                                val model = it.getValue(Models.MessageModel ::class.java)

                                if(model!!.file_local_path.isNotEmpty() && File(model.file_local_path).exists())
                                    messageModels.add(it.getValue(Models.MessageModel ::class.java)!!)
                            }


                            if(messageModels.isEmpty())
                                return

                            if(mediaRecyclerView.adapter != null)
                                mediaRecyclerView.adapter!!.notifyDataSetChanged()
                            else {

                                mediaRecyclerView.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                                    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): RecyclerView.ViewHolder {
                                        //0 for image
                                        //1 for video right now

                                        return if (p1 == 0) imageHolder(layoutInflater.inflate(R.layout.item_image, p0, false))
                                        else videoHolder(layoutInflater.inflate(R.layout.item_video, p0, false))


                                    }

                                    override fun getItemCount(): Int = messageModels.size

                                    override fun getItemViewType(position: Int): Int {
                                        return if (messageModels[position].messageType == utils.constants.FILE_TYPE_IMAGE) 0 else 1
                                    }

                                    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, p1: Int) {

                                        if (holder is imageHolder) {
                                            Picasso.get().load(File(messageModels[p1].file_local_path))
                                                .fit()
                                                .centerCrop()
                                                .into(holder.imageView)
                                            //  holder.imageView.setImageBitmap(BitmapFactory.decodeFile(messageModels[p1].file_local_path))

                                            holder.imageView.setOnClickListener {
                                                startActivity(
                                                    Intent(this@UserProfileActivity, ImagePreviewActivity::class.java)
                                                        .putExtra(
                                                            utils.constants.KEY_LOCAL_PATH,
                                                            messageModels[p1].file_local_path
                                                        )
                                                )
                                            }
                                        } else if (holder is videoHolder) {

                                            utils.loadVideoThumbnailFromLocalAsync(this@UserProfileActivity, holder.imageView, messageModels[p1].file_local_path)
                                            holder.length.text = utils.getVideoLength(
                                                this@UserProfileActivity,
                                                messageModels[p1].file_local_path
                                            )

                                            holder.imageView.setOnClickListener {
                                                utils.startVideoIntent(
                                                    this@UserProfileActivity,
                                                    messageModels[p1].file_local_path
                                                )
                                            }
                                        }
                                    }

                                }

                            }
                        }

                    })

            }

        }

        phone_textview.setOnClickListener {
            if(isPhoneLoaded && phone_textview.text.isNotEmpty())
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("tel:${phone_textview.text}")))
        }


        block_user.setOnClickListener {

            AlertDialog.Builder(this@UserProfileActivity).setMessage("${if (isBlockedByMe) "Unblock" else "Block"} this user")
                .setPositiveButton("Yes") { _, _ ->
                    FirebaseUtils.ref.blockedUser(myUID, targetUID)
                        .setValue(!isBlockedByMe)
                }
                .setNegativeButton("No", null)
                .show()
        }

        checkIfBlocked()


        //set notification switch enable/disable
        notification_switch.setOnCheckedChangeListener { _, isChecked ->
            FirebaseUtils.ref.notificationMute(targetUID)
                .setValue(isChecked)
        }

        //set switch initial value
        FirebaseUtils.ref.notificationMute(targetUID)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                }

                override fun onDataChange(p0: DataSnapshot) {
                    if(!p0.exists()) {
                        notification_switch.isChecked = false
                        return
                    }
                        notification_switch.isChecked = p0.getValue(Boolean::class.java)!!
                }
            })
    }


    override fun onDestroy() {

        asyncLoader?.cancel(true)

        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        if(phone_textview.text.toString() == name && !isGroup)
        menuInflater.inflate(R.menu.user_profile_menu, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        when(item!!.itemId){
            android.R.id.home -> finish()
            R.id.action_contact -> {
                val contactIntent = Intent(Intent.ACTION_INSERT)
                contactIntent.putExtra(ContactsContract.Intents.Insert.PHONE, phone_textview.text)
                contactIntent.type = ContactsContract.RawContacts.CONTENT_TYPE
                startActivity(contactIntent)
            }
        }

        return super.onOptionsItemSelected(item)

    }

    private fun checkIfBlocked(){
        //check if i have blocked
        FirebaseUtils.ref.blockedUser(myUID, targetUID)
            .addValueEventListener(object : ValueEventListener {
                @SuppressLint("SetTextI18n")
                override fun onDataChange(dataSnapshot: DataSnapshot) {

                    isBlockedByMe = if (dataSnapshot.exists())
                        dataSnapshot.getValue(Boolean::class.java)!!
                    else
                        false

                    block_user.text = "${if(isBlockedByMe) "Unblock" else "Block" } this user"

                }

                override fun onCancelled(databaseError: DatabaseError) {

                }
            })
    }


    class imageHolder(itemView:View): RecyclerView.ViewHolder(itemView){
        val imageView = itemView.iv_thumbnail_image

    }

    class videoHolder(itemView:View): RecyclerView.ViewHolder(itemView){
        val imageView = itemView.iv_thumbnail_video
        val length = itemView.txt_duration

    }
}
