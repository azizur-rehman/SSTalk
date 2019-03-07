package com.aziz.sstalk

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_create_group.*

class CreateGroupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_group)


        add_participant_btn.setOnClickListener {
            startActivityForResult(Intent(this, MultiContactChooserActivity::class.java),101)
        }
    }



}
