package com.nmims.careconnect.data.model

/**
 * Sorting options available for hospital and doctor search results.
 */
enum class SortOption(val label: String) {
    RATING_DESC("Top rated"),
    FEE_ASC("Fee: low to high"),
    FEE_DESC("Fee: high to low"),
    EXPERIENCE_DESC("Most experienced"),
    NAME_ASC("Name A-Z")
}
