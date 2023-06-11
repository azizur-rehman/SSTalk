package com.aziz.sstalk

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.aziz.sstalk.utils.FirebaseUtils
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.database.FirebaseDatabase

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        try{
            FirebaseDatabase.getInstance().setPersistenceEnabled(true)
//            FirebaseDatabase.getInstance().setPersistenceCacheSizeBytes(Long.MAX_VALUE)

//            FirebaseDatabase.getInstance().reference
//                .child(FirebaseUtils.NODE_USER_ACTIVITY_STATUS)
//                .keepSynced(true)
            /*FirebaseDatabase.getInstance().reference
                .child(NODE_MESSAGES)
                .keepSynced(true)



            FirebaseDatabase.getInstance().reference
                .child(NODE_MESSAGE_STATUS)
                .keepSynced(true)*/

        }
        catch (e: java.lang.Exception){ e.printStackTrace() }

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
