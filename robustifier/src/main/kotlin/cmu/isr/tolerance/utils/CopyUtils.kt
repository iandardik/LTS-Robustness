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
fun copyLTS(T : CompactLTS<String>) : CompactLTS<String> {
    val newNFA = AutomatonBuilders.newNFA(T.inputAlphabet).create()
    for (s in T.states) {
        if (T.initialStates.contains(s)) {
            newNFA.addInitialState(T.isAccepting(s))
        }
        else {
            newNFA.addState(T.isAccepting(s))
        }
    }
    for (t in ltsTransitions(T)) {
        newNFA.addTransition(t.first, t.second, t.third)
    }
    return newNFA.asLTS()
}

fun copyLTSFull(T : CompactLTS<String>) : CompactLTS<String> {
    val newNFA = AutomatonBuilders.newNFA(T.inputAlphabet).create()
    for (s in T.states) {
        if (T.initialStates.contains(s)) {
            newNFA.addInitialState(T.isAccepting(s))
        }
        else {
            newNFA.addState(T.isAccepting(s))
        }
    }
    for (src in T.states) {
        for (a in T.alphabet()) {
            for (dst in T.states) {
                newNFA.addTransition(src, a, dst)
            }
        }
    }
    return newNFA.asLTS()
}

fun copyLTSAcceptingOnly(T : NFA<Int, String>) : NFA<Int, String> {
    val newNFA = AutomatonBuilders.newNFA(T.alphabet()).create()
    for (s in T.states) {
        if (T.isAccepting(s)) {
            if (T.initialStates.contains(s)) {
                newNFA.addInitialState(T.isAccepting(s))
            } else {
                newNFA.addState(T.isAccepting(s))
            }
        }
    }
    for (t in ltsTransitions(T)) {
        if (newNFA.states.contains(t.first) && newNFA.states.contains(t.third))
            newNFA.addTransition(t.first, t.second, t.third)
    }
    return newNFA
}
