package org.ukky.suggestfulltext.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import org.ukky.suggestfulltext.entity.StationEntity

@Repository
interface StationRepository : JpaRepository<StationEntity, Long>
