package pitampoudel.komposeauth.organization.repository

import pitampoudel.komposeauth.organization.entity.Organization
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface OrganizationRepository : MongoRepository<Organization, ObjectId> {
    fun findAllByUserIdsContains(userId: ObjectId): List<Organization>
    fun findAllByIdIn(ids: List<ObjectId>): List<Organization>
}

