package org.ukky.suggestfulltext.controller

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.multipart
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver
import org.ukky.suggestfulltext.model.ImportResult
import org.ukky.suggestfulltext.model.StationSearchResult
import org.ukky.suggestfulltext.service.StationService

class StationControllerTest {

    private val stationService = mockk<StationService>()
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(StationController(stationService))
            .setHandlerExceptionResolvers(ResponseStatusExceptionResolver())
            .build()
    }

    private fun stationSearchResult(
        stationCd: Long = 1110101L,
        stationName: String = "東京",
        distanceKm: Double? = null,
    ) = StationSearchResult(
        stationCd = stationCd,
        stationName = stationName,
        stationNameK = "トウキョウ",
        stationNameR = "Tokyo",
        address = "東京都千代田区丸の内一丁目",
        lon = 139.7671,
        lat = 35.6812,
        weight = 1.5,
        score = 1.0,
        distanceKm = distanceKm,
    )

    @Nested
    inner class PostImport {

        @Test
        fun `正常なCSVファイルをインポートすると挿入件数が返る`() {
            val bytes = checkNotNull(javaClass.classLoader.getResourceAsStream("csv/valid_stations.csv"))
                .readBytes()
            val file = MockMultipartFile("file", "valid_stations.csv", "text/csv", bytes)

            every { stationService.importCsv(any()) } returns ImportResult(inserted = 3)

            mockMvc.multipart("/api/stations/import") {
                file(file)
            }.andExpect {
                status { isOk() }
                content { contentType(MediaType.APPLICATION_JSON) }
                jsonPath("$.inserted") { value(3) }
            }
        }

        @Test
        fun `サービスが400エラーをスローした場合は400が返る`() {
            val bytes = checkNotNull(javaClass.classLoader.getResourceAsStream("csv/invalid_stations.csv"))
                .readBytes()
            val file = MockMultipartFile("file", "invalid_stations.csv", "text/csv", bytes)

            every { stationService.importCsv(any()) } throws
                ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV header must include: station_cd, ...")

            mockMvc.multipart("/api/stations/import") {
                file(file)
            }.andExpect {
                status { isBadRequest() }
            }
        }
    }

    @Nested
    inner class GetSearch {

        @Test
        fun `キーワードで検索すると駅リストが返る`() {
            val results = listOf(
                stationSearchResult(stationCd = 1110101L, stationName = "東京"),
                stationSearchResult(stationCd = 1130101L, stationName = "新宿"),
            )
            every { stationService.search("東京", 5) } returns results

            mockMvc.get("/api/stations/search") {
                param("keyword", "東京")
                param("limit", "5")
            }.andExpect {
                status { isOk() }
                content { contentType(MediaType.APPLICATION_JSON) }
                jsonPath("$.length()") { value(2) }
                jsonPath("$[0].stationName") { value("東京") }
                jsonPath("$[0].stationCd") { value(1110101) }
                jsonPath("$[1].stationName") { value("新宿") }
            }
        }

        @Test
        fun `limitを省略すると既定値20が使われる`() {
            every { stationService.search("東京", 20) } returns listOf(stationSearchResult())

            mockMvc.get("/api/stations/search") {
                param("keyword", "東京")
            }.andExpect {
                status { isOk() }
            }
        }

        @Test
        fun `サービスが400エラーをスローした場合は400が返る`() {
            every { stationService.search(any(), any()) } throws
                ResponseStatusException(HttpStatus.BAD_REQUEST, "keyword is required")

            mockMvc.get("/api/stations/search") {
                param("keyword", " ")
            }.andExpect {
                status { isBadRequest() }
            }
        }
    }

    @Nested
    inner class GetSearchGeo {

        @Test
        fun `キーワードと座標で検索すると距離付き駅リストが返る`() {
            val results = listOf(
                stationSearchResult(distanceKm = 0.3),
                stationSearchResult(stationCd = 1130101L, stationName = "新宿", distanceKm = 1.2),
            )
            every { stationService.searchWithGeo("東京", 35.6812, 139.7671, 5) } returns results

            mockMvc.get("/api/stations/search-geo") {
                param("keyword", "東京")
                param("lat", "35.6812")
                param("lon", "139.7671")
                param("limit", "5")
            }.andExpect {
                status { isOk() }
                content { contentType(MediaType.APPLICATION_JSON) }
                jsonPath("$.length()") { value(2) }
                jsonPath("$[0].distanceKm") { value(0.3) }
                jsonPath("$[1].distanceKm") { value(1.2) }
            }
        }

        @Test
        fun `サービスが400エラーをスローした場合は400が返る`() {
            every { stationService.searchWithGeo(any(), any(), any(), any()) } throws
                ResponseStatusException(HttpStatus.BAD_REQUEST, "lat must be between -90 and 90")

            mockMvc.get("/api/stations/search-geo") {
                param("keyword", "東京")
                param("lat", "999.0")
                param("lon", "139.76")
                param("limit", "5")
            }.andExpect {
                status { isBadRequest() }
            }
        }
    }
}


