package pitampoudel.komposeauth.core.utils

import com.google.cloud.ServiceOptions

object GcpUtils {
    /**
     * Returns the current GCP project ID resolved from the runtime environment.
     */
    fun currentProjectId(): String? = ServiceOptions.getDefaultProjectId()

    /**
     * Ensures that the current project matches the expected one.
     *
     * @param expectedProjectId verifies it matches the resolved project ID
     * @throws IllegalStateException when project mismatch occurs
     */
    fun assertAuthenticatedProject(expectedProjectId: String?) {
        val resolved = currentProjectId()
        if (resolved != expectedProjectId) {
            throw IllegalStateException("GCP project mismatch. Expected '$expectedProjectId' but resolved '$resolved'.")
        }
    }
}