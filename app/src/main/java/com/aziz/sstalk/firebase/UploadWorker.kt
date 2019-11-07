package com.aziz.sstalk.firebase

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat.startActivity
import androidx.work.*
import br.com.goncalves.pugnotification.notification.PugNotification
import com.aziz.sstalk.BuildConfig
import com.aziz.sstalk.HomeActivity
import com.aziz.sstalk.R
import com.aziz.sstalk.models.Models
import com.aziz.sstalk.utils.*
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.vincent.filepicker.Constant
import kotlinx.android.synthetic.main.activity_forward.*
import org.jetbrains.anko.collections.forEachWithIndex
import org.jetbrains.anko.toast
import java.io.File
import java.lang.Exception

class UploadWorker(private val context: Context,private val workerParameters: WorkerParameters)
    :Worker(context, workerParameters){


    private val messageData = workerParameters.inputData.getString(msg_model)
    private val messageID = workerParameters.inputData.getString(msg_id)
    private val users = workerParameters.inputData.getString(selected_uids)
    private val _nameOrNumber = workerParameters.inputData.getString(nameOrNumber)

    val message = messageData?.toModel<Models.MessageModel>()?.apply {
        from  = FirebaseUtils.getUid()
    }

    override fun doWork(): Result {


        Log.d("UploadWorker", "doWork: ${workerParameters.inputData.keyValueMap}")


        return try {

            message?.let {
                if (messageID != null) {

                    uploadFile{ data ->
                        if(data.getString(exception).isNullOrEmpty())
                            Result.success(data)
                        else
                            Result.failure(data)
                    }
                }
            }

            Result.success()

        } catch (e:Exception){
            e.printStackTrace()
            Result.failure()
        }
    }


    private fun uploadFile( onUploaded:(data:Data) -> Unit){


        Log.d("UploadWorker", "uploadFile: started")

        val ref = FirebaseStorage.getInstance().reference
            .child(message!!.messageType)
            .child(messageID!!)

        val file = File(message.file_local_path)
        val uploadTask = ref.putFile(utils.getUriFromFile(context, file))

        uploadTask
            .addOnProgressListener {
                val percentage:Double = (100.0 * it.bytesTransferred) / it.totalByteCount
                val percent = String.format("%.2f",percentage)
                Log.d("UploadWorker", "uploadFile: $percent")
                onUploaded(workDataOf(progress to percent))

                showNotification(progress = percentage.toInt())

            }
            .continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
                if (!task.isSuccessful) {
                    task.exception?.let {
                        throw it
                    }

                }
                return@Continuation ref.downloadUrl
            })

            .addOnCompleteListener { task->


                val fileUrl = task.result.toString()

                //storing file meta data
                FirebaseUtils.storeFileMetaData(
                    Models.File(messageID,
                        message.timeInMillis, fileType = message.messageType,
                        fileSizeInBytes = file.length(),
                        bucket_path = ref.bucket,
                        file_url = fileUrl,
                        file_extension = utils.getFileExtension(file)
                    ))


                // start forward worker

                val requests:MutableList<OneTimeWorkRequest> = mutableListOf()

                users?.split(",")?.forEachWithIndex {i, it ->

                    // to key
                    message.to = it
                    message.message = fileUrl

                    val inputData = workDataOf(
                        msg_id to messageID,
                        msg_model to message.convertToJsonString(),
                        target_uid to it,
                        nameOrNumber to _nameOrNumber?.split(",")?.getOrNull(i)
                    )

                    Log.d("UploadWorker", "uploadFile: ${inputData.keyValueMap}")

                    val request = OneTimeWorkRequestBuilder<ForwardWorker>()
                        .setInputData(inputData).build()

                    requests.add(request)
                }

                // start database request
                WorkManager.getInstance().enqueue(requests)

                onUploaded(workDataOf(url to fileUrl))
            }
            .addOnFailureListener {

                onUploaded(workDataOf(exception to it.message))
            }



    }



    private fun showNotification(progress:Int){


        try {
            PugNotification.with(context)
                .load()
                .identifier(101)
                .smallIcon(R.drawable.ic_file_upload_white_24dp)
                .progress()
                .update(101, progress,100, false)
                .build()
        }
        catch (e:Exception) { e.printStackTrace() }
    }


}