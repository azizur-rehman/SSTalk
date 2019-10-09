package com.aziz.sstalk.fragments

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.aziz.sstalk.HomeActivity
import com.aziz.sstalk.MessageActivity
import com.aziz.sstalk.R
import com.aziz.sstalk.models.Models
import com.aziz.sstalk.utils.FirebaseUtils
import com.aziz.sstalk.utils.loadAvailableUsers
import com.aziz.sstalk.utils.utils
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.item_contact_layout.view.*
import kotlinx.android.synthetic.main.layout_recycler_view.*
import kotlinx.android.synthetic.main.layout_recycler_view.view.*
import org.jetbrains.anko.collections.forEachWithIndex
import java.lang.Exception

class FragmentSearch : Fragment() {


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.layout_recycler_view,container, false)
        loadRegisteredUsers(view)
        return view


    }


    private fun loadRegisteredUsers(view: View){

        context?.loadAvailableUsers { registeredAvailableUsers ->

        }

    }

}