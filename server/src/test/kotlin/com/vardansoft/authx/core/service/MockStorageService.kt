package com.vardansoft.authx.core.service

import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

/**
 * Mock implementation of GcpStorageService for testing.
 * Uses in-memory storage instead of connecting to GCP.
 */
@Service
@Primary
@Profile("test")
class MockStorageService() : StorageService {
    // In-memory storage map: blobName -> Pair(contentType, bytes)
    private val storage = mutableMapOf<String, Pair<String?, ByteArray>>()

    override fun upload(blobName: String, contentType: String?, bytes: ByteArray): String {
        storage[blobName] = Pair(contentType, bytes)
        return "http://mock-storage/$blobName"
    }

    override fun download(blobName: String): ByteArray? {
        return storage[blobName]?.second
    }

    override fun exists(blobName: String): Boolean {
        return storage.containsKey(blobName)
    }

    override fun delete(url: String): Boolean {
        // For mock storage URLs, extract the blob name from the URL path
        val blobName = url.removePrefix("http://mock-storage/")

        // Return true if the file was removed or if it didn't exist in the first place
        // This matches the behavior of the real GcpStorageService
        return storage.remove(blobName) != null || !exists(blobName)
    }

    // Helper method to clear all stored files (useful for test cleanup)
    fun clearAll() {
        storage.clear()
    }
}
