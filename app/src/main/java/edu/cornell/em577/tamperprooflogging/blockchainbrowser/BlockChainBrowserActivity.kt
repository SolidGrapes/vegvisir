package edu.cornell.em577.tamperprooflogging.blockchainbrowser

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import edu.cornell.em577.tamperprooflogging.R
import edu.cornell.em577.tamperprooflogging.data.protocol.EstablishRemoteExchangeProtocol
import org.jetbrains.anko.coroutines.experimental.bg

class BlockChainBrowserActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_block_chain_browser)
        EstablishRemoteExchangeProtocol.execute()
    }

    fun addBlockButtonListener(view: View) {
        // TODO: Render new activity for adding blocks
    }
}
