package com.sopa.viva_automotive.vehicleservice.impl

import com.sopa.viva_automotive.vehicleservice.api.UxRestrictionsRepository
import com.sopa.viva_automotive.vehicleservice.api.VehicleProperties
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Singleton
class MockUxRestrictionsRepository @Inject constructor(
    mockVehicleRepository: MockVehicleRepository,
) : UxRestrictionsRepository {

    override val isDistractionRestricted: Flow<Boolean> =
        mockVehicleRepository.observeProperty(VehicleProperties.PERF_VEHICLE_SPEED)
            .map { (it.floatValue() ?: 0f) > RESTRICTED_SPEED_THRESHOLD_MPS }
            .distinctUntilChanged()

    private companion object {
        const val RESTRICTED_SPEED_THRESHOLD_MPS = 1.4f // ~5 km/h creep speed
    }
}
