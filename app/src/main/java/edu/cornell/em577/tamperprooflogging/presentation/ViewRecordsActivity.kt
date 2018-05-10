package edu.cornell.em577.tamperprooflogging.presentation

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import edu.cornell.em577.tamperprooflogging.R
import edu.cornell.em577.tamperprooflogging.data.source.RecordRepository

/** Activity responsible for displaying. */
class ViewRecordsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_record)
        val recordRepo = RecordRepository.getInstance(Pair(applicationContext, resources))
        val record1Button = findViewById<Button>(R.id.button7)
        if (!recordRepo.canView(1)) {
            record1Button.alpha = .5f
            record1Button.isClickable = false
        }
        val record2Button = findViewById<Button>(R.id.button8)
        if (!recordRepo.canView(2)) {
            record2Button.alpha = .5f
            record2Button.isClickable = false
        }
        val record3Button = findViewById<Button>(R.id.button9)
        if (!recordRepo.canView(3)) {
            record3Button.alpha = .5f
            record3Button.isClickable = false
        }
        val record4Button = findViewById<Button>(R.id.button10)
        if (!recordRepo.canView(4)) {
            record4Button.alpha = .5f
            record4Button.isClickable = false
        }
        val record5Button = findViewById<Button>(R.id.button11)
        if (!recordRepo.canView(5)) {
            record5Button.alpha = .5f
            record5Button.isClickable = false
        }
    }

    private fun viewRecord(recordId: Int) {
        val intent = Intent(this, ViewRecordByIdActivity::class.java)
        intent.putExtra("RecordId", recordId)
        startActivity(intent)
    }

    fun viewRecordButtonListener1(view: View) {
        viewRecord(1)
    }

    fun viewRecordButtonListener2(view: View) {
        viewRecord(2)
    }

    fun viewRecordButtonListener3(view: View) {
        viewRecord(3)
    }

    fun viewRecordButtonListener4(view: View) {
        viewRecord(4)
    }

    fun viewRecordButtonListener5(view: View) {
        viewRecord(5)
    }
}