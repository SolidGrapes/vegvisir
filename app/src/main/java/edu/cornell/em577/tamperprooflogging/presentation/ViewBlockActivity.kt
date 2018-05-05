package edu.cornell.em577.tamperprooflogging.presentation

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.TextView
import com.vegvisir.data.ProtocolMessageProto
import edu.cornell.em577.tamperprooflogging.R
import edu.cornell.em577.tamperprooflogging.data.model.SignedBlock

/** Activity responsible for displaying the contents of a block on the blockchain. */
class ViewBlockActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val block = SignedBlock.fromProto(
            ProtocolMessageProto.SignedBlock.parseFrom(
                intent.getByteArrayExtra("SignedBlock")))
        setContentView(R.layout.activity_view_block)
        findViewById<TextView>(R.id.userIdValue).text = block.unsignedBlock.userId + "\n"
        findViewById<TextView>(R.id.timestampValue).text = block.unsignedBlock.timestamp.toString() + "\n"
        findViewById<TextView>(R.id.locationValue).text = block.unsignedBlock.location + "\n"
        findViewById<TextView>(R.id.parentHashesValue).text = block.unsignedBlock.parentHashes.joinToString("\n")
        findViewById<TextView>(R.id.transactionsValue).text = block.unsignedBlock.transactions.joinToString("\n") + "\n"
        findViewById<TextView>(R.id.signatureValue).text = block.signature
        findViewById<TextView>(R.id.cryptoHashValue).text = block.cryptoHash
    }
}