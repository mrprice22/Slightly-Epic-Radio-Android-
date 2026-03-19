package com.slightlyepic.radio.data

data class Station(
    val id: Int,
    val title: String,
    val streamUrl: String,
    val streamFormat: String,
    val logoResName: String,
    val metaUrl: String?,
    val metaType: MetaType
)

enum class MetaType {
    ICECAST,
    SHOUTCAST,
    NONE
}

object StationRepository {

    val stations: List<Station> = listOf(
        Station(
            id = 0,
            title = "Slightly Epic Mashups",
            streamUrl = "https://a6.asurahosting.com:6520/radio.mp3",
            streamFormat = "mp3",
            logoResName = "slightly_epic_mashups",
            metaUrl = "https://a6.asurahosting.com:6520/status-json.xsl",
            metaType = MetaType.ICECAST
        ),
        Station(
            id = 1,
            title = "K.G.L.W. Bootlegger",
            streamUrl = "https://gizzradio.live/listen/listen/radio.mp3",
            streamFormat = "mp3",
            logoResName = "kglw_bootlegger",
            metaUrl = "https://gizzradio.live/listen/listen/status-json.xsl",
            metaType = MetaType.ICECAST
        ),
        Station(
            id = 2,
            title = "EDM Techno Forever",
            streamUrl = "http://ec1.yesstreaming.net:3500/stream",
            streamFormat = "mp3",
            logoResName = "edm_techno_forever",
            metaUrl = "http://ec1.yesstreaming.net:3500/status-json.xsl",
            metaType = MetaType.ICECAST
        ),
        Station(
            id = 3,
            title = "Electronic Dance Radio",
            streamUrl = "http://mpc1.mediacp.eu:18000/stream",
            streamFormat = "mp3",
            logoResName = "edr",
            metaUrl = "http://mpc1.mediacp.eu:18000/status-json.xsl",
            metaType = MetaType.ICECAST
        ),
        Station(
            id = 4,
            title = "Classical Public Domain Radio",
            streamUrl = "http://relay.publicdomainradio.org/classical.mp3",
            streamFormat = "mp3",
            logoResName = "classical_public_domain_radio",
            metaUrl = null,
            metaType = MetaType.NONE
        ),
        Station(
            id = 5,
            title = "Cafe HD",
            streamUrl = "http://live.playradio.org:9090/CafeHD",
            streamFormat = "aac",
            logoResName = "cafe_hd",
            metaUrl = "http://live.playradio.org:9090/status-json.xsl",
            metaType = MetaType.ICECAST
        ),
        Station(
            id = 6,
            title = "The Epic Channel",
            streamUrl = "http://fra-pioneer08.dedicateware.com:1100/stream",
            streamFormat = "mp3",
            logoResName = "the_epic_channel",
            metaUrl = "http://fra-pioneer08.dedicateware.com:1100/status-json.xsl",
            metaType = MetaType.ICECAST
        ),
        Station(
            id = 7,
            title = "Badlands Classic Rock",
            streamUrl = "http://ec3.yesstreaming.net:2040/stream",
            streamFormat = "aac",
            logoResName = "badlands_classic_rock",
            metaUrl = "http://ec3.yesstreaming.net:2040/status-json.xsl",
            metaType = MetaType.ICECAST
        ),
        Station(
            id = 8,
            title = "UTurn Classic Rock",
            streamUrl = "http://listen.uturnradio.com:7000/classic_rock",
            streamFormat = "mp3",
            logoResName = "uturn_classic_rock",
            metaUrl = "http://listen.uturnradio.com:7000/status-json.xsl",
            metaType = MetaType.ICECAST
        ),
        Station(
            id = 9,
            title = "Alternative",
            streamUrl = "http://stream.xrm.fm:8000/xrm-alt.aac",
            streamFormat = "aac",
            logoResName = "alternative",
            metaUrl = "http://stream.xrm.fm:8000/7.html",
            metaType = MetaType.SHOUTCAST
        ),
        Station(
            id = 10,
            title = "Stacey Radio",
            streamUrl = "http://stacey-campbell.com:8001/dadradio.mp3",
            streamFormat = "mp3",
            logoResName = "stacey_radio",
            metaUrl = "http://stacey-campbell.com:8001/status-json.xsl",
            metaType = MetaType.ICECAST
        ),
        Station(
            id = 11,
            title = "SOL FM",
            streamUrl = "http://radiosolfm.bounceme.net:8002/solfm",
            streamFormat = "aac",
            logoResName = "sol_fm",
            metaUrl = "http://radiosolfm.bounceme.net:8002/status-json.xsl",
            metaType = MetaType.ICECAST
        ),
        Station(
            id = 12,
            title = "Chill Lounge",
            streamUrl = "http://harddanceradio.ddns.is74.ru:8000/lounge",
            streamFormat = "aac",
            logoResName = "chill_lounge_radio",
            metaUrl = "http://harddanceradio.ddns.is74.ru:8000/status-json.xsl",
            metaType = MetaType.ICECAST
        ),
        Station(
            id = 13,
            title = "Time 2 Chill Radio",
            streamUrl = "http://ec6.yesstreaming.net:3610/stream",
            streamFormat = "mp3",
            logoResName = "time_2_chill_radio",
            metaUrl = "http://ec6.yesstreaming.net:3610/status-json.xsl",
            metaType = MetaType.ICECAST
        ),
        Station(
            id = 14,
            title = "Ultimate Chill (Pop)",
            streamUrl = "http://ec1.yesstreaming.net:3290/stream",
            streamFormat = "mp3",
            logoResName = "ultimate_chill_radio",
            metaUrl = "http://ec1.yesstreaming.net:3290/status-json.xsl",
            metaType = MetaType.ICECAST
        ),
        Station(
            id = 15,
            title = "Zen Garden",
            streamUrl = "https://kathy.torontocast.com:3250/stream",
            streamFormat = "mp3",
            logoResName = "zen_garden",
            metaUrl = "https://kathy.torontocast.com:3250/status-json.xsl",
            metaType = MetaType.ICECAST
        )
    )
}
