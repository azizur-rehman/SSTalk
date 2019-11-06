package com.aziz.sstalk.firebase

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.aziz.sstalk.BuildConfig
import com.aziz.sstalk.R
import com.aziz.sstalk.models.Models
import com.aziz.sstalk.utils.*
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.vincent.filepicker.Constant
import org.jetbrains.anko.toast
import java.io.File
import java.lang.Exception

class UploadWorker(private val context: Context,private val workerParameters: WorkerParameters)
    :Worker(context, workerParameters){


    override fun doWork(): Result {

        val messageData = workerParameters.inputData.getString(utils.constants.KEY_MSG_MODEL)
        val messageID = workerParameters.inputData.getString(utils.constants.KEY_MSG_ID)

        val message = messageData?.toModel<Models.MessageModel>()

        return try {

            message?.let {
                if (messageID != null) {
                    uploadFile(messageID, it)
                }
            }

            Result.success()
        } catch (e:Exception){
            Result.failure()
        }
    }


    private fun uploadFile(messageID:String, message:Models.MessageModel){




        val ref = FirebaseStorage.getInstance().reference
            .child(message.file_local_path)
            .child(messageID)

        val uploadTask = ref.putFile(utils.getUriFromFile(context, File(message.file_local_path)))

        uploadTask
            .addOnProgressListener {
                val percentage:Double = (100.0 * it.bytesTransferred) / it.totalByteCount
                val percent = String.format("%.2f",percentage)

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

//                val model = (Models.MessageModel(task.result.toString(), isFile = true,
//                    file_local_path = originalFile.path, file_size_in_bytes = file.length(),
//                    messageType = fileType))
//                messageModels!!.add(model)


//                FirebaseUtils.storeFileMetaData(
//                    Models.File(messageID,
//                        model.timeInMillis, fileType = fileType,
//                        fileSizeInBytes = file.length(),
//                        bucket_path = ref.bucket,
//                        file_url = task.result.toString(),
//                        file_extension = utils.getFileExtension(file)
//                    ))

            }



    }

}