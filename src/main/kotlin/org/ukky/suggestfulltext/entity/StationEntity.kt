package org.ukky.suggestfulltext.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "stations_with_fulltext")
data class StationEntity(
    @Id
    @Column(name = "station_cd")
    val stationCd: Long,

    @Column(name = "station_name", nullable = false)
    val stationName: String,

    @Column(name = "station_name_k", nullable = false)
    val stationNameK: String,

    @Column(name = "station_name_r", nullable = false)
    val stationNameR: String,

    @Column(name = "address", nullable = false)
    val address: String,

    @Column(name = "lon", nullable = false)
    val lon: Double,

    @Column(name = "lat", nullable = false)
    val lat: Double,

    @Column(name = "weight", nullable = false)
    val weight: Double = 1.0,
)
