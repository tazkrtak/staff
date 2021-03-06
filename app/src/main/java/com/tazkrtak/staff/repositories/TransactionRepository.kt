package com.tazkrtak.staff.repositories

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.tazkrtak.staff.models.Conductor
import com.tazkrtak.staff.models.Transaction
import com.tazkrtak.staff.util.Auth
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

object TransactionRepository {

    val COLLECTION = "transactions"

    suspend fun get(id: String): Transaction? {
        val db = FirebaseFirestore.getInstance()
        val doc = db.collection(COLLECTION).document(id).get().await()
        return doc.toObject(Transaction::class.java)
    }

    fun set(clientId: String, issuer: String, amount: Double) {

        val db = FirebaseFirestore.getInstance()

        db.runBatch { batch ->

            val clientRef = db.collection(ClientRepository.COLLECTION).document(clientId)
            val docRef = db.collection(COLLECTION).document()

            val transaction = Transaction(
                docRef.id,
                clientId,
                issuer,
                amount,
                Timestamp.now()
            )

            batch.update(clientRef, "balance", FieldValue.increment(amount))
            batch.update(clientRef, "lastTransactionId", docRef.id)
            batch.set(docRef, transaction)

        }

    }

    suspend fun getCountOfToday(): Int {
        val currentSeconds = System.currentTimeMillis() / 1000
        val startOfTheDayEpoch = currentSeconds - (currentSeconds % TimeUnit.DAYS.toSeconds(1))

        val db = FirebaseFirestore.getInstance()
        val busId = (Auth.currentUser!! as Conductor).bus!!.id
        val docs = db.collection(COLLECTION)
            .whereGreaterThanOrEqualTo("timestamp", Timestamp(startOfTheDayEpoch, 0))
            .whereEqualTo("issuer", busId).get().await()

        return docs.size()
    }

}