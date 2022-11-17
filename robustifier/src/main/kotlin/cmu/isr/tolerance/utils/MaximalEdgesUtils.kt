package cmu.isr.tolerance.utils

import addPerturbations
import cmu.isr.ts.*
import cmu.isr.ts.lts.CompactDetLTS
import cmu.isr.ts.lts.CompactLTS
import product
import satisfies

fun makeMaximal(d : Set<Triple<Int,String,Int>>,
                allEdges : Array<Triple<Int,String,Int>>,
                env : CompactLTS<String>,
                ctrl : CompactLTS<String>,
                prop : CompactDetLTS<String>)
        : Set<Triple<Int,String,Int>> {
    val dMax = d.toMutableSet()
    for (e in allEdges) {
        val envD = addPerturbations(env, dMax + e)
        val envDCtrl = parallel(envD, ctrl)
        if (satisfies(envDCtrl, prop)) {
            dMax += e
        }
    }
    return dMax
}

fun makeMaximal(d : Set<Triple<Int,String,Int>>,
                allEdges : Array<Triple<Int,String,Int>>,
                env : CompactLTS<String>,
                ctrl : CompactLTS<String>,
                prop : CompactDetLTS<String>,
                envProp : DetLTS<Int, String>)
        : Set<Triple<Int,String,Int>> {
    val dMax = d.toMutableSet()
    for (e in allEdges) {
        val envD = addPerturbations(env, dMax + e)
        val envDCtrl = parallel(envD, ctrl)
        if (satisfies(envDCtrl, prop) && satisfies(envD, envProp)) {
            dMax += e
        }
    }
    return dMax
}

fun isMaximalAccepting(env : CompactLTS<String>,
                       ctrl : CompactLTS<String>,
                       prop : CompactDetLTS<String>)
        : Boolean {
    val allEdges = product(env.states, env.inputAlphabet.toSet(), env.states)
        .filter { env.isAccepting(it.first) && env.isAccepting(it.third) }
        .toSet()
    val unusedEdges = allEdges - ltsTransitions(env)
    for (e in unusedEdges) {
        val envE = addPerturbations(env, setOf(e))
        if (satisfies(parallel(envE, ctrl), prop)) {
            println("Can add edge: $e")
            return false
        }
    }
    return true
}
