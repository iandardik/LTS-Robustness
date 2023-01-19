package cmu.isr.tolerance.utils

import org.junit.jupiter.api.Test

class SetUtilsTest {
    @Test
    fun toyTest() {
        val ans = setOf(emptySet(), setOf(1), setOf(2), setOf(4), setOf(1,2), setOf(2,4), setOf(1,4), setOf(1,2,4))

        val ints1 = setOf(1,2,4)
        val psInts1 = powerset(ints1)
        assert(ans == psInts1)

        val ints2 = setOf(2,1,4)
        val psInts2 = powerset(ints2)
        assert(ans == psInts2)

        val ints3 = setOf(1,1,4,2)
        val psInts3 = powerset(ints3)
        assert(ans == psInts3)
    }
}