package org.ukky.suggestfulltext.service

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.server.ResponseStatusException
import org.ukky.suggestfulltext.entity.StationEntity
import org.ukky.suggestfulltext.model.StationSearchResult
import org.ukky.suggestfulltext.repository.StationRepository
import org.ukky.suggestfulltext.repository.StationSearchRepository
import kotlin.test.assertEquals

class StationServiceTest {

    private val stationRepository = mockk<StationRepository>()
    private val stationSearchRepository = mockk<StationSearchRepository>()
    private val stationService = StationService(stationRepository, stationSearchRepository)

    private fun loadCsvBytes(resourcePath: String): ByteArray =
        checkNotNull(javaClass.classLoader.getResourceAsStream(resourcePath)) {
            "Test resource not found: $resourcePath"
        }.readBytes()

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
    inner class ImportCsv {

        @Test
        fun `正常なCSVをインポートすると挿入件数が返る`() {
            val bytes = loadCsvBytes("csv/valid_stations.csv")
            val file = MockMultipartFile("file", "valid_stations.csv", "text/csv", bytes)

            justRun { stationRepository.deleteAllInBatch() }
            every { stationRepository.saveAll(any<List<StationEntity>>()) } answers { firstArg() }

            val result = stationService.importCsv(file)

            assertEquals(3, result.inserted)
            verify(exactly = 1) { stationRepository.deleteAllInBatch() }
            verify(exactly = 1) { stationRepository.saveAll(any<List<StationEntity>>()) }
        }

        @Test
        fun `不正なヘッダーのCSVは400エラーになる`() {
            val bytes = loadCsvBytes("csv/invalid_stations.csv")
            val file = MockMultipartFile("file", "invalid_stations.csv", "text/csv", bytes)

            val ex = assertThrows<ResponseStatusException> { stationService.importCsv(file) }

            assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
        }
    }

    @Nested
    inner class Search {

        @Test
        fun `キーワードで検索すると結果リストが返る`() {
            val expected = listOf(stationSearchResult(stationName = "東京"))
            every { stationSearchRepository.search("東京", 5) } returns expected

            val result = stationService.search("東京", 5)

            assertEquals(expected, result)
        }

        @Test
        fun `キーワードの前後に空白があってもトリムされて検索される`() {
            val expected = listOf(stationSearchResult(stationName = "東京"))
            every { stationSearchRepository.search("東京", 10) } returns expected

            val result = stationService.search("  東京  ", 10)

            assertEquals(expected, result)
            verify { stationSearchRepository.search("東京", 10) }
        }

        @ParameterizedTest(name = "キーワード: \"{0}\"")
        @ValueSource(strings = ["", " ", "　", "   "])
        fun `空白キーワードは400エラーになる`(keyword: String) {
            val ex = assertThrows<ResponseStatusException> { stationService.search(keyword, 10) }

            assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
        }
    }

    @Nested
    inner class SearchWithGeo {

        @Test
        fun `キーワードと座標で検索すると距離付き結果リストが返る`() {
            val expected = listOf(stationSearchResult(distanceKm = 0.5))
            every { stationSearchRepository.searchWithGeo("東京", 35.68, 139.76, 5) } returns expected

            val result = stationService.searchWithGeo("東京", 35.68, 139.76, 5)

            assertEquals(expected, result)
        }

        @ParameterizedTest(name = "キーワード: \"{0}\"")
        @ValueSource(strings = ["", " ", "　"])
        fun `空白キーワードは400エラーになる`(keyword: String) {
            val ex = assertThrows<ResponseStatusException> {
                stationService.searchWithGeo(keyword, 35.68, 139.76, 5)
            }

            assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
        }

        @ParameterizedTest(name = "緯度: {0}")
        @ValueSource(doubles = [-91.0, 91.0, -90.1, 90.1])
        fun `緯度が範囲外の場合は400エラーになる`(lat: Double) {
            val ex = assertThrows<ResponseStatusException> {
                stationService.searchWithGeo("東京", lat, 139.76, 5)
            }

            assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
        }

        @ParameterizedTest(name = "経度: {0}")
        @ValueSource(doubles = [-181.0, 181.0, -180.1, 180.1])
        fun `経度が範囲外の場合は400エラーになる`(lon: Double) {
            val ex = assertThrows<ResponseStatusException> {
                stationService.searchWithGeo("東京", 35.68, lon, 5)
            }

            assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
        }

        @ParameterizedTest(name = "緯度: {0}")
        @ValueSource(doubles = [-90.0, 0.0, 90.0])
        fun `緯度の境界値は正常に処理される`(lat: Double) {
            val expected = listOf(stationSearchResult(distanceKm = 1.0))
            every { stationSearchRepository.searchWithGeo("東京", lat, 139.76, 5) } returns expected

            val result = stationService.searchWithGeo("東京", lat, 139.76, 5)

            assertEquals(expected, result)
        }

        @ParameterizedTest(name = "経度: {0}")
        @ValueSource(doubles = [-180.0, 0.0, 180.0])
        fun `経度の境界値は正常に処理される`(lon: Double) {
            val expected = listOf(stationSearchResult(distanceKm = 1.0))
            every { stationSearchRepository.searchWithGeo("東京", 35.68, lon, 5) } returns expected

            val result = stationService.searchWithGeo("東京", 35.68, lon, 5)

            assertEquals(expected, result)
        }
    }
}



