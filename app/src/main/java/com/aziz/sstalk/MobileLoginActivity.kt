package com.aziz.sstalk

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.aziz.sstalk.fragments.FragmentOTP
import com.aziz.sstalk.utils.utils
import com.hbb20.CountryCodePicker
import kotlinx.android.synthetic.main.input_phone.*

class MobileLoginActivity : AppCompatActivity() {

    object KEY {
        val PHONE = "phone"
        val COUNTRY = "country"
        val COUNTRY_CODE = "countryCode"
        val COUNTRY_LOCALE_CODE = "locale"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.input_phone)

        country_picker.registerCarrierNumberEditText(mobile_number)

        generate_otp.setOnClickListener {


            if(!country_picker.isValidFullNumber) {
                mobile_number.error = "Input valid number"
                return@setOnClickListener
            }


            utils.toast(this, "Generating OTP")
            val fragmentOTP = FragmentOTP()
            val bundle = Bundle()
            val countryCode = country_picker.selectedCountryCode
            val countryName = country_picker.selectedCountryName
            val countryLocale = country_picker.selectedCountryNameCode

            bundle.putString(KEY.PHONE, country_picker.fullNumberWithPlus)
            bundle.putString(KEY.COUNTRY, countryName)
            bundle.putString(KEY.COUNTRY_LOCALE_CODE, countryLocale)
            bundle.putString(KEY.COUNTRY_CODE, countryCode)




            fragmentOTP.arguments = bundle

            Log.d("MobileLoginActivity", "onCreate: bundle = ${bundle.toString()}")

            supportFragmentManager.beginTransaction()
                .replace(R.id.container, fragmentOTP)
                .commit()
        }
    }
}
