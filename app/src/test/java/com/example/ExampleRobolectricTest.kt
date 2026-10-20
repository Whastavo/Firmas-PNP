package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.util.SignatureStampGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("FirmaTransfer", appName)
    }

    @Test
    fun `name capitalization and surname uppercase formatting`() {
        val surname = SignatureStampGenerator.formatUppercaseSurname("Andrade Salvador")
        val name = SignatureStampGenerator.formatCapitalizedName("brayan shamir")

        assertEquals("ANDRADE SALVADOR", surname)
        assertEquals("Brayan Shamir", name)
    }

    @Test
    fun `generate authentic Peru digital signature stamp bitmap`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val stamp = SignatureStampGenerator.generateAuthenticPeruStamp(
            context = context,
            apellidos = "ANDRADE SALVADOR",
            nombres = "Brayan Shamir",
            firId = "FIR 46725678 hard",
            signerReason = "Soy el autor del documento",
            dateString = "20/10/2026 09:30:15-0500",
            transparentBackground = true
        )
        assertNotNull(stamp)
        assertEquals(540, stamp.width)
        assertEquals(150, stamp.height)
    }

    @Test
    fun `remove white background turns white pixels to transparent`() {
        val whiteBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        whiteBitmap.eraseColor(Color.WHITE)

        val transparentResult = SignatureStampGenerator.removeWhiteBackground(whiteBitmap, threshold = 230)
        assertEquals(Color.TRANSPARENT, transparentResult.getPixel(5, 5))
    }

    @Test
    fun `patch date on cropped signature updates bitmap`() {
        val original = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888)
        original.eraseColor(Color.WHITE)

        val patched = SignatureStampGenerator.patchDateOnCroppedSignature(
            source = original,
            newDateString = "20/10/2026 12:00:00-0500",
            eraseWhiteBackground = false
        )
        assertNotNull(patched)
        assertEquals(200, patched.width)
        assertEquals(100, patched.height)
    }
}
