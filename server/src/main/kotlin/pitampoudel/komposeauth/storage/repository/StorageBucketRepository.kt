package pitampoudel.komposeauth.storage.repository

import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import pitampoudel.komposeauth.storage.entity.StorageBucket

interface StorageBucketRepository : MongoRepository<StorageBucket, ObjectId>
