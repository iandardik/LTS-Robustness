package cmu.isr.tolerance

import addPerturbations
import cmu.isr.ts.LTS
import cmu.isr.ts.lts.CompactDetLTS
import cmu.isr.ts.lts.CompactLTS
import cmu.isr.ts.parallel
import satisfies
import java.lang.RuntimeException

class DeltaBuilder(private val E : LTS<Int, String>, private val C : CompactLTS<String>, private val P : CompactDetLTS<String>) {
    private var delta = mutableSetOf<Set<Triple<Int,String,Int>>>()

    fun add(newD : Set<Triple<Int,String,Int>>) {
        /*
        // for debugging only
        val Ed = addPerturbations(E, newD)
        val EdComposeC = parallel(Ed, C)
        if (!satisfies(EdComposeC, P)) {
            throw RuntimeException("Trying to add unsafe set: $newD")
        }
         */

        var toDelete = mutableSetOf<Set<Triple<Int,String,Int>>>()
        for (d in delta) {
            if (d.containsAll(newD)) {
                return
            }
            if (newD.containsAll(d)) {
                toDelete.add(d)
            }
        }
        delta.removeAll(toDelete)
        delta.add(newD)
    }

    fun contains(e : Set<Triple<Int,String,Int>>) : Boolean {
        return delta.contains(e)
    }

    operator fun plusAssign(newD : Set<Triple<Int,String,Int>>) {
        add(newD)
    }

    fun toSet() : Set<Set<Triple<Int,String,Int>>> {
        return delta
    }
}

/*
private var deltaMap = mutableMapOf<Int, Set<Set<Triple<Int,String,Int>>>>()

fun add(newD : Set<Triple<Int,String,Int>>) {
    val sz = newD.size

    // check larger sets to see if newD is subsumed
    val largerEqKeys = deltaMap.keys.filter { it >= sz }
    for (k in largerEqKeys) {
        val szSlice = deltaMap[k] ?: throw RuntimeException("DeltaBuilder error")
        for (d in szSlice) {
            if (d.containsAll(newD)) {
                return
            }
        }
    }

    // if we've made it here then newD must be added. now we check all smaller sets
    // to see if they should be removed
    val smallerKeys = deltaMap.keys.filter { it < sz }
    for (k in smallerKeys) {
        val szSlice = deltaMap[k] ?: throw RuntimeException("DeltaBuilder error")
        val toRemove = mutableSetOf<Set<Triple<Int,String,Int>>>()
        for (d in szSlice) {
            if (newD.containsAll(d)) {
                toRemove.add(d)
            }
        }
        deltaMap[k] = szSlice - toRemove
    }
}

operator fun plusAssign(newD : Set<Triple<Int,String,Int>>) {
    add(newD)
}

fun toSet() : Set<Set<Triple<Int,String,Int>>> {
    return deltaMap.values.fold(emptySet()) { acc, next -> acc union next }
}
 */