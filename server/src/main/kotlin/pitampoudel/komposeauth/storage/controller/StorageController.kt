package pitampoudel.komposeauth.storage.controller

import io.swagger.v3.oas.annotations.Operation
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import pitampoudel.komposeauth.storage.dto.BucketResponse
import pitampoudel.komposeauth.storage.dto.CreateBucketRequest
import pitampoudel.komposeauth.storage.dto.EditBucketRequest
import pitampoudel.komposeauth.storage.dto.ProbeResponse
import pitampoudel.komposeauth.storage.dto.ReadFromRequest
import pitampoudel.komposeauth.storage.dto.StorageStateResponse
import pitampoudel.komposeauth.storage.dto.bucketResponse
import pitampoudel.komposeauth.storage.service.StorageBucketService
import pitampoudel.komposeauth.storage.service.StorageBuckets

/**
 * What the admin console's Storage page reads and does.
 *
 * Both admin tiers can see where files are kept. Every write is a SUPER_ADMIN's: a bucket holds a
 * cloud credential, and which bucket takes new files or answers a copied bucket's addresses decides
 * where every file is written and read.
 */
@RestController
@RequestMapping("/admin/storage")
@PreAuthorize("hasRole('ADMIN')")
class StorageController(
    private val buckets: StorageBuckets,
    private val bucketService: StorageBucketService
) {

    @Operation(summary = "List the storage buckets")
    @GetMapping("/state")
    fun state(authentication: Authentication): StorageStateResponse {
        buckets.forget()
        val rows = buckets.all().map { it.row }
        val problem = when {
            rows.isEmpty() -> "No bucket is listed, so nothing can be uploaded. Add one to keep files in."
            rows.none { it.primary } -> "No bucket takes new files, so uploads fail. Choose one to take them."
            else -> null
        }
        return StorageStateResponse(
            canManage = authentication.authorities.any { it.authority == "ROLE_SUPER_ADMIN" },
            buckets = rows.map { bucketResponse(it, rows) },
            problem = problem
        )
    }

    @Operation(summary = "Add a storage bucket", description = "Requires SUPER_ADMIN.")
    @PostMapping("/buckets")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun create(@RequestBody request: CreateBucketRequest): BucketResponse = respond(bucketService.create(request).id)

    @Operation(
        summary = "Edit a storage bucket's name, project or credentials",
        description = "The bucket and how its addresses are written can't change. Requires SUPER_ADMIN."
    )
    @PatchMapping("/buckets/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun edit(@PathVariable id: String, @RequestBody request: EditBucketRequest): BucketResponse =
        respond(bucketService.edit(objectId(id), request).id)

    @Operation(summary = "Write, read and delete a test file in a bucket", description = "Requires SUPER_ADMIN.")
    @PostMapping("/buckets/{id}/test")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun test(@PathVariable id: String): ProbeResponse = bucketService.test(objectId(id))

    @Operation(summary = "Send new files to this bucket", description = "Requires SUPER_ADMIN.")
    @PostMapping("/buckets/{id}/primary")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun makePrimary(@PathVariable id: String, authentication: Authentication): StorageStateResponse {
        bucketService.makePrimary(objectId(id))
        return state(authentication)
    }

    @Operation(
        summary = "Open this bucket's files from the bucket an admin copied them to",
        description = "Nothing is copied or checked. Requires SUPER_ADMIN."
    )
    @PutMapping("/buckets/{id}/read-from")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun readFrom(@PathVariable id: String, @RequestBody request: ReadFromRequest, authentication: Authentication): StorageStateResponse {
        bucketService.readFrom(objectId(id), objectId(request.bucketId))
        return state(authentication)
    }

    @Operation(summary = "Open this bucket's files from its own bucket again", description = "Requires SUPER_ADMIN.")
    @DeleteMapping("/buckets/{id}/read-from")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun readOwnFiles(@PathVariable id: String, authentication: Authentication): StorageStateResponse {
        bucketService.readOwnFiles(objectId(id))
        return state(authentication)
    }

    @Operation(
        summary = "Remove a bucket from the list",
        description = "Removes the entry only, never the bucket or its files. Requires SUPER_ADMIN."
    )
    @DeleteMapping("/buckets/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun remove(@PathVariable id: String, authentication: Authentication): StorageStateResponse {
        bucketService.remove(objectId(id))
        return state(authentication)
    }

    private fun respond(id: ObjectId): BucketResponse {
        val rows = buckets.all().map { it.row }
        return bucketResponse(rows.first { it.id == id }, rows)
    }

    private fun objectId(id: String): ObjectId =
        if (ObjectId.isValid(id)) ObjectId(id) else throw ResponseStatusException(HttpStatus.NOT_FOUND, "There is no such bucket")
}
