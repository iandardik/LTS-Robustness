package cmu.isr.tolerance

import addPerturbations
import atLeastAsPowerful
import cmu.isr.supervisory.supremica.write
import cmu.isr.ts.LTS
import cmu.isr.ts.alphabet
import cmu.isr.ts.lts.*
import cmu.isr.ts.nfa.NFAParallelComposition
import cmu.isr.ts.parallel
import copyLTSFull
import ltsTransitions
import net.automatalib.util.automata.builders.AutomatonBuilders
import net.automatalib.words.Alphabet
import net.automatalib.words.impl.Alphabets
import powerset
import product
import safe
import satisfies

fun allPerturbations(states : Collection<Int>, alphabet : Alphabet<String>) : Set<Set<Triple<Int, String, Int>>> {
    fun pertHelper(perturbations : MutableSet<MutableSet<Triple<Int,String,Int>>>,
                   powerset : Set<Triple<Int,String,Int>>) {
        if (powerset.isNotEmpty()) {
            val elem = powerset.first()
            pertHelper(perturbations, powerset - elem)

            val dPlusElems = mutableSetOf<MutableSet<Triple<Int, String, Int>>>()
            for (d in perturbations) {
                dPlusElems += (d + elem) as MutableSet<Triple<Int, String, Int>>
            }
            perturbations += dPlusElems
        }
    }
    val perturbations : MutableSet<MutableSet<Triple<Int,String,Int>>> = mutableSetOf(mutableSetOf())
    val powerset = product(states, alphabet.toMutableSet(), states)
    pertHelper(perturbations, powerset)
    return perturbations
}

fun deltaNaiveBruteForce(T : CompactLTS<String>, P : CompactDetLTS<String>) : Set<Set<Triple<Int,String,Int>>> {
    val delta = mutableSetOf<Set<Triple<Int,String,Int>>>()
    val QXActXQ = allPerturbations(T.states, T.inputAlphabet)

    for (d in QXActXQ) {
        val Td = addPerturbations(T, d)
        if (satisfies(Td, P)) {
            delta += d
        }
    }
    //println("#delta before: ${delta.size}")

    val toDelete = mutableSetOf<Set<Triple<Int,String,Int>>>()
    for (d2 in delta) {
        for (d1 in delta) {
            if (d1 != d2 && atLeastAsPowerful(T, d2, d1)) {
                toDelete.add(d2)
                break
            }
        }
    }
    delta.removeAll(toDelete)

    return delta
}

fun acceptingStates(F : NFAParallelComposition<Int, Int, String>,
                    nfaF : LTS<Int, String>,
                    E : CompactLTS<String>,
                    P : CompactDetLTS<String>)
                    : Set<Pair<Int, Int>> {
    return F.getStates(nfaF.alphabet())
        .filter { E.isAccepting(it.first) && P.isAccepting(it.second) }
        .toSet()
}

fun deltaBruteForce(E : CompactLTS<String>, P : CompactDetLTS<String>) : Set<Set<Triple<Int,String,Int>>> {
    val Efull = copyLTSFull(E)
    val nfaF = parallel(Efull, P)
    val F = NFAParallelComposition(Efull, P)
    val QfMinusErr = acceptingStates(F, nfaF, E, P)
    val W = safe(E, F, QfMinusErr)

    val Rf = ltsTransitions(F, nfaF.alphabet())
    val A = product(E.states, E.inputAlphabet.toSet(), E.states)
    val delta = mutableSetOf<Set<Triple<Int,String,Int>>>()
    for (S in powerset(W)) {
        val SxActxS = product(S, nfaF.alphabet().toSet(), S)
        val Rt = Rf.filter { SxActxS.contains(it) }
        val RtProjE = Rt.map { Triple(it.first.first,it.second,it.third.first) }.toSet()
        if (RtProjE.containsAll(ltsTransitions(E))) {
            val del = Rf
                .filter { S.contains(it.first) && !S.contains(it.third) }
                .map { Triple(it.first.first, it.second, it.third.first) }
                .toSet()
            delta.add(A - del)
        }
    }

    val toDelete = mutableSetOf<Set<Triple<Int,String,Int>>>()
    for (d2 in delta) {
        for (d1 in delta) {
            if (d1 != d2 && atLeastAsPowerful(E, d2, d1)) {
                toDelete.add(d2)
                break
            }
        }
    }
    delta.removeAll(toDelete)

    return delta
}

/**
 * Cuts down on the exponential in the runtime by |Re X Rc X Rp|. Produces the right answers for our simple example,
 * I'm not sure if it's correct in general though.
 */
fun deltaBruteForceShortcut(E : CompactLTS<String>, P : CompactDetLTS<String>) : Set<Set<Triple<Int,String,Int>>> {
    val Efull = copyLTSFull(E)
    val nfaF = parallel(Efull, P)
    val F = NFAParallelComposition(Efull, P)
    val QfMinusErr = acceptingStates(F, nfaF, E, P)
    val W = safe(E, F, QfMinusErr)

    val Rf = ltsTransitions(F, nfaF.alphabet())
    val A = product(E.states, E.inputAlphabet.toSet(), E.states)
    val delta = mutableSetOf<Set<Triple<Int,String,Int>>>()

    val QeInF = parallel(E, P).states

    for (partialS in powerset(W - QeInF)) {
        val S = partialS + QeInF
        //val S = partialS // this shouldn't be correct in general, and it's fishy that it works in our example...

        val SxActxS = product(S, nfaF.alphabet().toSet(), S)
        val Rt = Rf.filter { SxActxS.contains(it) }
        val RtProjE = Rt.map { Triple(it.first.first,it.second,it.third.first) }.toSet()
        if (RtProjE.containsAll(ltsTransitions(E))) {
            val del = Rf
                .filter { S.contains(it.first) && !S.contains(it.third) }
                .map { Triple(it.first.first, it.second, it.third.first) }
                .toSet()
            delta.add(A - del)
        }
    }

    val toDelete = mutableSetOf<Set<Triple<Int,String,Int>>>()
    for (d2 in delta) {
        for (d1 in delta) {
            if (d1 != d2 && atLeastAsPowerful(E, d2, d1)) {
                toDelete.add(d2)
                break
            }
        }
    }
    delta.removeAll(toDelete)

    return delta
}


fun main() {
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

    //write(System.out, T, T.alphabet())

    //val delta = deltaNaiveBruteForce(T, P)
    //val delta = deltaBruteForce(T, makeErrorState(P) as CompactDetLTS<String>)
    val delta = deltaBruteForceShortcut(T, makeErrorState(P) as CompactDetLTS<String>)

    println("#delta: ${delta.size}")
    for (d in delta) {
        println("  {${d.joinToString()}}")
    }
}