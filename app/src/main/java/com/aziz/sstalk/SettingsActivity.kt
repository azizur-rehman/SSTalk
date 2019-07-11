package com.aziz.sstalk

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.Switch
import com.aziz.sstalk.utils.FirebaseUtils
import com.aziz.sstalk.utils.Pref
import com.aziz.sstalk.utils.utils.longToast
import com.aziz.sstalk.views.TitleSubtitleView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage
import kotlinx.android.synthetic.main.activity_settings.*
import org.jetbrains.anko.selector
import java.util.*
import kotlin.collections.ArrayList

class SettingsActivity : AppCompatActivity() {

    val context = this@SettingsActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        title = "Settings"
        if(supportActionBar!=null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setHomeButtonEnabled(true)
        }

        setting_nav_view.setNavigationItemSelectedListener {

            when(it.itemId){
                R.id.setting_block_list -> {
                    startActivity(Intent(context, BlockListActivity::class.java))
                }
            }


            true
        }

        val enableSound = setting_nav_view.menu.findItem(R.id.setting_sound_enable).actionView as Switch
        val enableVibration = setting_nav_view.menu.findItem(R.id.setting_vibration_enable).actionView as Switch

        val mediaVisiblity = setting_nav_view.menu.findItem(R.id.setting_media_visibility).actionView as Switch

        mediaVisiblity.isChecked = Pref.isMediaVisible(this)

        enableSound.isChecked = Pref.Notification.hasSoundEnabled(context)
        enableVibration.isChecked = Pref.Notification.hasVibrationEnabled(context)

        enableSound.setOnCheckedChangeListener { _, isChecked ->
            Pref.Notification.setSoundEnabled(context, isChecked)
        }

        enableVibration.setOnCheckedChangeListener { _, isChecked ->
            Pref.Notification.setVibrationEnabled(context, isChecked)
        }

        mediaVisiblity.setOnCheckedChangeListener{_,isChecked ->
            Pref.setMediaVisibility(context, isChecked)
        }


        with(setting_nav_view.menu){

           val defaultLangView =  findItem(R.id.setting_default_language).actionView as TitleSubtitleView

            val defaultLanguage = Pref.getSettingFile(this@SettingsActivity)
                .getInt(Pref.KEY_DEFAULT_TRANSLATION_LANG, FirebaseTranslateLanguage.HI)


            Log.d("SettingsActivity", "onCreate: default Language = $defaultLanguage")
            defaultLangView.setOnClickListener {


                val languages:MutableList<String> = ArrayList()
                FirebaseTranslateLanguage.getAllLanguages().forEach {
                    val code = FirebaseTranslateLanguage.languageCodeForLanguage(it)
                    languages.add(Locale(code).displayName) }



                selector("Choose your Default Language", languages){ _, position ->
                    run {
                        Pref.setDefaultLanguage(this@SettingsActivity, position)
                    }
                }

            }
            val smartReply = findItem(R.id.setting_smart_reply).actionView as Switch
            smartReply.isChecked = Pref.isTapToReply(this@SettingsActivity)
            smartReply.setOnCheckedChangeListener { _, isChecked -> Pref.isTapToReply(this@SettingsActivity, isChecked) }
        }

    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        finish()
        return super.onOptionsItemSelected(item)
    }


    fun onLogoutClick(view: View){


        AlertDialog.Builder(this)
            .setMessage("Logout from this account")
            .setPositiveButton("Yes") { _, _ ->
                FirebaseAuth.getInstance().signOut()
                FirebaseUtils.deleteCurrentToken()
                val intent = Intent(this@SettingsActivity, SplashActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                }


                longToast("You have been logged out")

                startActivity(intent)
                finish()
            }
            .setNegativeButton("No",null)
            .show()


    }

}
