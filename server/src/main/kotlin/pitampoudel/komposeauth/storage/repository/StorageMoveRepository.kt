package pitampoudel.komposeauth.storage.repository

import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import pitampoudel.komposeauth.storage.entity.StorageMove

interface StorageMoveRepository : MongoRepository<StorageMove, ObjectId> {
    fun findFirstByActiveTrue(): StorageMove?
    fun findFirstByOrderByStartedAtDesc(): StorageMove?
}
