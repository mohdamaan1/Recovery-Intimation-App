package com.tensorlabs.amaanat

import android.app.Application
import android.telephony.SmsManager
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tensorlabs.amaanat.data.AccountEntity
import com.tensorlabs.amaanat.data.AppDatabase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).accountDao()

    // Search State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Filtered List Logic
    val filteredAccounts = combine(dao.getAllAccounts(), _searchQuery) { list, query ->
        if (query.isBlank()) {
            list
        } else {
            list.filter {
                it.accountNumber.contains(query, ignoreCase = true) ||
                        it.name.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun saveAccount(account: AccountEntity) {
        viewModelScope.launch {
            if (account.id == 0) dao.insertAccount(account) else dao.updateAccount(account)
        }
    }

    fun deleteAccount(account: AccountEntity) {
        viewModelScope.launch { dao.deleteAccount(account) }
    }


    fun sendTargetedSms(
        context: android.content.Context,
        borrowerMobile: String,
        borrowerMsg: String,
        guarantorMobiles: List<String>,
        guarantorMsg: String
    ) {
        val smsManager = SmsManager.getDefault()
        var sentCount = 0

        try {
            // 1. Send to Borrower
            if (borrowerMobile.length == 10) {
                val parts = smsManager.divideMessage(borrowerMsg)
                smsManager.sendMultipartTextMessage(borrowerMobile, null, parts, null, null)
                sentCount++
            }

            // 2. Send to Guarantors
            if (guarantorMobiles.isNotEmpty()) {
                val gParts = smsManager.divideMessage(guarantorMsg)
                for (mobile in guarantorMobiles) {
                    if (mobile.length == 10) {
                        smsManager.sendMultipartTextMessage(mobile, null, gParts, null, null)
                        sentCount++
                    }
                }
            }

            Toast.makeText(context, "Sent to $sentCount people!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

}
