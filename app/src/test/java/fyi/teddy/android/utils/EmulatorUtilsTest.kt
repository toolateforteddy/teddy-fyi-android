package fyi.teddy.android.utils

import android.os.Build
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
class EmulatorUtilsTest {

    @Test
    fun isEmulator_detectsGenericFingerprint() {
        ReflectionHelpers.setStaticField(Build::class.java, "FINGERPRINT", "generic/vbox86p/vbox86p:6.0/MASTER/64:user/release-keys")
        assertTrue(EmulatorUtils.isEmulator())
    }

    @Test
    fun isEmulator_detectsSdkGphone() {
        ReflectionHelpers.setStaticField(Build::class.java, "FINGERPRINT", "google/sdk_gphone16k_x86_64/emu64xa16k:17")
        assertTrue(EmulatorUtils.isEmulator())
    }

    @Test
    fun isEmulator_detectsGoldfishHardware() {
        ReflectionHelpers.setStaticField(Build::class.java, "FINGERPRINT", "not-emulator")
        ReflectionHelpers.setStaticField(Build::class.java, "HARDWARE", "goldfish")
        assertTrue(EmulatorUtils.isEmulator())
    }

    @Test
    fun isEmulator_detectsRanchuHardware() {
        ReflectionHelpers.setStaticField(Build::class.java, "FINGERPRINT", "not-emulator")
        ReflectionHelpers.setStaticField(Build::class.java, "HARDWARE", "ranchu")
        assertTrue(EmulatorUtils.isEmulator())
    }

    @Test
    fun isEmulator_returnsFalseForRealDevice() {
        ReflectionHelpers.setStaticField(Build::class.java, "FINGERPRINT", "google/pixel/pixel:12/SP1A.210812.015/7679544:user/release-keys")
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "Pixel 6")
        ReflectionHelpers.setStaticField(Build::class.java, "MANUFACTURER", "Google")
        ReflectionHelpers.setStaticField(Build::class.java, "HARDWARE", "cheetah")
        ReflectionHelpers.setStaticField(Build::class.java, "PRODUCT", "pixel")
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", "google")
        ReflectionHelpers.setStaticField(Build::class.java, "DEVICE", "pixel")
        
        assertFalse(EmulatorUtils.isEmulator())
    }
}
