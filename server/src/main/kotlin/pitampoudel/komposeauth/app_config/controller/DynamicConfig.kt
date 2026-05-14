package pitampoudel.komposeauth.app_config.controller

import jakarta.validation.constraints.Email
import org.hibernate.validator.constraints.URL
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField


data class Group(
    val title: String,
    val members: List<String>
)

fun <T : Any> buildFieldGroups(
    schema: KClass<T>,
    inputTypeFor: ((KProperty1<T, *>) -> String?)? = null,
    value: T? = null,
    excludedFieldNames: Set<String> = setOf(),
    groups: List<Group> = emptyList(),
    optionsFor: (KProperty1<T, *>) -> List<ConfigFieldGroup.ConfigField.SelectOption>? = { null },
): List<ConfigFieldGroup> {
    val fields = buildFields(
        klass = schema,
        value = value,
        excludedFieldNames = excludedFieldNames,
        inputTypeFor = inputTypeFor,
        optionsFor = optionsFor
    )
    if (groups.isEmpty()) return listOf(ConfigFieldGroup(fields = fields))

    val fieldByName = fields.associateBy { it.name }
    val usedFieldNames = mutableSetOf<String>()

    val grouped = groups.map { definition ->
        val groupedFields = definition.members.mapNotNull { fieldName ->
            fieldByName[fieldName]?.also { usedFieldNames += fieldName }
        }
        ConfigFieldGroup(
            title = definition.title,
            fields = groupedFields
        )

    }.toMutableList()

    val remainingFields = fields.filter { it.name !in usedFieldNames }
    if (remainingFields.isNotEmpty()) {
        grouped += ConfigFieldGroup(
            title = "Other",
            fields = remainingFields
        )
    }
    return grouped
}


private fun <T : Any> buildFields(
    klass: KClass<T>,
    value: T?,
    excludedFieldNames: Set<String>,
    inputTypeFor: ((KProperty1<T, *>) -> String?)?,
    optionsFor: (KProperty1<T, *>) -> List<ConfigFieldGroup.ConfigField.SelectOption>?,
): List<ConfigFieldGroup.ConfigField> {
    val declarationOrder = klass.java.declaredFields
        .mapIndexed { index, field -> field.name to index }
        .toMap()

    return klass.memberProperties
        .filter { it.name !in excludedFieldNames }
        .sortedBy { declarationOrder[it.name] ?: Int.MAX_VALUE }
        .map { property ->
            ConfigFieldGroup.ConfigField(
                name = property.name,
                label = property.name,
                inputType = inputTypeFor?.invoke(property) ?: when {
                    property.returnType.classifier == Int::class -> "number"
                    property.javaField?.isAnnotationPresent(URL::class.java) == true -> "url"
                    property.javaField?.isAnnotationPresent(Email::class.java) == true -> "email"
                    else -> when (property.name) {
                        else -> "text"
                    }
                },
                value = value?.let { property.get(it) },
                options = optionsFor(property).orEmpty()
            )
        }
}

data class ConfigFieldGroup(
    val title: String? = null,
    val fields: List<ConfigField>
) {
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
}

