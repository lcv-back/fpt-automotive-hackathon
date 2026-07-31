package com.sopa.viva_automotive.vehicleservice.api

import kotlinx.coroutines.flow.Flow

interface VehicleRepository {

        fun observeProperty(propertyId: Int): Flow<CarPropertyResult>

    suspend fun getProperty(propertyId: Int, areaId: Int): Result<CarPropertyResult>

    suspend fun setProperty(propertyId: Int, areaId: Int, value: Any): Result<Unit>
}
