package cmu.isr.tolerance.utils

import cmu.isr.ts.*
import cmu.isr.ts.nfa.NFAParallelComposition
import net.automatalib.automata.fsa.NFA
import net.automatalib.words.Alphabet
import java.util.*

fun <S,I> outgoingStates(set : Set<S>, lts : NFA<S,I>) : Set<S> {
    val outgoing = mutableSetOf<S>()
    for (src in set) {
        for (a in lts.alphabet()) {
            outgoing.addAll(lts.getTransitions(src, a))
        }
    }
    return outgoing
}

fun <S,I> incomingStates(set : Set<S>, lts : NFA<S,I>) : Set<S> {
    val incoming = mutableSetOf<S>()
    for (src in lts.getStates(lts.alphabet())) {
        for (a in lts.alphabet()) {
            for (dst in lts.getTransitions(src, a)) {
                if (set.contains(dst)) {
                    incoming.add(src)
                }
            }
        }
    }
    return incoming
}

fun <S,I> reachableStates(lts : NFA<S,I>, init : Set<S> = lts.initialStates) : Set<S> {
    val reach = mutableSetOf<S>()
    val queue : Queue<S> = LinkedList()
    queue.addAll(init)
    while (queue.isNotEmpty()) {
        val src = queue.remove()
        if (!reach.contains(src)) {
            reach.add(src)
            for (a in lts.alphabet()) {
                for (dst in lts.getTransitions(src, a)) {
                    queue.add(dst)
                }
            }
        }
    }
    return reach
}

/**
 * Finds all transitions in T
 */
fun <S,I> ltsTransitions(lts : NFA<S, I>, alph : Alphabet<I> = lts.alphabet()) : Set<Triple<S,I,S>> {
    val transitions = mutableSetOf<Triple<S,I,S>>()
    for (src in lts.states) {
        for (a in alph) {
            for (dst in lts.getTransitions(src, a)) {
                transitions.add(Triple(src, a, dst))
            }
        }
    }
    return transitions
}

/**
 * Performs a greatest fixpoint computation on S
 */
fun <S,I> gfp(set : Set<S>, lts : NFA<S,I>) : Set<S> {
    val setPrime = set
        .filter { set.containsAll(outgoingStates(setOf(it), lts)) }
        .toSet()
    return if (setPrime == set) {
        set
    }
    else {
        gfp(setPrime, lts)
    }
}
