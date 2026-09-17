package com.example.aria.connector

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Concrete wrapper for [AppCommand] to allow passing it over AIDL.
 * AIDL generated Java code references the static CREATOR field, which
 * is not generated on abstract sealed classes directly.
 */
@Parcelize
data class AppCommandContainer(val command: AppCommand) : Parcelable
