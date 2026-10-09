package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric checks for the assets the design system depends on. If a bundled font
 * or colour is renamed or dropped, the app still compiles but silently falls back to
 * system type — so assert the packaged resources actually exist.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BundledAssetsTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private fun assertResourceExists(type: String, name: String) {
        val id = context.resources.getIdentifier(name, type, context.packageName)
        assertNotEquals("$type/$name is missing from the APK", 0, id)
    }

    @Test
    fun `bundled variable fonts are packaged`() {
        // Offline-first typography: both families must ship inside the APK.
        assertResourceExists("font", "inter_variable")
        assertResourceExists("font", "manrope_variable")
    }

    @Test
    fun `theme colours are packaged`() {
        assertResourceExists("color", "pos_window_background")
    }

    @Test
    fun `app name is present`() {
        assertEquals("SajiloPOS", context.getString(R.string.app_name))
    }

    @Test
    fun `font resource ids resolve through R`() {
        assertTrue(R.font.inter_variable != 0)
        assertTrue(R.font.manrope_variable != 0)
    }
}