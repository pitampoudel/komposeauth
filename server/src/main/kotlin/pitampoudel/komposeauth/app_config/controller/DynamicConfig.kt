package pitampoudel.komposeauth.app_config.controller

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties


fun <T : Any> buildFields(
    klass: KClass<T>,
    value: T,
    excludedFieldNames: Set<String>,
    inputTypeFor: (KProperty1<T, *>) -> String,
    optionsFor: (KProperty1<T, *>) -> List<ConfigField.SelectOption>?,
): List<ConfigField> {
    val declarationOrder = klass.java.declaredFields
        .mapIndexed { index, field -> field.name to index }
        .toMap()

    return klass.memberProperties
        .filter { it.name !in excludedFieldNames }
        .sortedBy { declarationOrder[it.name] ?: Int.MAX_VALUE }
        .map { property ->
            ConfigField(
                name = property.name,
                label = property.name,
                inputType = inputTypeFor(property),
                value = property.get(value),
                options = optionsFor(property).orEmpty()
            )
        }
}


data class ConfigField(
    val name: String,
    val label: String,
    val inputType: String,
    val value: Any?,
    val options: List<SelectOption> = emptyList()
) {
    data class SelectOption(
        val value: String,
        val label: String
    )
}

