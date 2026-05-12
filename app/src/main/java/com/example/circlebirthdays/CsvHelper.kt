package com.example.circlebirthdays

import java.util.UUID

object CsvHelper {

    /**
     * Parses CSV text and returns a list of Members.
     * Handles specific formats provided by user:
     * FamilyID,, Name ,,M/F, DD, MM, YYYY, WeddingDate, BereavementDate
     */
    fun parse(csvText: String): List<Member> {
        val lines = csvText.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        val members = mutableListOf<Member>()

        for (line in lines) {
            // Detect separator: comma or tab
            val cols = line.split(Regex("[,\\t]|\\s{3,}")).map { it.trim().removeSurrounding("\"").trim() }

            // Skip headers or too-short lines
            if (cols.size < 6) continue
            if (cols[0].contains("Family ID", ignoreCase = true)) continue
            if (cols.getOrNull(2)?.contains("Name", ignoreCase = true) == true) continue
            if (cols.getOrNull(5)?.contains("DD", ignoreCase = true) == true) continue

            try {
                val familyId: String
                val name: String
                val genderRaw: String
                val dd: String
                val mm: String
                val yyyy: String
                val weddingRaw: String?
                val bereavementRaw: String?

                if (cols.size >= 9) {
                    familyId = cols[0]
                    name = cols[2]
                    genderRaw = cols[4]
                    dd = cols[5].padStart(2, '0')
                    mm = cols[6].padStart(2, '0')
                    yyyy = cols[7]
                    weddingRaw = cols.getOrNull(8)
                    bereavementRaw = cols.getOrNull(9)
                } else {
                    // Fallback to original logic if format varies
                    familyId = cols[0]
                    name = cols[1]
                    genderRaw = cols[2]
                    dd = cols[3].padStart(2, '0')
                    mm = cols[4].padStart(2, '0')
                    yyyy = cols[5]
                    weddingRaw = cols.getOrNull(6)
                    bereavementRaw = cols.getOrNull(7)
                }

                val gender = when (genderRaw.trim().uppercase()) {
                    "M", "MALE" -> "Male"
                    "F", "FEMALE" -> "Female"
                    else -> genderRaw.trim()
                }

                if (name.isBlank() || name.lowercase() == "name") continue

                val phoneRaw = if (cols.size > 10) cols[10] else ""

                members.add(
                    Member(
                        id = familyId, // Use familyId as the unique document ID
                        familyId = familyId,
                        name = name,
                        gender = gender,
                        dateOfBirth = "$yyyy-$mm-$dd",
                        marriageDate = formatWeddingDate(weddingRaw),
                        bereavementDate = formatWeddingDate(bereavementRaw),
                        phoneNumber = phoneRaw.replace(Regex("\\D"), "")
                    )
                )
            } catch (e: Exception) {
                // Skip lines that fail to parse
            }
        }
        return members
    }

    private fun formatWeddingDate(raw: String?): String? {
        if (raw == null || raw.isBlank() || raw.contains("*") || raw.trim() == "-") return null
        return try {
            val trimmed = raw.trim()
            // Handle DD-MM-YYYY format
            val parts = trimmed.split("-")
            if (parts.size == 3) {
                val d = parts[0].trim().padStart(2, '0')
                val m = parts[1].trim().padStart(2, '0')
                val y = parts[2].trim()
                if (y.length == 4) "$y-$m-$d" else null
            } else if (trimmed.contains("/")) {
                 val partsSlash = trimmed.split("/")
                 if (partsSlash.size == 3) {
                     val d = partsSlash[0].trim().padStart(2, '0')
                     val m = partsSlash[1].trim().padStart(2, '0')
                     val y = partsSlash[2].trim()
                     if (y.length == 4) "$y-$m-$d" else null
                 } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
