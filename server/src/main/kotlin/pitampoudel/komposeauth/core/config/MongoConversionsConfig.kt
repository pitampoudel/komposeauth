package pitampoudel.komposeauth.core.config

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Date
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import org.springframework.data.mongodb.core.convert.MongoCustomConversions

@Configuration
class MongoConversionsConfig {

    @Bean
    fun mongoCustomConversions(): MongoCustomConversions {
        val converters = listOf(
            LocalDateToDateConverter,
            DateToLocalDateConverter,
            InstantToDateConverter,
            DateToInstantConverter
        )

        return MongoCustomConversions(converters)
    }
}

@WritingConverter
object LocalDateToDateConverter : Converter<LocalDate, Date> {
    override fun convert(source: LocalDate): Date =
        Date.from(source.atStartOfDay(ZoneOffset.UTC).toInstant())
}

@ReadingConverter
object DateToLocalDateConverter : Converter<Date, LocalDate> {
    override fun convert(source: Date): LocalDate =
        Instant.ofEpochMilli(source.time).atZone(ZoneOffset.UTC).toLocalDate()
}

@WritingConverter
object InstantToDateConverter : Converter<Instant, Date> {
    override fun convert(source: Instant): Date = Date.from(source)
}

@ReadingConverter
object DateToInstantConverter : Converter<Date, Instant> {
    override fun convert(source: Date): Instant = source.toInstant()
}
