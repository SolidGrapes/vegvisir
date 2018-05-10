package edu.cornell.em577.tamperprooflogging.presentation

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.TextView
import edu.cornell.em577.tamperprooflogging.R

/** Activity responsible for displaying a record with a certain Id. */
class ViewRecordByIdActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_request_record_by_id)
        val recordId = intent.getIntExtra("RecordId", 0)
        val recordTextView = findViewById<TextView>(R.id.textView2)
        recordTextView.text = "This is record $recordId"
        recordTextView.visibility = View.VISIBLE
    }
}