package com.aziz.sstalk.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import com.aziz.sstalk.HomeActivity
import com.aziz.sstalk.MessageActivity
import com.aziz.sstalk.R
import com.aziz.sstalk.databinding.LayoutRecyclerViewBinding
import com.aziz.sstalk.models.Models
import com.aziz.sstalk.utils.FirebaseUtils
import com.aziz.sstalk.utils.utils
import com.aziz.sstalk.utils.visible

class FragmentOnlineFriends : Fragment() {


    lateinit var binding:LayoutRecyclerViewBinding

    fun setOnlineListener() = object : HomeActivity.OnlineUsersLoaded{
        override fun onLoaded(users: List<Models.Contact>) {
            //setOnlineAdapter(users)
        }

    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        binding = LayoutRecyclerViewBinding.inflate(layoutInflater, container, false)

        activity?.let {

        ViewModelProviders.of(requireActivity())[OnlineVM::class.java]
            .getOnlineUsers(context)
            .observe(it, Observer { contacts ->
                Log.d("FragmentOnlineFriends", "onCreateView: $contacts")
                binding.setOnlineAdapter(contacts)
            })

        }

        return binding.root


    }





    private fun LayoutRecyclerViewBinding.setOnlineAdapter(onlineUsers: List<Models.Contact>){




        recyclerBackMessage?.visible =onlineUsers.isEmpty()

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
            val name = itemView.findViewById<TextView>(R.id.name)
            val pic = itemView.findViewById<ImageView>(R.id.pic)

        }

        recyclerView?.adapter = object : RecyclerView.Adapter<ViewHolder>() {
            override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder {
                return ViewHolder(
                    layoutInflater.inflate(R.layout.item_layout_online,
                    p0, false))
            }

            override fun getItemCount(): Int = onlineUsers.size

            override fun onBindViewHolder(p0: ViewHolder, p1: Int) {
                p0.name.text = utils.getNameFromNumber(requireContext(), onlineUsers[p1].number)
                FirebaseUtils.loadProfileThumbnail(requireContext(), onlineUsers[p1].uid, p0.pic)

                p0.itemView.setOnClickListener {
                    startActivity(
                        Intent(this@FragmentOnlineFriends.context, MessageActivity::class.java)
                            .apply {
                                putExtra(FirebaseUtils.KEY_UID, onlineUsers[p1].uid)
                                putExtra(
                                    utils.constants.KEY_TARGET_TYPE,
                                    FirebaseUtils.KEY_CONVERSATION_SINGLE
                                )
                                putExtra(
                                    utils.constants.KEY_NAME_OR_NUMBER,
                                    onlineUsers[p1].number
                                )
                            })
                }
            }

        }


    }
}