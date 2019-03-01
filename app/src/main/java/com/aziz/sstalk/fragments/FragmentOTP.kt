package com.aziz.sstalk.fragments

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.aziz.sstalk.EditProfile
import com.aziz.sstalk.MobileLoginActivity
import com.aziz.sstalk.R
import com.aziz.sstalk.models.Models
import com.aziz.sstalk.utils.FirebaseUtils
import com.aziz.sstalk.utils.utils
import com.futuremind.recyclerviewfastscroll.Utils
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.android.synthetic.main.input_otp.view.*
import java.util.concurrent.TimeUnit

class FragmentOTP : Fragment() {

    var mobile_no:String? = null
    var lastGenerated:Long? = System.currentTimeMillis()
    var verificationID = ""
    var mResendToken:PhoneAuthProvider.ForceResendingToken? = null
    var userInfoBundle:Bundle? = null

    private var verificationStateChangedCallbacks = object  : PhoneAuthProvider.OnVerificationStateChangedCallbacks(){
        override fun onVerificationCompleted(p0: PhoneAuthCredential?) {
        }

        override fun onVerificationFailed(p0: FirebaseException?) {
            utils.toast(context, p0!!.message.toString())
            Log.d("FragmentOTP", "onVerificationFailed: ${p0.message.toString()}")

        }

        override fun onCodeSent(p0: String?, p1: PhoneAuthProvider.ForceResendingToken?) {
            super.onCodeSent(p0, p1)

            verificationID = p0!!
            mResendToken = p1!!
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

        mobile_no = arguments!!.getString(MobileLoginActivity.KEY.PHONE)
        Log.d("FragmentOTP", "bindViews: mob = $mobile_no")

        view.pinView.setAnimationEnable(true)
        view.verify.setOnClickListener{
            val inputOtp = view.pinView.text.toString()

            if(inputOtp.length != 6){
                return@setOnClickListener
            }

            signInWithCredential(PhoneAuthProvider.getCredential(verificationID, inputOtp))

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
        mobile_no = arguments!!.getString(MobileLoginActivity.KEY.PHONE)

        Log.d("FragmentOTP", "generateOTP: sending OTP to ----> ${mobile_no.toString()}")

        PhoneAuthProvider.getInstance()
            .verifyPhoneNumber(mobile_no.toString(),60, TimeUnit.SECONDS,this.activity!!, verificationStateChangedCallbacks)
    }


    private fun signInWithCredential(credential: PhoneAuthCredential) {

        val progressDialog = ProgressDialog.show(context, "", "Please wait...", false, false)

        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener {
                progressDialog.dismiss()
                if(it.isSuccessful){
                    //utils.toast(context, "Sign in successfully")

                    userInfoBundle = arguments

                    val countryCode = arguments!!.getString(MobileLoginActivity.KEY.COUNTRY_CODE)!!
                    val countryName = arguments!!.getString(MobileLoginActivity.KEY.COUNTRY)!!
                    val countryLocale = arguments!!.getString(MobileLoginActivity.KEY.COUNTRY_LOCALE_CODE)!!


                    val user = it.result!!.user
                    FirebaseUtils.ref.user(user.uid)
                        .setValue(Models.User("",user.metadata!!.creationTimestamp,
                            user.metadata!!.lastSignInTimestamp,
                            user.phoneNumber!!,
                            "",
                            user.uid, countryName,
                            countryCode, countryLocale
                            ))
                        .addOnSuccessListener {

                            val intent = Intent(context, EditProfile::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                                putExtra(utils.constants.KEY_IS_ON_ACCOUNT_CREATION, true)
                            }

                                startActivity(intent)
                        }

                }
                else{
                    utils.toast(context, "Incorrect OTP")
                }
            }
    }
}

