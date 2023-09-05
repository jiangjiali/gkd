package li.songe.gkd.data

import android.os.Parcelable
import blue.endless.jankson.Jankson
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import li.songe.gkd.util.Singleton
import li.songe.selector.Selector


@Parcelize
@Serializable
data class SubscriptionRaw(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String,
    @SerialName("version") val version: Int,
    @SerialName("author") val author: String? = null,
    @SerialName("updateUrl") val updateUrl: String? = null,
    @SerialName("supportUri") val supportUri: String? = null,
    @SerialName("apps") val apps: List<AppRaw> = emptyList(),
) : Parcelable {

    @Parcelize
    @Serializable
    data class NumberFilter(
        @SerialName("enum") val enum: List<Int>? = null,
        @SerialName("minimum") val minimum: Int? = null,
        @SerialName("maximum") val maximum: Int? = null,
    ) : Parcelable

    @Parcelize
    @Serializable
    data class StringFilter(
        @SerialName("enum") val enum: List<String>? = null,
        @SerialName("minLength") val minLength: Int? = null,
        @SerialName("maxLength") val maxLength: Int? = null,
        @SerialName("pattern") val pattern: String? = null,
    ) : Parcelable {

        @IgnoredOnParcel
        val patternRegex by lazy {
            if (pattern != null) try {
                Regex(pattern)
            } catch (e: Exception) {
                null
            } else null
        }
    }

    @Parcelize
    @Serializable
    data class AppFilter(
        @SerialName("name") val name: StringFilter? = null,
        @SerialName("versionName") val versionName: StringFilter? = null,
        @SerialName("versionCode") val versionCode: NumberFilter? = null,
    ) : Parcelable

    @Parcelize
    @Serializable
    data class DeviceFilter(
        @SerialName("device") val device: StringFilter? = null,
        @SerialName("model") val model: StringFilter? = null,
        @SerialName("manufacturer") val manufacturer: StringFilter? = null,
        @SerialName("brand") val brand: StringFilter? = null,
        @SerialName("sdkInt") val sdkInt: NumberFilter? = null,
        @SerialName("release") val release: StringFilter? = null,
    ) : Parcelable

    interface CommonProps {
        val activityIds: List<String>?
        val excludeActivityIds: List<String>?
        val cd: Long?
        val appFilter: AppFilter?
        val deviceFilter: DeviceFilter?
    }

    @Parcelize
    @Serializable
    data class AppRaw(
        @SerialName("id") val id: String,
        @SerialName("cd") override val cd: Long? = null,
        @SerialName("activityIds") override val activityIds: List<String>? = null,
        @SerialName("excludeActivityIds") override val excludeActivityIds: List<String>? = null,
        @SerialName("groups") val groups: List<GroupRaw> = emptyList(),
        @SerialName("appFilter") override val appFilter: AppFilter? = null,
        @SerialName("deviceFilter") override val deviceFilter: DeviceFilter? = null,
    ) : Parcelable, CommonProps

    @Parcelize
    @Serializable
    data class GroupRaw(
        @SerialName("name") val name: String? = null,
        @SerialName("desc") val desc: String? = null,
        @SerialName("enable") val enable: Boolean? = null,
        @SerialName("key") val key: Int,
        @SerialName("cd") override val cd: Long? = null,
        @SerialName("activityIds") override val activityIds: List<String>? = null,
        @SerialName("excludeActivityIds") override val excludeActivityIds: List<String>? = null,
        @SerialName("rules") val rules: List<RuleRaw> = emptyList(),
        override val appFilter: AppFilter? = null,
        override val deviceFilter: DeviceFilter? = null,
    ) : Parcelable, CommonProps {

        @IgnoredOnParcel
        val valid by lazy {
            rules.all { r ->
                r.matches.all { s -> Selector.check(s) } && r.excludeMatches.all { s ->
                    Selector.check(
                        s
                    )
                }
            }
        }
    }

    @Parcelize
    @Serializable
    data class RuleRaw(
        @SerialName("name") val name: String? = null,
        @SerialName("key") val key: Int? = null,
        @SerialName("preKeys") val preKeys: List<Int> = emptyList(),
        @SerialName("cd") override val cd: Long? = null,
        @SerialName("activityIds") override val activityIds: List<String>? = null,
        @SerialName("excludeActivityIds") override val excludeActivityIds: List<String>? = null,
        @SerialName("matches") val matches: List<String> = emptyList(),
        @SerialName("excludeMatches") val excludeMatches: List<String> = emptyList(),
        override val appFilter: AppFilter? = null,
        override val deviceFilter: DeviceFilter? = null,
    ) : Parcelable, CommonProps

    companion object {


        private fun getStringIArray(json: JsonObject? = null, name: String): List<String>? {
            return when (val element = json?.get(name)) {
                JsonNull, null -> null
                is JsonObject -> error("Element ${this::class} can not be object")
                is JsonArray -> element.map {
                    when (it) {
                        is JsonObject, is JsonArray, JsonNull -> error("Element ${this::class} is not a int")
                        is JsonPrimitive -> it.content
                    }
                }

                is JsonPrimitive -> listOf(element.content)
            }
        }

        @Suppress("SameParameterValue")
        private fun getIntIArray(json: JsonObject? = null, name: String): List<Int>? {
            return when (val element = json?.get(name)) {
                JsonNull, null -> null
                is JsonArray -> element.map {
                    when (it) {
                        is JsonObject, is JsonArray, JsonNull -> error("Element $it is not a int")
                        is JsonPrimitive -> it.int
                    }
                }

                is JsonPrimitive -> listOf(element.int)
                else -> error("Element $element is not a Array")
            }
        }

        private fun getString(json: JsonObject? = null, key: String): String? =
            when (val p = json?.get(key)) {
                JsonNull, null -> null
                is JsonPrimitive -> {
                    if (p.isString) {
                        p.content
                    } else {
                        error("Element $p is not a string")
                    }
                }

                else -> error("Element $p is not a string")
            }

        @Suppress("SameParameterValue")
        private fun getLong(json: JsonObject? = null, key: String): Long? =
            when (val p = json?.get(key)) {
                JsonNull, null -> null
                is JsonPrimitive -> {
                    p.long
                }

                else -> error("Element $p is not a long")
            }

        private fun getInt(json: JsonObject? = null, key: String): Int? =
            when (val p = json?.get(key)) {
                JsonNull, null -> null
                is JsonPrimitive -> {
                    p.int
                }

                else -> error("Element $p is not a int")
            }

        @Suppress("SameParameterValue")
        private fun getBoolean(json: JsonObject? = null, key: String): Boolean? =
            when (val p = json?.get(key)) {
                JsonNull, null -> null
                is JsonPrimitive -> {
                    p.boolean
                }

                else -> error("Element $p is not a boolean")
            }

        private fun jsonToRuleRaw(rulesRawJson: JsonElement): RuleRaw {
            val rulesJson = when (rulesRawJson) {
                JsonNull -> error("miss current rule")
                is JsonObject -> rulesRawJson
                is JsonPrimitive, is JsonArray -> JsonObject(mapOf("matches" to rulesRawJson))
            }
            return RuleRaw(
                activityIds = getStringIArray(rulesJson, "activityIds"),
                excludeActivityIds = getStringIArray(rulesJson, "excludeActivityIds"),
                cd = getLong(rulesJson, "cd"),
                matches = (getStringIArray(
                    rulesJson, "matches"
                ) ?: emptyList()),
                excludeMatches = (getStringIArray(
                    rulesJson, "excludeMatches"
                ) ?: emptyList()),
                key = getInt(rulesJson, "key"),
                name = getString(rulesJson, "name"),
                preKeys = getIntIArray(rulesJson, "preKeys") ?: emptyList(),
                deviceFilter = rulesJson["deviceFilter"]?.let {
                    Singleton.json.decodeFromJsonElement(it)
                },
                appFilter = rulesJson["appFilter"]?.let {
                    Singleton.json.decodeFromJsonElement(it)
                },
            )
        }


        private fun jsonToGroupRaw(groupIndex: Int, groupsRawJson: JsonElement): GroupRaw {
            val groupsJson = when (groupsRawJson) {
                JsonNull -> error("")
                is JsonObject -> groupsRawJson
                is JsonPrimitive, is JsonArray -> JsonObject(mapOf("rules" to groupsRawJson))
            }
            return GroupRaw(
                activityIds = getStringIArray(groupsJson, "activityIds"),
                excludeActivityIds = getStringIArray(groupsJson, "excludeActivityIds"),
                cd = getLong(groupsJson, "cd"),
                name = getString(groupsJson, "name"),
                desc = getString(groupsJson, "desc"),
                enable = getBoolean(groupsJson, "enable"),
                key = getInt(groupsJson, "key") ?: groupIndex,
                rules = when (val rulesJson = groupsJson["rules"]) {
                    null, JsonNull -> emptyList()
                    is JsonPrimitive, is JsonObject -> JsonArray(listOf(rulesJson))
                    is JsonArray -> rulesJson
                }.map {
                    jsonToRuleRaw(it)
                },
                deviceFilter = groupsJson["deviceFilter"]?.let {
                    Singleton.json.decodeFromJsonElement(it)
                },
                appFilter = groupsJson["appFilter"]?.let {
                    Singleton.json.decodeFromJsonElement(it)
                },
            )
        }

        private fun jsonToAppRaw(appsJson: JsonObject, appIndex: Int): AppRaw {
            return AppRaw(
                activityIds = getStringIArray(appsJson, "activityIds"),
                excludeActivityIds = getStringIArray(appsJson, "excludeActivityIds"),
                cd = getLong(appsJson, "cd"),
                id = getString(appsJson, "id") ?: error("miss subscription.apps[$appIndex].id"),
                groups = (when (val groupsJson = appsJson["groups"]) {
                    null, JsonNull -> emptyList()
                    is JsonPrimitive, is JsonObject -> JsonArray(listOf(groupsJson))
                    is JsonArray -> groupsJson
                }).mapIndexed { index, jsonElement ->
                    jsonToGroupRaw(index, jsonElement)
                },
                deviceFilter = appsJson["deviceFilter"]?.let {
                    Singleton.json.decodeFromJsonElement(it)
                },
                appFilter = appsJson["appFilter"]?.let {
                    Singleton.json.decodeFromJsonElement(it)
                },
            )
        }

        private fun jsonToSubscriptionRaw(rootJson: JsonObject): SubscriptionRaw {
            return SubscriptionRaw(id = getLong(rootJson, "id") ?: error("miss subscription.id"),
                name = getString(rootJson, "name") ?: error("miss subscription.name"),
                version = getInt(rootJson, "version") ?: error("miss subscription.version"),
                author = getString(rootJson, "author"),
                updateUrl = getString(rootJson, "updateUrl"),
                supportUri = getString(rootJson, "supportUrl"),
                apps = rootJson["apps"]?.jsonArray?.mapIndexed { index, jsonElement ->
                    jsonToAppRaw(
                        jsonElement.jsonObject, index
                    )
                } ?: emptyList())
        }

//        订阅文件状态: 文件不存在, 文件正常, 文件损坏(损坏原因)
        fun stringify(source: SubscriptionRaw) = Singleton.json.encodeToString(source)

        fun parse(source: String): SubscriptionRaw {
            return jsonToSubscriptionRaw(Singleton.json.parseToJsonElement(source).jsonObject)

//            val duplicatedApps = obj.apps.groupingBy { it }.eachCount().filter { it.value > 1 }.keys
//            if (duplicatedApps.isNotEmpty()) {
//                error("duplicated app: ${duplicatedApps.map { it.id }}")
//            }
//            obj.apps.forEach { appRaw ->
//                val duplicatedGroups =
//                    appRaw.groups.groupingBy { it }.eachCount().filter { it.value > 1 }.keys
//                if (duplicatedGroups.isNotEmpty()) {
//                    error("app:${appRaw.id}, duplicated group: ${duplicatedGroups.map { it.key }}")
//                }
//                appRaw.groups.forEach { groupRaw ->
//                    val duplicatedRules =
//                        groupRaw.rules.mapNotNull { r -> r.key }.groupingBy { it }.eachCount()
//                            .filter { it.value > 1 }.keys
//                    if (duplicatedRules.isNotEmpty()) {
//                        error("app:${appRaw.id}, group:${groupRaw.key},  duplicated rule: $duplicatedRules")
//                    }
//                }
//            }
        }

        fun parse5(source: String): SubscriptionRaw {
            return parse(
                Jankson.builder().build().load(source).toJson()
            )
        }
    }

}









