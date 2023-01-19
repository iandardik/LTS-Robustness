package cmu.isr.tolerance

import addPerturbations
import cmu.isr.tolerance.delta.DeltaBuilder
import cmu.isr.tolerance.utils.powerset
import cmu.isr.ts.DetLTS
import cmu.isr.ts.alphabet
import cmu.isr.ts.lts.CompactDetLTS
import cmu.isr.ts.lts.CompactLTS
import cmu.isr.ts.parallel
import net.automatalib.words.Alphabet
import product
import satisfies

fun deltaNaiveBruteForce(env : CompactLTS<String>,
                         ctrl : CompactLTS<String>,
                         prop : CompactDetLTS<String>)
        : Set<Set<Triple<Int,String,Int>>> {
    val delta = DeltaBuilder(env, ctrl, prop)
    val stateSpace = product(env.states, env.alphabet().toSet(), env.states)
    val allPerts = powerset(stateSpace)
    for (d in allPerts) {
        val envD = addPerturbations(env, d)
        val envDCompC = parallel(envD, ctrl)
        if (satisfies(envDCompC, prop)) {
            delta += d
        }
    }
    return delta.toSet()
}

fun deltaNaiveBruteForceEnvProp(env : CompactLTS<String>,
                                ctrl : CompactLTS<String>,
                                prop : CompactDetLTS<String>,
                                envProp : DetLTS<Int, String>)
        : Set<Set<Triple<Int,String,Int>>> {
    val delta = DeltaBuilder(env, ctrl, prop)
    val stateSpace = product(env.states, env.alphabet().toSet(), env.states)
    val allPerts = powerset(stateSpace)
    for (d in allPerts) {
        val envD = addPerturbations(env, d)
        val envDCompC = parallel(envD, ctrl)
        if (satisfies(envDCompC, prop) && satisfies(envD, envProp)) {
            delta += d
        }
    }
    return delta.toSet()
}
