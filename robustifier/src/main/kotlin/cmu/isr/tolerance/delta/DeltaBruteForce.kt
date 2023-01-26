package cmu.isr.tolerance.delta

import cmu.isr.tolerance.utils.*
import cmu.isr.ts.DetLTS
import cmu.isr.ts.alphabet
import cmu.isr.ts.lts.CompactDetLTS
import cmu.isr.ts.lts.CompactLTS
import cmu.isr.ts.lts.makeErrorState
import cmu.isr.ts.nfa.NFAParallelComposition
import net.automatalib.util.automata.builders.AutomatonBuilders
import net.automatalib.words.impl.Alphabets
import product

fun deltaBruteForce(env : CompactLTS<String>,
                    ctrl : CompactLTS<String>,
                    prop : CompactDetLTS<String>)
        : Set<Set<Triple<Int,String,Int>>> {
    // set up
    val envFull = copyLTSFull(env)
    val propErr = makeErrorState(copyLTS(prop))
    val metaCtrl = NFAParallelComposition(envFull, NFAParallelComposition(ctrl, propErr))
    val metaCtrlTransitions = ltsTransitions(metaCtrl)

    val envFullTransitions = product(env.states, env.alphabet().toSet(), env.states)
        .filter { env.isAccepting(it.first) && env.isAccepting(it.third) }
        .toSet()

    val metaCtrlNotFull = NFAParallelComposition(env, NFAParallelComposition(ctrl, propErr))
    val allMetaCtrlStates = metaCtrlNotFull.states
        .filter { metaCtrl.isAccepting(it) }
        .toSet()
    val winningSet = gfp(allMetaCtrlStates, metaCtrlNotFull) intersect reachableStates(metaCtrl)
    println("#W: ${winningSet.size}")
    val transClosureTable = metaCtrlNotFull.states
        .associateWith { reachableStates(metaCtrlNotFull, setOf(it)) }

    // build delta
    val delta = DeltaBuilder(env, ctrl, prop)
    val visited = mutableSetOf<Set<Pair<Int, Pair<Int,Int>>>>()
    for (setRaw in powerset(winningSet)) {
        val transClosureSet = setRaw
            .mapNotNull { transClosureTable[it] }
            .fold(setRaw) { acc, e -> acc union e }
        val setInit = metaCtrl.initialStates intersect transClosureSet
        val setReach = reachableStates(metaCtrl, setInit)
        val set = transClosureSet intersect setReach

        if (set.isEmpty() || visited.contains(set)) {
            continue
        }
        visited.add(set)

        // compute delta
        val del = metaCtrlTransitions
            .filter { set.contains(it.first) && !set.contains(it.third) }
            .map { Triple(it.first.first, it.second, it.third.first) }
            .toSet()
        delta.add(envFullTransitions - del)
    }

    return delta.toSet()
}

fun deltaBruteForceEnvProp(env : CompactLTS<String>,
                           ctrl : CompactLTS<String>,
                           prop : CompactDetLTS<String>,
                           envProp : DetLTS<Int, String>)
        : Set<Set<Triple<Int,String,Int>>> {
    val emptyCtrl = AutomatonBuilders.newNFA(Alphabets.fromArray<String>())
        .withInitial(0)
        .withAccepting(0)
        .create()
    val emptyCtrlLts = CompactLTS<String>(emptyCtrl)
    val envPropDelta = deltaBruteForce(env, emptyCtrlLts, envProp as CompactDetLTS<String>)
    val propDelta = deltaBruteForce(env, ctrl, prop)

    val delta = mutableSetOf<Set<Triple<Int,String,Int>>>()
    for (epd in envPropDelta) {
        for (pd in propDelta) {
            delta.add(epd intersect pd)
        }
    }
    return delta
}
