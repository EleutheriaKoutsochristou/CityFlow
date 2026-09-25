package com.example.cityflow.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.cityflow.data.model.ReportCategory
import com.example.cityflow.data.model.ReportStatus
import com.example.cityflow.domain.PriorityCalculator
import com.example.cityflow.util.AppSettings

/**
 * Extension properties for Compose UI to dynamically display localized model titles
 * based on the active language in AppSettings.
 */
val ReportCategory.localizedTitle: String
    @Composable
    get() {
        val lang by AppSettings.currentLanguage.collectAsState()
        return getTitle(lang)
    }

val ReportCategory.localizedAdvice: String
    @Composable
    get() {
        val lang by AppSettings.currentLanguage.collectAsState()
        return getDefaultAdvice(lang)
    }

val ReportStatus.localizedTitle: String
    @Composable
    get() {
        val lang by AppSettings.currentLanguage.collectAsState()
        return getTitle(lang)
    }

val PriorityCalculator.PriorityLevel.localizedTitle: String
    @Composable
    get() {
        val lang by AppSettings.currentLanguage.collectAsState()
        return getTitle(lang)
    }

fun com.example.cityflow.data.model.Report.getLocalizedAdvice(lang: String): String {
    val cat = reportCategory

    if (practicalAdvice.isBlank() ||
        practicalAdvice.equals(cat.defaultAdviceEl, ignoreCase = true) ||
        practicalAdvice.equals(cat.defaultAdviceEn, ignoreCase = true)
    ) {
        return cat.getDefaultAdvice(lang)
    }

    for (c in ReportCategory.entries) {
        if (practicalAdvice.equals(c.defaultAdviceEl, ignoreCase = true) ||
            practicalAdvice.equals(c.defaultAdviceEn, ignoreCase = true)
        ) {
            return c.getDefaultAdvice(lang)
        }
    }

    return practicalAdvice
}

/**
 * Επιστρέφει την πρακτική οδηγία της αναφοράς μεταφρασμένη στη γλώσσα της εφαρμογής
 * εφόσον αντιστοιχεί σε προεπιλεγμένη οδηγία κατηγορίας.
 */
val com.example.cityflow.data.model.Report.localizedAdvice: String
    @Composable
    get() {
        val lang by AppSettings.currentLanguage.collectAsState()
        return getLocalizedAdvice(lang)
    }

private val commonDescriptionMap = listOf(
    // RAMP_BLOCKED
    "Αποκλεισμένο πεζοδρόμιο / ράμπα ΑμεΑ" to "Blocked sidewalk / ramp (PRM)",
    "Αποκλεισμένο πεζοδρόμιο" to "Blocked sidewalk",
    "Αποκλεισμένη ράμπα ΑμεΑ" to "Blocked PRM ramp",
    "Αποκλεισμένη ράμπα" to "Blocked ramp",
    "Μπλοκαρισμένη ράμπα" to "Blocked ramp",
    "Μπλοκαρισμένο πεζοδρόμιο" to "Blocked sidewalk",
    "Ράμπα ΑμεΑ" to "PRM Ramp",
    "Παρκαρισμένο αυτοκίνητο σε ράμπα" to "Parked car on ramp",
    "Παρκαρισμένο όχημα σε πεζοδρόμιο" to "Vehicle parked on sidewalk",
    "Παρκαρισμένο σε ράμπα" to "Parked on ramp",
    "Εμπόδιο σε ράμπα" to "Obstacle on ramp",

    // MICROMOBILITY_OBSTACLE
    "Εμπόδιο σε ποδηλατόδρομο / μικροκινητικότητα" to "Obstacle in bike / micromobility lane",
    "Εμπόδιο σε ποδηλατόδρομο" to "Obstacle in bike lane",
    "Εμπόδιο στον ποδηλατόδρομο" to "Obstacle on the bike lane",
    "Παρατημένο πατίνι" to "Abandoned scooter",
    "Παρατημένο πατίνι στον ποδηλατόδρομο" to "Abandoned scooter in bike lane",
    "Κάδος στον ποδηλατόδρομο" to "Trash bin in bike lane",

    // POTHOLE
    "Λακκούβα ή φθορά οδοστρώματος" to "Pothole or road surface hazard",
    "Λακκούβα" to "Pothole",
    "Λακκούβα στο δρόμο" to "Pothole on the road",
    "Μεγάλη λακκούβα" to "Large pothole",
    "Επικίνδυνη λακκούβα" to "Dangerous pothole",
    "Φθορά οδοστρώματος" to "Road surface hazard",
    "Καθίζηση οδοστρώματος" to "Road subsidence",
    "Σπασμένο οδόστρωμα" to "Damaged road surface",
    "Σπασμένη πλάκα" to "Broken sidewalk tile",

    // ROAD_WORK
    "Προσωρινά έργα ή κλειστός δρόμος" to "Road works or closed street",
    "Προσωρινά έργα" to "Road works",
    "Έργα στο δρόμο" to "Road construction works",
    "Έργα στο οδόστρωμα" to "Road works on surface",
    "Κλειστός δρόμος" to "Closed road",
    "Κλειστή λωρίδα" to "Closed lane",
    "Εργασίες οδοποιίας" to "Road construction works",

    // TRAFFIC_LIGHT
    "Βλάβη σε σηματοδότη ή διάβαση" to "Traffic light or crossing malfunction",
    "Βλάβη σε σηματοδότη" to "Traffic light malfunction",
    "Βλάβη σηματοδότη" to "Traffic light failure",
    "Χαλασμένο φανάρι" to "Broken traffic light",
    "Φανάρι εκτός λειτουργίας" to "Traffic light out of order",
    "Σηματοδότης εκτός λειτουργίας" to "Traffic light out of order",
    "Βλάβη διάβασης" to "Pedestrian crossing malfunction",

    // TRANSIT_OVERCROWDING
    "Καθυστέρηση ή συνωστισμός σε ΜΜΜ" to "Transit delay or overcrowding",
    "Καθυστέρηση λεωφορείου" to "Bus delay",
    "Καθυστέρηση δρομολογίου" to "Transit route delay",
    "Συνωστισμός σε ΜΜΜ" to "Transit overcrowding",
    "Συνωστισμός στη στάση" to "Overcrowding at bus stop",

    // OTHER
    "Άλλο πρόβλημα προσβασιμότητας / μετακίνησης" to "Other mobility / accessibility issue",
    "Άλλο πρόβλημα" to "Other problem",
    "Πρόβλημα προσβασιμότητας" to "Accessibility problem",
    "Πρόβλημα μετακίνησης" to "Mobility problem"
)

fun com.example.cityflow.data.model.Report.getLocalizedDescription(lang: String): String {
    val isEn = lang == "en"
    val trimmed = description.trim()

    if (trimmed.isBlank()) {
        return reportCategory.getTitle(lang)
    }

    // 1. Έλεγχος αντιστοίχισης με τίτλο ή προεπιλεγμένη οδηγία κατηγορίας
    for (c in ReportCategory.entries) {
        if (trimmed.equals(c.titleEl, ignoreCase = true) ||
            trimmed.equals(c.titleEn, ignoreCase = true)
        ) {
            return c.getTitle(lang)
        }
        if (trimmed.equals(c.defaultAdviceEl, ignoreCase = true) ||
            trimmed.equals(c.defaultAdviceEn, ignoreCase = true)
        ) {
            return c.getDefaultAdvice(lang)
        }
    }

    // 2. Έλεγχος ακριβούς αντιστοίχισης στο λεξικό συχνών περιγραφών
    for ((greek, english) in commonDescriptionMap) {
        if (isEn && trimmed.equals(greek, ignoreCase = true)) {
            return english
        }
        if (!isEn && trimmed.equals(english, ignoreCase = true)) {
            return greek
        }
    }

    // 3. Έλεγχος υποσυμβολοσειράς (contains) στο λεξικό
    for ((greek, english) in commonDescriptionMap) {
        if (isEn && trimmed.contains(greek, ignoreCase = true)) {
            return english
        }
        if (!isEn && trimmed.contains(english, ignoreCase = true)) {
            return greek
        }
    }

    // 4. Fallback λέξεων-κλειδιών
    val lower = trimmed.lowercase()
    if (isEn) {
        when {
            lower.contains("ράμπα") || lower.contains("πεζοδρόμι") -> return "Blocked sidewalk / ramp"
            lower.contains("λακκούβ") || lower.contains("λακουβ") -> return "Pothole on road"
            lower.contains("φανάρι") || lower.contains("σηματοδότ") -> return "Traffic light malfunction"
            lower.contains("έργα") || lower.contains("κλειστός δρόμος") -> return "Road works / closed street"
            lower.contains("πατίνι") || lower.contains("ποδηλατ") -> return "Obstacle in bike lane"
            lower.contains("λεωφορεί") || lower.contains("συνωστισμ") -> return "Transit delay / overcrowding"
        }
    } else {
        when {
            lower.contains("ramp") || lower.contains("sidewalk") -> return "Αποκλεισμένο πεζοδρόμιο / ράμπα"
            lower.contains("pothole") -> return "Λακκούβα στο οδόστρωμα"
            lower.contains("traffic light") -> return "Βλάβη σε φωτεινό σηματοδότη"
            lower.contains("road work") || lower.contains("closed") -> return "Προσωρινά έργα ή κλειστός δρόμος"
            lower.contains("bike") || lower.contains("scooter") -> return "Εμπόδιο σε ποδηλατόδρομο"
            lower.contains("transit") || lower.contains("bus") -> return "Καθυστέρηση σε ΜΜΜ"
        }
    }

    return description
}

/**
 * Επιστρέφει την περιγραφή της αναφοράς μεταφρασμένη στη γλώσσα της εφαρμογής
 * βάσει λεξικού όρων, τίτλων κατηγοριών ή αναγνώρισης λέξεων-κλειδιών.
 */
val com.example.cityflow.data.model.Report.localizedDescription: String
    @Composable
    get() {
        val lang by AppSettings.currentLanguage.collectAsState()
        return getLocalizedDescription(lang)
    }


