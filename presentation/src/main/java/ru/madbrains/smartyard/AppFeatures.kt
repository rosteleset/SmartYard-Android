package ru.madbrains.smartyard

object AppFeatures {
    enum class Features {
        //основное меню
        MENU_ADDRESS,
        MENU_NOTIFICATIONS,
        MENU_CHAT,
        MENU_PAYMENT,
        MENU_ADDITIONAL,

        CCTV,
        EVENTS,
        FRS,
        CITY_CAMS,
        ISSUES
    }

    //доступные приложению фичи
    private var appFeatures: HashSet<Features> = hashSetOf(
        //для теста

        //меню
        Features.MENU_ADDRESS, Features.MENU_NOTIFICATIONS, Features.MENU_ADDITIONAL,

        Features.CCTV
    )

    fun hasFeature(feature: Features): Boolean {
        return appFeatures.contains(feature)
    }
}
