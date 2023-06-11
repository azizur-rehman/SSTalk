package com.aziz.sstalk

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.PopupMenu
import android.widget.Switch
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.aziz.sstalk.databinding.ActivitySettingsBinding
import com.aziz.sstalk.databinding.DialogListSelectorBinding
import com.aziz.sstalk.databinding.ItemSelectorBinding
import com.aziz.sstalk.utils.FirebaseUtils
import com.aziz.sstalk.utils.Pref
import com.aziz.sstalk.utils.utils.longToast
import com.aziz.sstalk.views.AnimCheckBox
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager
import com.google.firebase.ml.common.modeldownload.FirebaseRemoteModel
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateRemoteModel
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.toast
import java.util.*
import kotlin.collections.ArrayList

class SettingsActivity : AppCompatActivity() {

    val context = this@SettingsActivity

    var languageDialog:BottomSheetDialog? = null
    lateinit var binding:ActivitySettingsBinding
    

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        title = "Settings"
        if(supportActionBar!=null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setHomeButtonEnabled(true)
        }



        val enableSound = binding.settingNavView.menu.findItem(R.id.setting_sound_enable).actionView as Switch
        val enableVibration = binding.settingNavView.menu.findItem(R.id.setting_vibration_enable).actionView as Switch

        val mediaVisiblity = binding.settingNavView.menu.findItem(R.id.setting_media_visibility).actionView as Switch

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


        with(binding.settingNavView.menu){


            val defaultLanguage = Pref.getSettingFile(context)
                .getInt(Pref.KEY_DEFAULT_TRANSLATION_LANG, FirebaseTranslateLanguage.HI)


            Log.d("SettingsActivity", "onCreate: default Language = $defaultLanguage")

            val smartReply = findItem(R.id.setting_smart_reply).actionView as Switch
            smartReply.isChecked = Pref.isTapToReply(context)
            smartReply.setOnCheckedChangeListener { _, isChecked -> Pref.isTapToReply(context, isChecked) }
        }



        //load language list
        val languages:MutableList<String> = ArrayList()
        FirebaseTranslateLanguage.getAllLanguages().forEach {
            val code = FirebaseTranslateLanguage.languageCodeForLanguage(it)
            languages.add(Locale(code).displayName)
        }

        if(Pref.getDefaultLanguage(this) > -1)
        binding.settingNavView.menu?.findItem(R.id.setting_default_language)?.title = "Default Language (${languages[Pref.getDefaultLanguage(this)]})"

        binding.settingNavView.setNavigationItemSelectedListener {

            when(it.itemId){
                R.id.setting_block_list -> {
                    startActivity(Intent(context, BlockListActivity::class.java))
                }

                R.id.setting_default_language -> {
                    onDefaultLanguageClick(languages)
                }
            }


            true
        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        finish()
        return super.onOptionsItemSelected(item)
    }


    private fun onDefaultLanguageClick(languages:MutableList<String> ){

        if(languageDialog != null){
            languageDialog?.show()
            if(Pref.getDefaultLanguage(context) > -1)
                languageDialog?.findViewById<RecyclerView>(R.id.recyclerView)?.scrollToPosition(Pref.getDefaultLanguage(context))
            return
        }

        val dialog = BottomSheetDialog(context)
        languageDialog = dialog
        with(dialog){
            val dialogBinding = DialogListSelectorBinding.inflate(layoutInflater)
            setContentView(dialogBinding.root)
            dialog.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
            show()
            bindLanguageDialog(languages, dialogBinding)
        }

    }

    var selectedPosition = -1
    private fun BottomSheetDialog.bindLanguageDialog(languages:MutableList<String> , dialogBinding: DialogListSelectorBinding ){

        dialogBinding.saveBtn.setOnClickListener {
            dismiss()
            if(selectedPosition > -1)
            {
                Pref.setDefaultLanguage(context, selectedPosition)
                binding.logoutBtn.snackbar("Language Changed to ${languages[selectedPosition]}")
                this@SettingsActivity.binding.settingNavView.menu?.findItem(R.id.setting_default_language)?.title = "Default Language (${languages[selectedPosition]})"
            }


        }
        dialogBinding.cancelBtn.setOnClickListener { dismiss() }

        var lastCheckbox:AnimCheckBox? = null

        dialogBinding.recyclerView.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                return object : RecyclerView.ViewHolder(LayoutInflater.from(context)
                    .inflate(R.layout.item_selector,parent, false)){}
            }

            override fun getItemCount(): Int = languages.size

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val itemBinding = ItemSelectorBinding.bind(holder.itemView)
                with(itemBinding){
                    itemTitle.text = languages[position]
                    itemSubTitle.text = ""

                    itemCheckbox.isChecked = Pref.getDefaultLanguage(context) == position
                    if(itemCheckbox.isChecked) lastCheckbox = itemCheckbox

                    FirebaseModelManager.getInstance().getDownloadedModels(FirebaseTranslateRemoteModel::class.java)
                        .addOnSuccessListener {

                            if (it.any { it.language == position }){
                                itemSubTitle.text = "Downloaded"
                            }
                        }
                    itemSelectorLayout.setOnClickListener {
                        lastCheckbox?.isChecked = false
                        itemCheckbox.isChecked = true
                        lastCheckbox = itemCheckbox
                        selectedPosition = position
                    }

                    itemBinding.root.setOnLongClickListener {
                        val popupMenu = PopupMenu(context, it)
                        popupMenu.menu.add("Delete")
                        popupMenu.setOnMenuItemClickListener {
                            //delete selected model
                            FirebaseModelManager.getInstance()
                                .deleteDownloadedModel(FirebaseTranslateRemoteModel.Builder(position).build())
                                .addOnSuccessListener {
                                    toast("Language deleted")
                                    if(position == Pref.getDefaultLanguage(context)){
                                        Pref.getSettingFile(context).edit().remove(Pref.KEY_DEFAULT_TRANSLATION_LANG).apply()
                                        dialogBinding.recyclerView.adapter?.notifyItemChanged(position)
                                        selectedPosition = -1
                                        this@SettingsActivity.binding.settingNavView.menu?.findItem(R.id.setting_default_language)?.title = "Default Language (Tap to choose)"
                                    }
                                }
                            return@setOnMenuItemClickListener true
                        }
                        popupMenu.show()
                        return@setOnLongClickListener true
                    }
                }
            }

        }

        if(Pref.getDefaultLanguage(context) > -1)
            dialogBinding.recyclerView.scrollToPosition(Pref.getDefaultLanguage(context))


    }

    fun onLogoutClick(view: View){


        AlertDialog.Builder(this)
            .setMessage("Logout from this account")
            .setPositiveButton("Yes") { _, _ ->
                FirebaseAuth.getInstance().signOut()
                FirebaseUtils.deleteCurrentToken()
                val intent = Intent(context, SplashActivity::class.java).apply {
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
