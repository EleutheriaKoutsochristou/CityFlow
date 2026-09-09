package com.example.cityflow.data.model

/**
 * Κατηγορίες προβλημάτων αστικής κινητικότητας και προσβασιμότητας.
 *
 * Περιλαμβάνει:
 * - defaultSeverity: Βαθμός σοβαρότητας (1-5) για τον αλγόριθμο προτεραιότητας.
 * - defaultRadiusMeters: Ακτίνα ειδοποίησης σε μέτρα για τους χρήστες.
 * - defaultAdviceEl / defaultAdviceEn: Πρακτική οδηγία που συνοδεύει την ειδοποίηση.
 */
enum class ReportCategory(
    val titleEl: String,
    val titleEn: String,
    val defaultSeverity: Int,
    val defaultRadiusMeters: Double,
    val defaultAdviceEl: String,
    val defaultAdviceEn: String
) {
    RAMP_BLOCKED(
        titleEl = "Αποκλεισμένο πεζοδρόμιο / ράμπα ΑμεΑ",
        titleEn = "Blocked sidewalk / ramp (PRM)",
        defaultSeverity = 4,
        defaultRadiusMeters = 150.0,
        defaultAdviceEl = "Η πρόσβαση σε ράμπα είναι προσωρινά περιορισμένη.",
        defaultAdviceEn = "Ramp access is temporarily restricted."
    ),
    MICROMOBILITY_OBSTACLE(
        titleEl = "Εμπόδιο σε ποδηλατόδρομο / μικροκινητικότητα",
        titleEn = "Obstacle in bike / micromobility lane",
        defaultSeverity = 3,
        defaultRadiusMeters = 250.0,
        defaultAdviceEl = "Προσοχή για ποδήλατα και πατίνια στο σημείο.",
        defaultAdviceEn = "Caution for bikes and scooters at this point."
    ),
    POTHOLE(
        titleEl = "Λακκούβα ή φθορά οδοστρώματος",
        titleEn = "Pothole or road surface hazard",
        defaultSeverity = 3,
        defaultRadiusMeters = 200.0,
        defaultAdviceEl = "Μειώστε ταχύτητα και προσέξτε για ανωμαλίες στο δρόμο.",
        defaultAdviceEn = "Reduce speed and watch out for road defects."
    ),
    ROAD_WORK(
        titleEl = "Προσωρινά έργα ή κλειστός δρόμος",
        titleEn = "Road works or closed street",
        defaultSeverity = 5,
        defaultRadiusMeters = 600.0,
        defaultAdviceEl = "Αποφύγετε το σημείο, προτιμήστε εναλλακτική διαδρομή.",
        defaultAdviceEn = "Avoid the area, choose an alternative route."
    ),
    TRAFFIC_LIGHT(
        titleEl = "Βλάβη σε σηματοδότη ή διάβαση",
        titleEn = "Traffic light or crossing malfunction",
        defaultSeverity = 5,
        defaultRadiusMeters = 400.0,
        defaultAdviceEl = "Αυξημένη προσοχή στη διασταύρωση λόγω βλάβης σηματοδότη.",
        defaultAdviceEn = "Increased caution at intersection due to signal failure."
    ),
    TRANSIT_OVERCROWDING(
        titleEl = "Καθυστέρηση ή συνωστισμός σε ΜΜΜ",
        titleEn = "Transit delay or overcrowding",
        defaultSeverity = 2,
        defaultRadiusMeters = 300.0,
        defaultAdviceEl = "Αναμένονται καθυστερήσεις στα δρομολόγια.",
        defaultAdviceEn = "Transit delays expected in the area."
    ),
    OTHER(
        titleEl = "Άλλο πρόβλημα προσβασιμότητας / μετακίνησης",
        titleEn = "Other mobility / accessibility issue",
        defaultSeverity = 2,
        defaultRadiusMeters = 200.0,
        defaultAdviceEl = "Προσοχή κατά τη διέλευση από το σημείο.",
        defaultAdviceEn = "Exercise caution when moving through this point."
    );

    companion object {
        fun fromString(value: String?): ReportCategory {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: OTHER
        }
    }
}
