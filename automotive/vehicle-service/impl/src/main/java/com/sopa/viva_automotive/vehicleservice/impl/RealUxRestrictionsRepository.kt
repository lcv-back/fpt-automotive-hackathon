package com.sopa.viva_automotive.vehicleservice.impl

import android.car.Car
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.Context
import com.sopa.viva_automotive.vehicleservice.api.UxRestrictionsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest

@Singleton
class RealUxRestrictionsRepository @Inject constructor(
    @ApplicationContext context: Context,
) : UxRestrictionsRepository {

    private val manager = MutableStateFlow<CarUxRestrictionsManager?>(null)

    init {
        Car.createCar(context, null, Car.CAR_WAIT_TIMEOUT_WAIT_FOREVER) { car, ready ->
            manager.value = if (ready) {
                car.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as CarUxRestrictionsManager
            } else {
                null
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val isDistractionRestricted: Flow<Boolean> =
        manager.filterNotNull().flatMapLatest { uxManager ->
            callbackFlow {
                trySend(
                    uxManager.currentCarUxRestrictions?.isRequiresDistractionOptimization ?: false,
                )
                uxManager.registerListener { restrictions ->
                    trySend(restrictions.isRequiresDistractionOptimization)
                }
                awaitClose { runCatching { uxManager.unregisterListener() } }
            }
        }.distinctUntilChanged()
}
