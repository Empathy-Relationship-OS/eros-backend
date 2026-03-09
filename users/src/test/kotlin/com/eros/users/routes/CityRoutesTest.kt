package com.eros.users.routes

import com.eros.auth.firebase.FirebaseUserPrincipal
import com.eros.common.errors.NotFoundException
import com.eros.common.plugins.configureExceptionHandling
import com.eros.users.models.City
import com.eros.users.models.CityDTO
import com.eros.users.models.CreateCityRequest
import com.eros.users.models.NearestCityResponse
import com.eros.users.models.UpdateCityRequest
import com.eros.users.service.CityService
import com.google.firebase.auth.FirebaseToken
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class CityRoutesTest{

    private val mockCityService = mockk<CityService>()


    @Nested
    inner class `CREATE City`{

        @Test
        fun `successfully create a city`() = testApplication {
            setupTestApp("ADMIN")
            val client = configuredClient()

            val request = CreateCityRequest("London",-5.2, 45.2)
            val createdCity = createCity(longitude = -5.2, latitude = 45.2)

            coEvery { mockCityService.createCity(request) } returns createdCity
            coEvery { mockCityService.doesExists(request.cityName) } returns false

            val response = client.post("/city/admin"){
                setAuthenticatedUser("test-user-id")
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            assertEquals(HttpStatusCode.Created, response.status)

            val city = response.body<CityDTO>()
            assertEquals("London",city.cityName)

        }

        @Test
        fun `successfully create a city EMPLOYEE`() = testApplication {
            setupTestApp("EMPLOYEE")
            val client = configuredClient()

            val request = CreateCityRequest("London",-5.2, 45.2)
            val createdCity = createCity(longitude = -5.2, latitude = 45.2)

            coEvery { mockCityService.createCity(request) } returns createdCity
            coEvery { mockCityService.doesExists(request.cityName) } returns false

            val response = client.post("/city/admin"){
                setAuthenticatedUser("test-user-id")
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            assertEquals(HttpStatusCode.Created, response.status)

            val city = response.body<CityDTO>()
            assertEquals("London",city.cityName)

        }

        @Test
        fun `ensure USER role can't create city`() = testApplication {
            setupTestApp()
            val client = configuredClient()

            val request = CreateCityRequest("London",-5.2, 45.2)

            val response = client.post("/city/admin"){
                setAuthenticatedUser("test-user-id")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }

        @Test
        fun `Ensure can't create city with empty name`() = testApplication {
            setupTestApp("ADMIN")
            val client = configuredClient()

            val response = client.post("/city/admin") {
                setAuthenticatedUser("test-user-id")
                contentType(ContentType.Application.Json)
                setBody("""{"cityName":"    ","longitude":-5.2,"latitude":45.2}""")
            }

            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

    }

    @Nested
    inner class `GET all City`{

        @Test
        fun `get ALL cities USER`() = testApplication{

            setupTestApp()
            val client = configuredClient()

            coEvery { mockCityService.getAllCities() } returns listOf(createCity(),createCity(),createCity())

            val response = client.get("/city/all") {
                setAuthenticatedUser("test-user-id")
                contentType(ContentType.Application.Json)
            }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(3, response.body<List<CityDTO>>().size)
        }

        @Test
        fun `get ALL cities ADMIN`() = testApplication{
            setupTestApp("ADMIN")
            val client = configuredClient()

            coEvery { mockCityService.getAllCities() } returns listOf(createCity(),createCity(),createCity())

            val response = client.get("/city/all") {
                setAuthenticatedUser("test-user-id")
                contentType(ContentType.Application.Json)
            }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(3, response.body<List<CityDTO>>().size)
        }

        @Test
        fun `get ALL cities EMPLOYEE`() = testApplication{

            setupTestApp("EMPLOYEE")
            val client = configuredClient()

            coEvery { mockCityService.getAllCities() } returns listOf(createCity(),createCity(),createCity())

            val response = client.get("/city/all") {
                setAuthenticatedUser("test-user-id")
                contentType(ContentType.Application.Json)
            }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(3, response.body<List<CityDTO>>().size)
        }

    }

    @Nested
    inner class `GET single City`{

        @Test
        fun `successfully retrieve 1 city`() = testApplication{
            setupTestApp("USER")
            val client = configuredClient()

            val city = createCity()

            coEvery { mockCityService.findByCityId(0L) } returns city

            val response = client.get("/city/0") {
                setAuthenticatedUser("test-user-id")
                contentType(ContentType.Application.Json)
            }

            assertEquals(HttpStatusCode.OK, response.status)
            val returnedCity = response.body<CityDTO>()
            assertEquals(city.cityName, returnedCity.cityName)

        }

        @Test
        fun `successfully retrieve 1 city ADMIN`() = testApplication{
            setupTestApp("ADMIN")
            val client = configuredClient()

            val city = createCity()

            coEvery { mockCityService.findByCityId(0L) } returns city

            val response = client.get("/city/0") {
                setAuthenticatedUser("test-user-id")
                contentType(ContentType.Application.Json)
            }

            assertEquals(HttpStatusCode.OK, response.status)
            val returnedCity = response.body<CityDTO>()
            assertEquals(city.cityName, returnedCity.cityName)

        }

        @Test
        fun `return error if city not found ADMIN`() = testApplication{
            setupTestApp("ADMIN")
            val client = configuredClient()
            
            coEvery { mockCityService.findByCityId(0L) } throws NotFoundException("city not found.")

            val response = client.get("/city/0") {
                setAuthenticatedUser("test-user-id")
                contentType(ContentType.Application.Json)
            }

            assertEquals(HttpStatusCode.NotFound, response.status)

        }

    }


    @Nested
    inner class `DELETE City`{

        @Test
        fun `should delete city ADMIN`() = testApplication{
            setupTestApp("ADMIN")
            val client = configuredClient()

            coEvery { mockCityService.deleteCity(0L) } returns 1

            val response = client.delete("/city/admin/0") {
                setAuthenticatedUser("test-user-id")
            }

            assertEquals(HttpStatusCode.NoContent, response.status)

        }

        @Test
        fun `should delete city EMPLOYEE`() = testApplication{

            setupTestApp("EMPLOYEE")
            val client = configuredClient()

            coEvery { mockCityService.deleteCity(0L) } returns 1

            val response = client.delete("/city/admin/0") {
                setAuthenticatedUser("test-user-id")
            }

            assertEquals(HttpStatusCode.NoContent, response.status)

        }

        @Test
        fun `USER can't delete city`() = testApplication{
            setupTestApp()
            val client = configuredClient()

            val response = client.delete("/city/admin/0") {
                setAuthenticatedUser("test-user-id")
            }
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }

        @Test
        fun `unauthorized can't delete city`() = testApplication{
            setupTestApp()
            val client = configuredClient()

            val response = client.delete("/city/admin/0")

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

        @Test
        fun `can't delete city that does not exist`() = testApplication{

            setupTestApp("ADMIN")
            val client = configuredClient()

            coEvery { mockCityService.deleteCity(0L) } returns 0

            val response = client.delete("/city/admin/0") {
                setAuthenticatedUser("test-user-id")
            }

            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }

    @Nested
    inner class `PATCH City`{

        @Test
        fun `successfully patch every field`()= testApplication{

            setupTestApp("ADMIN")
            val client = configuredClient()
            val updatedCity = createCity(0L,"Liverpool",3.4,-4.5)
            val request = UpdateCityRequest(0L,"Liverpool",3.4,-4.5)

            coEvery { mockCityService.updateCity(0L,request) } returns updatedCity

            val response = client.patch("/city/admin/0") {
                setAuthenticatedUser("test-user-id")
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            assertEquals(HttpStatusCode.OK, response.status)

            val returnedCity = response.body<CityDTO>()
            assertEquals(updatedCity.cityId,returnedCity.cityId)
            assertEquals(updatedCity.cityName,returnedCity.cityName)
            assertEquals(updatedCity.longitude,returnedCity.longitude)
            assertEquals(updatedCity.latitude,returnedCity.latitude)

        }

        @Test
        fun `USER CAN'T patch a city`()= testApplication{
            setupTestApp()
            val client = configuredClient()

            val response = client.patch("/city/admin/0") {
                setAuthenticatedUser("test-user-id")
            }
            assertEquals(HttpStatusCode.Forbidden, response.status)
        }

        @Test
        fun `successfully patch single field`()= testApplication{

            setupTestApp("ADMIN")
            val client = configuredClient()
            val city = createCity()
            val updatedCity = createCity(0L,"Liverpool",-5.0,45.0)
            val request = UpdateCityRequest(0L,"Liverpool")
            coEvery { mockCityService.updateCity(0L,request) } returns updatedCity

            val response = client.patch("/city/admin/0") {
                setAuthenticatedUser("test-user-id")
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            assertEquals(HttpStatusCode.OK, response.status)

            val returnedCity = response.body<CityDTO>()
            assertEquals(city.cityId,returnedCity.cityId)
            assertNotEquals(city.cityName,returnedCity.cityName)
            assertEquals(city.longitude,returnedCity.longitude)
            assertEquals(city.latitude,returnedCity.latitude)

        }
    }


    @Nested
    inner class `GET nearest` {

        @Test
        fun `successfully get nearest`() = testApplication {
            setupTestApp()
            val client = configuredClient()
            val lat = 2.45
            val long = 50.3
            val city = createCity()
            coEvery { mockCityService.findNearestCities(1,lat, long) } returns listOf(city)

            val response = client.get("/city/nearest") {
                setAuthenticatedUser("test-user-id")
                contentType(ContentType.Application.Json)
                parameter("lat", lat)
                parameter("lon", long)
            }

            assertEquals(HttpStatusCode.OK, response.status)
            val returnedCities = response.body<NearestCityResponse>()
            assertEquals(city.cityId, returnedCities.cities[0].cityId)
            assertEquals(city.cityName, returnedCities.cities[0].cityName)
        }

        @Test
        fun `successfully get nearest cities`() = testApplication {
            setupTestApp()
            val client = configuredClient()
            val lat = 2.45
            val long = 50.3
            val city = createCity()
            val city2 = createCity(cityName = "Liverpool")
            coEvery { mockCityService.findNearestCities(2,lat, long) } returns listOf(city,city2)

            val response = client.get("/city/nearest") {
                setAuthenticatedUser("test-user-id")
                contentType(ContentType.Application.Json)
                parameter("lat", lat)
                parameter("lon", long)
                parameter("limit",2)
            }

            assertEquals(HttpStatusCode.OK, response.status)
            val returnedCities = response.body<NearestCityResponse>()
            assertEquals(2, returnedCities.count)
            assertEquals(city.cityId, returnedCities.cities[0].cityId)
            assertEquals(city.cityName, returnedCities.cities[0].cityName)
            assertEquals(city2.cityName, returnedCities.cities[1].cityName)
        }

    }


    // Helper functions

    fun createCity(id : Long = 0L, cityName : String = "London", longitude : Double = -5.0, latitude : Double = 45.0) : City {
        val now = Instant.now()
        return City(id, cityName, longitude, latitude,now,now)
    }

    /**
     * Creates a configured HTTP client for tests with JSON content negotiation.
     * Each test should create its own client instance to ensure proper serialization.
     */
    private fun ApplicationTestBuilder.configuredClient() = createClient {
        install(ClientContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
    }

    private fun ApplicationTestBuilder.setupTestApp(role : String = "USER") {
        application {
            configureExceptionHandling()
            // Install server-side content negotiation
            install(ServerContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                })
            }

            // Install authentication
            install(Authentication) {
                bearer("firebase-auth") {
                    realm = "test-realm"
                    authenticate { credential ->
                        // Return mock principal based on credential token
                        // Token format: "user-{userId}"
                        val userId = credential.token.removePrefix("user-")
                        val mockToken = mockk<FirebaseToken>(relaxed = true) {
                            coEvery { uid } returns userId
                        }
                        FirebaseUserPrincipal(
                            uid = userId,
                            email = "$userId@example.com",
                            phoneNumber = null,
                            emailVerified = true,
                            token = mockToken,
                            role = role
                        )
                    }
                }
            }

            routing {
                authenticate("firebase-auth") {
                    cityRoutes(mockCityService)
                }
            }
        }
    }

    private fun HttpRequestBuilder.setAuthenticatedUser(userId: String) {
        header(HttpHeaders.Authorization, "Bearer user-$userId")
    }
}