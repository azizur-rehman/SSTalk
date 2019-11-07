package com.aziz.sstalk.firebase

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat.startActivity
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.aziz.sstalk.BuildConfig
import com.aziz.sstalk.HomeActivity
import com.aziz.sstalk.R
import com.aziz.sstalk.models.Models
import com.aziz.sstalk.utils.*
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.vincent.filepicker.Constant
import kotlinx.android.synthetic.main.activity_forward.*
import org.jetbrains.anko.collections.forEachWithIndex
import org.jetbrains.anko.toast
import java.io.File
import java.lang.Exception


class ForwardWorker(private val context: Context, private val workerParameters: WorkerParameters)
    :Worker(context, workerParameters){


    override fun doWork(): Result {

        val messageData = workerParameters.inputData.getString(msg_model)
        val newMessageID = workerParameters.inputData.getString(msg_id)
        val nameOrNumber = workerParameters.inputData.getString(nameOrNumber)


        val message = messageData?.toModel<Models.MessageModel>()
        val targetUID = message?.to

        Log.d("ForwardWorker", "doWork: ${workerParameters.inputData.keyValueMap}")

        try{
            onForwardToSelectedUID(newMessageID!!,  targetUID!!, nameOrNumber!!,  message )
        }
        catch (e:Exception){
            e.printStackTrace()
            context.toast("Failed to forward messages")
            Result.failure()
        }

        return Result.success()

    }


    private fun onForwardToSelectedUID(messageID:String, targetUID:String, nameOrNumber:String, model:Models.MessageModel) {




        val myUID = FirebaseUtils.getUid()
        val conversationType = if(utils.isGroupID(targetUID)) FirebaseUtils.KEY_CONVERSATION_GROUP else FirebaseUtils.KEY_CONVERSATION_SINGLE

        //send to my node
         FirebaseUtils.ref.getChatRef(FirebaseUtils.getUid(), targetUID)
                    .child(messageID)
                    .setValue(model)
                    .addOnSuccessListener {
                        FirebaseUtils.setMessageStatusToDB(messageID, myUID, targetUID, true, isRead = true,
                            groupNameIf = nameOrNumber)

                        FirebaseUtils.ref.lastMessage(myUID)
                            .child(targetUID)
                            .setValue(Models.LastMessageDetail(nameOrNumber =  nameOrNumber ,
                                type = conversationType))

                    }

                model.file_local_path = ""


                if(utils.isGroupID(targetUID)){
                    //send to group members
                    Log.d("ForwardWorker", "onForwardToSelectedUIDs: group id = $targetUID")
                    addMessageToGroupMembers(messageID, model, targetUID, nameOrNumber)
                }
                else {
                    //send to target node
                    FirebaseUtils.ref.getChatRef(targetUID, FirebaseUtils.getUid())
                        .child(messageID)
                        .setValue(model)
                        .addOnSuccessListener {
                            FirebaseUtils.setMessageStatusToDB(
                                messageID,
                                targetUID,
                                myUID,
                                false,
                                isRead = false,
                                groupNameIf = ""
                            )

                            FirebaseUtils.ref.lastMessage(targetUID)
                                .child(myUID)
                                .setValue(
                                    Models.LastMessageDetail(
                                        nameOrNumber = FirebaseUtils.getPhoneNumber(),
                                        type = conversationType
                                    )
                                )

                        }


                }

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
                            if(memberID != FirebaseUtils.getUid()) {

                                Log.d("Forward", "addMessageToGroupMembers: targets -> $memberID")

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



}