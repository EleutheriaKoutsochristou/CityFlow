package com.example.cityflow.data.model

/**
 * Incident categories for urban mobility, traffic, and accessibility challenges.
 *
 * Each category includes:
 * - [defaultSeverity]: Inherent severity weight (1-5 scale) used in the ranking algorithm.
 * - [defaultRadiusMeters]: Default spatial alert radius in meters for geofenced proximity alerts.
 * - [defaultAdviceEl] / [defaultAdviceEn]: Standard practical safety / routing advice shown to citizens.
 */
enum class ReportCategory(
    val titleEl: String,
    val titleEn: String,
    val defaultSeverity: Int,
    val defaultRadiusMeters: Double,
    val defaultAdviceEl: String,
    val defaultAdviceEn: String
) {
    /** Obstructions on sidewalks, pedestrian crosswalks, or wheelchair access ramps */
    RAMP_BLOCKED(
        titleEl = "Αποκλεισμένο πεζοδρόμιο / ράμπα ΑμεΑ",
        titleEn = "Blocked sidewalk / ramp (PRM)",
        defaultSeverity = 4,
        defaultRadiusMeters = 150.0,
        defaultAdviceEl = "Η πρόσβαση σε ράμπα είναι προσωρινά περιορισμένη.",
        defaultAdviceEn = "Ramp access is temporarily restricted."
    ),
    /** Debris, illegal parking, or hazards on designated bicycle/micromobility paths */
    MICROMOBILITY_OBSTACLE(
        titleEl = "Εμπόδιο σε ποδηλατόδρομο / μικροκινητικότητα",
        titleEn = "Obstacle in bike / micromobility lane",
        defaultSeverity = 3,
        defaultRadiusMeters = 250.0,
        defaultAdviceEl = "Προσοχή για ποδήλατα και πατίνια στο σημείο.",
        defaultAdviceEn = "Caution for bikes and scooters at this point."
    ),
    /** Dangerous potholes, subsidence, or road pavement damage */
    POTHOLE(
        titleEl = "Λακκούβα ή φθορά οδοστρώματος",
        titleEn = "Pothole or road surface hazard",
        defaultSeverity = 3,
        defaultRadiusMeters = 200.0,
        defaultAdviceEl = "Μειώστε ταχύτητα και προσέξτε για ανωμαλίες στο δρόμο.",
        defaultAdviceEn = "Reduce speed and watch out for road defects."
    ),
    /** Street construction, maintenance works, or unexpected closures */
    ROAD_WORK(
        titleEl = "Προσωρινά έργα ή κλειστός δρόμος",
        titleEn = "Road works or closed street",
        defaultSeverity = 5,
        defaultRadiusMeters = 600.0,
        defaultAdviceEl = "Αποφύγετε το σημείο, προτιμήστε εναλλακτική διαδρομή.",
        defaultAdviceEn = "Avoid the area, choose an alternative route."
    ),
    /** Broken or flashing traffic lights and malfunctioning pedestrian signals */
    TRAFFIC_LIGHT(
        titleEl = "Βλάβη σε σηματοδότη ή διάβαση",
        titleEn = "Traffic light or crossing malfunction",
        defaultSeverity = 5,
        defaultRadiusMeters = 400.0,
        defaultAdviceEl = "Αυξημένη προσοχή στη διασταύρωση λόγω βλάβης σηματοδότη.",
        defaultAdviceEn = "Increased caution at intersection due to signal failure."
    ),
    /** Public transportation disruptions, major delays, or bus stop crowding */
    TRANSIT_OVERCROWDING(
        titleEl = "Καθυστέρηση ή συνωστισμός σε ΜΜΜ",
        titleEn = "Transit delay or overcrowding",
        defaultSeverity = 2,
        defaultRadiusMeters = 300.0,
        defaultAdviceEl = "Αναμένονται καθυστερήσεις στα δρομολόγια.",
        defaultAdviceEn = "Transit delays expected in the area."
    ),
    /** General accessibility, public lighting, or infrastructure issues */
    OTHER(
        titleEl = "Άλλο πρόβλημα προσβασιμότητας / μετακίνησης",
        titleEn = "Other mobility / accessibility issue",
        defaultSeverity = 2,
        defaultRadiusMeters = 200.0,
        defaultAdviceEl = "Προσοχή κατά τη διέλευση από το σημείο.",
        defaultAdviceEn = "Exercise caution when moving through this point."
    );

    /** Returns category title in specified language ("en" or "el"). */
    fun getTitle(lang: String): String = if (lang == "en") titleEn else titleEl

    /** Returns localized default safety advice according to language. */
    fun getDefaultAdvice(lang: String): String = if (lang == "en") defaultAdviceEn else defaultAdviceEl

    companion object {
        /**
         * Resolves a category string safely to a [ReportCategory] enum constant, defaulting to [OTHER].
         */
        fun fromString(value: String?): ReportCategory {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: OTHER
        }
    }
}
