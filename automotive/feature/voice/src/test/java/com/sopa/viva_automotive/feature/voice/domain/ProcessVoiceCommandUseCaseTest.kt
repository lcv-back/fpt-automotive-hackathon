package com.sopa.viva_automotive.feature.voice.domain

import com.sopa.viva_automotive.feature.voice.FakeCommandMappingDao
import com.sopa.viva_automotive.feature.voice.FakeSemanticIntentMatcher
import com.sopa.viva_automotive.feature.voice.data.CommandMappingRepository
import com.sopa.viva_automotive.feature.voice.domain.model.VehicleIntent
import com.sopa.viva_automotive.feature.voice.domain.model.VehicleIntentTypes
import com.sopa.viva_automotive.vehicleservice.api.VehicleZone
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProcessVoiceCommandUseCaseTest {

    private lateinit var useCase: ProcessVoiceCommandUseCase

    @Before
    fun setUp() = runTest {
        val repository = CommandMappingRepository(FakeCommandMappingDao())
        repository.seedIfEmpty()
        useCase = ProcessVoiceCommandUseCase(repository, FakeSemanticIntentMatcher())
    }

    @Test
    fun `set temperature with spoken number`() = runTest {
        val intent = useCase("set the temperature to twenty two degrees")
        assertEquals(VehicleIntent.SetTemperature(22.0, VehicleZone.DRIVER), intent)
    }

    @Test
    fun `set temperature for passenger zone`() = runTest {
        val intent = useCase("set passenger temperature to 19")
        assertEquals(VehicleIntent.SetTemperature(19.0, VehicleZone.PASSENGER), intent)
    }

    @Test
    fun `warmer maps to positive temperature adjustment`() = runTest {
        val intent = useCase("make it warmer")
        assertEquals(VehicleIntent.AdjustTemperature(1.0, VehicleZone.DRIVER), intent)
    }

    @Test
    fun `too hot maps to negative temperature adjustment`() = runTest {
        val intent = useCase("it is too hot in here")
        assertEquals(VehicleIntent.AdjustTemperature(-1.0, VehicleZone.DRIVER), intent)
    }

    @Test
    fun `ac on and off`() = runTest {
        assertEquals(VehicleIntent.SetAc(true), useCase("turn on the ac"))
        assertEquals(VehicleIntent.SetAc(false), useCase("turn off the ac"))
    }

    @Test
    fun `stt spaced a c normalizes to ac off`() = runTest {
        assertEquals(VehicleIntent.SetAc(false), useCase("turn off the a c"))
    }

    @Test
    fun `embedding fallback maps aircon paraphrase`() = runTest {
        val repository = CommandMappingRepository(FakeCommandMappingDao())
        repository.seedIfEmpty()
        val semantic = FakeSemanticIntentMatcher(
            mapOf("turn off aircon" to VehicleIntentTypes.AC_OFF),
        )
        val nlu = ProcessVoiceCommandUseCase(repository, semantic)
        assertEquals(VehicleIntent.SetAc(false), nlu("turn off aircon"))
    }

    @Test
    fun `fan speed with spoken number`() = runTest {
        assertEquals(VehicleIntent.SetFanSpeed(5), useCase("set fan speed to five"))
    }

    @Test
    fun `unlock wins over lock for unlock utterances`() = runTest {
        assertEquals(VehicleIntent.SetDoorLock(true), useCase("lock the doors"))
        assertEquals(VehicleIntent.SetDoorLock(false), useCase("unlock the doors please"))
    }

    @Test
    fun `status queries`() = runTest {
        assertEquals(
            VehicleIntent.QueryStatus(VehicleIntent.StatusQueryKind.SPEED),
            useCase("how fast am i going"),
        )
        assertEquals(
            VehicleIntent.QueryStatus(VehicleIntent.StatusQueryKind.FUEL),
            useCase("how much fuel is left"),
        )
    }

    @Test
    fun `vietnamese ac on`() = runTest {
        assertEquals(VehicleIntent.SetAc(true), useCase("bật điều hòa"))
    }

    @Test
    fun `unrecognized utterance maps to unknown`() = runTest {
        val intent = useCase("play some jazz music")
        assertTrue(intent is VehicleIntent.Unknown)
    }

    @Test
    fun `set temperature without number maps to unknown`() = runTest {
        val intent = useCase("set the temperature")
        assertTrue(intent is VehicleIntent.Unknown)
    }
}
