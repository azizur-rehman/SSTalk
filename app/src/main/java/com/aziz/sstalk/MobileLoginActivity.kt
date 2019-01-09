package com.aziz.sstalk

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.aziz.sstalk.Fragments.FragmentOTP
import com.aziz.sstalk.utils.utils
import kotlinx.android.synthetic.main.input_phone.*

class MobileLoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.input_phone)

        generate_otp.setOnClickListener {

            utils.toast(this, "Generating OTP")
            val fragmentOTP = FragmentOTP()
            val bundle = Bundle()
            bundle.putString("phone", "+91${mobile_number.text}")

            fragmentOTP.arguments = bundle

            supportFragmentManager.beginTransaction()
                .replace(R.id.container, fragmentOTP)
                .commit()
        }
    }
}
