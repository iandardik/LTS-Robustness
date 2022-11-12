package cmu.isr.tolerance

import cmu.isr.tolerance.utils.ltsTransitions
import cmu.isr.tolerance.utils.makeMaximal
import cmu.isr.ts.lts.CompactDetLTS
import cmu.isr.ts.lts.CompactLTS
import product

const val defaultNumIters = 1000

fun deltaNaiveRand(env : CompactLTS<String>,
                   ctrl : CompactLTS<String>,
                   prop : CompactDetLTS<String>,
                   n : Int = defaultNumIters)
                   : Set<Set<Triple<Int,String,Int>>> {
    val Re = ltsTransitions(env)
        .filter { env.isAccepting(it.first) && env.isAccepting(it.third) }
        .toSet()
    val A = product(env.states, env.inputAlphabet.toSet(), env.states)
        .filter { env.isAccepting(it.first) && env.isAccepting(it.third) }
        .toSet() - Re
    val delta = DeltaBuilder(env, ctrl, prop)
    val Aarr = A.toTypedArray()
    for (i in 1..n) {
        Aarr.shuffle()
        val maximalElement = makeMaximal(Re, Aarr, env, ctrl, prop)
        delta.add(maximalElement)
    }
    return delta.toSet()
}