package com.aziz.sstalk

import android.os.Bundle
import android.view.View
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.aziz.sstalk.databinding.ItemFeedbackBinding
import com.aziz.sstalk.models.Models
import com.aziz.sstalk.utils.FirebaseUtils
import com.aziz.sstalk.utils.utils
import com.firebase.ui.database.FirebaseListAdapter
import com.firebase.ui.database.FirebaseListOptions
import org.jetbrains.anko.alert
import org.jetbrains.anko.noButton
import org.jetbrains.anko.toast
import org.jetbrains.anko.yesButton

class FeedbackActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback)
        title = "Feedbacks"
        showFeedbacks()
    }


    private fun showFeedbacks(){

        val options = FirebaseListOptions.Builder<Models.Feedback>()
            .setLifecycleOwner(this)
            .setQuery(FirebaseUtils.ref.feedback()
                .orderByChild(FirebaseUtils.KEY_REVERSE_TIMESTAMP), Models.Feedback::class.java)
            .setLayout(R.layout.item_feedback)
            .build()

        val adapter = object : FirebaseListAdapter<Models.Feedback>(options){
            override fun populateView(view: View, model: Models.Feedback, position: Int) {

                val v = ItemFeedbackBinding.bind(view)

                FirebaseUtils.setUserDetailFromUID(this@FeedbackActivity, v.reportedBy,
                    model.uid, utils.hasContactPermission(this@FeedbackActivity))


                val dateTime =  "Reported on :     "+utils.getHeaderFormattedDate(model.addedOn) +  " at "+utils.getLocalTime(model.addedOn)
                v.reportedAt.text = dateTime

                 v.report.text = "Feedback -->     "+ model.feedback
                 v.reportedByUID.text ="UID :      "+ model.uid

                v.deleteFeedbackBtn.setOnClickListener {
                    alert { message = "Delete feedback?"
                    yesButton { getRef(position).removeValue().addOnSuccessListener { toast("Feedback deleted") } }
                        noButton {  }
                    }.show()
                }

            }

        }

                findViewById<ListView>(R.id.listView).adapter = adapter


    }
}
