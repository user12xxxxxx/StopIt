package com.nautesh.stopit

import org.junit.Assert.assertEquals
import org.junit.Test

class OrdinalTest {
    @Test
    fun suffixes() {
        assertEquals(
            listOf("1st", "2nd", "3rd", "4th", "11th", "12th", "13th", "21st", "22nd", "111th", "101st"),
            listOf(1, 2, 3, 4, 11, 12, 13, 21, 22, 111, 101).map(::ordinal),
        )
    }
}
