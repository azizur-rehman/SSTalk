package com.aziz.sstalk.fragments

import android.content.DialogInterface
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import cafe.adriel.androidaudiorecorder.model.AudioSampleRate
import com.aziz.sstalk.R
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
                    setAudioSamplingRate(AudioSampleRate.HZ_16000.sampleRate)
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
            else
                onFinished?.onRecorded(File(filePath).takeIf { it.exists() })

            (it as ImageView).setImageResource(R.drawable.ic_done_tick_white_24dp)
            view.cancel.show()
        }

        view.cancel.setOnClickListener {
            if(isRecording)
                stopRecording()

            File(filePath).delete()
            onFinished?.onCancelled()
        }

    }


    override fun onStart() {
        super.onStart()

        initRecorder()

        try {
            mediaRecorder.prepare()
            mediaRecorder.start()

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