package com.purawale.app

import java.time.LocalDate

object PanchangData {
    val festivals2026 = mapOf(
        1 to mapOf(
            1 to "ईस्वी सन् नव वर्ष (New Year)",
            2 to "व्रत पूर्णिमा",
            5 to "परमहंस योगानंद जयंती",
            10 to "सावित्री बा फुले जयंती",
            12 to "राष्ट्रीय युवा दिवस",
            13 to "लोहड़ी उत्सव",
            14 to "मकर संक्रांति",
            19 to "Bhisma Ekadashi",
            23 to "बसंत पंचमी",
            26 to "Republic Day",
            29 to "जया एकादशी",
            31 to "गुरु हरराय जयंती"
        ),
        2 to mapOf(
            1 to "माघ पूर्णिमा",
            12 to "स्वामी दयानंद सरस्वती जयंती",
            15 to "महाशिवरात्रि",
            17 to "फाल्गुन अमावस्या"
        ),
        3 to mapOf(
            3 to "होली (Dhulandi)",
            10 to "शीतला अष्टमी",
            19 to "गुड़ी पड़वा (Gudi Padwa)",
            21 to "गौरी पूजा (Gangaur)",
            27 to "राम नवमी (Ram Navami)",
            31 to "हनुमान जयंती"
        ),
        4 to mapOf(
            13 to "वैशाखी",
            14 to "अम्बेडकर जयंती",
            20 to "अक्षय तृतीया"
        ),
        5 to mapOf(
            1 to "बुद्ध पूर्णिमा, कूर्म अवतार",
            3 to "नारद जयंती",
            13 to "अपरा एकादशी व्रत",
            15 to "शिव चतुर्दशी व्रत",
            16 to "वट सावित्री अमावस्या व्रत",
            18 to "चन्द्रदर्शन",
            20 to "विनायकी चतुर्थी व्रत",
            26 to "गंगा दशहारा",
            27 to "निर्जला/कमला एकादशी",
            28 to "प्रदोष व्रत",
            30 to "ज्येष्ठ पूर्णिमा, वट पूर्णिमा"
        ),
        6 to mapOf(
            16 to "आषाढ़ एकादशी (Devshayani)",
            30 to "गुरु पूर्णिमा"
        ),
        7 to mapOf(
            26 to "प्रदोष व्रत"
        ),
        8 to mapOf(
            15 to "स्वतंत्रता दिवस (Independence Day)",
            16 to "रक्षा बंधन",
            25 to "कृष्ण जन्माष्टमी",
            28 to "भाद्रपद अमावस्या"
        ),
        9 to mapOf(
            15 to "गणेश चतुर्थी (Ganesh Chaturthi)",
            26 to "अनंत चतुर्दशी"
        ),
        10 to mapOf(
            2 to "गांधी जयंती",
            11 to "दशहरा (Vijayadashami)",
            20 to "करवा चौथ",
            31 to "नरक चतुर्दशी"
        ),
        11 to mapOf(
            8 to "दीपावली (Diwali)",
            9 to "गोवर्धन पूजा",
            10 to "भाई दूज",
            16 to "छठ पूजा",
            20 to "देवोत्थान एकादशी",
            24 to "कार्तिक पूर्णिमा"
        ),
        12 to mapOf(
            25 to "क्रिसमस (Christmas)"
        )
    )

    val panchak2026 = mapOf(
        5 to listOf(12..16, 24..26)
    )

    val muhurats2026 = mapOf(
        5 to mapOf(
            "Vivah" to listOf(1, 3, 8, 12, 13, 14),
            "Namkaran" to listOf(4, 11, 13, 14, 18, 20, 21, 25, 27, 28),
            "Vyapar Prarambh" to listOf(3, 8, 18, 31),
            "Annaprashan" to listOf(8, 14, 20, 21, 28, 29),
            "Griharambh" to listOf(1, 4, 6)
        )
    )
}
