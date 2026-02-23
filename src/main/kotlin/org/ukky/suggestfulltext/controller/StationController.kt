package org.ukky.suggestfulltext.controller

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.ukky.suggestfulltext.model.ImportResult
import org.ukky.suggestfulltext.model.StationSearchResult
import org.ukky.suggestfulltext.service.StationService

@RestController
@RequestMapping("/api/stations")
class StationController(
    private val stationService: StationService,
) {
    @PostMapping("/import", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun importCsv(@RequestParam("file") file: MultipartFile): ImportResult {
        return stationService.importCsv(file)
    }

    @GetMapping("/search")
    fun search(
        @RequestParam("keyword") keyword: String,
        @RequestParam("limit", defaultValue = "20") limit: Int,
    ): List<StationSearchResult> {
        return stationService.search(keyword, limit)
    }

    @GetMapping("/search-geo")
    fun searchGeo(
        @RequestParam("keyword") keyword: String,
        @RequestParam("lat") lat: Double,
        @RequestParam("lon") lon: Double,
        @RequestParam("limit", defaultValue = "20") limit: Int,
    ): List<StationSearchResult> {
        return stationService.searchWithGeo(keyword, lat, lon, limit)
    }
}
