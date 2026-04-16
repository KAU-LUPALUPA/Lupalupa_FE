package com.example.lupapj.ui.scene

data class DepthSortable<T>(
    val key: String,
    val sortDepth: Float,
    val value: T
)

fun <T> sortFloorRenderables(items: List<DepthSortable<T>>): List<DepthSortable<T>> {
    return items.sortedWith(
        compareBy<DepthSortable<T>> { it.sortDepth }
            .thenBy { it.key }
    )
}
