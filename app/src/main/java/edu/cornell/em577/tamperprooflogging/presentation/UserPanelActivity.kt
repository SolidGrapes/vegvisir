package edu.cornell.em577.tamperprooflogging.presentation

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import edu.cornell.em577.tamperprooflogging.R
import edu.cornell.em577.tamperprooflogging.protocol.EstablishRemoteExchangeProtocol

/** Activity responsible for managing the user control panel. */
class UserPanelActivity : AppCompatActivity() {

    var userPassword: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_panel)
        userPassword = intent.getStringExtra("UserPassword")

        EstablishRemoteExchangeProtocol.getInstance(Triple(applicationContext, resources, userPassword!!)).execute()
    }

    /**
     * Listener that triggers when the requestRecordsButton is pressed. Takes the user to the
     * RequestRecordsActivity.
     */
    fun requestRecordsButtonListener(view: View) {
        val intent = Intent(this, RequestRecordsActivity::class.java)
        intent.putExtra("UserPassword", userPassword!!)
        startActivity(intent)
    }

    /**
     * Listener that triggers when the viewRecordsButton is pressed. Takes the user to the
     * RequestRecordsActivity.
     */
    fun viewRecordsButtonListener(view: View) {
        val intent = Intent(this, ViewRecordsActivity::class.java)
        startActivity(intent)
    }

    /**
     * Listener that triggers when the addBlockButton is pressed. Takes the user to the
     * AddBlockActivity.
     */
    fun addBlockButtonListener(view: View) {
        val intent = Intent(this, AddBlockActivity::class.java)
        intent.putExtra("UserPassword", userPassword!!)
        startActivity(intent)
    }

    /**
     * Listener that triggers when the viewBlockBrowserButton is pressed. Takes the user to the
     * BlockChainBrowserActivity.
     */
    fun viewBlockBrowserButtonListener(view: View) {
        val intent = Intent(this, BlockChainBrowserActivity::class.java)
        startActivity(intent)
    }

    /**
     * Listener that triggers when the viewRecordRequestButton is pressed. Takes the user to the
     * ViewRecordRequestActivity.
     */
    fun viewRecordRequestButtonListener(view: View) {
        val intent = Intent(this, ViewRecordRequestActivity::class.java)
        startActivity(intent)
    }

    /**
     * Listener that triggers when the viewUserCertificateButton is pressed. Takes the user to the
     * ViewUserCertificateActivity.
     */
    fun viewUserCertificateButtonListener(view: View) {
        val intent = Intent(this, ViewUserCertificateActivity::class.java)
        startActivity(intent)
    }

    /** Listener that triggers when the user presses the logoutButton. User logs out. */
    fun logoutButtonListener(view: View) {
        finish()
    }
}