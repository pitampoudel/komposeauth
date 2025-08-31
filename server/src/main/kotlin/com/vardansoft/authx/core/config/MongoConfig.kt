package com.vardansoft.authx.core.config


import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.MongoDatabaseFactory
import org.springframework.data.mongodb.config.EnableMongoAuditing
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper
import org.springframework.data.mongodb.core.convert.MappingMongoConverter
import org.springframework.data.mongodb.core.convert.MongoCustomConversions
import org.springframework.data.mongodb.core.mapping.MongoMappingContext

@Configuration
@EnableMongoAuditing
class MongoConfig {
    @Bean
    fun mappingMongoConverter(
        factory: MongoDatabaseFactory,
        context: MongoMappingContext,
        conversions: MongoCustomConversions
    ): MappingMongoConverter {
        val converter = MappingMongoConverter(
            DefaultDbRefResolver(factory),
            context
        )
        converter.customConversions = conversions
        converter.setTypeMapper(DefaultMongoTypeMapper("_type", context))

        // Configure map key dot replacement to handle keys with dots (like java.security.Principal)
        converter.setMapKeyDotReplacement("_DOT_")

        return converter
    }
}
