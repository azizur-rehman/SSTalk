package com.aziz.sstalk.fragments

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.aziz.sstalk.R
import com.aziz.sstalk.utils.utils
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.android.synthetic.main.input_otp.view.*
import java.util.concurrent.TimeUnit

class FragmentOTP : Fragment() {

    var mobile_no:String? = null
    var lastGenerated:Long? = System.currentTimeMillis()

    var verificationStateChangedCallbacks = object  : PhoneAuthProvider.OnVerificationStateChangedCallbacks(){
        override fun onVerificationCompleted(p0: PhoneAuthCredential?) {
            val otp = p0!!.smsCode.toString()
        }

        override fun onVerificationFailed(p0: FirebaseException?) {
            utils.toast(context, p0!!.message.toString())
            println(p0.message.toString())

            Log.d("tag", p0.toString())


        }

        override fun onCodeSent(p0: String?, p1: PhoneAuthProvider.ForceResendingToken?) {
            super.onCodeSent(p0, p1)
            utils.toast(context, "otp sent to $mobile_no")
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

       val view = inflater.inflate(R.layout.input_otp,container, false)
        bindViews(view)

        return view
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        generateOTP()
    }

    private fun bindViews(view: View){

        mobile_no = arguments!!.getString("phone")
        println("mob = $mobile_no")

        view.pinView.setAnimationEnable(true)
        view.verify.setOnClickListener{
            val otp = view.pinView.text.toString()
               utils.toast(context, "$otp on $mobile_no")

//            activity!!.supportFragmentManager.beginTransaction()
//                .remove(this)
//                .commit()
        }

        view.resendBtn.setOnClickListener {
            if(System.currentTimeMillis() - lastGenerated!! > 30000)
            generateOTP()
            else
                utils.toast(context, "wait for 30 seconds")
        }
    }

    private fun generateOTP(){
        lastGenerated = System.currentTimeMillis()
        PhoneAuthProvider.getInstance()
            .verifyPhoneNumber(mobile_no.toString(),60, TimeUnit.SECONDS,this.activity!!, verificationStateChangedCallbacks)
    }
}

