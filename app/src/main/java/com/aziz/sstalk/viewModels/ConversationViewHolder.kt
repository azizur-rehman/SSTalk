package com.aziz.sstalk.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot

class ConversationViewHolder : ViewModel() {

    private val _snapshots = MutableLiveData<DataSnapshot>()

    val snapshots:LiveData<DataSnapshot>
    get() = _snapshots

    fun loadConversations(){

    }
}