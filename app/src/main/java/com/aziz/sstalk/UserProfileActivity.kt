package com.aziz.sstalk

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.os.Bundle
import android.provider.MediaStore
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import com.aziz.sstalk.models.Models
import com.aziz.sstalk.utils.FirebaseUtils
import com.aziz.sstalk.utils.utils
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_user_profile.*
import kotlinx.android.synthetic.main.content_user_profile.*
import kotlinx.android.synthetic.main.item_image.view.*
import kotlinx.android.synthetic.main.item_video.view.*
import java.io.File
import java.util.ArrayList

class UserProfileActivity : AppCompatActivity() {

    val  messageModels:MutableList<Models.MessageModel> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)
        setSupportActionBar(toolbar)


        var user1 = "user---1"
        var user2 = "user---2"


        val layoutManager = GridLayoutManager(this, 3)

        mediaRecyclerView.layoutManager = layoutManager

        FirebaseUtils.ref.getChatRef(user2,user1)
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
                                    holder.imageView.setImageBitmap(BitmapFactory.decodeFile(messageModels[p1].file_local_path))

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
                                    holder.imageView.setImageBitmap(
                                        ThumbnailUtils.createVideoThumbnail(
                                            messageModels[p1].file_local_path,
                                            MediaStore.Video.Thumbnails.MINI_KIND
                                        )
                                    )

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


    public class imageHolder(itemView:View): RecyclerView.ViewHolder(itemView){
        val imageView = itemView.iv_thumbnail_image

    }

    public class videoHolder(itemView:View): RecyclerView.ViewHolder(itemView){
        val imageView = itemView.iv_thumbnail_video
        val length = itemView.txt_duration

    }
}
