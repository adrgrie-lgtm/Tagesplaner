package com.example.ui.theme

import androidx.compose.ui.graphics.Color

data class MemberColorPalette(
    val primary: Color,
    val container: Color,
    val onContainer: Color
)

fun getMemberPalette(colorKey: String): MemberColorPalette {
    return when (colorKey.lowercase()) {
        "louis" -> MemberColorPalette(
            primary = LouisPrimary,
            container = LouisContainer,
            onContainer = LouisOnContainer
        )
        "leonie" -> MemberColorPalette(
            primary = LeoniePrimary,
            container = LeonieContainer,
            onContainer = LeonieOnContainer
        )
        "mama" -> MemberColorPalette(
            primary = MamaPrimary,
            container = MamaContainer,
            onContainer = MamaOnContainer
        )
        "papa" -> MemberColorPalette(
            primary = PapaPrimary,
            container = PapaContainer,
            onContainer = PapaOnContainer
        )
        else -> MemberColorPalette(
            primary = FamiliePrimary,
            container = FamilieContainer,
            onContainer = FamilieOnContainer
        )
    }
}
