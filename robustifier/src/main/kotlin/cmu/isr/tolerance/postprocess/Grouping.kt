package cmu.isr.tolerance.postprocess

import addPerturbations
import cmu.isr.ts.LTS
import cmu.isr.ts.lts.CompactDetLTS
import cmu.isr.ts.lts.CompactLTS
import cmu.isr.ts.nfa.determinise
import cmu.isr.ts.parallel
import net.automatalib.automata.fsa.impl.compact.CompactDFA
import satisfies

fun filterControlledDuplicates(delta : Set<Set<Triple<Int,String,Int>>>,
                               env : CompactLTS<String>,
                               ctrl : CompactLTS<String>)
        : Set<Set<Triple<Int,String,Int>>> {
    val controlledDelta = delta.associateWith { parallel(addPerturbations(env,it), ctrl) }
    val ls = delta.toMutableList()
    val toRemove = mutableSetOf<Int>()
    for (i in 0 until ls.size) {
        if (i in toRemove) {
            continue
        }
        val di = ls[i]
        val cdi = controlledDelta[di] ?: throw RuntimeException("cdi bug")
        val cdiDet = CompactDetLTS(determinise(cdi) as CompactDFA<String>)
        for (j in i+1 until ls.size) {
            if (j in toRemove) {
                continue
            }
            val dj = ls[j]
            val cdj = controlledDelta[dj] ?: throw RuntimeException("cdj bug")
            val cdjDet = CompactDetLTS(determinise(cdj) as CompactDFA<String>)
            if (satisfies(cdi, cdjDet)) {
                toRemove.add(j)
            }
            else if (satisfies(cdj, cdiDet)) {
                toRemove.add(i)
            }

            /*
            if (satisfies(cdi, cdjDet) && satisfies(cdj, cdiDet)) {
                toRemove.add(j)
            }
             */
            // only keep maximal behaviors, JDEDS style
            /*
            else if (satisfies(cdi, cdjDet)) {
                toRemove.add(i)
            }
            else if (satisfies(cdj, cdiDet)) {
                toRemove.add(j)
            }
             */
        }
    }
    return ls.filterIndexedTo(HashSet()) { i,_ -> !toRemove.contains(i) }
}

fun bucketControlledDuplicates(delta : Set<Set<Triple<Int,String,Int>>>,
                               env : CompactLTS<String>,
                               ctrl : CompactLTS<String>)
        : Set<Set<Set<Triple<Int,String,Int>>>> {
    val controlledDelta = delta.associateWith { parallel(addPerturbations(env,it), ctrl) }
    val buckets = mutableMapOf<Pair<LTS<Int, String>,CompactDetLTS<String>>, Set<Set<Triple<Int,String,Int>>>>()
    for (d in delta) {
        val cd = controlledDelta[d] ?: throw RuntimeException("cdi bug")
        val cdDet = CompactDetLTS(determinise(cd) as CompactDFA<String>)
        var foundBucket = false
        for (k in buckets.keys) {
            val dk = k.first
            val dkDet = k.second
            if (satisfies(cd, dkDet) && satisfies(dk, cdDet)) {
                val bucketk = buckets[k] ?: throw RuntimeException("bucket error")
                buckets[k] = bucketk union setOf(d)
                foundBucket = true
                break
            }
        }
        if (!foundBucket) {
            val k = Pair(cd,cdDet)
            buckets[k] = setOf(d)
        }
    }
    return buckets.values.toSet()
}
