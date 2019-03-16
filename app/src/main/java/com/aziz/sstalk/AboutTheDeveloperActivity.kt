package com.aziz.sstalk

import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import com.aziz.sstalk.models.Models
import com.aziz.sstalk.utils.FirebaseUtils
import com.yarolegovich.lovelydialog.LovelyTextInputDialog
import mehdi.sakout.aboutpage.AboutPage
import mehdi.sakout.aboutpage.Element
import org.jetbrains.anko.toast

class AboutTheDeveloperActivity : AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



        val aboutView = AboutPage(this)
            .isRTL(false)
            .setImage(R.mipmap.ic_launcher)
            .setDescription("SS Talk\n\nAn Open Source Chat Project for Android")
            .addItem(Element("Version "+BuildConfig.VERSION_NAME,R.mipmap.ic_launcher))
            .addGroup("Connect with us")
            .addEmail("azizur.rehman007@gmail.com")
            .addWebsite("https://azizur-rehman.github.io/", "Visit my website")
            .addFacebook("shanu.siddiqui.568","Connect on Facebook")
            .addPlayStore(BuildConfig.APPLICATION_ID)
            .addGitHub("azizur-rehman")
            .addItem(Element("Check for update",R.mipmap.ic_launcher).setOnClickListener
            { FirebaseUtils.checkForUpdate(this@AboutTheDeveloperActivity, true) })
            .addItem(Element("Feedback to the developer",android.R.drawable.ic_dialog_info).setOnClickListener
            { showFeedbackDialog(FirebaseUtils.getUid()) })
            .create()

        setContentView(aboutView)


        supportActionBar?.setDisplayHomeAsUpEnabled(true)

    }


    private fun showFeedbackDialog(uid:String){


        LovelyTextInputDialog(this)
            .setTopColorRes(R.color.colorAccent)
            .setTopTitleColor(Color.WHITE)
            .setTopTitle("Feedback")
            .setTitle("Type your feedback here...")
            .setInputFilter("Too short") {
                return@setInputFilter it.isNotBlank() && it.length > 5
            }
            .setConfirmButton("Submit") {

                FirebaseUtils.ref.feedback()
                    .push()
                    .setValue(Models.Feedback(uid, feedback = it))
                    .addOnSuccessListener { toast("Feedback submitted. Thanks!") }


            }
            .setNegativeButton("Cancel"){}
            .show()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        //just finishing this activity
        finish()
        return super.onOptionsItemSelected(item)
    }
}