package edu.cornell.em577.tamperprooflogging.presentation

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import edu.cornell.em577.tamperprooflogging.R
import edu.cornell.em577.tamperprooflogging.protocol.EstablishRemoteExchangeProtocol

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContentView(R.layout.activity_login)
//        EstablishRemoteExchangeProtocol.getInstance(Pair(applicationContext, resources)).execute()
    }
}