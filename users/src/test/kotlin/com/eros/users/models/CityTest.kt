package com.eros.users.models;

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class CityTest {

    @Nested
    inner class `LAT LONG`{

        @Test
        fun `should create city with min latitude`() {
            val city = CreateCityRequest("Blah", 5.0, -90.0)
            assertEquals(-90.0, city.latitude)
        }

        @Test
        fun `should create city with min latitude 2`() {
            val city = UpdateCityRequest(0L,"Blah", 5.0, -90.0)
            assertEquals(-90.0, city.newCityLatitude)
        }

        @Test
        fun `should create city with max latitude`() {
            val city = CreateCityRequest("Blah", 5.0, 90.0)
            assertEquals(90.0, city.latitude)
        }

        @Test
        fun `should create city with max latitude 2`() {
            val city = UpdateCityRequest(1L,"Blah", 5.0, 90.0)
            assertEquals(90.0, city.newCityLatitude)
        }

        @Test
        fun `should create city with min longitude`() {
            val city = CreateCityRequest("Blah", -180.0, -5.0)
            assertEquals(-180.0, city.longitude)

        }

        @Test
        fun `should create city with min longitude 2`() {
            val city = UpdateCityRequest(1L, "Blah", -180.0, -5.0)
            assertEquals(-180.0, city.newCityLongitude)

        }

        @Test
        fun `should create city with max longitude`() {
            val city = CreateCityRequest("Blah", 180.0, 50.0)
            assertEquals(180.0, city.longitude)
        }

        @Test
        fun `should create city with max longitude 2`() {
            val city = UpdateCityRequest(1L, "Blah", 180.0, -5.0)
            assertEquals(180.0, city.newCityLongitude)
        }

        @Test
        fun `should throw exception with invalid latitude`(){
            val exception = assertThrows<IllegalArgumentException> {
                CreateCityRequest("Blah",5.0,-90.1)
            }
            assertEquals("Latitude must be between -90 and 90", exception.message)
        }

        @Test
        fun `should throw exception with invalid latitude 2`(){
            val exception = assertThrows<IllegalArgumentException> {
                CreateCityRequest("Blah",5.0,90.1)
            }
            assertEquals("Latitude must be between -90 and 90", exception.message)
        }

        @Test
        fun `should throw exception with invalid latitude 3`(){
            val exception = assertThrows<IllegalArgumentException> {
                UpdateCityRequest(1L, newCityLatitude = -90.1)
            }
            assertEquals("Latitude must be between -90 and 90", exception.message)
        }

        @Test
        fun `should throw exception with invalid latitude 4`(){
            val exception = assertThrows<IllegalArgumentException> {
                UpdateCityRequest(1L, newCityLatitude = 90.1)
            }
            assertEquals("Latitude must be between -90 and 90", exception.message)
        }

        @Test
        fun `should throw exception with invalid latitude 5`(){
            val exception = assertThrows<IllegalArgumentException> {
                CityDTO(1L,"Blah", 5.0, 90.1)
            }
            assertEquals("Latitude must be between -90 and 90", exception.message)
        }

        @Test
        fun `should throw exception with invalid longitude`(){
            val exception = assertThrows<IllegalArgumentException> {
                CreateCityRequest("Blah",180.1,5.0)
            }
            assertEquals("Longitude must be between -180 and 180", exception.message)
        }

        @Test
        fun `should throw exception with invalid longitude 2`(){
            val exception = assertThrows<IllegalArgumentException> {
                CreateCityRequest("Blah",-180.1,5.0)
            }
            assertEquals("Longitude must be between -180 and 180", exception.message)
        }

        @Test
        fun `should throw exception with invalid longitude 3`(){
            val exception = assertThrows<IllegalArgumentException> {
                CityDTO(1L,"Blah",-180.1,5.0)
            }
            assertEquals("Longitude must be between -180 and 180", exception.message)
        }

        @Test
        fun `should throw exception with invalid longitude 4`(){
            val exception = assertThrows<IllegalArgumentException> {
                UpdateCityRequest(1L,"Blah",-180.1,5.0)
            }
            assertEquals("Longitude must be between -180 and 180", exception.message)
        }

        @Test
        fun `should throw exception with invalid longitude 5`(){
            val exception = assertThrows<IllegalArgumentException> {
                UpdateCityRequest(1L,"Blah",180.1,5.0)
            }
            assertEquals("Longitude must be between -180 and 180", exception.message)
        }

    }

    @Nested
    inner class `City name`{

        @Test
        fun `should throw exception with empty name`(){
            val exception = assertThrows<IllegalArgumentException> {
                CreateCityRequest("    ",-56.1,5.0)
            }
            assertEquals("City name must not be empty.", exception.message)
        }

        @Test
        fun `should throw exception with empty name 2`(){
            val exception = assertThrows<IllegalArgumentException> {
                UpdateCityRequest(1L,"    ")
            }
            assertEquals("City name must not be empty.", exception.message)
        }

        @Test
        fun `should throw exception with empty name 3`(){
            val exception = assertThrows<IllegalArgumentException> {
                CityDTO(1L,"    ",5.0,5.0)
            }
            assertEquals("City name must not be empty.", exception.message)
        }

    }

}
