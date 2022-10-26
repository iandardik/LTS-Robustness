package cmu.isr.tolerance

import addPerturbations
import atLeastAsPowerful
import cmu.isr.assumption.SubsetConstructionGenerator
import cmu.isr.ts.LTS
import cmu.isr.ts.alphabet
import cmu.isr.ts.lts.*
import cmu.isr.ts.lts.ltsa.write
import cmu.isr.ts.nfa.NFAParallelComposition
import cmu.isr.ts.parallel
import containsSubsetOf
import copyLTS
import copyLTSAcceptingOnly
import copyLTSFull
import dfaToNfa
import divide
import elementwiseComplement
import errorStates
import fspToDFA
import fspToNFA
import gfp
import incomingStates
import isClosedWithRespectToTable
import isMaximal
import ltsTransitions
import net.automatalib.util.automata.builders.AutomatonBuilders
import net.automatalib.words.Alphabet
import net.automatalib.words.impl.Alphabets
import outgoingEdges
import outgoingStates
import outgoingStatesMap
import powerset
import product
import reachableStates
import safe
import satisfies
import stripTauTransitions
import subsetOfAMaximalStateSubset
import transClosureTable
import java.util.*
import kotlin.collections.HashMap

fun <T> allPerturbations(states : Collection<T>, alphabet : Alphabet<String>) : Set<Set<Triple<T, String, T>>> {
    fun pertHelper(perturbations : MutableSet<MutableSet<Triple<T,String,T>>>,
                   powerset : Set<Triple<T,String,T>>) {
        if (powerset.isNotEmpty()) {
            val elem = powerset.first()
            pertHelper(perturbations, powerset - elem)

            val dPlusElems = mutableSetOf<MutableSet<Triple<T, String, T>>>()
            for (d in perturbations) {
                dPlusElems += (d + elem) as MutableSet<Triple<T, String, T>>
            }
            perturbations += dPlusElems
        }
    }
    val perturbations : MutableSet<MutableSet<Triple<T,String,T>>> = mutableSetOf(mutableSetOf())
    val powerset = product(states, alphabet.toMutableSet(), states)
    pertHelper(perturbations, powerset)
    return perturbations
}

fun deltaNaiveBruteForce(E : CompactLTS<String>,
                         C : CompactLTS<String>,
                         P : CompactDetLTS<String>)
                        : Set<Set<Triple<Int,String,Int>>> {
    val delta = DeltaBuilder(E, C, P)
    val QXActXQ = allPerturbations(E.states, E.alphabet())

    for (d in QXActXQ) {
        val Ed = addPerturbations(E, d)
        val EdCompC = parallel(Ed, C)
        if (satisfies(EdCompC, P)) {
            delta += d
        }
    }
    //println("#delta before: ${delta.size}")

    return delta.toSet()
}

fun acceptingStates(F : NFAParallelComposition<Int, Int, String>,
                    nfaF : LTS<Int, String>,
                    E : LTS<Int, String>,
                    P : LTS<Int, String>)
                    : Set<Pair<Int, Int>> {
    return F.getStates(nfaF.alphabet())
        .filter { E.isAccepting(it.first) && P.isAccepting(it.second) }
        .toSet()
}

fun deltaBruteForce(E : CompactLTS<String>, C : CompactLTS<String>, P : CompactDetLTS<String>) : Set<Set<Triple<Int,String,Int>>> {
    val Efull = copyLTSFull(E)
    val ltsCCompP = parallel(C, P)
    val nfaF = parallel(Efull, ltsCCompP)
    val F = NFAParallelComposition(Efull, ltsCCompP)
    val QfMinusErr = acceptingStates(F, nfaF, E, ltsCCompP)

    // small optimization to W
    //val W = safe(E, F, QfMinusErr)
    val W = safe(E, F, QfMinusErr) intersect reachableStates(F, nfaF)
    println("#W: ${W.size}")

    //val Re = ltsTransitions(E)
    val ltsECompC = parallel(E, C)
    val ECompC = NFAParallelComposition(E, C)
    val Re = ltsTransitions(ECompC, ltsECompC.alphabet())
        .map { Triple(it.first.first, it.second, it.third.first) }
        .toSet()
    val Rf = ltsTransitions(F, nfaF.alphabet())
    val A = product(E.states, E.inputAlphabet.toSet(), E.states)
    val delta = DeltaBuilder(E, C, P)
    for (S in powerset(W)) {
        val SxActxS = product(S, nfaF.alphabet().toSet(), S)
        val Rt = Rf.filter { SxActxS.contains(it) }
        val RtProjE = Rt.map { Triple(it.first.first,it.second,it.third.first) }.toSet()

        val del = Rf
            .filter { S.contains(it.first) && !S.contains(it.third) }
            //.filter { S.contains(it.first) && (Rf - Rt).contains(it) }
            .map { Triple(it.first.first, it.second, it.third.first) }
            .toSet()
        val deltaCandidate = A - del
        if (RtProjE.containsAll(Re) && deltaCandidate.containsAll(Re)) {
            delta.add(deltaCandidate)
        }
    }

    return delta.toSet()
}

fun heuristicSubsets(W : Set<Pair<Int, Int>>,
                     F : NFAParallelComposition<Int,Int,String>,
                     nfaF : LTS<Int,String>,
                     F_notfull : NFAParallelComposition<Int,Int,String>,
                     nfaF_notfull : LTS<Int,String>)
                     : Set<Set<Pair<Int, Int>>> {
    val Rfnf = ltsTransitions(F_notfull, nfaF_notfull.alphabet())
    val nec = W intersect (Rfnf.map { it.first } union Rfnf.map { it.third })
    val init = nec intersect reachableStates(F_notfull, nfaF_notfull)
    //println("nec: $nec")
    val queue : Queue<Set<Pair<Int, Int>>> = LinkedList()
    queue.add(init)

    val subsets = mutableSetOf<Set<Pair<Int, Int>>>()
    while (queue.isNotEmpty()) {
        val S = queue.remove()
        if (!subsets.contains(S)) {
            subsets.add(S)
            val dstStates = (outgoingStates(S, F, nfaF) - S) intersect W
            //val dstNec = emptySet<Pair<Int,Int>>()
            //val dstNec = dstStates intersect nec
            for (additionalStates in powerset(dstStates)) {
                val Sprime = S union additionalStates
                queue.add(Sprime)
            }
        }
    }

    return subsets
}

fun deltaHeuristic(E : CompactLTS<String>, C : CompactLTS<String>, P : CompactDetLTS<String>) : Set<Set<Triple<Int,String,Int>>> {
    val Efull = copyLTSFull(E)
    val ltsCCompP = parallel(C, P)
    val nfaF = parallel(Efull, ltsCCompP)
    val F = NFAParallelComposition(Efull, ltsCCompP)
    val QfMinusErr = acceptingStates(F, nfaF, E, ltsCCompP)

    // small optimization to W
    //val W = safe(E, F, QfMinusErr)
    val W = safe(E, F, QfMinusErr) intersect reachableStates(F, nfaF)
    println("#W: ${W.size}")

    //val Re = ltsTransitions(E)
    val ltsECompC = parallel(E, C)
    val ECompC = NFAParallelComposition(E, C)
    val RecProjE = ltsTransitions(ECompC, ltsECompC.alphabet())
        .map { Triple(it.first.first, it.second, it.third.first) }
        .toSet()
    val Rf = ltsTransitions(F, nfaF.alphabet())
    val A = product(E.states, E.inputAlphabet.toSet(), E.states)
    val delta = DeltaBuilder(E, C, P)

    val F_notfull = NFAParallelComposition(E, ltsCCompP)
    val nfaF_notfull = parallel(E, ltsCCompP)
    val subsets = heuristicSubsets(W, F, nfaF, F_notfull, nfaF_notfull)
    //val subsets = powerset(W)
    println("#subsets/2^n: ${subsets.size} / ${Math.pow(2.0, W.size.toDouble())}")

    //println("Rf: ${Rf.joinToString { "$it\n" }}")
    //println()
    for (S in subsets) {
        val SxActxS = product(S, nfaF.alphabet().toSet(), S)
        val Rt = Rf.filter { SxActxS.contains(it) }
        val RtProjE = Rt.map { Triple(it.first.first,it.second,it.third.first) }.toSet()

        val del = Rf //(Rf - Rt)
            .filter { S.contains(it.first) && !S.contains(it.third) }
            .map { Triple(it.first.first, it.second, it.third.first) }
            .toSet()
        val deltaCandidate = A - del
        if (RtProjE.containsAll(RecProjE) && deltaCandidate.containsAll(RecProjE)) {
            delta.add(deltaCandidate)
        }
    }

    return delta.toSet()
}

fun deltaHeuristicMonolith(E : CompactLTS<String>, C : CompactLTS<String>, P : CompactDetLTS<String>) : Set<Set<Triple<Int,String,Int>>> {
    val Efull = copyLTSFull(E)
    val ltsCCompP = parallel(C, P)
    val nfaF = parallel(Efull, ltsCCompP)
    val F = NFAParallelComposition(Efull, ltsCCompP)
    val QfMinusErr = acceptingStates(F, nfaF, E, ltsCCompP)

    val W = safe(E, F, QfMinusErr) intersect reachableStates(F, nfaF)

    val ltsECompC = parallel(E, C)
    val ECompC = NFAParallelComposition(E, C)
    val RecProjE = ltsTransitions(ECompC, ltsECompC.alphabet())
        .map { Triple(it.first.first, it.second, it.third.first) }
        .toSet()
    val Rf = ltsTransitions(F, nfaF.alphabet())
    val A = product(E.states, E.inputAlphabet.toSet(), E.states)
    val delta = mutableSetOf<Set<Triple<Int,String,Int>>>()

    val F_notfull = NFAParallelComposition(E, ltsCCompP)
    val nfaF_notfull = parallel(E, ltsCCompP)
    val transClosures = transClosureTable(F_notfull, nfaF_notfull)

    val Rfnf = ltsTransitions(F_notfull, nfaF_notfull.alphabet())
    val nec = W intersect (Rfnf.map { it.first } union Rfnf.map { it.third })
    val init = nec intersect reachableStates(F_notfull, nfaF_notfull)
    val queue : Queue<Set<Pair<Int, Int>>> = LinkedList()
    queue.add(init)

    println("#W: ${W.size}")
    val visited = mutableSetOf(init)
    val maximumSubsets = mutableSetOf<Set<Pair<Int, Int>>>()
    while (queue.isNotEmpty()) {
        val S = queue.remove()
        if (!subsetOfAMaximalStateSubset(S, maximumSubsets)) {

            //////////////// begin S code ////////////////
            val SxActxS = product(S, nfaF.alphabet().toSet(), S)
            val Rt = Rf.filter { SxActxS.contains(it) }
            val RtProjE = Rt.map { Triple(it.first.first,it.second,it.third.first) }.toSet()

            val del = Rf
                .filter { S.contains(it.first) && !S.contains(it.third) }
                .map { Triple(it.first.first, it.second, it.third.first) }
                .toSet()
            val deltaCandidate = A - del
            if (RtProjE.containsAll(RecProjE) && deltaCandidate.containsAll(RecProjE)) {
                if (isMaximal(E, deltaCandidate, C, P, A)) {
                    delta.add(deltaCandidate)
                    maximumSubsets.add(S)
                }
            }
            //////////////// end S code ////////////////

            val dstStates = (outgoingStates(S, F, nfaF) - S) intersect W
            println("Computing powerset size: ${dstStates.size}")
            for (additionalStates in powerset(dstStates)) {
                val Sprime = S union additionalStates
                if (!visited.contains(Sprime) && isClosedWithRespectToTable(Sprime, transClosures)) {
                    queue.add(Sprime)
                    visited.add(Sprime)
                }
            }
        }
    }

    return delta
}

fun deltaBackwardsHeuristicMonolith(E : CompactLTS<String>, C : CompactLTS<String>, P : CompactDetLTS<String>) : Set<Set<Triple<Int,String,Int>>> {
    val Efull = copyLTSFull(E)
    val ltsCCompP = parallel(C, P)
    val nfaF = parallel(Efull, ltsCCompP)
    val F = NFAParallelComposition(Efull, ltsCCompP)
    val QfMinusErr = acceptingStates(F, nfaF, E, ltsCCompP)

    // small optimization to W
    //val W = safe(E, F, QfMinusErr)
    val Freach = reachableStates(F, nfaF)
    val W = safe(E, F, QfMinusErr) intersect Freach

    //val Re = ltsTransitions(E)
    val ltsECompC = parallel(E, C)
    val ECompC = NFAParallelComposition(E, C)
    val RecProjE = ltsTransitions(ECompC, ltsECompC.alphabet())
        .map { Triple(it.first.first, it.second, it.third.first) }
        .toSet()
    val Rf = ltsTransitions(F, nfaF.alphabet())
    val A = product(E.states, E.inputAlphabet.toSet(), E.states)
    //val delta = DeltaBuilder(E, C, P)
    val delta = mutableSetOf<Set<Triple<Int,String,Int>>>()

    val F_notfull = NFAParallelComposition(E, ltsCCompP)
    val nfaF_notfull = parallel(E, ltsCCompP)
    val transClosures = transClosureTable(F_notfull, nfaF_notfull)
    //val subsets = heuristicSubsets(W, F, nfaF, F_notfull, nfaF_notfull)
    //val subsets = powerset(W)
    //println("#subsets/2^n: ${subsets.size} / ${Math.pow(2.0, W.size.toDouble())}")
    //println("subsets: $subsets")

    val Rfnf = ltsTransitions(F_notfull, nfaF_notfull.alphabet())
    val nec = W intersect (Rfnf.map { it.first } union Rfnf.map { it.third })
    val init = nec intersect reachableStates(F_notfull, nfaF_notfull)
    //val err = errorStates(F, nfaF) // TODO add ALL error states, i.e. search thru F_notfull to make sure we get the closure of err states
    val err = Freach - W
    val queue : Queue<Set<Pair<Int, Int>>> = LinkedList()
    queue.add(err)
    val visited = mutableSetOf(err)
    val maximalStateSubets = mutableSetOf<Set<Pair<Int, Int>>>()

    println("#W: ${W.size}")
    //println("err: $err")
    while (queue.isNotEmpty()) {
        val Sneg = queue.remove()
        val S = W - Sneg
        if (!subsetOfAMaximalStateSubset(S, maximalStateSubets)) {
            //////////////// begin S code ////////////////
            val SxActxS = product(S, nfaF.alphabet().toSet(), S)
            val Rt = Rf.filter { SxActxS.contains(it) }
            val RtProjE = Rt.map { Triple(it.first.first,it.second,it.third.first) }.toSet()

            val del = Rf
                .filter { S.contains(it.first) && !S.contains(it.third) }
                .map { Triple(it.first.first, it.second, it.third.first) }
                .toSet()
            val deltaCandidate = A - del
            if (RtProjE.containsAll(RecProjE) && deltaCandidate.containsAll(RecProjE)) {
                if (isMaximal(E, deltaCandidate, C, P, A)) {
                    delta.add(deltaCandidate)
                    maximalStateSubets.add(S)
                }
            }
            //////////////// end S code ////////////////

            val srcStates = (incomingStates(Sneg, F, nfaF) - Sneg) intersect W
            println("powerset size: ${srcStates.size}")
            for (additionalStates in powerset(srcStates)) {
                val SnegPrime = Sneg union additionalStates
                val Sprime = W - SnegPrime
                //println("SnegPrime: $SnegPrime")
                //println("Sprime: $Sprime")
                if (!visited.contains(SnegPrime) && isClosedWithRespectToTable(Sprime, transClosures)) {
                    queue.add(SnegPrime)
                    visited.add(SnegPrime)
                }
            }
        }
    }
    //println("Rf: ${Rf.joinToString { "$it\n" }}")
    //println()

    //return delta.toSet()
    return delta
}

fun deltaDFSHelper(Sraw : Set<Pair<Int,Int>>,
                   delta : DeltaBuilder,
                   visited : MutableSet<Set<Pair<Int,Int>>>,
                   A : Set<Triple<Int,String,Int>>,
                   F : NFAParallelComposition<Int,Int,String>,
                   nfaF : LTS<Int,String>,
                   Rf : Set<Triple<Pair<Int,Int>, String, Pair<Int,Int>>>,
                   outgoingStates : Map<Pair<Int,Int>, Set<Pair<Int,Int>>>,
                   transClosures : Map<Pair<Int,Int>, Set<Pair<Int,Int>>>,
                   W : Set<Pair<Int,Int>>) {
    val S = Sraw
        .mapNotNull { transClosures[it] }
        .fold(Sraw) { acc, e -> acc union e }

    if (visited.contains(S)) {
        return
    }
    visited.add(S)

    // compute delta
    val del = Rf
        .filterTo(HashSet()) { S.contains(it.first) && !S.contains(it.third) }
        .map { Triple(it.first.first, it.second, it.third.first) }
    delta.add(A - del)

    //val toExplore = (outgoingStates(S, F, nfaF) - S) intersect W
    val toExplore = (S
        .mapNotNull { outgoingStates[it] }
        .fold(emptySet<Pair<Int,Int>>()) { acc,outgoing -> acc union outgoing }
        ) - S
    if (toExplore.isNotEmpty()) {
        if (toExplore.size > 15) {
            println("Exploring set size: ${toExplore.size}")
        }
        deltaDFSHelperRec(S, toExplore.toList(), visited, delta, A, F, nfaF, Rf, outgoingStates, transClosures, W)
    }
}
fun deltaDFSHelperRec(S : Set<Pair<Int,Int>>,
                      toExplore : List<Pair<Int,Int>>,
                      visited : MutableSet<Set<Pair<Int,Int>>>,
                      delta : DeltaBuilder,
                      A : Set<Triple<Int,String,Int>>,
                      F : NFAParallelComposition<Int,Int,String>,
                      nfaF : LTS<Int,String>,
                      Rf : Set<Triple<Pair<Int,Int>, String, Pair<Int,Int>>>,
                      outgoingStates : Map<Pair<Int,Int>, Set<Pair<Int,Int>>>,
                      transClosures : Map<Pair<Int,Int>, Set<Pair<Int,Int>>>,
                      W : Set<Pair<Int,Int>>) {
    if (toExplore.isEmpty()) {
        deltaDFSHelper(S, delta, visited, A, F, nfaF, Rf, outgoingStates, transClosures, W)
    }
    else {
        val head = toExplore.first()
        val tail = toExplore.drop(1)
        val Skeep = S + head
        deltaDFSHelperRec(S, tail, visited, delta, A, F, nfaF, Rf, outgoingStates, transClosures, W)
        deltaDFSHelperRec(Skeep, tail, visited, delta, A, F, nfaF, Rf, outgoingStates, transClosures, W)
    }
}

fun deltaDFS(E : CompactLTS<String>, C : CompactLTS<String>, P : CompactDetLTS<String>) : Set<Set<Triple<Int,String,Int>>> {
    val Efull = copyLTSFull(E)
    val ltsCCompP = parallel(C, P)
    val nfaF = parallel(Efull, ltsCCompP)
    val F = NFAParallelComposition(Efull, ltsCCompP)
    val F_notfull = NFAParallelComposition(E, ltsCCompP)
    val nfaF_notfull = parallel(E, ltsCCompP)

    val QfMinusErr = acceptingStates(F, nfaF, E, ltsCCompP)
    val W = gfp(E, F_notfull, nfaF_notfull, QfMinusErr, QfMinusErr) intersect reachableStates(F, nfaF)

    val Rf = ltsTransitions(F, nfaF.alphabet())
    val A = product(E.states, E.inputAlphabet.toSet(), E.states)

    val outgoingStates = outgoingStatesMap(W, F, nfaF)
    val transClosures = transClosureTable(F_notfull, nfaF_notfull)

    val init = F.initialStates
    val delta = DeltaBuilder(E, C, P)
    val visited = mutableSetOf<Set<Pair<Int,Int>>>()

    deltaDFSHelper(init, delta, visited, A, F, nfaF, Rf, outgoingStates, transClosures, W)

    return delta.toSet()
}


fun deltaBackwardDFSHelper(SerrRaw : Set<Pair<Int,Int>>,
                   delta : DeltaBuilder,
                   visited : MutableSet<Set<Pair<Int,Int>>>,
                   init : Set<Pair<Int,Int>>,
                   A : Set<Triple<Int,String,Int>>,
                   F : NFAParallelComposition<Int,Int,String>,
                   nfaF : LTS<Int,String>,
                   Rf : Set<Triple<Pair<Int,Int>, String, Pair<Int,Int>>>,
                   transClosures : Map<Pair<Int,Int>, Set<Pair<Int,Int>>>,
                   W : Set<Pair<Int,Int>>) {
    // add the transitive closure in from states outside of SerrRaw
    val Serr = Rf
        .filter { !SerrRaw.contains(it.first) }
        .filter { (transClosures[it.first]?.intersect(SerrRaw))?.isNotEmpty() ?: false }
        .fold(SerrRaw) { acc,t -> acc + t.first }

    if (visited.contains(Serr) || !(W-Serr).containsAll(init)) {
        return
    }
    visited.add(Serr)

    // compute delta
    val del = Rf
        .filterTo(HashSet()) { !Serr.contains(it.first) && Serr.contains(it.third) }
        .map { Triple(it.first.first, it.second, it.third.first) }
    delta.add(A - del)

    val toExplore = (incomingStates(Serr, F, nfaF) - Serr) intersect W
    if (toExplore.isNotEmpty()) {
        if (toExplore.size > 15) {
            println("Exploring set size: ${toExplore.size}")
        }
        deltaBackwardDFSHelperRec(Serr, toExplore.toList(), visited, init, delta, A, F, nfaF, Rf, transClosures, W)
    }
}
fun deltaBackwardDFSHelperRec(Serr : Set<Pair<Int,Int>>,
                      toExplore : List<Pair<Int,Int>>,
                      visited : MutableSet<Set<Pair<Int,Int>>>,
                      init : Set<Pair<Int,Int>>,
                      delta : DeltaBuilder,
                      A : Set<Triple<Int,String,Int>>,
                      F : NFAParallelComposition<Int,Int,String>,
                      nfaF : LTS<Int,String>,
                      Rf : Set<Triple<Pair<Int,Int>, String, Pair<Int,Int>>>,
                      transClosures : Map<Pair<Int,Int>, Set<Pair<Int,Int>>>,
                      W : Set<Pair<Int,Int>>) {
    if (toExplore.isEmpty()) {
        deltaBackwardDFSHelper(Serr, delta, visited, init, A, F, nfaF, Rf, transClosures, W)
    }
    else {
        val head = toExplore.first()
        val tail = toExplore.drop(1)
        val Skeep = Serr + head
        deltaBackwardDFSHelperRec(Serr, tail, visited, init, delta, A, F, nfaF, Rf, transClosures, W)
        deltaBackwardDFSHelperRec(Skeep, tail, visited, init, delta, A, F, nfaF, Rf, transClosures, W)
    }
}

fun deltaBackwardDFS(E : CompactLTS<String>, C : CompactLTS<String>, P : CompactDetLTS<String>) : Set<Set<Triple<Int,String,Int>>> {
    val Efull = copyLTSFull(E)
    val ltsCCompP = parallel(C, P)
    val nfaF = parallel(Efull, ltsCCompP)
    val F = NFAParallelComposition(Efull, ltsCCompP)
    val F_notfull = NFAParallelComposition(E, ltsCCompP)
    val nfaF_notfull = parallel(E, ltsCCompP)

    val QfMinusErr = acceptingStates(F, nfaF, E, ltsCCompP)
    val W = gfp(E, F_notfull, nfaF_notfull, QfMinusErr, QfMinusErr) intersect reachableStates(F, nfaF)

    val Rf = ltsTransitions(F, nfaF.alphabet())
    val A = product(E.states, E.inputAlphabet.toSet(), E.states)

    val transClosures = transClosureTable(F_notfull, nfaF_notfull)

    val errorStates = F.getStates(nfaF.alphabet()) - W
    val delta = DeltaBuilder(E, C, P)
    val visited = mutableSetOf<Set<Pair<Int,Int>>>()

    deltaBackwardDFSHelper(errorStates, delta, visited, F.initialStates, A, F, nfaF, Rf, transClosures, W)

    return delta.toSet()
}

fun actions(edges : Set<Triple<Pair<Int,Int>, String, Pair<Int,Int>>>) : Set<String> {
    return edges.mapTo(HashSet()) { it.second }
}

fun endStates(edges : Set<Triple<Pair<Int,Int>, String, Pair<Int,Int>>>) : Set<Pair<Int,Int>> {
    return edges.mapTo(HashSet()) { it.third }
}

fun deltaWAHelper(S : Set<Pair<Int,Int>>,
                  waTree : WATree,
                  delta : DeltaBuilder,
                  visited : MutableSet<Set<Pair<Int,Int>>>,
                  A : Set<Triple<Int,String,Int>>,
                  F : NFAParallelComposition<Int,Int,String>,
                  nfaF : LTS<Int,String>,
                  Rf : Set<Triple<Pair<Int,Int>, String, Pair<Int,Int>>>,
                  forcedByEnv : Map<Pair<Int,Int>, Set<Pair<Int,Int>>>,
                  transClosures : Map<Pair<Int,Int>, Set<Pair<Int,Int>>>,
                  W : Set<Pair<Int,Int>>) {

    if (visited.contains(S)) {
        return
    }
    visited.add(S)

    // compute delta
    val containsEnv = S.fold(true) { acc,s -> acc && (transClosures[s]?.let { S.containsAll(it) } ?: true) }
    if (containsEnv) {
        val del = Rf
            .filterTo(HashSet()) { S.contains(it.first) && !S.contains(it.third) }
            .map { Triple(it.first.first, it.second, it.third.first) }
        delta.add(A - del)
    }

    // compute next state sets to explore
    val allowedActions = waTree.actionsAtLevel()
    val outgoingEdges = outgoingEdges(S, F, nfaF)
        .filterTo(HashSet()) { !S.contains(it.third) }
        .filterTo(HashSet()) { W.contains(it.third) }
        .filterTo(HashSet()) { allowedActions.contains(it.second) }
    val necessaryEdges = outgoingEdges
        .filterTo(HashSet()) { forcedByEnv[it.first]?.contains(it.third) ?: false }
    val possibleEdges = outgoingEdges - necessaryEdges

    val toExplore = possibleEdges
    if (toExplore.size > 15) {
        println("Exploring set size: ${toExplore.size}")
    }
    val Sprime = S + endStates(necessaryEdges)
    deltaWAPowerset(Sprime, toExplore.toList(), emptySet(), actions(necessaryEdges), visited, waTree, delta, A, F, nfaF, Rf, forcedByEnv, transClosures, W)
}
fun deltaWAPowerset(S : Set<Pair<Int,Int>>,
                    toExplore : List<Triple<Pair<Int,Int>,String,Pair<Int,Int>>>,
                    willExplore : Set<Triple<Pair<Int,Int>,String,Pair<Int,Int>>>,
                    necessaryActions : Set<String>,
                    visited : MutableSet<Set<Pair<Int,Int>>>,
                    waTree : WATree,
                    delta : DeltaBuilder,
                    A : Set<Triple<Int,String,Int>>,
                    F : NFAParallelComposition<Int,Int,String>,
                    nfaF : LTS<Int,String>,
                    Rf : Set<Triple<Pair<Int,Int>, String, Pair<Int,Int>>>,
                    forcedByEnv : Map<Pair<Int,Int>, Set<Pair<Int,Int>>>,
                    transClosures : Map<Pair<Int,Int>, Set<Pair<Int,Int>>>,
                    W : Set<Pair<Int,Int>>) {
    if (toExplore.isEmpty()) {
        val newStates = endStates(willExplore)
        val chosenActions = actions(willExplore) union necessaryActions
        deltaWAHelper(S + newStates, waTree.nextLevel(chosenActions), delta, visited, A, F, nfaF, Rf, forcedByEnv, transClosures, W)
    }
    else {
        val head = toExplore.first()
        val tail = toExplore.drop(1)
        deltaWAPowerset(S, tail, willExplore, necessaryActions, visited, waTree, delta, A, F, nfaF, Rf, forcedByEnv, transClosures, W)
        deltaWAPowerset(S, tail, willExplore + head, necessaryActions, visited, waTree, delta, A, F, nfaF, Rf, forcedByEnv, transClosures, W)
    }
}

fun deltaWA(E : CompactLTS<String>, C : CompactLTS<String>, P : CompactDetLTS<String>) : Set<Set<Triple<Int,String,Int>>> {
    val Efull = copyLTSFull(E)
    val ltsCCompP = parallel(C, P)
    val nfaF = parallel(Efull, ltsCCompP)
    val F = NFAParallelComposition(Efull, ltsCCompP)
    val F_notfull = NFAParallelComposition(E, ltsCCompP)
    val nfaF_notfull = parallel(E, ltsCCompP)

    val QfMinusErr = acceptingStates(F, nfaF, E, ltsCCompP)
    val W = gfp(E, F_notfull, nfaF_notfull, QfMinusErr, QfMinusErr) intersect reachableStates(F, nfaF)

    val Rf = ltsTransitions(F, nfaF.alphabet())
    val A = product(E.states, E.inputAlphabet.toSet(), E.states)

    // we can add this optimization back in later
    //val outgoingStates = outgoingStatesMap(W, F, nfaF)

    val transClosures = transClosureTable(F_notfull, nfaF_notfull)
    val forcedByEnv = W.fold(HashMap<Pair<Int,Int>, Set<Pair<Int,Int>>>())
                        { acc,s ->  acc[s] = outgoingStates(setOf(s), F_notfull, nfaF_notfull); acc }

    val init = F.initialStates
    val delta = DeltaBuilder(E, C, P)
    val visited = mutableSetOf<Set<Pair<Int,Int>>>()

    val waGen = SubsetConstructionGenerator(C, E, P)
    val wa = waGen.generate()
    val waTree = WATree(wa)

    deltaWAHelper(init, waTree, delta, visited, A, F, nfaF, Rf, forcedByEnv, transClosures, W)

    return delta.toSet()
}

/*
fun deltaWA(Eorig : CompactLTS<String>, C : CompactLTS<String>, P : CompactDetLTS<String>) : Set<Set<Triple<Int,String,Int>>> {
    val waGen = SubsetConstructionGenerator(C, Eorig, P)
    val wa = waGen.generate()
    val E = dfaToNfa(wa)

    val Efull = copyLTSFull(E)
    val ltsCCompP = parallel(C, P)
    val nfaF = parallel(Efull, ltsCCompP)
    val F = NFAParallelComposition(Efull, ltsCCompP)
    val F_notfull = NFAParallelComposition(E, ltsCCompP)
    val nfaF_notfull = parallel(E, ltsCCompP)

    val QfMinusErr = acceptingStates(F, nfaF, E, ltsCCompP)
    val W = gfp(E, F_notfull, nfaF_notfull, QfMinusErr, QfMinusErr) intersect reachableStates(F, nfaF)

    val Rf = ltsTransitions(F, nfaF.alphabet())
    val A = product(E.states, E.alphabet().toSet(), E.states)

    val transClosures = transClosureTable(F_notfull, nfaF_notfull)

    val errorStates = F.getStates(nfaF.alphabet()) - W
    val delta = DeltaBuilder(E, C, P)
    val visited = mutableSetOf<Set<Pair<Int,Int>>>()

    deltaBackwardDFSHelper(errorStates, delta, visited, F.initialStates, A, F, nfaF, Rf, transClosures, W)

    return delta.toSet()
}
 */


fun main(args : Array<String>) {
    /*
    val T = AutomatonBuilders.newNFA(Alphabets.fromArray("a"))
        .withInitial(0)
        .from(0).on("a").to(1)
        .withAccepting(0, 1, 2)
        .create()
        .asLTS()
    val P = AutomatonBuilders.newDFA(Alphabets.fromArray("a"))
        .withInitial(0)
        .from(0).on("a").to(1)
        .from(1).on("a").to(2)
        //.from(0).on("b").to(0)
        .withAccepting(0, 1, 2)
        .create()
        .asLTS()
    write(System.out, P, P.alphabet())
    */

    if (args.size < 4) {
        println("usage: tolerance <alg> <env> <ctrl> <prop>")
        return
    }

    val alg = args[0]
    val E = stripTauTransitions(fspToNFA(args[1]))
    val C = stripTauTransitions(fspToNFA(args[2]))
    val P = fspToDFA(args[3])

    if (!satisfies(parallel(E,C), P)) {
        println("E||C does not satisfy P")
        return
    }

    val delta =
        when (alg) {
            "e" -> emptySet()
            "0" -> deltaNaiveBruteForce(E, C, P)
            "1" -> deltaBruteForce(E, C, P)
            "2" -> deltaHeuristic(E, C, P)
            "3" -> deltaHeuristicMonolith(E, C, P)
            "4" -> deltaBackwardsHeuristicMonolith(E, C, P)
            "5" -> deltaDFS(E, C, P)
            "6" -> deltaBackwardDFS(E, C, P)
            "7" -> deltaWA(E, C, P)
            else -> {
                println("Invalid algorithm")
                return
            }
        }

    // prints delta
    println("#delta: ${delta.size}")
    for (d in delta) {
        val sortedD = d.sortedWith { a, b ->
            if (a.second != b.second) {
                a.second.compareTo(b.second)
            }
            else if (a.first != b.first) {
                a.first - b.first
            }
            else {
                a.third - b.third
            }
        }
        println("  {${sortedD.joinToString()}}")
    }

    // print the FSP for each Ed
    for (d in delta) {
        val Ed = copyLTSAcceptingOnly(addPerturbations(E, d))
        //println()
        //write(System.out, Ed, Ed.alphabet())
    }

    // checks to make sure the solution is sound
    var sound = true
    for (d in delta) {
        val Ed = addPerturbations(E, d)
        val EdComposeC = parallel(Ed, C)
        if (!satisfies(EdComposeC, P)) {
            sound = false
            println("Violation for Ed||P |= P: $d")
        }
    }
    if (sound) {
        println()
        println("Solution is sound")
    }

    // checks to make sure every member of delta is maximal
    var maximal = true
    for (d in delta) {
        if (!isMaximal(E, d, C, P)) {
            maximal = false
            println("Found non-maximal d: $d")
        }
    }
    if (maximal) {
        println("All solutions are maximal")
    }

    /*
    val oneSol = deltaBruteForce(E, C, P)
    val correct = delta == oneSol
    println("Correct compared to 1? $correct")
    if (!correct) {
        println("Num missing from choice: ${(delta - oneSol).size}")
        println("Num missing from one sol: ${(oneSol - delta).size}")
    }
     */

    /*
    val waGen = SubsetConstructionGenerator(C, E, P)
    val wa = waGen.generate()
    println()
    println("WA:")
    write(System.out, wa, wa.alphabet())
    */
}
