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
import java.io.File
import java.util.ArrayList

class UserProfileActivity : AppCompatActivity() {

    val  messageModels:MutableList<Models.MessageModel> = ArrayList()
    var myUID = ""
    var targetUID = ""
    var isBlockedByMe = false
    var isPhoneLoaded = false
    var name = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)
        setSupportActionBar(toolbar)

        if(supportActionBar!=null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setHomeButtonEnabled(true)
        }




        var user1 = "user---1"
        var user2 = "user---2"

        //todo change this to uid

        myUID = utils.constants.debugUserID

        targetUID = intent.getStringExtra(FirebaseUtils.KEY_UID)
        name = intent.getStringExtra(FirebaseUtils.KEY_NAME)

        title = name


        FirebaseUtils.ref.getAllUserRef()
            .child(targetUID)
            .child(FirebaseUtils.KEY_PHONE)
            .addValueEventListener(object : ValueEventListener{
                override fun onCancelled(p0: DatabaseError) {
                }

                override fun onDataChange(p0: DataSnapshot) {
                    phone_textview.text = p0.getValue(String::class.java)
                    isPhoneLoaded = true
                }

            })


        FirebaseUtils.loadProfilePic(this, targetUID, user_profile_imageview)

        val layoutManager = GridLayoutManager(this, 4)
        mediaRecyclerView.addItemDecoration(DividerGridItemDecoration(this))
        mediaRecyclerView.isNestedScrollingEnabled = true

        mediaRecyclerView.layoutManager = layoutManager

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
//                                    holder.imageView.setImageBitmap(
//                                        ThumbnailUtils.createVideoThumbnail(
//                                            messageModels[p1].file_local_path,
//                                            MediaStore.Video.Thumbnails.MICRO_KIND
//                                        )
//                                    )
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


        phone_textview.setOnClickListener {
            if(isPhoneLoaded && phone_textview.text.isNotEmpty())
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("tel:${phone_textview.text}")))
        }


        block_user.setOnClickListener {

            AlertDialog.Builder(this@UserProfileActivity).setMessage("${if (isBlockedByMe) "Unblock" else "Block"} this user")
                .setPositiveButton("Yes") { _, _ ->
                    FirebaseUtils.ref.getBlockedUserRef(myUID, targetUID)
                        .setValue(!isBlockedByMe)
                }
                .setNegativeButton("No", null)
                .show()
        }

        checkIfBlocked()

    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        menuInflater.inflate(R.menu.user_profile_menu, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        when(item!!.itemId){
            android.R.id.home -> finish()
            R.id.action_contact -> {
                val contactIntent = Intent(Intent.ACTION_INSERT)
                contactIntent.putExtra(ContactsContract.Intents.Insert.PHONE, phone_textview.text)
                contactIntent.putExtra(ContactsContract.Intents.Insert.NAME, name)
                contactIntent.type = ContactsContract.RawContacts.CONTENT_TYPE
                startActivity(contactIntent)
            }
        }

        return super.onOptionsItemSelected(item)

    }

    private fun checkIfBlocked(){
        //check if i have blocked
        FirebaseUtils.ref.getBlockedUserRef(myUID, targetUID)
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
