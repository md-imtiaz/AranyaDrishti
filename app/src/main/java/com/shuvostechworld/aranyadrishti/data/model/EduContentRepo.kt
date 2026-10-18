package com.shuvostechworld.aranyadrishti.data.model

data class EduTopic(
    val topicName: String,
    val imageQuery: String,
    val offlineTitle: String,
    val offlineDesc: String,
    val fallbackResId: Int
)

object EduContentRepo {
    val topics = listOf(
        EduTopic(
            topicName = "Elephant encounters",
            imageQuery = "wild elephant",
            offlineTitle = "Respect Elephant Space",
            offlineDesc = "If you encounter a wild elephant, stay calm, maintain distance, and slowly back away without making sudden noises.",
            fallbackResId = android.R.drawable.ic_menu_gallery
        ),
        EduTopic(
            topicName = "Snake safety",
            imageQuery = "wild snake",
            offlineTitle = "Snake Safety",
            offlineDesc = "Most snakes avoid humans. If spotted, stand still and let it pass; never try to hit or capture it.",
            fallbackResId = android.R.drawable.ic_menu_gallery
        ),
        EduTopic(
            topicName = "Plastic pollution",
            imageQuery = "nature plastic waste",
            offlineTitle = "Leave No Trace",
            offlineDesc = "Plastic waste harms wildlife. Carry out all your trash and help keep our forests pristine.",
            fallbackResId = android.R.drawable.ic_menu_gallery
        ),
        EduTopic(
            topicName = "Forest fires",
            imageQuery = "forest fire",
            offlineTitle = "Prevent Forest Fires",
            offlineDesc = "Never leave campfires unattended and always ensure they are fully extinguished with water and soil.",
            fallbackResId = android.R.drawable.ic_menu_camera
        ),
        EduTopic(
            topicName = "Monkey encounters",
            imageQuery = "wild monkey",
            offlineTitle = "Monkey Etiquette",
            offlineDesc = "Do not feed or touch wild monkeys. Keep your food hidden and avoid direct eye contact to prevent aggression.",
            fallbackResId = android.R.drawable.picture_frame
        ),
        EduTopic(
            topicName = "Big Cat sightings",
            imageQuery = "wild tiger leopard",
            offlineTitle = "Big Cat Alert",
            offlineDesc = "Never run from a big cat. Stand tall, maintain eye contact, and back away slowly while making yourself look large.",
            fallbackResId = android.R.drawable.ic_menu_agenda
        )
    )
}

