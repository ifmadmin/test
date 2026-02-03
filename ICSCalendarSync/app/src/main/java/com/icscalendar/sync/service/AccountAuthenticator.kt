package com.icscalendar.sync.service

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.accounts.NetworkErrorException
import android.content.Context
import android.os.Bundle

class AccountAuthenticator(private val context: Context) : AbstractAccountAuthenticator(context) {

    override fun editProperties(
        response: AccountAuthenticatorResponse?,
        accountType: String?
    ): Bundle? = null

    override fun addAccount(
        response: AccountAuthenticatorResponse?,
        accountType: String?,
        authTokenType: String?,
        requiredFeatures: Array<out String>?,
        options: Bundle?
    ): Bundle? = null

    override fun confirmCredentials(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        options: Bundle?
    ): Bundle? = null

    override fun getAuthToken(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        authTokenType: String?,
        options: Bundle?
    ): Bundle? = null

    override fun getAuthTokenLabel(authTokenType: String?): String? = null

    override fun updateCredentials(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        authTokenType: String?,
        options: Bundle?
    ): Bundle? = null

    override fun hasFeatures(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        features: Array<out String>?
    ): Bundle {
        return Bundle().apply {
            putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false)
        }
    }

    companion object {
        const val ACCOUNT_TYPE = "com.icscalendar.sync"
        const val ACCOUNT_NAME = "O.M.A. Termine"

        fun getAccount(): Account {
            return Account(ACCOUNT_NAME, ACCOUNT_TYPE)
        }

        fun createAccount(context: Context): Boolean {
            val accountManager = AccountManager.get(context)
            val account = getAccount()

            // Check if account already exists
            val existingAccounts = accountManager.getAccountsByType(ACCOUNT_TYPE)
            if (existingAccounts.isNotEmpty()) {
                return true
            }

            // Create account
            return accountManager.addAccountExplicitly(account, null, null)
        }

        fun accountExists(context: Context): Boolean {
            val accountManager = AccountManager.get(context)
            val accounts = accountManager.getAccountsByType(ACCOUNT_TYPE)
            return accounts.isNotEmpty()
        }
    }
}
