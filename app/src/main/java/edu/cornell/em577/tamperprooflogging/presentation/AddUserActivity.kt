package edu.cornell.em577.tamperprooflogging.presentation

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.EditText
import edu.cornell.em577.tamperprooflogging.R
import edu.cornell.em577.tamperprooflogging.data.source.BlockRepository
import edu.cornell.em577.tamperprooflogging.data.source.UserDataRepository

class AddUserActivity : AppCompatActivity() {

    private var adminPassword: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_user)
        adminPassword = intent.getStringExtra("AdminPassword")
    }

    fun addUserButtonListener(view: View) {
        val identifierEditText = findViewById<EditText>(R.id.enterUserIdentifier)
        val locationEditText = findViewById<EditText>(R.id.enterUserLocation)
        val passwordEditText = findViewById<EditText>(R.id.enterUserPassword)
        val identifier = identifierEditText.text.toString()
        val location = locationEditText.text.toString()
        val password = passwordEditText.text.toString()
        val userRepo = UserDataRepository.getInstance(Pair(applicationContext, resources))
        userRepo.registerUser(identifier, location, password)
        val blockRepo = BlockRepository.getInstance(Pair(applicationContext, resources))
        blockRepo.bootstrap(adminPassword!!)
        finish()
    }
}