package edu.cornell.em577.tamperprooflogging.presentation

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import edu.cornell.em577.tamperprooflogging.R

class UserPanelActivity : AppCompatActivity() {

    var userPassword: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_panel)
        userPassword = intent.getStringExtra("UserPassword")

//        val userRepo = UserDataRepository.getInstance(Pair(applicationContext, resources))
//        val blockRepo = BlockRepository.getInstance(Pair(applicationContext, resources))
//        EstablishRemoteExchangeProtocol.getInstance(Triple(blockRepo,userRepo, userPassword!!)).execute()
    }

    fun addBlockButtonListener(view: View) {
        val intent = Intent(this, AddBlockActivity::class.java)
        intent.putExtra("UserPassword", userPassword!!)
        startActivity(intent)
    }

    fun viewBlockBrowserButtonListener(view: View) {
        val intent = Intent(this, BlockChainBrowserActivity::class.java)
        startActivity(intent)
    }

    fun viewRecordAccessButtonListener(view: View) {
        val intent = Intent(this, ViewRecordAccessActivity::class.java)
        startActivity(intent)
    }

    fun viewUserCertificateButtonListener(view: View) {
        val intent = Intent(this, ViewUserCertificateActivity::class.java)
        startActivity(intent)
    }

    fun logoutButtonListener(view: View) {
        finish()
    }
}