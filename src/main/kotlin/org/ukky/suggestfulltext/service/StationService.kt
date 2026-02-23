package org.ukky.suggestfulltext.service

import org.apache.commons.csv.CSVFormat
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import org.ukky.suggestfulltext.entity.StationEntity
import org.ukky.suggestfulltext.model.ImportResult
import org.ukky.suggestfulltext.model.StationRow
import org.ukky.suggestfulltext.model.StationSearchResult
import org.ukky.suggestfulltext.repository.StationRepository
import org.ukky.suggestfulltext.repository.StationSearchRepository
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

@Service
class StationService(
    private val stationRepository: StationRepository,
    private val stationSearchRepository: StationSearchRepository,
) {

    @Transactional
    fun importCsv(file: MultipartFile): ImportResult {
        val bytes = file.bytes
        val rows = parseCsv(bytes)
        stationRepository.deleteAllInBatch()
        val entities = rows.map { row ->
            StationEntity(
                stationCd = row.stationCd,
                stationName = row.stationName,
                stationNameK = row.stationNameK,
                stationNameR = row.stationNameR,
                address = row.address,
                lon = row.lon,
                lat = row.lat,
                weight = row.weight,
            )
        }
        stationRepository.saveAll(entities)
        return ImportResult(entities.size)
    }

    fun search(keyword: String, limit: Int): List<StationSearchResult> {
        val trimmed = keyword.trim()
        if (trimmed.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "keyword is required")
        }
        return stationSearchRepository.search(trimmed, limit)
    }

    fun searchWithGeo(keyword: String, lat: Double, lon: Double, limit: Int): List<StationSearchResult> {
        val trimmed = keyword.trim()
        if (trimmed.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "keyword is required")
        }
        if (lat !in -90.0..90.0) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "lat must be between -90 and 90")
        }
        if (lon !in -180.0..180.0) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "lon must be between -180 and 180")
        }
        return stationSearchRepository.searchWithGeo(trimmed, lat, lon, limit)
    }

    private fun parseCsv(bytes: ByteArray): List<StationRow> {
        val reader = InputStreamReader(bytes.inputStream(), StandardCharsets.UTF_8)
        val format = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setTrim(true)
            .build()
        val parser = format.parse(reader)
        val requiredHeaders = listOf(
            "station_cd",
            "station_name",
            "station_name_k",
            "station_name_r",
            "address",
            "lon",
            "lat",
        )
        val headerSet = parser.headerMap.keys
        if (!headerSet.containsAll(requiredHeaders)) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "CSV header must include: ${requiredHeaders.joinToString(", ")}"
            )
        }
        return parser.records.map { record ->
            StationRow(
                stationCd = record.get("station_cd").trim().toLong(),
                stationName = record.get("station_name").trim(),
                stationNameK = record.get("station_name_k").trim(),
                stationNameR = record.get("station_name_r").trim(),
                address = record.get("address").trim(),
                lon = record.get("lon").trim().toDouble(),
                lat = record.get("lat").trim().toDouble(),
            )
        }
    }
}
