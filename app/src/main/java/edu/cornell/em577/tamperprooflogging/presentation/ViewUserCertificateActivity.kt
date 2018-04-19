package edu.cornell.em577.tamperprooflogging.presentation

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.ArrayAdapter
import android.widget.ListView
import edu.cornell.em577.tamperprooflogging.R
import edu.cornell.em577.tamperprooflogging.data.source.UserDataRepository
import java.security.PublicKey

class ViewUserCertificateActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_user_certificate)

        val certificateListView = findViewById<ListView>(R.id.certificateList)
        val userRepo = UserDataRepository.getInstance(Pair(applicationContext, resources))
        val adapter = ArrayAdapter<Pair<String, PublicKey>>(
            this@ViewUserCertificateActivity,
            R.layout.transaction_list_item,
            userRepo.getAllUserCertificates())
        certificateListView.adapter = adapter
    }
}