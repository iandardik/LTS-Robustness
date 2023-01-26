package cmu.isr.tolerance.delta

import cmu.isr.tolerance.utils.*
import cmu.isr.ts.alphabet
import cmu.isr.ts.lts.CompactDetLTS
import cmu.isr.ts.lts.CompactLTS
import cmu.isr.ts.lts.makeErrorState
import cmu.isr.ts.nfa.NFAParallelComposition
import product

class DeltaDFS(private val env : CompactLTS<String>,
               private val ctrl : CompactLTS<String>,
               private val prop : CompactDetLTS<String>,
               private val verbose : Boolean = false) {

    private val metaCtrl : NFAParallelComposition<Int, Pair<Int,Int>, String>
    private val metaCtrlTransitions : Set<Triple<Pair<Int, Pair<Int, Int>>, String, Pair<Int, Pair<Int, Int>>>>
    private val envFullTransitions : Set<Triple<Int,String,Int>>
    private val winningSet : Set<Pair<Int, Pair<Int,Int>>>
    private val transClosureTable : Map<Pair<Int, Pair<Int,Int>>, Set<Pair<Int, Pair<Int,Int>>>>

    init {
        val envFull = copyLTSFull(env)
        val propErr = makeErrorState(copyLTS(prop))
        metaCtrl = NFAParallelComposition(envFull, NFAParallelComposition(ctrl, propErr))
        metaCtrlTransitions = ltsTransitions(metaCtrl)

        envFullTransitions = product(env.states, env.alphabet().toSet(), env.states)
            .filter { env.isAccepting(it.first) && env.isAccepting(it.third) }
            .toSet()

        val metaCtrlNotFull = NFAParallelComposition(env, NFAParallelComposition(ctrl, propErr))
        val allMetaCtrlStates = metaCtrlNotFull.states
            .filter { metaCtrl.isAccepting(it) }
            .toSet()
        winningSet = gfp(allMetaCtrlStates, metaCtrlNotFull) intersect reachableStates(metaCtrl)
        transClosureTable = metaCtrlNotFull.states
            .associateWith { reachableStates(metaCtrlNotFull, setOf(it)) }
        if (verbose) {
            println("#W: ${winningSet.size}")
        }
    }

    fun compute() : Set<Set<Triple<Int, String, Int>>> {
        val init = metaCtrl.initialStates
        val delta = DeltaBuilder(env, ctrl, prop)
        val visited = mutableSetOf<Set<Pair<Int, Pair<Int,Int>>>>()
        val initLevel = 0
        recCompute(init, initLevel, delta, visited)
        return delta.toSet()
    }

    private fun recCompute(setRaw : Set<Pair<Int, Pair<Int,Int>>>,
                           level : Int,
                           delta : DeltaBuilder,
                           visited : MutableSet<Set<Pair<Int, Pair<Int,Int>>>>) {
        val set = setRaw
            .mapNotNull { transClosureTable[it] }
            .fold(setRaw) { acc, e -> acc union e }

        if (visited.contains(set)) {
            return
        }
        visited.add(set)

        // compute delta
        val del = metaCtrlTransitions
            .filter { set.contains(it.first) && !set.contains(it.third) }
            .map { Triple(it.first.first, it.second, it.third.first) }
            .toSet()
        delta.add(envFullTransitions - del)

        val toExplore = (outgoingStates(set, metaCtrl) - set) intersect winningSet
        if (toExplore.isNotEmpty()) {
            if (verbose && toExplore.size > 15) {
                println("Exploring set size: ${toExplore.size}")
            }
            powersetCompute(set, toExplore.toList(), level, delta, visited)
        }
    }

    private fun powersetCompute(set : Set<Pair<Int, Pair<Int,Int>>>,
                                toExplore : List<Pair<Int, Pair<Int,Int>>>,
                                level : Int,
                                delta : DeltaBuilder,
                                visited : MutableSet<Set<Pair<Int, Pair<Int,Int>>>>) {
        if (toExplore.isEmpty()) {
            recCompute(set, level+1, delta, visited)
        } else {
            val head = toExplore.first()
            val tail = toExplore.drop(1)
            val setKeep = set + head
            powersetCompute(set, tail, level, delta, visited)
            powersetCompute(setKeep, tail, level, delta, visited)
        }
    }

}
