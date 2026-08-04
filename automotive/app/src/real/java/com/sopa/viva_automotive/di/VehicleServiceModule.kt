package com.sopa.viva_automotive.di

import com.sopa.viva_automotive.vehicleservice.api.UxRestrictionsRepository
import com.sopa.viva_automotive.vehicleservice.api.VehicleRepository
import com.sopa.viva_automotive.vehicleservice.impl.DefaultSafetyGuard
import com.sopa.viva_automotive.vehicleservice.impl.GuardedVehicleRepository
import com.sopa.viva_automotive.vehicleservice.impl.RealUxRestrictionsRepository
import com.sopa.viva_automotive.vehicleservice.impl.RealVehicleRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class VehicleServiceModule {
    @Binds
    @Singleton
    abstract fun bindUxRestrictionsRepository(
        impl: RealUxRestrictionsRepository,
    ): UxRestrictionsRepository

    companion object {
        @Provides
        @Singleton
        fun provideVehicleRepository(
            delegate: RealVehicleRepository,
            guard: DefaultSafetyGuard,
        ): VehicleRepository = GuardedVehicleRepository(delegate, guard)
    }
}
