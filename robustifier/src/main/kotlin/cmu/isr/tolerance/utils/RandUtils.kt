package cmu.isr.tolerance.utils

import java.util.Random

val randGen = Random()

fun <T> randSubset(set : Set<T>, prob : Double) : Set<T> {
    val subset = mutableSetOf<T>()
    for (e in set) {
        if (randGen.nextDouble() <= prob) {
            subset.add(e)
        }
    }
    return subset
}
