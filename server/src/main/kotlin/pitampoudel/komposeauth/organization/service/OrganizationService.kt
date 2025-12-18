package pitampoudel.komposeauth.organization.service

import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import pitampoudel.komposeauth.organization.entity.Organization
import pitampoudel.komposeauth.organization.repository.OrganizationRepository
import kotlin.jvm.optionals.getOrNull


@Service
class OrganizationService(private val repository: OrganizationRepository) {

    suspend fun findByIds(objectIds: List<String>): Map<String, Organization> {
        val ids = objectIds.mapNotNull { runCatching { ObjectId(it) }.getOrNull() }
        if (ids.isEmpty()) return emptyMap()
        val orgs = repository.findAllByIdIn(ids)
        return orgs.associateBy { it.id.toHexString() }
    }

    suspend fun findOrgs(ids: List<String>): List<Organization> {
        return repository.findAllByIdIn(ids.map { ObjectId(it) })
    }

    suspend fun findOrgsForUser(userId: ObjectId): List<Organization> {
        return repository.findAllByUserIdsContains(userId)
    }

    suspend fun findById(id: String): Organization? {
        return runCatching { ObjectId(id) }.mapCatching { repository.findById(it).getOrNull() }
            .getOrNull()
    }

    suspend fun delete(id: ObjectId) {
        repository.deleteById(id)
    }

    suspend fun save(organization: Organization): Organization {
        return repository.save(organization)
    }
}