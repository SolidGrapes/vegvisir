package edu.cornell.em577.tamperprooflogging.presentation

import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import edu.cornell.em577.tamperprooflogging.R
import edu.cornell.em577.tamperprooflogging.data.model.Transaction
import edu.cornell.em577.tamperprooflogging.data.source.BlockRepository
import edu.cornell.em577.tamperprooflogging.data.source.UserDataRepository
import org.jetbrains.anko.textColor
import java.security.PublicKey

class RevokeUserActivity : AppCompatActivity() {

    private var adminPassword: String? = null
    private var certificateList = ArrayList<Pair<String, PublicKey>>()
    private var certificateAdapter: ArrayAdapter<Pair<String, PublicKey>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_revoke_user)
        adminPassword = intent.getStringExtra("AdminPassword")
        val revocableCertificateListView = findViewById<ListView>(R.id.revocableCertificateList)
        val userRepo = UserDataRepository.getInstance(Pair(applicationContext, resources))
        certificateList.addAll(userRepo.getAllUserCertificates())
        certificateAdapter = ArrayAdapter(
            this@RevokeUserActivity,
            R.layout.transaction_list_item,
            certificateList)
        revocableCertificateListView.adapter = certificateAdapter
    }

    fun revokeUserCertificateButtonListener(view: View) {
        val userRepo = UserDataRepository.getInstance(Pair(applicationContext, resources))
        val blockRepo = BlockRepository.getInstance(Pair(applicationContext, resources))

        val userIdToRevoke = findViewById<EditText>(R.id.userIdentifierToRevoke).text.toString()
        val transactionToAdd = Transaction.generateRevocation(userIdToRevoke)
        val revokeUserResultTextView = findViewById<TextView>(R.id.revokeUserResult)
        revokeUserResultTextView.visibility = View.VISIBLE
        if (!userRepo.isActiveUser(userIdToRevoke)) {
            revokeUserResultTextView.text = resources.getText(R.string.user_is_not_active)
            revokeUserResultTextView.textColor = Color.RED
        } else if (blockRepo.addAdminBlock(listOf(transactionToAdd), adminPassword!!)) {
            certificateList.clear()
            certificateList.addAll(userRepo.getAllUserCertificates())
            certificateAdapter?.notifyDataSetChanged()
            revokeUserResultTextView.text = resources.getText(R.string.successfully_revoked_user_certificate)
            revokeUserResultTextView.textColor = Color.GREEN
        } else {
            revokeUserResultTextView.text = resources.getText(R.string.merge_in_progress)
            revokeUserResultTextView.textColor = Color.RED
        }
    }

    fun adminLogoutButtonListener(view: View) {
        finish()
    }
}