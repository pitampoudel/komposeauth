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
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import pitampoudel.komposeauth.core.config.UserContextService
import pitampoudel.komposeauth.storage.dto.CreateStoreRequest
import pitampoudel.komposeauth.storage.dto.EditStoreRequest
import pitampoudel.komposeauth.storage.dto.MoveResponse
import pitampoudel.komposeauth.storage.dto.ProbeResponse
import pitampoudel.komposeauth.storage.dto.StartMoveRequest
import pitampoudel.komposeauth.storage.dto.StorageStateResponse
import pitampoudel.komposeauth.storage.dto.StoreResponse
import pitampoudel.komposeauth.storage.dto.storeResponse
import pitampoudel.komposeauth.storage.dto.toResponse
import pitampoudel.komposeauth.storage.service.StorageMoveService
import pitampoudel.komposeauth.storage.service.StorageStoreService
import pitampoudel.komposeauth.storage.service.StorageStores

/**
 * What the admin console's Storage page reads and does.
 *
 * Both admin tiers can see where files are kept and how a move is going. Every write is a
 * SUPER_ADMIN's, stepping a move included: a store holds a cloud credential, and pointing new files
 * at the wrong bucket or rewriting every saved address is not something to undo by hand.
 */
@RestController
@RequestMapping("/admin/storage")
@PreAuthorize("hasRole('ADMIN')")
class StorageController(
    private val stores: StorageStores,
    private val storeService: StorageStoreService,
    private val moveService: StorageMoveService,
    private val userContextService: UserContextService
) {

    @Operation(summary = "List the storage stores and the current move")
    @GetMapping("/state")
    fun state(authentication: Authentication): StorageStateResponse {
        stores.forget()
        val rows = stores.all().map { it.row }
        val problem = when {
            rows.isEmpty() -> "No bucket is listed, so nothing can be uploaded. Add one to keep files in."
            rows.none { it.primary } -> "No store takes new files, so uploads fail. Choose one to take them."
            else -> null
        }
        return StorageStateResponse(
            canManage = authentication.authorities.any { it.authority == "ROLE_SUPER_ADMIN" },
            stores = rows.map { storeResponse(it, rows) },
            move = moveService.current()?.toResponse(),
            problem = problem
        )
    }

    @Operation(summary = "Add a storage store", description = "Requires SUPER_ADMIN.")
    @PostMapping("/stores")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun create(@RequestBody request: CreateStoreRequest): StoreResponse = respond(storeService.create(request).id)

    @Operation(
        summary = "Edit a storage store's name, project or credentials",
        description = "The bucket and how its addresses are written can't change. Requires SUPER_ADMIN."
    )
    @PatchMapping("/stores/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun edit(@PathVariable id: String, @RequestBody request: EditStoreRequest): StoreResponse =
        respond(storeService.edit(objectId(id), request).id)

    @Operation(summary = "Write, read, list and delete a test file in a store", description = "Requires SUPER_ADMIN.")
    @PostMapping("/stores/{id}/test")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun test(@PathVariable id: String): ProbeResponse = storeService.test(objectId(id))

    @Operation(summary = "Send new files to this store", description = "Requires SUPER_ADMIN.")
    @PostMapping("/stores/{id}/primary")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun makePrimary(@PathVariable id: String, authentication: Authentication): StorageStateResponse {
        storeService.makePrimary(objectId(id))
        return state(authentication)
    }

    @Operation(
        summary = "Remove a store from the list",
        description = "Removes the entry only, never the bucket or its files. Requires SUPER_ADMIN."
    )
    @DeleteMapping("/stores/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun remove(@PathVariable id: String, authentication: Authentication): StorageStateResponse {
        storeService.remove(objectId(id))
        return state(authentication)
    }

    @Operation(summary = "Start moving every file of one store into another", description = "Requires SUPER_ADMIN.")
    @PostMapping("/moves")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun startMove(@RequestBody request: StartMoveRequest): MoveResponse {
        val startedBy = userContextService.authenticatedUserOrNull()?.let {
            listOfNotNull(it.firstName, it.lastName).joinToString(" ").takeIf(String::isNotBlank) ?: it.email
        }
        return moveService.start(objectId(request.fromStoreId), objectId(request.toStoreId), startedBy).toResponse()
    }

    @Operation(
        summary = "Advance a move by one bounded slice",
        description = "The Storage page calls this every second or two while a move is under way. Requires SUPER_ADMIN."
    )
    @PostMapping("/moves/{id}/step")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun step(@PathVariable id: String): MoveResponse = moveService.step(objectId(id)).toResponse()

    @Operation(summary = "Cancel a move", description = "Requires SUPER_ADMIN.")
    @PostMapping("/moves/{id}/cancel")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun cancel(@PathVariable id: String): MoveResponse = moveService.cancel(objectId(id)).toResponse()

    private fun respond(id: ObjectId): StoreResponse {
        val rows = stores.all().map { it.row }
        return storeResponse(rows.first { it.id == id }, rows)
    }

    private fun objectId(id: String): ObjectId =
        if (ObjectId.isValid(id)) ObjectId(id) else throw ResponseStatusException(HttpStatus.NOT_FOUND, "There is no such store or move")
}
