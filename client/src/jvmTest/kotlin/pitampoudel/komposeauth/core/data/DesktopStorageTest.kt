package pitampoudel.komposeauth.core.data

import java.util.prefs.Preferences
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DesktopStorageTest {
    private val apps = listOf("komposeauth-test-a", "komposeauth-test-b")

    @AfterTest
    fun removeNodes() {
        val root = Preferences.userRoot().node("komposeauth")
        apps.filter { root.nodeExists(it) }.forEach { root.node(it).removeNode() }
    }

    @Test
    fun `each app keeps its own sign-in`() {
        desktopSettings(apps[0]).putString("token_data", "a")

        assertNull(desktopSettings(apps[1]).getStringOrNull("token_data"))
        assertEquals("a", desktopSettings(apps[0]).getStringOrNull("token_data"))
        assertEquals("a", Preferences.userRoot().node("komposeauth/${apps[0]}").get("token_data", null))
    }
}
