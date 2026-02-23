package org.ukky.suggestfulltext.repository

import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository
import org.ukky.suggestfulltext.model.StationSearchResult

@Repository
class StationSearchRepository(
    private val entityManager: EntityManager,
) {
    fun search(keyword: String, limit: Int): List<StationSearchResult> {
        val query = entityManager.createNativeQuery(KEYWORD_SEARCH_SQL)
        bindCommonParams(query, keyword)
        query.maxResults = limit.coerceIn(1, 200)
        @Suppress("UNCHECKED_CAST")
        val rows = query.resultList as List<Array<Any?>>
        return rows.map { mapRow(it, false) }
    }

    fun searchWithGeo(keyword: String, lat: Double, lon: Double, limit: Int): List<StationSearchResult> {
        val query = entityManager.createNativeQuery(KEYWORD_SEARCH_GEO_SQL)
        bindCommonParams(query, keyword)
        query.setParameter("lat", lat)
        query.setParameter("lon", lon)
        query.maxResults = limit.coerceIn(1, 200)
        @Suppress("UNCHECKED_CAST")
        val rows = query.resultList as List<Array<Any?>>
        return rows.map { mapRow(it, true) }
    }

    private fun bindCommonParams(query: jakarta.persistence.Query, keyword: String) {
        val escapedLike = escapeLike(keyword)
        query.setParameter("ftsQuery", ftsQuery(keyword))
        query.setParameter("kw", keyword)
        query.setParameter("kwPrefix", "$escapedLike%")
        query.setParameter("kwContains", "%$escapedLike%")
    }

    private fun mapRow(row: Array<Any?>, hasDistance: Boolean): StationSearchResult {
        // column order must match SELECT
        var idx = 0
        val stationCd = (row[idx++] as Number).toLong()
        val stationName = row[idx++] as String
        val stationNameK = row[idx++] as String
        val stationNameR = row[idx++] as String
        val address = row[idx++] as String
        val lon = (row[idx++] as Number).toDouble()
        val lat = (row[idx++] as Number).toDouble()
        val weight = (row[idx++] as Number).toDouble()
        val distanceKm = if (hasDistance) (row[idx++] as Number?)?.toDouble() else null
        val score = (row[idx] as Number).toDouble()
        return StationSearchResult(
            stationCd = stationCd,
            stationName = stationName,
            stationNameK = stationNameK,
            stationNameR = stationNameR,
            address = address,
            lon = lon,
            lat = lat,
            weight = weight,
            score = score,
            distanceKm = distanceKm,
        )
    }

    private fun ftsQuery(keyword: String): String {
        val escaped = keyword.replace("\"", "\"\"")
        return "\"$escaped\" OR \"${escaped}*\""
    }

    private fun escapeLike(keyword: String): String {
        return keyword
            .replace("!", "!!")
            .replace("%", "!%")
            .replace("_", "!_")
            .replace("[", "![")
    }

    companion object {
        private val KEYWORD_SEARCH_SQL = """
            WITH ft AS (
                SELECT [KEY] AS station_cd, RANK AS fulltext_rank
                FROM CONTAINSTABLE(
                    stations_with_fulltext,
                    (station_name, station_name_k, station_name_r),
                    :ftsQuery
                )
            ),
            w AS (
                SELECT TOP 1 *
                FROM fulltext_weight
            )
            SELECT
                s.station_cd,
                s.station_name,
                s.station_name_k,
                s.station_name_r,
                s.address,
                s.lon,
                s.lat,
                s.weight,
                CAST(
                    w.fulltext_rank_weight * COALESCE(ft.fulltext_rank, 0)
                    + w.exact_match_weight * m.exact_match
                    + w.prefix_match_weight * m.prefix_match
                    + w.partial_match_weight * m.partial_match
                    + w.station_weight_weight * s.weight
                    AS FLOAT
                ) AS score
            FROM stations_with_fulltext s
            LEFT JOIN ft ON ft.station_cd = s.station_cd
            CROSS JOIN w
            CROSS APPLY (
                SELECT
                    CASE
                        WHEN s.station_name = :kw
                            OR s.station_name_k = :kw
                            OR s.station_name_r = :kw
                        THEN 1 ELSE 0
                    END AS exact_match,
                    CASE
                        WHEN s.station_name LIKE :kwPrefix ESCAPE '!'
                            OR s.station_name_k LIKE :kwPrefix ESCAPE '!'
                            OR s.station_name_r LIKE :kwPrefix ESCAPE '!'
                        THEN 1 ELSE 0
                    END AS prefix_match,
                    CASE
                        WHEN s.station_name LIKE :kwContains ESCAPE '!'
                            OR s.station_name_k LIKE :kwContains ESCAPE '!'
                            OR s.station_name_r LIKE :kwContains ESCAPE '!'
                        THEN 1 ELSE 0
                    END AS partial_match
            ) m
            WHERE ft.station_cd IS NOT NULL
                OR m.exact_match = 1
                OR m.prefix_match = 1
                OR m.partial_match = 1
            ORDER BY score DESC, s.station_cd ASC
        """.trimIndent()

        private val KEYWORD_SEARCH_GEO_SQL = """
            WITH ft AS (
                SELECT [KEY] AS station_cd, RANK AS fulltext_rank
                FROM CONTAINSTABLE(
                    stations_with_fulltext,
                    (station_name, station_name_k, station_name_r),
                    :ftsQuery
                )
            ),
            w AS (
                SELECT TOP 1 *
                FROM fulltext_weight
            )
            SELECT
                s.station_cd,
                s.station_name,
                s.station_name_k,
                s.station_name_r,
                s.address,
                s.lon,
                s.lat,
                s.weight,
                d.distance_km,
                CAST(
                    w.fulltext_rank_weight * COALESCE(ft.fulltext_rank, 0)
                    + w.exact_match_weight * m.exact_match
                    + w.prefix_match_weight * m.prefix_match
                    + w.partial_match_weight * m.partial_match
                    + w.station_weight_weight * s.weight
                    + w.distance_weight * (1.0 / NULLIF(w.distance_km_scale + d.distance_km, 0))
                    AS FLOAT
                ) AS score
            FROM stations_with_fulltext s
            LEFT JOIN ft ON ft.station_cd = s.station_cd
            CROSS JOIN w
            CROSS APPLY (
                SELECT
                    CASE
                        WHEN s.station_name = :kw
                            OR s.station_name_k = :kw
                            OR s.station_name_r = :kw
                        THEN 1 ELSE 0
                    END AS exact_match,
                    CASE
                        WHEN s.station_name LIKE :kwPrefix ESCAPE '!'
                            OR s.station_name_k LIKE :kwPrefix ESCAPE '!'
                            OR s.station_name_r LIKE :kwPrefix ESCAPE '!'
                        THEN 1 ELSE 0
                    END AS prefix_match,
                    CASE
                        WHEN s.station_name LIKE :kwContains ESCAPE '!'
                            OR s.station_name_k LIKE :kwContains ESCAPE '!'
                            OR s.station_name_r LIKE :kwContains ESCAPE '!'
                        THEN 1 ELSE 0
                    END AS partial_match
            ) m
            CROSS APPLY (
                SELECT
                    s.geog.STDistance(geography::Point(:lat, :lon, 4326)) / 1000.0 AS distance_km
            ) d
            WHERE ft.station_cd IS NOT NULL
                OR m.exact_match = 1
                OR m.prefix_match = 1
                OR m.partial_match = 1
            ORDER BY score DESC, s.station_cd ASC
        """.trimIndent()
    }
}
