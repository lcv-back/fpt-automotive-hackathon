package com.sopa.viva_automotive.feature.voice.domain

import com.sopa.viva_automotive.feature.voice.domain.model.VehicleIntent
import com.sopa.viva_automotive.vehicleservice.api.CarPropertyResult
import com.sopa.viva_automotive.vehicleservice.api.SafetyConfirmationRequiredException
import com.sopa.viva_automotive.vehicleservice.api.SafetyDeniedException
import com.sopa.viva_automotive.vehicleservice.api.SafetyRules
import com.sopa.viva_automotive.vehicleservice.api.VehicleProperties
import com.sopa.viva_automotive.vehicleservice.api.VehicleRepository
import com.sopa.viva_automotive.vehicleservice.api.VehicleWriteContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unlocking the door by voice is two turns: the guard asks `G2_CONFIRM_DOOR`,
 * and the repeat of the same command answers it. Without the second turn the
 * intent is dead — the guard refuses every unlock and nothing ever reaches the
 * setter.
 *
 * `ExecuteVehicleControlUseCase` cannot be constructed in a JVM unit test
 * (SettingsDataStore needs an Android Context and there is no mocking library
 * on the test classpath), so this reproduces the turn loop with the REAL
 * production decisions — [ExecuteVehicleControlUseCase.retainsPendingDoorUnlock]
 * and [ExecuteVehicleControlUseCase.armsDoorConfirmation] — against a fake
 * repository standing in for `GuardedVehicleRepository`.
 *
 * The fake answers exactly what the real guard answers for the door rules
 * (`DefaultSafetyGuard.doorDenyRules` / `doorConfirmRule`); the rules themselves
 * are covered by `SafetyGuardTest` and `GuardedVehicleRepositoryTest` in the
 * `vehicle-service:impl` module, which this module cannot depend on. What is
 * under test here is the turn loop: when the confirmation is armed, when it is
 * cleared, and what `isConfirmed` each write carries.
 */
class DoorUnlockConfirmationTurnTest {

    /** Stands in for GuardedVehicleRepository over the two door rules. */
    private class FakeGuardedRepository(var speedKmh: Float = 0f) : VehicleRepository {
        val writes = mutableListOf<Pair<Any, Boolean>>()

        override fun observeProperty(propertyId: Int): Flow<CarPropertyResult> = emptyFlow()

        override suspend fun getProperty(propertyId: Int, areaId: Int): Result<CarPropertyResult> =
            Result.failure(UnsupportedOperationException("not used"))

        override suspend fun setProperty(
            propertyId: Int,
            areaId: Int,
            value: Any,
            context: VehicleWriteContext,
        ): Result<Unit> {
            val isUnlock = propertyId == VehicleProperties.DOOR_LOCK && value == false
            if (isUnlock && speedKmh > 5f) {
                return Result.failure(
                    SafetyDeniedException(
                        SafetyRules.SPEED_LOCK,
                        "Xe đang chạy, mình chưa mở cửa được.",
                    ),
                )
            }
            if (isUnlock && !context.isConfirmed) {
                return Result.failure(
                    SafetyConfirmationRequiredException(
                        SafetyRules.CONFIRM_DOOR,
                        "Bạn có chắc muốn mở khoá cửa không?",
                    ),
                )
            }
            writes += value to context.isConfirmed
            return Result.success(Unit)
        }
    }

    private val repository = FakeGuardedRepository()
    private var pendingDoorUnlock = false

    /** Mirrors ExecuteVehicleControlUseCase: clear first, dispatch, then re-arm. */
    private suspend fun turn(intent: VehicleIntent): Result<Unit> {
        if (!ExecuteVehicleControlUseCase.retainsPendingDoorUnlock(intent)) {
            pendingDoorUnlock = false
        }
        if (intent !is VehicleIntent.SetDoorLock) return Result.success(Unit)

        val result = repository.setProperty(
            VehicleProperties.DOOR_LOCK,
            1,
            intent.locked,
            VehicleWriteContext(isConfirmed = pendingDoorUnlock),
        )
        pendingDoorUnlock = ExecuteVehicleControlUseCase.armsDoorConfirmation(result.exceptionOrNull())
        return result
    }

    @Test
    fun `the first unlock asks and writes nothing, the repeat unlocks`() = runTest {
        val first = turn(VehicleIntent.SetDoorLock(locked = false))
        assertTrue("$first", first.exceptionOrNull() is SafetyConfirmationRequiredException)
        assertTrue("nothing may reach the setter before the driver answers", repository.writes.isEmpty())

        val second = turn(VehicleIntent.SetDoorLock(locked = false))
        assertTrue("$second", second.isSuccess)
        assertEquals(listOf<Pair<Any, Boolean>>(false to true), repository.writes)
    }

    @Test
    fun `an unrelated turn between the question and the answer forces a re-ask`() = runTest {
        turn(VehicleIntent.SetDoorLock(locked = false)) // asked
        turn(VehicleIntent.SetAc(true)) // driver moved on

        val third = turn(VehicleIntent.SetDoorLock(locked = false))

        assertTrue("$third", third.exceptionOrNull() is SafetyConfirmationRequiredException)
        assertTrue("the stale confirmation must not unlock", repository.writes.isEmpty())
    }

    @Test
    fun `locking cancels a pending unlock instead of confirming it`() = runTest {
        turn(VehicleIntent.SetDoorLock(locked = false)) // asked
        turn(VehicleIntent.SetDoorLock(locked = true)) // "khóa cửa" — changed their mind

        assertEquals("locking is never gated", listOf<Pair<Any, Boolean>>(true to false), repository.writes)

        val third = turn(VehicleIntent.SetDoorLock(locked = false))
        assertTrue("$third", third.exceptionOrNull() is SafetyConfirmationRequiredException)
    }

    @Test
    fun `confirming is not a bypass — a car that started moving is still denied`() = runTest {
        turn(VehicleIntent.SetDoorLock(locked = false)) // asked while stopped
        repository.speedKmh = 60f // and then the car pulls away

        val second = turn(VehicleIntent.SetDoorLock(locked = false))

        assertTrue("$second", second.exceptionOrNull() is SafetyDeniedException)
        assertEquals(SafetyRules.SPEED_LOCK, (second.exceptionOrNull() as SafetyDeniedException).rule)
        assertTrue(repository.writes.isEmpty())
    }

    @Test
    fun `a denial does not arm the confirmation`() {
        // After "xe đang chạy" the next unlock is a fresh request, not an answer
        // to a question that was never asked.
        assertFalse(
            ExecuteVehicleControlUseCase.armsDoorConfirmation(
                SafetyDeniedException(SafetyRules.SPEED_LOCK, "Xe đang chạy, mình chưa mở cửa được."),
            ),
        )
    }

    @Test
    fun `a low-confidence question does not arm the door confirmation`() {
        // G3_LOW_CONFIDENCE asks the driver to REPEAT, not to approve. Arming on
        // it would let a misheard unlock through on the repeat without the
        // safety question ever being asked.
        assertFalse(
            ExecuteVehicleControlUseCase.armsDoorConfirmation(
                SafetyConfirmationRequiredException(
                    SafetyRules.LOW_CONFIDENCE,
                    "Mình nghe chưa rõ. Bạn nhắc lại giúp mình nhé?",
                ),
            ),
        )
    }

    @Test
    fun `only a repeated unlock keeps the question open`() {
        assertTrue(
            ExecuteVehicleControlUseCase.retainsPendingDoorUnlock(
                VehicleIntent.SetDoorLock(locked = false),
            ),
        )
        assertFalse(
            ExecuteVehicleControlUseCase.retainsPendingDoorUnlock(
                VehicleIntent.SetDoorLock(locked = true),
            ),
        )
        // "có" is not in the grammar (GrammarIntentRouter.kt:54) — it routes to
        // Unknown, which must cancel rather than confirm.
        assertFalse(
            ExecuteVehicleControlUseCase.retainsPendingDoorUnlock(VehicleIntent.Unknown("có")),
        )
        assertFalse(
            ExecuteVehicleControlUseCase.retainsPendingDoorUnlock(VehicleIntent.SetAc(true)),
        )
    }
}
