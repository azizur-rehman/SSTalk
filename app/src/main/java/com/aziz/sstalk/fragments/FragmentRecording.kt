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
import com.aziz.sstalk.utils.hide
import com.aziz.sstalk.utils.max_file_size
import com.aziz.sstalk.utils.show
import com.aziz.sstalk.utils.utils
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.dialog_audio_recorder.view.*
import org.jetbrains.anko.runOnUiThread
import org.jetbrains.anko.toast
import java.io.File
import java.lang.Exception
import java.util.*
import kotlin.concurrent.timerTask

class FragmentRecording: BottomSheetDialogFragment() {

    private val mediaRecorder = MediaRecorder()
    private val timer:Timer = Timer()
    lateinit var rootView:View
    var seconds = 0
    var onFinished:OnRecordingFinished? = null
    var isRecording = false
    private val filePath = utils.sentAudioPath+"/REC_${System.currentTimeMillis()}.aac"

    fun setRecordingListener(onRecordingFinished: OnRecordingFinished){
        onFinished = onRecordingFinished
    }


    private fun initRecorder() {
            with(mediaRecorder){
                try {
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
        return layoutInflater.inflate(R.layout.dialog_audio_recorder, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rootView = view

        view.stop.setOnClickListener {

            if(isRecording)
                stopRecording()

            view.cancel.show()
            view.accept.show()
            it.hide()
        }

        view.cancel.setOnClickListener {
            if(isRecording)
                stopRecording()

            File(filePath).delete()
            onFinished?.onCancelled()
        }

        view.accept.setOnClickListener {
            onFinished?.onRecorded(File(filePath).takeIf { it.exists() })
        }

    }


    override fun onStart() {
        super.onStart()

        initRecorder()

        try {
            rootView.cancel.hide()
            rootView.accept.hide()

            mediaRecorder.prepare()
            mediaRecorder.start()

            rootView.pulse_layout.startRippleAnimation()

            timer.schedule(timerTask {

                context?.runOnUiThread {
                    rootView.timer.text = utils.getDurationString(((seconds++)*1000).toLong())
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

        rootView.pulse_layout.stopRippleAnimation()
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