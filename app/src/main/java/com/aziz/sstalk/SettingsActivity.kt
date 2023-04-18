package com.aziz.sstalk

import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Switch
import com.aziz.sstalk.utils.FirebaseUtils
import com.aziz.sstalk.utils.Pref
import com.aziz.sstalk.utils.utils.longToast
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_settings.*

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
                R.id.action_block_list -> {
                    startActivity(Intent(context, BlockListActivity::class.java))
                }
            }


            true
        }

        val enableSound = setting_nav_view.menu.findItem(R.id.action_sound_enable).actionView as Switch
        val enableVibration = setting_nav_view.menu.findItem(R.id.action_vibration_enable).actionView as Switch

        val mediaVisiblity = setting_nav_view.menu.findItem(R.id.action_media_visibility).actionView as Switch

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

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
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
