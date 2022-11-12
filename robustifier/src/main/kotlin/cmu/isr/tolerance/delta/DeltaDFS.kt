package cmu.isr.tolerance.delta

import acceptingStates
import cmu.isr.tolerance.DeltaBuilder
import cmu.isr.ts.LTS
import cmu.isr.ts.alphabet
import cmu.isr.ts.lts.CompactDetLTS
import cmu.isr.ts.lts.CompactLTS
import cmu.isr.ts.nfa.NFAParallelComposition
import cmu.isr.ts.parallel
import copyLTSFull
import gfp
import ltsTransitions
import outgoingStatesMap
import product
import reachableStates
import transClosureTable

class DeltaDFS(private val env : CompactLTS<String>,
               private val ctrl : CompactLTS<String>,
               private val prop : CompactDetLTS<String>) {
    private val allTransitions : Set<Triple<Int,String,Int>>
    private val f : NFAParallelComposition<Int, Int, String>
    private val fNFA : LTS<Int, String>
    private val fNotFull : NFAParallelComposition<Int, Int, String>
    private val fNotFullNFA : LTS<Int, String>
    private val fR : Set<Triple<Pair<Int, Int>, String, Pair<Int, Int>>>
    private val outgoingStates : Map<Pair<Int, Int>, Set<Pair<Int, Int>>>
    private val transClosures : Map<Pair<Int, Int>, Set<Pair<Int, Int>>>

    init {
        val envFull = copyLTSFull(env)
        val ltsCCompP = parallel(ctrl, prop)
        f = NFAParallelComposition(envFull, ltsCCompP)
        fNFA = parallel(envFull, ltsCCompP)
        fNotFull = NFAParallelComposition(env, ltsCCompP)
        fNotFullNFA = parallel(env, ltsCCompP)
        fR = ltsTransitions(f, fNFA.alphabet())
        allTransitions = product(env.states, env.inputAlphabet.toSet(), env.states)

        val qfMinusErr = acceptingStates(f, fNFA, env, ltsCCompP)
        val winningSet = gfp(env, fNotFull, fNotFullNFA, qfMinusErr, qfMinusErr) intersect reachableStates(f)
        outgoingStates = outgoingStatesMap(winningSet, f, fNFA)
        transClosures = transClosureTable(fNotFull, fNotFullNFA)
    }

    fun compute() : Set<Set<Triple<Int, String, Int>>> {
        val init = f.initialStates
        val delta = DeltaBuilder(env, ctrl, prop)
        val visited = mutableSetOf<Set<Pair<Int, Int>>>()
        recCompute(init, delta, visited)
        return delta.toSet()
    }

    private fun recCompute(setRaw : Set<Pair<Int, Int>>,
                           delta : DeltaBuilder,
                           visited : MutableSet<Set<Pair<Int, Int>>>) {
        val set = setRaw
            .mapNotNull { transClosures[it] }
            .fold(setRaw) { acc, e -> acc union e }

        if (visited.contains(set)) {
            return
        }
        visited.add(set)

        // compute delta
        val del = fR
            .filterTo(HashSet()) { set.contains(it.first) && !set.contains(it.third) }
            .map { Triple(it.first.first, it.second, it.third.first) }
        delta.add(allTransitions - del)

        //val toExplore = (outgoingStates(S, F, nfaF) - S) intersect W
        val toExplore = (set
            .mapNotNull { outgoingStates[it] }
            .fold(emptySet<Pair<Int, Int>>()) { acc, outgoing -> acc union outgoing }
                ) - set
        if (toExplore.isNotEmpty()) {
            if (toExplore.size > 15) {
                println("Exploring set size: ${toExplore.size}")
            }
            powersetCompute(set, toExplore.toList(), visited, delta)
        }
    }

    private fun powersetCompute(set: Set<Pair<Int, Int>>,
                                toExplore: List<Pair<Int, Int>>,
                                visited: MutableSet<Set<Pair<Int, Int>>>,
                                delta: DeltaBuilder) {
        if (toExplore.isEmpty()) {
            recCompute(set, delta, visited)
        } else {
            val head = toExplore.first()
            val tail = toExplore.drop(1)
            val setKeep = set + head
            powersetCompute(set, tail, visited, delta)
            powersetCompute(setKeep, tail, visited, delta)
        }
    }

}
