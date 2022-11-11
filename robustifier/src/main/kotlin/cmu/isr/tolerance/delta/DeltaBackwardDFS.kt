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
import incomingStates
import ltsTransitions
import outgoingStatesMap
import product
import reachableStates
import transClosureTable

class DeltaBackwardDFS(private val env : CompactLTS<String>,
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
    private val winningSet : Set<Pair<Int,Int>>

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
        winningSet = gfp(env, fNotFull, fNotFullNFA, qfMinusErr, qfMinusErr) intersect reachableStates(f, fNFA)
        outgoingStates = outgoingStatesMap(winningSet, f, fNFA)
        transClosures = transClosureTable(fNotFull, fNotFullNFA)
    }

    fun compute() : Set<Set<Triple<Int, String, Int>>> {
        val errorStates = f.getStates(fNFA.alphabet()) - winningSet
        val delta = DeltaBuilder(env, ctrl, prop)
        val visited = mutableSetOf<Set<Pair<Int,Int>>>()

        recCompute(errorStates, delta, visited, f.initialStates)
        return delta.toSet()
    }

    private fun recCompute(setErrRaw : Set<Pair<Int,Int>>,
                           delta : DeltaBuilder,
                           visited : MutableSet<Set<Pair<Int,Int>>>,
                           init : Set<Pair<Int,Int>>) {
        // add the transitive closure in from states outside of setErrRaw
        val setErr = fR
            .filter { !setErrRaw.contains(it.first) }
            .filter { (transClosures[it.first]?.intersect(setErrRaw))?.isNotEmpty() ?: false }
            .fold(setErrRaw) { acc,t -> acc + t.first }

        if (visited.contains(setErr) || !(winningSet-setErr).containsAll(init)) {
            return
        }
        visited.add(setErr)

        // compute delta
        val del = fR
            .filterTo(HashSet()) { !setErr.contains(it.first) && setErr.contains(it.third) }
            .map { Triple(it.first.first, it.second, it.third.first) }
        delta.add(allTransitions - del)

        val toExplore = (incomingStates(setErr, f, fNFA) - setErr) intersect winningSet
        if (toExplore.isNotEmpty()) {
            if (toExplore.size > 15) {
                println("Exploring set size: ${toExplore.size}")
            }
            powersetCompute(setErr, toExplore.toList(), delta, visited, init)
        }
    }

    private fun powersetCompute(setErr : Set<Pair<Int,Int>>,
                                toExplore : List<Pair<Int,Int>>,
                                delta : DeltaBuilder,
                                visited : MutableSet<Set<Pair<Int,Int>>>,
                                init : Set<Pair<Int,Int>>) {
        if (toExplore.isEmpty()) {
            recCompute(setErr, delta, visited, init)
        }
        else {
            val head = toExplore.first()
            val tail = toExplore.drop(1)
            val setKeep = setErr + head
            powersetCompute(setErr, tail, delta, visited, init)
            powersetCompute(setKeep, tail, delta, visited, init)
        }
    }
}
