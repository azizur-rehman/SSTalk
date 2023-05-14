package com.aziz.sstalk

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.aziz.sstalk.utils.FirebaseUtils
import com.google.firebase.crashlytics.FirebaseCrashlytics

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)


        if(FirebaseUtils.isLoggedIn()){
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
            try {
                    FirebaseCrashlytics.getInstance().setUserId(FirebaseUtils.getUid())
            }
            catch (e:Exception){e.printStackTrace()}
        }


    }


    fun onGetStartedClick(v: View){
        startActivity(Intent(this, MobileLoginActivity::class.java))
    }
}
