package org.ukky.suggestfulltext.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "fulltext_weight")
data class FulltextWeightEntity(
    @Id
    @Column(name = "id")
    val id: Int,

    @Column(name = "fulltext_rank_weight", nullable = false)
    val fulltextRankWeight: Double,

    @Column(name = "exact_match_weight", nullable = false)
    val exactMatchWeight: Double,

    @Column(name = "prefix_match_weight", nullable = false)
    val prefixMatchWeight: Double,

    @Column(name = "partial_match_weight", nullable = false)
    val partialMatchWeight: Double,

    @Column(name = "station_weight_weight", nullable = false)
    val stationWeightWeight: Double,

    @Column(name = "distance_weight", nullable = false)
    val distanceWeight: Double,

    @Column(name = "distance_km_scale", nullable = false)
    val distanceKmScale: Double,
)
