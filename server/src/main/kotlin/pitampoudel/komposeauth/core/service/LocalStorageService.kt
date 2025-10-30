package pitampoudel.komposeauth.core.service

import pitampoudel.komposeauth.AppProperties
import java.net.URI
import java.net.URLDecoder
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * A simple local-disk based StorageService implementation used when cloud storage
 * is not configured. Files are stored under user.home/<app.name>/storage.
 */
class LocalStorageService(
    private val appProperties: AppProperties
) : StorageService {

    private val root: Path by lazy {
        val dir = Path.of(System.getProperty("user.home"), appProperties.name, "storage")
        Files.createDirectories(dir)
        dir
    }

    override fun upload(blobName: String, contentType: String?, bytes: ByteArray): String {
        val target = root.resolve(blobName)
        Files.createDirectories(target.parent)
        Files.write(
            target,
            bytes,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE
        )
        return target.toUri().toString()
    }

    override fun download(blobName: String): ByteArray? {
        val target = root.resolve(blobName)
        return if (Files.exists(target)) Files.readAllBytes(target) else null
    }

    override fun exists(blobName: String): Boolean {
        return Files.exists(root.resolve(blobName))
    }

    override fun delete(url: String): Boolean {
        // Accept both file:// URLs and raw paths
        val path: Path = try {
            val uri = URI(url)
            if (uri.scheme != null) Path.of(uri) else Path.of(URLDecoder.decode(url, "UTF-8"))
        } catch (_: Exception) {
            Path.of(URLDecoder.decode(url, "UTF-8"))
        }
        return try {
            Files.deleteIfExists(path)
        } catch (_: Exception) {
            false
        }
    }
}