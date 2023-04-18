package com.aziz.sstalk.fragments

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.fragment.app.Fragment
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
import com.aziz.sstalk.utils.utils.longToast
import com.aziz.sstalk.utils.utils.toast
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.input_otp.*
import kotlinx.android.synthetic.main.input_otp.view.*
import java.time.Duration
import java.util.concurrent.TimeUnit

import org.jetbrains.anko.alert

class FragmentOTP : Fragment() {

    var mobile_no:String? = null
    var lastGenerated:Long? = System.currentTimeMillis()
    var verificationID = ""
    var mResendToken:PhoneAuthProvider.ForceResendingToken? = null
    var userInfoBundle:Bundle? = null

    var progressDialog:ProgressDialog? = null

    var otp_count = 1

    private var rootView:View? = null

    private var verificationStateChangedCallbacks = object  : PhoneAuthProvider.OnVerificationStateChangedCallbacks(){
        override fun onVerificationCompleted(p0: PhoneAuthCredential?) {

            Log.d("FragmentOTP", "onVerificationCompleted: ${p0.toString()}")
        }

        override fun onVerificationFailed(p0: FirebaseException?) {
            utils.toast(context, "Failed to send OTP. Perhaps you are using an emulator.")
            Log.e("FragmentOTP", "onVerificationFailed: ${p0?.message.toString()}")

            fragmentManager?.beginTransaction()!!
                .remove(this@FragmentOTP)
                .commit()

        }

        override fun onCodeSent(p0: String?, p1: PhoneAuthProvider.ForceResendingToken?) {
            super.onCodeSent(p0, p1)

            verificationID = p0!!
            mResendToken = p1!!

            otp_count++
            progressDialog?.dismiss()
            context?.toast("OTP sent")
        }


    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

       val view = inflater.inflate(R.layout.input_otp,container, false)
        rootView = view
        bindViews(view)

        return view
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        generateOTP()
    }

    @SuppressLint("CommitTransaction")
    private fun bindViews(view: View){

        mobile_no = arguments!!.getString(MobileLoginActivity.KEY.PHONE)
        Log.d("FragmentOTP", "bindViews: mob = $mobile_no")

        progressDialog = ProgressDialog(context)

        view.pinView.setAnimationEnable(true)
        view.verify.setOnClickListener{
            val inputOtp = view.pinView.text.toString()

            if(inputOtp.length != 6){
                return@setOnClickListener
            }

            if(verificationID.isEmpty()){
                longToast(context, "Please wait for the OTP.")
            }
            else
                signInWithCredential(PhoneAuthProvider.getCredential(verificationID, inputOtp))

        }

        view.resendBtn.setOnClickListener {
            if(System.currentTimeMillis() - lastGenerated!! > 30000)
            generateOTP()
            else
                Snackbar.make(rootView!!, "Wait for 30 seconds",
                    Snackbar.LENGTH_LONG)
                    .show()
        }


        rootView?.changeNumber?.setOnClickListener {

            AlertDialog.Builder(context)
                .setMessage("Change this number?")
                .setPositiveButton("Yes") { _,_ ->
                    fragmentManager?.beginTransaction()!!
                        .remove(this@FragmentOTP)
                        .commit()
                }
                .setNegativeButton("No", null)
                .show()


        }
    }

    private fun generateOTP(){
        lastGenerated = System.currentTimeMillis()
        mobile_no = arguments!!.getString(MobileLoginActivity.KEY.PHONE)



        if(otp_count>4){
            Snackbar.make(rootView!!,  "You have requested maximum number of OTP. Please again later.",
                Snackbar.LENGTH_LONG)
                .show()
            return
        }

        otp_count++

        Log.d("FragmentOTP", "generateOTP: sending OTP to ----> ${mobile_no.toString()}")

        progressDialog?.setMessage("Sending OTP")
        progressDialog?.setCancelable(false)
        progressDialog?.show()


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
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onCancelled(p0: DatabaseError) {
                            }

                            override fun onDataChange(p0: DataSnapshot) {
                                var profileURL = ""
                                var name = ""
                                if(p0.exists()){
                                    try { profileURL = p0.getValue(Models.User::class.java)?.profile_pic_url!!
                                    name = p0.getValue(Models.User::class.java)?.name!!
                                        }catch (e:Exception){}
                                }


                                FirebaseUtils.ref.user(user.uid)
                                    .setValue(Models.User(name,user.metadata!!.creationTimestamp,
                                        user.metadata!!.lastSignInTimestamp,
                                        user.phoneNumber!!,
                                        profileURL,
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

                        })



                }
                else{
                    utils.toast(context, "Incorrect OTP")
                }
            }
    }

    private fun setValueAnimator(duration: Long){
        val valueAnimator = ValueAnimator.ofInt( (duration/1000).toInt(),0)
        valueAnimator.duration = duration

        val resendText = rootView?.resendBtn

        valueAnimator.addUpdateListener {
            resendText?.text = "Resend in ${it.animatedValue}"
            Log.d("FragmentOTP", "setValueAnimator: ${it.animatedValue}")
            if(it.animatedValue == "0")
                resendText?.text ="Resend OTP"
        }


        valueAnimator.start()


    }
}

