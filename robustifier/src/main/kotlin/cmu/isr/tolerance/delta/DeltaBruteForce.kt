package cmu.isr.tolerance

import acceptingStates
import cmu.isr.ts.alphabet
import cmu.isr.ts.lts.CompactDetLTS
import cmu.isr.ts.lts.CompactLTS
import cmu.isr.ts.nfa.NFAParallelComposition
import cmu.isr.ts.parallel
import copyLTSFull
import gfp
import ltsTransitions
import powerset
import product
import reachableStates

fun deltaBruteForce(env : CompactLTS<String>,
                    ctrl : CompactLTS<String>,
                    prop : CompactDetLTS<String>)
                    : Set<Set<Triple<Int,String,Int>>> {
    val envFull = copyLTSFull(env)
    val ltsCCompP = parallel(ctrl, prop)
    val fNFA = parallel(envFull, ltsCCompP)
    val f = NFAParallelComposition(envFull, ltsCCompP)
    val fNotFull = NFAParallelComposition(env, ltsCCompP)
    val fNotFullNFA = parallel(env, ltsCCompP)
    val qfMinusErr = acceptingStates(f, fNFA, env, ltsCCompP)

    val winningSet = gfp(env, fNotFull, fNotFullNFA, qfMinusErr, qfMinusErr) intersect reachableStates(f)
    println("#W: ${winningSet.size}")

    val ltsECompC = parallel(env, ctrl)
    val envCompC = NFAParallelComposition(env, ctrl)
    val envR = ltsTransitions(envCompC, ltsECompC.alphabet())
        .map { Triple(it.first.first, it.second, it.third.first) }
        .toSet()
    val fR = ltsTransitions(f, fNFA.alphabet())
    val allTransitions = product(env.states, env.inputAlphabet.toSet(), env.states)
    val delta = DeltaBuilder(env, ctrl, prop)
    for (set in powerset(winningSet)) {
        val allSetTransitions = product(set, fNFA.alphabet().toSet(), set)
        val tR = fR.filter { allSetTransitions.contains(it) }
        val tRProjEnv = tR.map { Triple(it.first.first,it.second,it.third.first) }.toSet()

        val del = fR
            .filter { set.contains(it.first) && !set.contains(it.third) }
            .map { Triple(it.first.first, it.second, it.third.first) }
            .toSet()
        val deltaCandidate = allTransitions - del
        if (tRProjEnv.containsAll(envR) && deltaCandidate.containsAll(envR)) {
            delta.add(deltaCandidate)
        }
    }

    return delta.toSet()
}
