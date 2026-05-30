package com.feelsokman.auth

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.content.Context
import android.os.Bundle
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class AccountAuthenticator @Inject constructor(
    @ApplicationContext val context: Context
) {

    val impl = object : AbstractAccountAuthenticator(context) {

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
        ): Bundle? = null
    }
}