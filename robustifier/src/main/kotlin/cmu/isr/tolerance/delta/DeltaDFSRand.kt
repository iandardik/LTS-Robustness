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
import makeMaximal
import outgoingStatesMap
import product
import randSubset
import reachableStates
import transClosureTable

class DeltaDFSRand(private val env : CompactLTS<String>,
                   private val ctrl : CompactLTS<String>,
                   private val prop : CompactDetLTS<String>) {
    private val allTransitions : Set<Triple<Int,String,Int>>
    private val allTransitionsArray : Array<Triple<Int,String,Int>>
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
        allTransitionsArray = allTransitions.toTypedArray()

        val qfMinusErr = acceptingStates(f, fNFA, env, ltsCCompP)
        val winningSet = gfp(env, fNotFull, fNotFullNFA, qfMinusErr, qfMinusErr) intersect reachableStates(f, fNFA)
        outgoingStates = outgoingStatesMap(winningSet, f, fNFA)
        transClosures = transClosureTable(fNotFull, fNotFullNFA)
    }

    fun compute() : Set<Set<Triple<Int, String, Int>>> {
        val init = f.initialStates
        val delta = DeltaBuilder(env, ctrl, prop)
        val visited = mutableSetOf<Set<Pair<Int, Int>>>()
        val initLevel = 0
        recCompute(init, initLevel, delta, visited)
        return delta.toSet()
    }

    private fun recCompute(setRaw : Set<Pair<Int, Int>>,
                           level : Int,
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
        allTransitionsArray.shuffle()
        val maximalElement = makeMaximal(allTransitions - del, allTransitionsArray, env, ctrl, prop)
        delta.add(maximalElement)

        val succ = set
            .mapNotNull { outgoingStates[it] }
            .fold(emptySet<Pair<Int,Int>>()) { acc,outgoing -> acc union outgoing }
        val outgoing = succ - set

        // target a constant number of edges to explore
        val avgNumEdges = 5.0 //3.0
        val rawP = avgNumEdges / outgoing.size.toDouble() //0.2 //0.03
        val p = rawP / Math.pow(2.0, level.toDouble())
        val toExplore = randSubset(outgoing, p)

        if (toExplore.isNotEmpty()) {
            if (toExplore.size > 15) {
                println("Exploring set size: ${toExplore.size}")
            }
            powersetCompute(set, toExplore.toList(), level, delta, visited)
        }
    }

    private fun powersetCompute(set : Set<Pair<Int, Int>>,
                                toExplore : List<Pair<Int, Int>>,
                                level : Int,
                                delta : DeltaBuilder,
                                visited : MutableSet<Set<Pair<Int, Int>>>) {
        if (toExplore.isEmpty()) {
            recCompute(set, level, delta, visited)
        } else {
            val head = toExplore.first()
            val tail = toExplore.drop(1)
            val setKeep = set + head
            powersetCompute(set, tail, level, delta, visited)
            powersetCompute(setKeep, tail, level, delta, visited)
        }
    }

}
