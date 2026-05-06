package com.example.circlebirthdays

object FamilyUtils {
    private fun isFemale(gender: String?): Boolean {
        val g = gender?.lowercase() ?: ""
        return g == "female" || g == "f" || g == "woman"
    }

    private fun isMale(gender: String?): Boolean {
        val g = gender?.lowercase() ?: ""
        return g == "male" || g == "m" || g == "man"
    }

    private fun getParentBase(baseId: String): String {
        if (baseId == "P" || baseId.isEmpty()) return ""
        return if (baseId.length == 1) "P" else baseId.dropLast(1)
    }

    /**
     * Resolves the linked names (Spouse, Father, Mother) and immediate family
     * for a member based on the Family ID logic.
     */
    fun resolveLinks(member: Member, allMembers: List<Member>, currentUser: Member? = null): Member {
        val id = member.familyId
        if (id.isEmpty()) return member

        var spouseName: String?
        var fatherName: String? = null
        var motherName: String? = null
        val children = mutableListOf<String>()
        val siblings = mutableListOf<String>()

        // 1. Resolve Spouse
        var spouseMarriageDate: String?
        if (id.endsWith("0")) {
            val partnerId = id.substring(0, id.length - 1)
            val partner = allMembers.find { it.familyId == partnerId }
            spouseName = partner?.name
            spouseMarriageDate = partner?.marriageDate
        } else {
            val partnerId = id + "0"
            val partner = allMembers.find { it.familyId == partnerId }
            spouseName = partner?.name
            spouseMarriageDate = partner?.marriageDate
        }

        val effectiveMarriageDate = member.marriageDate ?: spouseMarriageDate

        // 2. Resolve Parents & Siblings
        val isSpouseSuffix = id.endsWith("0")
        val baseId = if (isSpouseSuffix) id.substring(0, id.length - 1) else id
        
        if (!isSpouseSuffix && (baseId.length > 1 || (baseId.length == 1 && baseId != "P"))) {
            val parentBaseId = if (baseId.length > 1) baseId.substring(0, baseId.length - 1) else "P"
            val parentSpouseId = if (parentBaseId == "P") "P0" else parentBaseId + "0"
            
            val p1 = allMembers.find { it.familyId == parentBaseId }
            val p2 = allMembers.find { it.familyId == parentSpouseId }
            
            if (p1 != null || p2 != null) {
                if (isMale(p1?.gender)) {
                    fatherName = p1?.name
                    motherName = p2?.name
                } else if (isFemale(p1?.gender)) {
                    motherName = p1?.name
                    fatherName = p2?.name
                } else if (isMale(p2?.gender)) {
                    fatherName = p2?.name
                    motherName = p1?.name
                } else {
                    fatherName = p1?.name
                    motherName = p2?.name
                }
            }
            
            val siblingsFilter: (Member) -> Boolean = if (parentBaseId == "P") {
                { it.familyId.length == 1 && it.familyId != "P" && !it.familyId.endsWith("0") && it.familyId != baseId }
            } else {
                { it.familyId.length == baseId.length && it.familyId.startsWith(parentBaseId) && !it.familyId.endsWith("0") && it.familyId != baseId }
            }
            
            allMembers.filter(siblingsFilter).sortedBy { it.name }.forEach { siblings.add(it.name) }
        }

        // 3. Resolve Children
        val childrenFilter: (Member) -> Boolean = if (baseId == "P") {
            { it.familyId.length == 1 && it.familyId != "P" && !it.familyId.endsWith("0") }
        } else {
            { it.familyId.length == baseId.length + 1 && it.familyId.startsWith(baseId) && !it.familyId.endsWith("0") }
        }
        allMembers.filter(childrenFilter).sortedBy { it.name }.forEach { children.add(it.name) }

        val familySummary = buildString {
            if (siblings.isNotEmpty()) append("Siblings: ${siblings.joinToString(", ")}. ")
            if (children.isNotEmpty()) append("Children: ${children.joinToString(", ")}.")
        }.trim()

        val inferredRel = (if (currentUser != null) member.manualRelationships[currentUser.id] else null)
            ?: if (currentUser != null) getRelationship(member, currentUser, allMembers) else null

        return member.copy(
            spouseName = spouseName ?: member.spouseName,
            fatherName = fatherName ?: member.fatherName,
            motherName = motherName ?: member.motherName,
            marriageDate = effectiveMarriageDate,
            immediateFamily = familySummary.ifEmpty { member.immediateFamily },
            relationship = inferredRel ?: member.relationship
        )
    }

    private fun isElder(id1: String, id2: String): Boolean {
        var i = 0
        while (i < id1.length && i < id2.length) {
            val c1 = id1[i]
            val c2 = id2[i]
            if (c1 != c2) {
                if (c1.isDigit() && c2.isDigit()) {
                    val n1 = id1.substring(i).takeWhile { it.isDigit() }.toIntOrNull() ?: 0
                    val n2 = id2.substring(i).takeWhile { it.isDigit() }.toIntOrNull() ?: 0
                    if (n1 != n2) return n1 < n2
                }
                return c1 < c2
            }
            i++
        }
        return id1.length < id2.length
    }

    /**
     * Determines the relationship label for 'target' relative to 'observer'.
     */
    fun getRelationship(target: Member, observer: Member, allMembers: List<Member>): String? {
        if (target.id == observer.id) return null
        val tId = target.familyId
        val oId = observer.familyId
        if (tId.isEmpty() || oId.isEmpty()) return null

        // Mirror relationships if observer is a spouse, with specific in-law terms
        if (oId.endsWith("0")) {
            val partnerBase = oId.dropLast(1)
            val partner = allMembers.find { it.familyId == partnerBase }
            if (partner != null) {
                val relToPartner = getRelationship(target, partner, allMembers)
                if (relToPartner != null) {
                    if (!isFemale(partner.gender)) { // Observer is Wife
                        return when (relToPartner) {
                            "Bhai" -> "Devar"
                            "Bhaiya" -> "Jeth"
                            "Behan", "Didi" -> "Nanad"
                            "Papa" -> "Sasurji"
                            "Mummy" -> "Saasuma"
                            else -> relToPartner
                        }
                    } else { // Observer is Husband
                        return when (relToPartner) {
                            "Bhai", "Bhaiya" -> "Saala"
                            "Behan", "Didi" -> "Saali"
                            "Papa" -> "Sasurji"
                            "Mummy" -> "Saasuma"
                            else -> relToPartner
                        }
                    }
                }
            }
        }

        val tBase = if (tId.endsWith("0")) tId.dropLast(1) else tId
        val oBase = if (oId.endsWith("0")) oId.dropLast(1) else oId

        val tGen = if (tBase == "P") 0 else tBase.length
        val oGen = if (oBase == "P") 0 else oBase.length
        val diff = oGen - tGen

        val isTargetSpouse = tId.endsWith("0")
        val isFemale = isFemale(target.gender)

        // Same Generation
        if (diff == 0) {
            if (tBase == oBase) {
                if (tId != oId) return if (isFemale) "Wife" else "Husband"
            }
            
            // Any same-generation relative in the core family tree (A-G branches)
            if (tBase != "P" && oBase != "P") {
                val isElder = isElder(tBase, oBase)
                if (isTargetSpouse) return if (isFemale) "Bhabhi" else "Jijaji"
                return if (isFemale) (if (isElder) "Didi" else "Behan") else (if (isElder) "Bhaiya" else "Bhai")
            }
        }

        // Parent Generation
        if (diff == 1) {
            // Direct Parents
            val oParentBase = getParentBase(oBase)
            if (tBase == oParentBase) {
                return if (isFemale || isTargetSpouse) "Mummy" else "Papa"
            }
            
            // Parent's Siblings or Extended Siblings (Cousins)
            val oParentParentBase = getParentBase(oParentBase)
            val tParentBase = getParentBase(tBase)

            val areExtendedSiblings = (tParentBase == oParentParentBase) || 
                (tParentBase.isNotEmpty() && oParentParentBase.isNotEmpty() && getParentBase(tParentBase) == getParentBase(oParentParentBase))

            if (areExtendedSiblings && tBase != "P") {
                val isElder = isElder(tBase, oParentBase)
                
                val oParent = allMembers.find { it.familyId == oParentBase }
                
                // If parent is a daughter of the main tree (e.g. A6), but target is also from main tree, use paternal terms
                // We use paternal terms for anyone in the A-G branches to match user preference for the core family.
                val isPaternal = oParent == null || isMale(oParent.gender) || 
                                (oBase.firstOrNull() in 'A'..'G' && tBase.firstOrNull() in 'A'..'G')

                if (isTargetSpouse) {
                    if (isFemale) return if (isPaternal) (if (isElder) "Badi Amma" else "Chachiji") else (if (isElder) "Badi Mamiji" else "Choti Mamiji")
                    return if (isPaternal) (if (isElder) "Bade Fufa" else "Chote Fufa") else (if (isElder) "Bade Mausa" else "Chote Mausa")
                }
                if (isFemale) return if (isPaternal) (if (isElder) "Badi Bua" else "Choti Bua") else (if (isElder) "Badi Mausi" else "Choti Mausi")
                return if (isPaternal) (if (isElder) "Bade Papa" else "Chachaji") else (if (isElder) "Bade Mamaji" else "Chote Mamaji")
            }
        }

        // Grandparent Generation
        if (diff == 2) {
            val oParentBase = getParentBase(oBase)
            val oParent = allMembers.find { it.familyId == oParentBase }
            
            // If parent is a daughter of the main tree, but target is grandparent branch, use paternal terms (Dada/Dadi)
            val isMaternal = oParent != null && isFemale(oParent.gender) && !(oParentBase.length == 1 && oParentBase != "P")

            val oGrandParentBase = getParentBase(oParentBase)
            if (tBase == oGrandParentBase) {
                if (isMaternal) return if (isFemale) "Nani" else "Nana"
                return if (isFemale) "Dadi" else "Dadaji"
            }
            // Grandparent's siblings
            if (tBase.isNotEmpty() && oGrandParentBase.isNotEmpty() && getParentBase(tBase) == getParentBase(oGrandParentBase)) {
                val isElder = isElder(tBase, oGrandParentBase)
                return if (isMaternal) {
                    if (isFemale) (if (isElder) "Badi Nani" else "Choti Nani") else (if (isElder) "Bade Nana" else "Chote Nana")
                } else {
                    if (isFemale) (if (isElder) "Badi Dadi" else "Choti Dadi") else (if (isElder) "Bade Dadaji" else "Chote Dadaji")
                }
            }
        }

        // Child Generation
        if (diff == -1) {
            if (tBase.startsWith(oBase)) {
                if (isTargetSpouse) return if (isFemale) "Bahu" else "Damand"
                return if (isFemale) "Beti" else "Beta"
            }
            // Nephew/Niece
            val tParentId = getParentBase(tBase)
            val tParent = allMembers.find { it.familyId == tParentId }
            if (tParent != null) {
                val relToParent = getRelationship(tParent, observer, allMembers)
                if (relToParent == "Bhai" || relToParent == "Bhaiya" || relToParent == "Didi" || relToParent == "Behan") {
                    if (isTargetSpouse) return if (isFemale) "Bahu" else "Damand"
                    return if (isFemale(tParent.gender)) {
                        if (isFemale) "Bhanji" else "Bhanja"
                    } else {
                        if (isFemale) "Bhatiji" else "Bhatija"
                    }
                }
            }
        }

        // Grandchild Generation
        if (diff == -2) {
            val tParentId = getParentBase(tBase)
            val tParent = allMembers.find { it.familyId == tParentId }
            val isDaughterChild = tParent != null && isFemale(tParent.gender)

            if (tBase.startsWith(oBase)) {
                if (isTargetSpouse) return if (isFemale) "Bahu" else "Damand"
                return if (isDaughterChild) {
                    if (isFemale) "Natin" else "Nati"
                } else {
                    if (isFemale) "Poti" else "Pota"
                }
            }
            
            // Siblings' grandchildren
            if (tParent != null) {
                val tGrandparentId = getParentBase(tParentId)
                val tGrandparent = allMembers.find { it.familyId == tGrandparentId }
                if (tGrandparent != null) {
                    val relToGP = getRelationship(tGrandparent, observer, allMembers)
                    if (relToGP == "Bhai" || relToGP == "Bhaiya" || relToGP == "Didi" || relToGP == "Behan") {
                        if (isTargetSpouse) return if (isFemale) "Bahu" else "Damand"
                        return if (isDaughterChild) {
                            if (isFemale) "Natin" else "Nati"
                        } else {
                            if (isFemale) "Poti" else "Pota"
                        }
                    }
                }
            }
        }

        // Catch-all for same family branch but unknown depth
        if (tBase.startsWith(oBase) && diff < -2) return "Grandchild"
        if (oBase.startsWith(tBase) && diff > 2) return "Grandparent"

        return null
    }



    /**
     * Processes a whole list to update all links.
     */
    fun populateAllLinks(members: List<Member>, currentUser: Member? = null): List<Member> {
        return members.map { resolveLinks(it, members, currentUser) }
    }
}
