package com.aziz.sstalk.fragments

import android.animation.LayoutTransition
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.aziz.sstalk.HomeActivity
import com.aziz.sstalk.R
import com.aziz.sstalk.databinding.FragmentSearchBinding
import com.aziz.sstalk.databinding.ItemContactLayoutBinding
import com.aziz.sstalk.databinding.ItemConversationLayoutBinding
import com.aziz.sstalk.models.Models
import com.aziz.sstalk.utils.*
import com.google.firebase.database.DataSnapshot
import java.util.*

class FragmentSearch : Fragment() {


    lateinit var rootView:FragmentSearchBinding
    private val handler = Handler(Looper.getMainLooper()); lateinit var runnable:Runnable

    private var registeredUsers:List<Models.Contact> = listOf()
    private var groups:MutableMap<String, Models.LastMessageDetail?> = mutableMapOf()
    private var messagesMap:MutableMap<String, DataSnapshot?> = mutableMapOf()
    private var messageSnapshot:DataSnapshot? = null

    lateinit var binding:FragmentSearchBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSearchBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    fun setSearchAdapter() = object : HomeActivity.OnSearched{
        override fun onSubmit(query: String) {
        }

        override fun onQueryChanged(newQuery: String) {

            val query = newQuery.trim()

            binding.contactRecyclerView.setContactAdapter(query)
            rootView.groupsRecyclerView.setGroupAdapter(query)

            runnable = Runnable { rootView.messagesRecyclerView.setMessageAdapter(query) }
            handler.removeCallbacks(runnable)

            handler.postDelayed(runnable, 500)



        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rootView = binding

        try {
            (activity?.findViewById<ViewGroup>(R.id.container_search))?.layoutTransition
                ?.enableTransitionType(LayoutTransition.CHANGING)
        }
        catch (e:Exception){e.printStackTrace()}

        binding.loadData()

    }


    private fun FragmentSearchBinding.loadData(){

        // load contacts
        context?.loadAvailableUsers { registeredUsers = it

            binding.contactRecyclerView.setContactAdapter()

            // load groups
            FirebaseUtils.ref.lastMessage(FirebaseUtils.getUid())
                .orderByChild("type").equalTo(FirebaseUtils.KEY_CONVERSATION_GROUP)
                .onSingleListEvent<Models.LastMessageDetail> { _, snapshot ->

                    snapshot.children.forEach {
                        groups[it.key.orEmpty()] = it.getValue(Models.LastMessageDetail::class.java)
                    }

                    groupsRecyclerView.setGroupAdapter()
                }
        }



        // load messages
        FirebaseUtils.ref.getChatRoot(FirebaseUtils.getUid())
            .onSingleListEvent<Any> { _, snapshot ->
                messageSnapshot = snapshot
            }

        messageHeading.hide()
        contactHeading.hide()
        groupHeading.hide()
    }


    private fun RecyclerView.setContactAdapter(query: String? = null){

        var list = registeredUsers

        if(query.orEmpty().isNotEmpty()){
           list =  registeredUsers.filter { it.name.contains(query!!, true) || it.number.contains(query) }
        }

        binding.contactHeading.visible = list.isNotEmpty()

        setCustomAdapter(context, list, R.layout.item_contact_layout){
                itemView, _, item ->

            val itemBinding = ItemContactLayoutBinding.bind(itemView)
            with(itemBinding){
                FirebaseUtils.loadProfileThumbnail(context, item.uid, pic)
                name.text = item.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                setOnClickListener {
                    context.startChat(item.uid, FirebaseUtils.KEY_CONVERSATION_SINGLE, item.number)
                    onItemClicked?.onClicked()
                }
            }

        }
    }

    private fun RecyclerView.setGroupAdapter(query: String? = null){

        var list = groups.entries

        if(query.orEmpty().isNotEmpty()){
            list =  groups.entries.filter{
                it.value?.nameOrNumber?.contains(query!!, true)?:true
            }.toMutableSet()
        }

        binding.groupHeading.visible = list.isNotEmpty()

        setCustomAdapter(context, list, R.layout.item_contact_layout){
                itemView, _, item ->

            val itemBinding = ItemContactLayoutBinding.bind(itemView)
            val uid = item.key

            with(itemBinding){
                val groupName = item.value?.nameOrNumber.orEmpty()

                name.text = groupName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                FirebaseUtils.loadGroupPicThumbnail(context, uid, pic)
                setOnClickListener {
                    context.startChat(uid, FirebaseUtils.KEY_CONVERSATION_GROUP,groupName)
                    onItemClicked?.onClicked()
                }
            }

        }
    }


    private fun RecyclerView.setMessageAdapter(query: String? = null){

        binding.messageHeading.hide()
        messagesMap.clear()
        binding.messagesRecyclerView.adapter = null

        if(query?.length?:0 < 4)
                return


        messageSnapshot?.children?.forEach { conversation ->
            val targetUID = conversation.key.orEmpty()

            conversation.children.filter { val message = it.getValue(Models.MessageModel::class.java)
                message?.message?.contains(query!!, true)?:false && message?.messageType == "message"
            }
                .forEach {
                    if(!it.key.isNullOrEmpty())
                        messagesMap[it.key!!] = it
                }
        }

        binding.messageHeading.visible = messagesMap.isNotEmpty()

        setCustomAdapter(context, messagesMap.entries, R.layout.item_conversation_layout ){ itemView, _, item ->

            val itemBinding = ItemConversationLayoutBinding.bind(itemView)
            with(itemBinding){

                val uid = item.value?.ref?.parent?.key?:""
                val messageID = item.value?.key
                val message = item.value?.getValue(Models.MessageModel::class.java)
                var nameOrNumber = ""

                var type = FirebaseUtils.KEY_CONVERSATION_SINGLE

                if(utils.isGroupID(uid)) {
                    FirebaseUtils.setGroupName(uid, name){ nameOrNumber = it; return@setGroupName Unit }

                    FirebaseUtils.loadGroupPicThumbnail(context, uid, pic)
                    type = FirebaseUtils.KEY_CONVERSATION_GROUP
                }
                else {
                    FirebaseUtils.setUserDetailFromUID(context, name, uid, utils.hasContactPermission(context)){ nameOrNumber = it; return@setUserDetailFromUID Unit }
                    FirebaseUtils.loadProfileThumbnail(context, uid, pic)
                }

                messageTime.text = message?.timeInMillis?.let { utils.getHeaderFormattedDate(it) }
                adLayout.conversationNativeAd.hide()
                conversationMuteIcon.hide()
                deliveryStatusLastMsg.hide()

                val messageTextView = mobileNumber
                var messageString = message?.message.orEmpty().trim().replace("\n"," ")
                var marquee = "... "

                try {
                    val index = messageString.indexOf(query!!, ignoreCase = true)

                    if(index == 0)
                        marquee = ""

                    messageString = marquee + messageString


                    val spannableString = SpannableString(marquee + messageString.substring(marquee.length + index))
                    spannableString.setSpan(
                        ForegroundColorSpan(ContextCompat.getColor(context, R.color.highlight_blue_color)),
                        marquee.length, marquee.length + query.length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    messageTextView.text = spannableString
                }
                catch (e:Exception){ messageTextView.text = marquee + messageString }

                setOnClickListener { if(nameOrNumber.isNotEmpty()) {
                    context.startChat(uid, type, nameOrNumber , message = message)
                    onItemClicked?.onClicked()
                }
                }

            }



        }


    }


    var onItemClicked:OnItemClicked? = null
    fun setOnItemClickedListener(onItemClicked: OnItemClicked){
        this.onItemClicked = onItemClicked
    }




    interface OnItemClicked{
        fun onClicked()
    }

}