package cmu.isr.tolerance.utils

import cmu.isr.ts.alphabet
import cmu.isr.ts.lts.CompactDetLTS
import cmu.isr.ts.lts.CompactLTS
import cmu.isr.ts.lts.asLTS
import net.automatalib.automata.fsa.NFA
import net.automatalib.util.automata.builders.AutomatonBuilders

/**
 * It's ridiculous how tough they make it to copy.
 * The algorithm makes a key assumption: that the initial state in a DFA has the lowest state ID (presumably 0), and it
 * will be added to newLTS with the same lowest state ID.
 */
fun <I> copyLTS(lts : CompactDetLTS<I>) : CompactDetLTS<I> {
    val newDFA = AutomatonBuilders.newDFA(lts.inputAlphabet)
        .withInitial(lts.initialState)
        .create()
    for (s in lts.states) {
        if (lts.initialState == s) {
            newDFA.setAccepting(s, lts.isAccepting(s))
        }
        else {
            newDFA.addState(lts.isAccepting(s))
        }
    }
    for (t in ltsTransitions(lts)) {
        newDFA.addTransition(t.first, t.second, t.third)
    }
    return newDFA.asLTS()
}

/**
 * Makes a copy of a NFA (in particular, a CompactLTS). This algorithm does not rely on as many assumptions as the
 * DFA copyLTS, but it does assume that T.states iterates on the state IDs in order, and T.addInitialState() and
 * T.addState() add new state IDs to T in order.
 */
fun <I> copyLTS(lts : CompactLTS<I>) : CompactLTS<I> {
    val newNFA = AutomatonBuilders.newNFA(lts.inputAlphabet).create()
    for (s in lts.states) {
        if (lts.initialStates.contains(s)) {
            newNFA.addInitialState(lts.isAccepting(s))
        }
        else {
            newNFA.addState(lts.isAccepting(s))
        }
    }
    for (t in ltsTransitions(lts)) {
        newNFA.addTransition(t.first, t.second, t.third)
    }
    return newNFA.asLTS()
}

fun <I> copyLTSFull(lts : CompactLTS<I>) : CompactLTS<I> {
    val newNFA = AutomatonBuilders.newNFA(lts.inputAlphabet).create()
    for (s in lts.states) {
        if (lts.initialStates.contains(s)) {
            newNFA.addInitialState(lts.isAccepting(s))
        }
        else {
            newNFA.addState(lts.isAccepting(s))
        }
    }
    for (src in lts.states) {
        for (a in lts.alphabet()) {
            for (dst in lts.states) {
                newNFA.addTransition(src, a, dst)
            }
        }
    }
    return newNFA.asLTS()
}

fun <I> copyLTSAcceptingOnly(lts : NFA<Int, I>) : NFA<Int, I> {
    val newNFA = AutomatonBuilders.newNFA(lts.alphabet()).create()
    for (s in lts.states) {
        if (lts.isAccepting(s)) {
            if (lts.initialStates.contains(s)) {
                newNFA.addInitialState(lts.isAccepting(s))
            } else {
                newNFA.addState(lts.isAccepting(s))
            }
        }
    }
    for (t in ltsTransitions(lts)) {
        if (newNFA.states.contains(t.first) && newNFA.states.contains(t.third))
            newNFA.addTransition(t.first, t.second, t.third)
    }
    return newNFA
}
