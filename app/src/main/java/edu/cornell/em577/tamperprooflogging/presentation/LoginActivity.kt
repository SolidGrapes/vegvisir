package edu.cornell.em577.tamperprooflogging.presentation

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.EditText
import android.widget.TextView
import edu.cornell.em577.tamperprooflogging.R
import edu.cornell.em577.tamperprooflogging.data.source.BlockRepository
import edu.cornell.em577.tamperprooflogging.data.source.UserDataRepository

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
    }

    fun loginButtonListener(view: View) {
        val passwordEditText = findViewById<EditText>(R.id.enterPassword)
        val password = passwordEditText.text.toString()
        val userRepo = UserDataRepository.getInstance(Pair(applicationContext, resources))
        BlockRepository.getInstance(Pair(applicationContext, resources))
        if (userRepo.authenticateAdmin(password)) {
            if (userRepo.inRegistration()) {
                val intent = Intent(this, AddUserActivity::class.java)
                intent.putExtra("AdminPassword", password)
                startActivity(intent)
            } else {
                val intent = Intent(this, RevokeUserActivity::class.java)
                intent.putExtra("AdminPassword", password)
                startActivity(intent)
            }
        } else if (!userRepo.inRegistration() && userRepo.authenticateUser(password)) {
            val intent = Intent(this, UserPanelActivity::class.java)
            intent.putExtra("UserPassword", password)
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