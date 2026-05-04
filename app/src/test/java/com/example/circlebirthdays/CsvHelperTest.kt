package com.example.circlebirthdays

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvHelperTest {

    @Test
    fun `test shifted CSV format`() {
        val csv = """
            Family ID,, Name ,,M/F, DD, MM, YYYY, WeddingDate, BereavementDate
            1,, John Doe ,,M, 15, 05, 1980, 10-12-2010, 
        """.trimIndent()

        val members = CsvHelper.parse(csv)
        assertEquals(1, members.size)
        val member = members[0]
        assertEquals("1", member.familyId)
        assertEquals("John Doe", member.name)
        assertEquals("Male", member.gender)
        assertEquals("1980-05-15", member.dateOfBirth)
        assertEquals("2010-12-10", member.marriageDate)
        assertEquals(null, member.bereavementDate)
    }

    @Test
    fun `test standard CSV format`() {
        val csv = """
            Family ID, Name, M/F, DD, MM, YYYY, WeddingDate, BereavementDate
            2, Jane Smith, F, 1, 1, 1990, 20/05/2015, 
        """.trimIndent()

        val members = CsvHelper.parse(csv)
        assertEquals(1, members.size)
        val member = members[0]
        assertEquals("2", member.familyId)
        assertEquals("Jane Smith", member.name)
        assertEquals("1990-01-01", member.dateOfBirth)
        assertEquals("2015-05-20", member.marriageDate)
    }

    @Test
    fun `test date formatting with slashes and dashes`() {
        val csv = """
            Family ID, Name, M/F, DD, MM, YYYY, WeddingDate, BereavementDate
            3, Bob, M, 10, 10, 1970, 01-01-2000, 02/02/2020
        """.trimIndent()

        val members = CsvHelper.parse(csv)
        val member = members[0]
        assertEquals("2000-01-01", member.marriageDate)
        assertEquals("2020-02-02", member.bereavementDate)
    }

    @Test
    fun `test invalid dates and stars`() {
        val csv = """
            Family ID, Name, M/F, DD, MM, YYYY, WeddingDate, BereavementDate
            4, Alice, F, 20, 08, 1985, *, -
        """.trimIndent()

        val members = CsvHelper.parse(csv)
        val member = members[0]
        assertEquals(null, member.marriageDate)
        assertEquals(null, member.bereavementDate)
    }

    @Test
    fun `test tab separated`() {
        val csv = "Family ID\tName\tM/F\tDD\tMM\tYYYY\tWeddingDate\tBereavementDate\n" +
                  "5\tCharlie\tM\t05\t05\t1975\t\t"
        
        val members = CsvHelper.parse(csv)
        assertEquals(1, members.size)
        assertEquals("Charlie", members[0].name)
    }
}
