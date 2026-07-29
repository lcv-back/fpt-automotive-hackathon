package com.sopa.viva_automotive.vehicleservice.api

import kotlinx.coroutines.flow.Flow

interface UxRestrictionsRepository {
    val isDistractionRestricted: Flow<Boolean>
}
