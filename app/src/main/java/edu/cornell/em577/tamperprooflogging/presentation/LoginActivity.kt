package edu.cornell.em577.tamperprooflogging.presentation

import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.TextView
import edu.cornell.em577.tamperprooflogging.R
import edu.cornell.em577.tamperprooflogging.data.source.UserDataRepository
import edu.cornell.em577.tamperprooflogging.protocol.EstablishRemoteExchangeProtocol

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContentView(R.layout.activity_login)
//        EstablishRemoteExchangeProtocol.getInstance(Pair(applicationContext, resources)).execute()
    }

    fun loginButtonListener(view: View) {
        val usernameEditText = findViewById<EditText>(R.id.enterUsername)
        val passwordEditText = findViewById<EditText>(R.id.enterPassword)
        val username = usernameEditText.text.toString()
        val password = passwordEditText.text.toString()
        val userRepository = UserDataRepository.getInstance(resources)
        if (userRepository.containsUserWithPassword(username, password)) {
            val currentUser = userRepository.getUser(username)
            userRepository.setCurrentUser(currentUser)
            val intent = Intent(this, BlockChainBrowserActivity::class.java)
            startActivity(intent)
        } else {
            val incorrectCredentialsTextView = findViewById<TextView>(R.id.incorrectCredentials)
            incorrectCredentialsTextView.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        val incorrectCredentialsTextView = findViewById<TextView>(R.id.incorrectCredentials)
        incorrectCredentialsTextView.visibility = View.INVISIBLE
    }
}