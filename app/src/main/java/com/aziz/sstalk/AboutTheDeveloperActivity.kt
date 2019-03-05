package com.aziz.sstalk

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import com.aziz.sstalk.utils.utils
import mehdi.sakout.aboutpage.AboutPage
import mehdi.sakout.aboutpage.Element
import org.jetbrains.anko.browse

class AboutTheDeveloperActivity : AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



        val aboutView = AboutPage(this)
            .isRTL(false)
            .setImage(R.mipmap.ic_launcher)
            .setDescription("SS Talk\n\nAn Open Source Chat Project for Android")
            .addItem(Element("Version "+BuildConfig.VERSION_NAME,R.mipmap.ic_launcher).setOnClickListener { browse(utils.constants.APP_SHORT_LINK) })
            .addGroup("Connect with us")
            .addEmail("azizur.rehman007@gmail.com")
            .addWebsite("https://azizur-rehman.github.io/", "Visit my website")
            .addFacebook("shanu.siddiqui.568","Connect on Facebook")
            .addPlayStore(BuildConfig.APPLICATION_ID)
            .addGitHub("azizur-rehman")
            .create()

        setContentView(aboutView)


        supportActionBar?.setDisplayHomeAsUpEnabled(true)

    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        //just finishing this activity
        finish()
        return super.onOptionsItemSelected(item)
    }
}