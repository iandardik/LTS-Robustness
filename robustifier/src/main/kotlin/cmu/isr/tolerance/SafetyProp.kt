package cmu.isr.tolerance

import addPerturbations
import atLeastAsPowerful
import cmu.isr.ts.lts.*
import net.automatalib.util.automata.builders.AutomatonBuilders
import net.automatalib.words.Alphabet
import net.automatalib.words.impl.Alphabets
import product
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

fun deltaBruteForce(T : CompactLTS<String>, P : CompactDetLTS<String>) : Set<Set<Triple<Int,String,Int>>> {
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


fun main() {
    val T = AutomatonBuilders.newNFA(Alphabets.fromArray("a")) //, "b"))
        .withInitial(0)
        .from(0).on("a").to(1)
        //.from(1).on("b").to(0)
        //.from(0).on("a").to(0)
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
    /*
    val P2 = AutomatonBuilders.newDFA(Alphabets.fromArray("a", "b"))
        .withInitial(0)
        .from(0).on("a").to(1)
        .from(1).on("b").to(0)
        .withAccepting(0, 1)
        .create()
        .asLTS()
     */

    val delta = deltaBruteForce(T, P)

    println("#delta: ${delta.size}")
    for (d in delta) {
        println("  {${d.joinToString()}}")
    }
}