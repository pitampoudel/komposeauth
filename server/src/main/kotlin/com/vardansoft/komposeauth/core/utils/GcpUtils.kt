package com.vardansoft.komposeauth.core.utils

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.ServiceOptions

object GcpUtils {
    /**
     * Returns the current GCP project ID resolved from the runtime environment.
     * This uses Application Default Credentials / metadata server.
     */
    fun currentProjectId(): String? = ServiceOptions.getDefaultProjectId()

    /**
     * Ensures that Application Default Credentials are available and optionally
     * verify that the current project matches the expected one.
     *
     * @param expectedProjectId when provided (non-blank), verifies it matches the resolved project ID
     * @throws IllegalStateException when ADC is not available or project mismatch occurs
     */
    fun assertAuthenticatedProject(expectedProjectId: String) {
        try {
            // Ensure ADC is available
            val credentials = GoogleCredentials.getApplicationDefault()
            if (credentials == null) {
                throw IllegalStateException("Google Application Default Credentials are not available.")
            }
        } catch (e: Exception) {
            throw IllegalStateException(
                "Failed to obtain Google Application Default Credentials: ${e.message}",
                e
            )
        }

        val resolved = currentProjectId()
        if (resolved.isNullOrBlank()) {
            throw IllegalStateException("Could not resolve GCP Project ID from the current environment.")
        }

        if (resolved != expectedProjectId) {
            throw IllegalStateException("GCP project mismatch. Expected '$expectedProjectId' but resolved '$resolved'.")
        }
    }
}