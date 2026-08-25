package pitampoudel.komposeauth.core.config

import com.mongodb.connection.ClusterSettings
import com.mongodb.connection.ConnectionPoolSettings
import com.mongodb.connection.SocketSettings
import org.springframework.boot.mongodb.autoconfigure.MongoClientSettingsBuilderCustomizer
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration
import java.util.concurrent.TimeUnit


@ConfigurationProperties(prefix = "app.mongo")
data class MongoConnectionProperties(
    /**
     * Sockets kept open even while idle.
     *
     * Opening one costs a TCP round trip plus a TLS handshake plus SCRAM authentication — against a
     * managed cluster in another region that is most of a request's budget. Holding a couple open
     * means the pool is warm by the time the first user arrives, because the startup path already
     * reads the app config and so forces the pool to fill while the platform's startup CPU boost is
     * still in effect.
     */
    val minPoolSize: Int = 2,
    /**
     * Ceiling on concurrently open sockets. Sized for one container, not one fleet: with graceful
     * shutdown bounded to seconds, a pool larger than the request concurrency only adds sockets the
     * cluster has to track and tear down.
     */
    val maxPoolSize: Int = 20,
    /**
     * How long a request waits for a free socket before failing rather than queueing behind the
     * whole pool.
     */
    val maxWaitTime: Duration = Duration.ofSeconds(5),
    /**
     * Idle sockets are discarded at this age. Kept under the ten minutes managed clusters and the
     * intervening NAT commonly cut idle connections at, so the driver retires its own connections
     * knowingly instead of handing a request one that died quietly and making it pay a failed
     * round trip and a retry to find out.
     */
    val maxConnectionIdleTime: Duration = Duration.ofMinutes(5),
    /**
     * How long to look for a usable cluster member. The default thirty seconds outlives most
     * front-end timeouts, which turns an unreachable database into a hung request; five seconds
     * still absorbs an ordinary replica-set election.
     */
    val serverSelectionTimeout: Duration = Duration.ofSeconds(5),
    /**
     * TCP connect timeout for a single socket.
     *
     * There is deliberately no matching read timeout. A socket-level one applies to every command
     * alike, including the `createIndex` calls [pitampoudel.komposeauth.core.migration
     * .MongoIndexMigration] issues, which on a collection large enough to matter can legitimately
     * run for minutes — cutting those off would fail startup on exactly the databases that most need
     * the index. Bounding a slow *query* is a per-operation concern (`maxTimeMS`), not a property of
     * the connection.
     */
    val connectTimeout: Duration = Duration.ofSeconds(5),
)

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(MongoConnectionProperties::class)
class MongoClientTuning {

    /**
     * Applied on top of whatever the connection string says, so a URI that spells out its own
     * `maxPoolSize` or `serverSelectionTimeoutMS` is not silently overridden — Spring Boot applies
     * the URI first and these customizers afterwards, so anything set here wins deliberately and
     * anything left at a driver default is what these values replace.
     */
    @Bean
    fun mongoConnectionCustomizer(
        properties: MongoConnectionProperties
    ): MongoClientSettingsBuilderCustomizer = MongoClientSettingsBuilderCustomizer { builder ->
        builder
            .applyToConnectionPoolSettings { pool: ConnectionPoolSettings.Builder ->
                pool.minSize(properties.minPoolSize)
                    .maxSize(properties.maxPoolSize)
                    .maxWaitTime(properties.maxWaitTime.toMillis(), TimeUnit.MILLISECONDS)
                    .maxConnectionIdleTime(
                        properties.maxConnectionIdleTime.toMillis(),
                        TimeUnit.MILLISECONDS
                    )
            }
            .applyToClusterSettings { cluster: ClusterSettings.Builder ->
                cluster.serverSelectionTimeout(
                    properties.serverSelectionTimeout.toMillis(),
                    TimeUnit.MILLISECONDS
                )
            }
            .applyToSocketSettings { socket: SocketSettings.Builder ->
                socket.connectTimeout(
                    properties.connectTimeout.toMillis(),
                    TimeUnit.MILLISECONDS
                )
            }
    }
}
