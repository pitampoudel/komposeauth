package pitampoudel.komposeauth.app_config.controller

import jakarta.validation.constraints.Email
import org.hibernate.validator.constraints.URL
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.jvmErasure


data class Group(
    val title: String,
    val members: List<String>
)

fun <T : Any> buildFieldGroups(
    schema: KClass<T>,
    inputTypeFor: ((KProperty1<out Any, *>) -> String?)? = null,
    value: T? = null,
    excludedFieldNames: Set<String> = setOf(),
    preferredGroups: List<Group> = emptyList(),
    optionsFor: ((KProperty1<out Any, *>) -> List<ConfigFieldGroup.ConfigField.SelectOption>?)? = null,
): List<ConfigFieldGroup> {
    val fieldGroups = buildFieldGroups(
        objectClass = schema,
        objectValue = value,
        excludedFieldNames = excludedFieldNames,
        inputTypeFor = inputTypeFor,
        optionsFor = optionsFor
    )
    if (preferredGroups.isEmpty()) return fieldGroups

    // Track which field names have been placed into a user-defined group (globally)
    val usedFieldNames = mutableSetOf<String>()

    return fieldGroups.flatMap { classGroup ->
        val fieldByName = classGroup.fields.associateBy { it.name }

        // Apply user-defined groups within this class group
        val userDefinedGroups = preferredGroups.map { definition ->
            val matchedFields = definition.members.mapNotNull { fieldName ->
                fieldByName[fieldName]?.also { usedFieldNames += fieldName }
            }
            ConfigFieldGroup(title = definition.title, fields = matchedFields)
        }.toMutableList()

        // Fields in this class group not assigned to any user-defined group
        val remainingFields = classGroup.fields.filter { it.name !in usedFieldNames }
        if (remainingFields.isNotEmpty()) {
            // Preserve the original class group title for ungrouped remainder
            userDefinedGroups += ConfigFieldGroup(
                title = classGroup.title,
                fields = remainingFields
            )
        }
        userDefinedGroups
    }.filter { it.fields.isNotEmpty() }
}


private fun inferInputType(property: KProperty1<*, *>): String {
    return when {
        property.returnType.classifier == Int::class -> "number"
        property.javaField?.isAnnotationPresent(URL::class.java) == true -> "url"
        property.javaField?.isAnnotationPresent(Email::class.java) == true -> "email"
        else -> "text"
    }
}

private fun <T : Any> buildFieldGroups(
    objectClass: KClass<T>,
    objectValue: T?,
    excludedFieldNames: Set<String>,
    inputTypeFor: ((KProperty1<out Any, *>) -> String?)?,
    optionsFor: ((KProperty1<out Any, *>) -> List<ConfigFieldGroup.ConfigField.SelectOption>?)? = null,
): List<ConfigFieldGroup> {
    // Preserve declaration order of the top-level schema
    val declarationOrder = objectClass.java.declaredFields
        .mapIndexed { index, field -> field.name to index }
        .toMap()

    return objectClass.memberProperties
        .asSequence()
        .filter { it.name !in excludedFieldNames }
        .sortedBy { declarationOrder[it.name] ?: Int.MAX_VALUE }
        .map { property ->
            val nestedClass = property.returnType.jvmErasure

            if (nestedClass.isData) {
                // Nested data class: expand its members as dot-notated fields
                val nestedValue = objectValue?.let { property.getter.call(it) }
                val nestedDeclarationOrder = nestedClass.java.declaredFields
                    .mapIndexed { index, field -> field.name to index }
                    .toMap()

                ConfigFieldGroup(
                    title = property.name,
                    nestedClass.memberProperties
                        .filter { it.name !in excludedFieldNames }
                        .sortedBy { nestedDeclarationOrder[it.name] ?: Int.MAX_VALUE }
                        .map { member ->
                            ConfigFieldGroup.ConfigField(
                                name = "${property.name}.${member.name}",
                                label = "${property.name}.${member.name}",
                                inputType = inputTypeFor?.invoke(member) ?: inferInputType(member),
                                value = nestedValue?.let { member.getter.call(it) },
                                options = optionsFor?.invoke(member).orEmpty()
                            )
                        }
                )
            } else {
                // Primitive / non-data-class property: emit directly
                ConfigFieldGroup(
                    title = null,
                    listOf(
                        ConfigFieldGroup.ConfigField(
                            name = property.name,
                            label = property.name,
                            inputType = inputTypeFor?.invoke(property) ?: inferInputType(property),
                            value = objectValue?.let { property.getter.call(it) },
                            options = optionsFor?.invoke(property).orEmpty()
                        )
                    )
                )
            }
        }.groupBy { it.title }.map { entry ->
            ConfigFieldGroup(title = entry.key, fields = entry.value.flatMap { it.fields })
        }
        .toList()
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

