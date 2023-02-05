package cmu.isr.tolerance.delta

import addPerturbations
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

typealias MetaState = Pair<Int, Pair<Int,Int>>

class DeltaDFSEnvProp(private val env : CompactLTS<String>,
                      private val ctrl : CompactLTS<String>,
                      private val prop : CompactDetLTS<String>,
                      envProp : DetLTS<Int, String>,
                      private val verbose : Boolean = false) {

    private val metaCtrl : NFAParallelComposition<Int, Pair<Int,Int>, String>
    private val metaCtrlTransitions : Set<Triple<MetaState, String, MetaState>>
    private val envFullTransitions : Set<Triple<Int,String,Int>>
    private val winningSet : Set<MetaState>
    private val transClosureTable : Map<MetaState, Set<MetaState>>
    private val envPropDelta : Set<Set<Triple<Int,String,Int>>>
    private val envPropWinningSets : Set<Set<MetaState>>

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
            .filter { metaCtrlNotFull.isAccepting(it) }
            .toSet()
        winningSet = gfp(allMetaCtrlStates, metaCtrlNotFull) intersect reachableStates(metaCtrl)
        transClosureTable = metaCtrlNotFull.states
            .associateWith { reachableStates(metaCtrlNotFull, setOf(it)) }

        if (verbose) {
            println("Calc'ing envProp transitions...")
        }
        val emptyCtrl = AutomatonBuilders.newNFA(Alphabets.fromArray<String>())
            .withInitial(0)
            .withAccepting(0)
            .create()
        val emptyCtrlLts = CompactLTS<String>(emptyCtrl)
        envPropDelta = DeltaDFS(env, emptyCtrlLts, envProp as CompactDetLTS<String>, false).compute()
        envPropWinningSets = envPropDelta
            .map { d ->
                val dEnvFull = addPerturbations(env, d)
                val dMetaCtrl = NFAParallelComposition(dEnvFull, NFAParallelComposition(ctrl, propErr))
                val dWinningSet = winningSet intersect reachableStates(dMetaCtrl)
                dWinningSet
            }
            .toSet()
        if (verbose) {
            println("#alpha: ${metaCtrl.alphabet().size}")
            println("#W: ${winningSet.size}")
            envPropWinningSets
                .forEach { w ->
                    println("#epw: ${w.size}")
                }
            println("Done calc'ing envProp transitions")
        }
    }

    fun compute() : Set<Set<Triple<Int, String, Int>>> {
        val init = metaCtrl.initialStates
        val delta = DeltaBuilder(env, ctrl, prop)
        val visited = mutableSetOf<Set<MetaState>>()
        recCompute(init, delta, visited)
        return delta.toSet()
    }

    private fun recCompute(setRaw : Set<MetaState>,
                           delta : DeltaBuilder,
                           visited : MutableSet<Set<MetaState>>) {
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
        val safeTransitions = envFullTransitions - del
        envPropDelta
            .map { safeTransitions intersect it }
            .forEach { delta.add(it) }

        // compute next set of states to explore
        val succ = outgoingStates(set, metaCtrl) - set
        val succEnvPropSafe = envPropWinningSets
            .map { reach -> reach intersect succ }
            .toSet()
        val toExploreList = removeSubsetDuplicates(succEnvPropSafe)

        for (toExplore in toExploreList) {
            if (verbose && toExplore.size > 15) {
                println("Exploring set size: ${toExplore.size}")
            }
            powersetCompute(set, toExplore.toList(), delta, visited)
        }
    }

    private fun powersetCompute(set : Set<MetaState>,
                                toExplore : List<MetaState>,
                                delta : DeltaBuilder,
                                visited : MutableSet<Set<MetaState>>) {
        if (toExplore.isEmpty()) {
            recCompute(set, delta, visited)
        } else {
            val head = toExplore.first()
            val tail = toExplore.drop(1)
            val setKeep = set + head
            powersetCompute(set, tail, delta, visited)
            powersetCompute(setKeep, tail, delta, visited)
        }
    }
}
