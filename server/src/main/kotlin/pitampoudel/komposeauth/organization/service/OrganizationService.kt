package pitampoudel.komposeauth.organization.service

import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import pitampoudel.komposeauth.organization.entity.Organization
import pitampoudel.komposeauth.organization.repository.OrganizationRepository
import kotlin.jvm.optionals.getOrNull


@Service
class OrganizationService(private val repository: OrganizationRepository) {

    fun findOrgs(ids: List<String>): List<Organization> {
        return repository.findAllByIdIn(ids.map { ObjectId(it) })
    }

    fun findOrgsForUser(userId: ObjectId): List<Organization> {
        return repository.findAllByUserIdsContains(userId)
    }

    fun findById(id: String): Organization? {
        return runCatching { ObjectId(id) }.mapCatching { repository.findById(it).getOrNull() }
            .getOrNull()
    }

    fun delete(id: ObjectId) {
        repository.deleteById(id)
    }

    fun save(organization: Organization): Organization {
        return repository.save(organization)
    }
}