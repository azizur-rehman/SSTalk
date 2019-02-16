package com.aziz.sstalk

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcelable
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
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
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.activity_forward.*
import kotlinx.android.synthetic.main.item__forward_contact_list.view.*
import kotlinx.android.synthetic.main.item_contact_layout.view.*
import java.lang.Exception

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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forward)

        setSupportActionBar(toolbar)

        if(supportActionBar!=null)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)

        fwd_snackbar = Snackbar.make(sendBtn, "", Snackbar.LENGTH_INDEFINITE)


        setFrequentAdapter()

        myUID = FirebaseUtils.getUid()

        try {
            messageModels = intent.getSerializableExtra(utils.constants.KEY_MSG_MODEL) as MutableList<Models.MessageModel>
        }
        catch (e:Exception){ messageModels = ArrayList()}

        if(messageModels!!.isEmpty())
            handleIncomingIntents(intent)


        sendBtn.setOnClickListener {



            selectedUIDs.forEach {targetUID->


                try {
                    if (intent.type!!.startsWith("image/") && intent.action == Intent.ACTION_SEND) {
                        uploadAndForward(targetUID)
                    } else {
                        onForward(targetUID)
                    }
                }catch (e:Exception) { onForward(targetUID) }
            }


        }
    }


    private fun onForward(targetUID:String){

        for(model in messageModels!!){
            val messageID = "MSG"+System.currentTimeMillis()
            model.from = myUID
            model.timeInMillis = System.currentTimeMillis()
            model.reverseTimeStamp = model.timeInMillis * -1
            model.to = targetUID
            model.caption = ""

            //send to my node
            FirebaseUtils.ref.getChatRef(myUID, targetUID)
                .child(messageID)
                .setValue(model)
                .addOnSuccessListener {
                    FirebaseUtils.setMessageStatusToDB(messageID, myUID, targetUID,true, true)

                    FirebaseUtils.ref.getLastMessageRef(myUID)
                        .child(targetUID)
                        .setValue(Models.LastMessageDetail())

                }

            //send to target node
            FirebaseUtils.ref.getChatRef(targetUID, FirebaseUtils.getUid())
                .child(messageID)
                .setValue(model)
                .addOnSuccessListener {
                    FirebaseUtils.setMessageStatusToDB(messageID, targetUID, myUID,false, false)

                    FirebaseUtils.ref.getLastMessageRef(targetUID)
                        .child(myUID)
                        .setValue(Models.LastMessageDetail())
                }
        }

        if(selectedUIDs.size == 1)
            startActivity(Intent(context, MessageActivity::class.java)
                .putExtra(FirebaseUtils.KEY_UID, selectedUIDs[0])
            )
        else
            startActivity(Intent(context, HomeActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))

        finish()
    }


    private fun handleIncomingIntents(intent: Intent){

        if(intent.action == Intent.ACTION_SEND){
            if(intent.type == "text/plain"){
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                messageModels!!.add(Models.MessageModel(text ))
            }

            else if(intent.type.startsWith( "image/")){

                if(!utils.hasStoragePermission(this)) {
                    utils.toast(this, "App does not have storage permission")
                    return
                }

            }
        }
    }


    private fun uploadAndForward(targetUID: String){

        val progressBar = ProgressDialog.show(context, "","Please wait...", true, false)

        val imageURI = intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as Uri

       val ref = FirebaseStorage.getInstance().getReference(utils.constants.FILE_TYPE_IMAGE)
            .child("MSG"+System.currentTimeMillis())

           val uploadTask = ref.putFile(imageURI)

               uploadTask.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
                   if (!task.isSuccessful) {
                       task.exception?.let {
                           throw it
                       }
                   }
                   return@Continuation ref.downloadUrl
               })

            uploadTask.addOnCompleteListener {
                progressBar.dismiss()

                //todo here
               // messageModels!!.add(Models.MessageModel(it.result!! ))
                onForward(targetUID)
            }



        Log.d("ForwardActivity", "handleIncomingIntents: "+imageURI.path)
    }

    private fun setFrequentAdapter(){

        val lastMsgQuery = FirebaseUtils.ref.getLastMessageRef(FirebaseUtils.getUid())
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
        FirebaseUtils.ref.getBlockedUserRef(myUID, uid)
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

        FirebaseUtils.ref.getAllUserRef()
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
}
