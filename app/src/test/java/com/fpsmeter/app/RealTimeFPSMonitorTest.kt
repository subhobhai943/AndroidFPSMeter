package com.fpsmeter.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RealTimeFPSMonitorTest {

    @Test
    fun testParseSurfaceFlingerFrameCount() {
        val output = "Result: Parcel(001194ad    '....')"
        val frameCount = RealTimeFPSMonitor.parseSurfaceFlingerFrameCount(output)
        assertEquals(0x001194adL, frameCount)
    }

    @Test
    fun testParseSurfaceFlingerInvalid() {
        val output = "Error with service 'SurfaceFlinger'"
        val frameCount = RealTimeFPSMonitor.parseSurfaceFlingerFrameCount(output)
        assertEquals(-1L, frameCount)
    }

    @Test
    fun testParseForegroundApp() {
        val output = "  mFocusedApp=ActivityRecord{927a528 u0 com.tencent.ig/.SplashActivity} t6}"
        val pkg = RealTimeFPSMonitor.parseForegroundApp(output)
        assertEquals("com.tencent.ig", pkg)
    }

    @Test
    fun testParseForegroundAppIgnoresSelf() {
        val output = "  mFocusedApp=ActivityRecord{bb6f0c u0 com.fpsmeter.app/.MainActivity} t175}"
        val pkg = RealTimeFPSMonitor.parseForegroundApp(output)
        assertNull(pkg)
    }

    @Test
    fun testParseGpuLoad() {
        assertEquals(13, RealTimeFPSMonitor.parseGpuLoad("13 %"))
        assertEquals(45, RealTimeFPSMonitor.parseGpuLoad("45"))
        assertNull(RealTimeFPSMonitor.parseGpuLoad("No such file or directory"))
    }
}
