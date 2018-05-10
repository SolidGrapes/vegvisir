package edu.cornell.em577.tamperprooflogging.presentation

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import edu.cornell.em577.tamperprooflogging.R
import edu.cornell.em577.tamperprooflogging.data.exception.PermissionNotFoundException
import edu.cornell.em577.tamperprooflogging.data.model.Transaction
import edu.cornell.em577.tamperprooflogging.data.source.BlockRepository
import edu.cornell.em577.tamperprooflogging.data.source.RecordRepository
import org.jetbrains.anko.textColor

/** Activity responsible for requesting records by id. */
class RequestRecordsActivity : AppCompatActivity() {

    private val recordIdList = ArrayList<String>()
    private var recordAdapter: ArrayAdapter<String>? = null
    var userPassword: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_request_record)
        recordAdapter = ArrayAdapter(
            this@RequestRecordsActivity,
            R.layout.transaction_list_item,
            recordIdList)
        val listView = findViewById<ListView>(R.id.transactionList)
        listView.adapter = recordAdapter
        userPassword = intent.getStringExtra("UserPassword")
    }

    /** Listener to add a transaction to the UI upon pressing the addRecordId. */
    fun addRecordIdButtonListener(view: View) {
        val recordId = findViewById<EditText>(R.id.enterRecordId).text.toString()
        val recordRepo = RecordRepository.getInstance(Pair(applicationContext, resources))

        val statusText = findViewById<TextView>(R.id.status)
        statusText.visibility = View.VISIBLE
        try {
            if (recordRepo.hasRequested(recordId.toInt())) {
                statusText.text = resources.getText(R.string.previously_requested_record_with_provided_id)
                statusText.textColor = Color.RED
            } else if (recordRepo.exists(recordId.toInt())) {
                statusText.text = resources.getText(R.string.added_record_id_to_request)
                statusText.textColor = Color.GREEN
                recordIdList.add(recordId)
                recordAdapter?.notifyDataSetChanged()
            } else {
                statusText.text = resources.getText(R.string.no_such_record_with_provided_id_on_this_device)
                statusText.textColor = Color.RED
            }
        } catch (nfe: NumberFormatException) {
            statusText.text = resources.getText(R.string.record_id_must_by_a_number)
            statusText.textColor = Color.RED
        }
    }

    /**
     * Listener to create a record request signed block based on the user password and meta-data as
     * well as the user supplied list of record Ids upon pressing the requestRecordsById.
     */
    fun requestRecordsByIdButtonListener(view: View) {
        val blockRepo = BlockRepository.getInstance(Pair(applicationContext, resources))
        val recordIdToRequest = recordIdList.map {
            Transaction(
                Transaction.TransactionType.RECORD_REQUEST,
                it,
                "Requesting record $it"
            )
        }
        val statusText = findViewById<TextView>(R.id.status)
        statusText.visibility = View.VISIBLE
        try {
            if (blockRepo.addUserBlock(recordIdToRequest, userPassword!!)) {
                statusText.text = resources.getText(R.string.requesting_records_now)
                statusText.textColor = Color.GREEN
            } else {
                statusText.text = resources.getText(R.string.merge_in_progress)
                statusText.textColor = Color.RED
            }
        } catch (e: PermissionNotFoundException) {
            val intent = Intent(this, LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }
    }
}