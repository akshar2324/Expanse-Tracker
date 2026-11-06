package com.akshar.data.activity

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * JSON form of [ActivityFilter], used for saved filters on disk and for the SavedStateHandle.
 * Decoding is lenient: unknown enum names and missing keys fall back to defaults, so a filter
 * saved by an older or newer build still loads.
 */
object ActivityFilterCodec {

    fun encode(filter: ActivityFilter): JSONObject = JSONObject().apply {
        put("query", filter.query)
        put("type", filter.type.name)
        put("categories", JSONArray(filter.categories.sorted()))
        filter.accountId?.let { put("accountId", it) }
        filter.paymentMethod?.let { put("paymentMethod", it) }
        put("datePreset", filter.datePreset.name)
        filter.customStartDay?.let { put("customStartDay", it) }
        filter.customEndDay?.let { put("customEndDay", it) }
        put("minAmount", filter.minAmountText)
        put("maxAmount", filter.maxAmountText)
        put("sort", filter.sort.name)
    }

    fun decode(json: JSONObject): ActivityFilter {
        val categories = json.optJSONArray("categories")?.let { array ->
            (0 until array.length()).mapNotNull { array.optString(it).takeIf(String::isNotEmpty) }.toSet()
        } ?: emptySet()
        return ActivityFilter(
            query = json.optString("query"),
            type = enumOr(json.optString("type"), ActivityType.ALL),
            categories = categories,
            accountId = if (json.has("accountId")) json.optLong("accountId") else null,
            paymentMethod = json.optString("paymentMethod").takeIf { json.has("paymentMethod") && it.isNotEmpty() },
            datePreset = enumOr(json.optString("datePreset"), DatePreset.ALL_TIME),
            customStartDay = if (json.has("customStartDay")) json.optInt("customStartDay") else null,
            customEndDay = if (json.has("customEndDay")) json.optInt("customEndDay") else null,
            minAmountText = json.optString("minAmount"),
            maxAmountText = json.optString("maxAmount"),
            sort = enumOr(json.optString("sort"), ActivitySort.NEWEST),
        )
    }

    fun encodeFilter(filter: ActivityFilter): String = encode(filter).toString()

    fun decodeFilter(text: String?): ActivityFilter? {
        if (text.isNullOrBlank()) return null
        return try { decode(JSONObject(text)) } catch (_: JSONException) { null }
    }

    fun encodeSaved(saved: List<SavedActivityFilter>): String = JSONArray().apply {
        saved.forEach { put(JSONObject().put("name", it.name).put("filter", encode(it.filter))) }
    }.toString()

    /** Corrupt entries are skipped rather than failing the whole list. */
    fun decodeSaved(text: String?): List<SavedActivityFilter> {
        if (text.isNullOrBlank()) return emptyList()
        val array = try { JSONArray(text) } catch (_: JSONException) { return emptyList() }
        return (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val name = item.optString("name").trim()
            val filter = item.optJSONObject("filter") ?: return@mapNotNull null
            if (name.isEmpty()) null else SavedActivityFilter(name, decode(filter))
        }
    }

    private inline fun <reified E : Enum<E>> enumOr(name: String, fallback: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: fallback
}
