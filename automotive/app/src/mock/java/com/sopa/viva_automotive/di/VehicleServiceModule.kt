package com.sopa.viva_automotive.di

import com.sopa.viva_automotive.vehicleservice.api.UxRestrictionsRepository
import com.sopa.viva_automotive.vehicleservice.api.VehicleRepository
import com.sopa.viva_automotive.vehicleservice.impl.MockUxRestrictionsRepository
import com.sopa.viva_automotive.vehicleservice.impl.MockVehicleRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class VehicleServiceModule {

    @Binds
    @Singleton
    abstract fun bindVehicleRepository(impl: MockVehicleRepository): VehicleRepository

    @Binds
    @Singleton
    abstract fun bindUxRestrictionsRepository(
        impl: MockUxRestrictionsRepository,
    ): UxRestrictionsRepository
}
