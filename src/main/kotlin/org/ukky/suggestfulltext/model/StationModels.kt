package org.ukky.suggestfulltext.model

data class StationRow(
    val stationCd: Long,
    val stationName: String,
    val stationNameK: String,
    val stationNameR: String,
    val address: String,
    val lon: Double,
    val lat: Double,
    val weight: Double = 1.0,
)

data class ImportResult(
    val inserted: Int,
)

data class StationSearchResult(
    val stationCd: Long,
    val stationName: String,
    val stationNameK: String,
    val stationNameR: String,
    val address: String,
    val lon: Double,
    val lat: Double,
    val weight: Double,
    val score: Double,
    val distanceKm: Double? = null,
)
