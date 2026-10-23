package dev.vendora.android

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun appContextHasExpectedPackageName() {
        val appContext = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("dev.vendora.android", appContext.packageName)
    }
}
