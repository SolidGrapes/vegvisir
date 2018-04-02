package edu.cornell.em577.tamperprooflogging.presentation

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import edu.cornell.em577.tamperprooflogging.R
import edu.cornell.em577.tamperprooflogging.data.model.Transaction
import edu.cornell.em577.tamperprooflogging.data.source.BlockChainRepository

class AddBlockActivity : AppCompatActivity() {

    private val transactionList = ArrayList<Pair<String, String>>()
    private var transactionAdapter: ArrayAdapter<Pair<String, String>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_block)
        transactionAdapter = ArrayAdapter(
            this@AddBlockActivity,
            R.layout.transaction_list_item,
            transactionList)
        val listView = findViewById<ListView>(R.id.transactionList)
        listView.adapter = transactionAdapter
    }

    fun createBlockButtonListener(view: View) {
        val blockRepository = BlockChainRepository.getInstance(Pair(applicationContext, resources))
        val transactionsToAdd = transactionList.map {
            Transaction(
                Transaction.TransactionType.RECORD_ACCESS,
                it.first,
                it.second
            )
        }
        if (blockRepository.addBlock(transactionsToAdd)) {
            finish()
        } else {
            val mergeInProgressTextView = findViewById<TextView>(R.id.mergeInProgress)
            mergeInProgressTextView.visibility = View.VISIBLE
        }
    }

    fun addTransactionButtonListener(view: View) {
        val content = findViewById<EditText>(R.id.enterContent).text.toString()
        val comment = findViewById<EditText>(R.id.enterComment).text.toString()

        transactionList.add(Pair(content, comment))
        transactionAdapter?.notifyDataSetChanged()
    }
}