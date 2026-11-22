package com.example.data

data class VehicleSpec(
    val id: String,
    val shortName: String,
    val fullName: String,
    val subtitle: String,
    val requiredSwag: Int,
    val topSpeedKmh: Int,
    val handling: Int,
    val clearance: Int,
    val swagFactor: Int,
    val accentColorHex: Long,
    val highlights: String,
)

data class MissionSpec(
    val id: String,
    val number: Int,
    val title: String,
    val hinglishBrief: String,
    val objectiveHint: String,
    val rewardSwag: Int,
    val rewardJholaId: String,
    val timeLimitSec: Int,
    val featuredCarId: String,
    val providesLoanerCar: Boolean,
)

data class JholaCatalogItem(
    val id: String,
    val emoji: String,
    val name: String,
    val caption: String,
    val unlockHint: String,
)

object GameCatalog {
    const val CENTER_LAT = 23.0035
    const val CENTER_LNG = 78.2215
    const val GEOFENCE_RADIUS_METERS = 1500

    val vehicles: List<VehicleSpec> = listOf(
        VehicleSpec(
            id = "scorpio",
            shortName = "Scorpio-N / Thar",
            fullName = "Black Mahindra Scorpio-N / Thar",
            subtitle = "Desi Vidhayak Edition",
            requiredSwag = 0,
            topSpeedKmh = 148,
            handling = 84,
            clearance = 98,
            swagFactor = 88,
            accentColorHex = 0xFFF59E0B,
            highlights = "Rugged black 3D SUV, roof LED bar, high village suspension",
        ),
        VehicleSpec(
            id = "gwagon",
            shortName = "G-Wagon G63",
            fullName = "Mercedes-Benz G-Wagon (G63 AMG)",
            subtitle = "Baraat King Edition",
            requiredSwag = 200,
            topSpeedKmh = 185,
            handling = 86,
            clearance = 90,
            swagFactor = 95,
            accentColorHex = 0xFFE2E8F0,
            highlights = "Boxy luxury 3D SUV, twin side exhausts, commanding stance",
        ),
        VehicleSpec(
            id = "rolls",
            shortName = "Rolls-Royce Phantom",
            fullName = "Rolls-Royce Phantom",
            subtitle = "Raisen Royal Edition",
            requiredSwag = 500,
            topSpeedKmh = 205,
            handling = 88,
            clearance = 66,
            swagFactor = 100,
            accentColorHex = 0xFFEAB308,
            highlights = "Long luxury sedan, illuminated Pantheon grille, Spirit of Ecstasy",
        ),
        VehicleSpec(
            id = "lambo",
            shortName = "Lamborghini Aventador",
            fullName = "Lamborghini Aventador",
            subtitle = "Desi Supercar (Sparks on Breakers!)",
            requiredSwag = 800,
            topSpeedKmh = 260,
            handling = 96,
            clearance = 34,
            swagFactor = 99,
            accentColorHex = 0xFFEF4444,
            highlights = "Low-slung wedge supercar, neon underglow, sparks on speed breakers",
        ),
    )

    val missions: List<MissionSpec> = listOf(
        MissionSpec(
            id = "chacha_chai",
            number = 1,
            title = "Chacha Ki Garam Chai",
            hinglishBrief = "Chacha khet par baith kar chai ka intezaar kar rahe hain! 90 seconds mein Rolls-Royce bhaga kar garam chai pahunchao, thandi hui toh daant padegi!",
            objectiveHint = "Pick up hot chai at Chai Tapri, then race to Chacha's Khet within 90s.",
            rewardSwag = 150,
            rewardJholaId = "kulhad_chai",
            timeLimitSec = 90,
            featuredCarId = "rolls",
            providesLoanerCar = true,
        ),
        MissionSpec(
            id = "baraat_entry",
            number = 2,
            title = "Baraat Mein G-Wagon Entry",
            hinglishBrief = "Gaon ke chowk par baraat ruki hai! G-Wagon lekar full volume horn bajate hue entry maaro!",
            objectiveHint = "Drive into Gram Panchayat Chowk Baraat ring and blast HORN 3 times!",
            rewardSwag = 160,
            rewardJholaId = "sarpanch_gamcha",
            timeLimitSec = 0,
            featuredCarId = "gwagon",
            providesLoanerCar = true,
        ),
        MissionSpec(
            id = "chana_bhaji",
            number = 3,
            title = "Sarpanch Ji Ki Chana Ki Bhaji",
            hinglishBrief = "Sarpanch ji ke ghar mehman aaye hain! Khet ke paas se taaza 3D Chana ki Bhaji pickup karke unke ghar deliver karo!",
            objectiveHint = "Collect glowing 3D Chana Ki Bhaji bundle at the fields and deliver to Sarpanch Haveli.",
            rewardSwag = 140,
            rewardJholaId = "chana_bhaji",
            timeLimitSec = 0,
            featuredCarId = "scorpio",
            providesLoanerCar = false,
        ),
        MissionSpec(
            id = "tarbooz_express",
            number = 4,
            title = "Tarbooz (Watermelon) Express",
            hinglishBrief = "Garmi bahut tez hai! Lamborghini ki diggi mein 10 bade 3D Tarbooz laad kar gaon ke nukkad tak bina phode pahunchao!",
            objectiveHint = "Slow down on speed breakers! Deliver 10 intact watermelons to Gaon Nukkad.",
            rewardSwag = 220,
            rewardJholaId = "meetha_tarbooz",
            timeLimitSec = 110,
            featuredCarId = "lambo",
            providesLoanerCar = true,
        ),
        MissionSpec(
            id = "bhains_jam",
            number = 5,
            title = "Bhains (Buffalo) Traffic Jam",
            hinglishBrief = "Aage 3D bhainsein road block karke khadi hain! Gaadi se utar kar paedal jao, horn/taali bajao aur rasta clear karke gaadi aage badhao!",
            objectiveHint = "Exit car (Walk), walk near the buffaloes, tap TAALI/HORN to clear them, then drive through!",
            rewardSwag = 170,
            rewardJholaId = "desi_ghee",
            timeLimitSec = 0,
            featuredCarId = "scorpio",
            providesLoanerCar = false,
        ),
        MissionSpec(
            id = "gully_drive",
            number = 6,
            title = "Bina Scratch Ke Gully Drive",
            hinglishBrief = "Itni patli 3D gully mein luxury car ghusa di? Bina deewar se touch kiye gaadi bahar nikalo!",
            objectiveHint = "Steer gently out of the tight walled Johar Barha gully with 0 wall hits.",
            rewardSwag = 180,
            rewardJholaId = "aam_achar",
            timeLimitSec = 0,
            featuredCarId = "rolls",
            providesLoanerCar = true,
        ),
        MissionSpec(
            id = "puncture_jugaad",
            number = 7,
            title = "Puncture Wala Jugaad",
            hinglishBrief = "Tyre ki hawa kam ho gayi hai! 60 seconds mein gaon ke nukkad wali dukaan tak pahuncho!",
            objectiveHint = "Counter-steer the flat tire pull and reach the Puncture Shop within 60 seconds!",
            rewardSwag = 150,
            rewardJholaId = "malai_kulfi",
            timeLimitSec = 60,
            featuredCarId = "scorpio",
            providesLoanerCar = false,
        ),
        MissionSpec(
            id = "doston_geedi",
            number = 8,
            title = "Doston Ke Saath Geedi",
            hinglishBrief = "Poore Johar Barha ka ek 3D chakkar lagao aur 500 Swag Points kamao!",
            objectiveHint = "Hit all 6 scenic village checkpoints around Johar Barha!",
            rewardSwag = 500,
            rewardJholaId = "sarpanch_gamcha",
            timeLimitSec = 0,
            featuredCarId = "scorpio",
            providesLoanerCar = false,
        ),
    )

    val jholaItems: List<JholaCatalogItem> = listOf(
        JholaCatalogItem(
            id = "chana_bhaji",
            emoji = "\uD83C\uDF3F",
            name = "Taaza Chana Ki Bhaji",
            caption = "Shuddh desi tadka, khushboo poore Raisen jile mein!",
            unlockHint = "Complete: Sarpanch Ji Ki Chana Ki Bhaji",
        ),
        JholaCatalogItem(
            id = "meetha_tarbooz",
            emoji = "\uD83C\uDF49",
            name = "Meetha Tarbooz (Watermelon)",
            caption = "Bareli tehsil ka sabse laal aur meetha tarbooz!",
            unlockHint = "Complete: Tarbooz (Watermelon) Express",
        ),
        JholaCatalogItem(
            id = "kulhad_chai",
            emoji = "\u2615",
            name = "Kulhad Wali Adrak Chai",
            caption = "Thakaan mitane ka asli desi tonic!",
            unlockHint = "Complete: Chacha Ki Garam Chai",
        ),
        JholaCatalogItem(
            id = "aam_achar",
            emoji = "\uD83E\uDD6D",
            name = "Desi Aam Ka Achar",
            caption = "Dadi ke haath ka chatpata achar!",
            unlockHint = "Complete: Bina Scratch Ke Gully Drive",
        ),
        JholaCatalogItem(
            id = "desi_ghee",
            emoji = "\uD83E\uDD5B",
            name = "Shuddh Desi Ghee Ka Dabba",
            caption = "Pehalwani aur luxury driving dono ke liye zaroori!",
            unlockHint = "Complete: Bhains (Buffalo) Traffic Jam",
        ),
        JholaCatalogItem(
            id = "sarpanch_gamcha",
            emoji = "\uD83E\uDDE3",
            name = "Sarpanch Ji Ka Gamcha",
            caption = "Ab tum Johar Barha ke VIP ho!",
            unlockHint = "Complete: Baraat Entry or Doston Ke Saath Geedi",
        ),
        JholaCatalogItem(
            id = "malai_kulfi",
            emoji = "\uD83C\uDF68",
            name = "Malai Kulfi",
            caption = "Garmi mein thandak ka ehsaas!",
            unlockHint = "Complete: Puncture Wala Jugaad",
        ),
    )

    fun vehicleById(id: String): VehicleSpec =
        vehicles.firstOrNull { it.id == id } ?: vehicles.first()

    fun missionById(id: String): MissionSpec? =
        missions.firstOrNull { it.id == id }

    fun jholaById(id: String): JholaCatalogItem? =
        jholaItems.firstOrNull { it.id == id }
}
