package com.example.aria.connector

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Concrete wrapper for [EcosystemEvent] to allow passing it over AIDL.
 */
@Parcelize
data class EcosystemEventContainer(val event: EcosystemEvent) : Parcelable
