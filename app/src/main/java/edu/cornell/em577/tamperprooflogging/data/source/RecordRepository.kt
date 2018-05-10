package edu.cornell.em577.tamperprooflogging.data.source

import android.content.Context
import android.content.res.Resources
import edu.cornell.em577.tamperprooflogging.data.model.Transaction
import edu.cornell.em577.tamperprooflogging.util.GrowOnlySet
import edu.cornell.em577.tamperprooflogging.util.SingletonHolder
import edu.cornell.em577.tamperprooflogging.util.TwoPhaseSet

/** Repository holding all recorded record requests. */
class RecordRepository private constructor(env: Pair<Context, Resources>) {

    companion object : SingletonHolder<RecordRepository, Pair<Context, Resources>>(::RecordRepository) {
        private const val MAX_RECORD_ID = 5
        private const val NUM_WITNESSES_NEEDED = 2
    }

    private val recordRequests = GrowOnlySet<Transaction>()

    private val userRecordRequests = TwoPhaseSet<Int>()

    /** Add a record request to this repository. */
    fun addRecordRequest(recordRequest: Transaction) {
        synchronized(recordRequests) {
            recordRequests.add(recordRequest)
        }
    }

    /** Retrieve all recorded record requests known to this repository. */
    fun getAllRecordRequests(): List<Transaction> {
        synchronized(recordRequests) {
            return recordRequests.toList()
        }
    }

    /** Returns whether a record with the following id exists on this device. */
    fun exists(recordId: Int): Boolean {
        return recordId in 1..MAX_RECORD_ID
    }

    /** Add all user record requests. */
    fun addUserRecordRequests(recordIds: List<Int>) {
        for (recordId in recordIds) {
            userRecordRequests.add(recordId)
        }
    }

    fun hasRequested(recordId: Int): Boolean {
        return userRecordRequests.lookup(recordId) || userRecordRequests.hasRemoved(recordId)
    }

    /** Returns whether the record with the provided id can be viewed by this device's user. */
    fun canView(recordId: Int): Boolean {
        return userRecordRequests.hasRemoved(recordId)
    }

    fun completedRequests(recordIds: Set<Int>) {
        for (recordId in recordIds) {
            userRecordRequests.remove(recordId)
        }
    }

    fun getNumWitnessesNeeded(): Int {
        return NUM_WITNESSES_NEEDED
    }
}