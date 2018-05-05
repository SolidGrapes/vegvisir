package edu.cornell.em577.tamperprooflogging.presentation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.EditText
import android.widget.TextView
import edu.cornell.em577.tamperprooflogging.R
import edu.cornell.em577.tamperprooflogging.data.source.BlockRepository
import edu.cornell.em577.tamperprooflogging.data.source.UserDataRepository

/** Activity responsible for managing the login page. */
class LoginActivity : AppCompatActivity() {

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        private const val REQUEST_CODE_REQUIRED_PERMISSIONS = 1

        /** Returns `true` if the app was granted all the permissions. Otherwise, returns `false`. */
        private fun hasPermissions(context: Context, permissions: Array<String>): Boolean {
            return permissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
    }

    /** Request permissions for Google Nearby if permissions were not obtained previously. */
    override fun onStart() {
        super.onStart()
        if (!hasPermissions(this, REQUIRED_PERMISSIONS)) {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSIONS)
        }
    }

    /**
     * Listener that triggers when the loginButton is pressed. Specifically, the password will
     * be authenticated to see if it is a user or an admin that is logging in.
     */
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