package com.aziz.sstalk.fragments

import android.content.DialogInterface
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.aziz.sstalk.R
import com.aziz.sstalk.databinding.DialogAudioRecorderBinding
import com.aziz.sstalk.utils.hide
import com.aziz.sstalk.utils.max_file_size
import com.aziz.sstalk.utils.show
import com.aziz.sstalk.utils.utils
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.jetbrains.anko.runOnUiThread
import org.jetbrains.anko.toast
import java.io.File
import java.lang.Exception
import java.util.*
import kotlin.concurrent.timerTask

class FragmentRecording: BottomSheetDialogFragment() {

    private val mediaRecorder = MediaRecorder()
    private val timer:Timer = Timer()
    var seconds = 0
    var onFinished:OnRecordingFinished? = null
    var isRecording = false
    private val filePath = utils.sentAudioPath+"/REC_${System.currentTimeMillis()}.aac"

    lateinit var binding:DialogAudioRecorderBinding

    fun setRecordingListener(onRecordingFinished: OnRecordingFinished){
        onFinished = onRecordingFinished
    }


    private fun initRecorder() {
            with(mediaRecorder){
                try {
                    File(utils.sentAudioPath).mkdirs()
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB)
                    setAudioSamplingRate(16000)
                    setOutputFile(filePath)
                    setMaxFileSize(max_file_size)
                }
                catch (e: Exception){
                    e.printStackTrace()
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogAudioRecorderBinding.inflate(layoutInflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.stop.setOnClickListener {

            if(isRecording)
                stopRecording()

            binding.cancel.show()
            binding.accept.show()
//            it.hide()
        }

        binding.cancel.setOnClickListener {
            if(isRecording)
                stopRecording()

            File(filePath).delete()
            onFinished?.onCancelled()
        }

        binding.accept.setOnClickListener {
            onFinished?.onRecorded(File(filePath).takeIf { it.exists() })
        }

    }


    override fun onStart() {
        super.onStart()

        initRecorder()

        try {
            binding.cancel.hide()
            binding.accept.hide()

            mediaRecorder.prepare()
            mediaRecorder.start()

            binding.pulseLayout.startRippleAnimation()

            timer.schedule(timerTask {

                context?.runOnUiThread {
                    binding.timer.text = utils.getDurationString(((seconds++)*1000).toLong())
                }

            },0,1000)
            isRecording = true
        }
        catch (e:Exception){
            e.printStackTrace()
            context?.toast("Failed to start recorder")
        }

    }

    override fun onDestroy() {
        super.onDestroy()

       stopRecording()

    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        File(filePath).delete()
        Log.d("FragmentRecording", "onCancel: file deleted")
        onFinished?.onCancelled()

    }

    private fun stopRecording(){

        if(!isRecording) return

        binding.pulseLayout.stopRippleAnimation()
        timer.cancel()
        try {
            mediaRecorder.stop()
            mediaRecorder.reset()
            mediaRecorder.release()
        }
        catch (e:Exception){
            e.printStackTrace()
        }

        isRecording = false

    }

    interface OnRecordingFinished{
        fun onRecorded(file:File?)
        fun onCancelled()
    }
}