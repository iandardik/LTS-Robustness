package cmu.isr.tolerance.utils

import cmu.isr.tolerance.utils.copyLTS
import cmu.isr.tolerance.utils.ltsTransitions
import cmu.isr.ts.*
import cmu.isr.ts.lts.CompactDetLTS
import cmu.isr.ts.lts.asLTS
import cmu.isr.ts.lts.CompactLTS
import cmu.isr.ts.lts.checkSafety
import cmu.isr.ts.lts.makeErrorState
import cmu.isr.ts.nfa.NFAParallelComposition
import cmu.isr.ts.nfa.determinise
import com.fasterxml.jackson.databind.RuntimeJsonMappingException
import net.automatalib.automata.fsa.NFA
import net.automatalib.automata.fsa.impl.compact.CompactDFA
import net.automatalib.util.automata.builders.AutomatonBuilders
import net.automatalib.words.Word
import java.lang.RuntimeException
import java.util.*

object LtsUtils {
    /**
     * returns whether:
     * T |= P
     */
    fun satisfies(lts: LTS<Int, String>, prop: DetLTS<Int, String>): Boolean {
        val propCopy = copyLTS(prop as CompactDetLTS<String>)
        //val propCopy = prop as CompactDetLTS<String>
        val propErr = makeErrorState(propCopy)
        val result = checkSafety(lts, propErr)
        return !result.violation
    }

    fun errorTrace(lts: LTS<Int, String>, prop: DetLTS<Int, String>): Word<String> {
        val propCopy = copyLTS(prop as CompactDetLTS<String>)
        val propErr = makeErrorState(propCopy)
        val result = checkSafety(lts, propErr)
        assert(result.violation)
        return checkNotNull(result.trace)
    }

    fun <I> addPerturbations(lts: CompactLTS<I>, d: Set<Triple<Int, I, Int>>): CompactLTS<I> {
        val ltsD = copyLTS(lts)
        for (t in d) {
            ltsD.addTransition(t.first, t.second, t.third)
        }
        return ltsD
    }

    /**
     * Turns an NFA (T) into a DFA
     */
    fun <I> toDeterministic(lts: CompactLTS<I>): MutableDetLTS<Int, I> {
        val det = determinise(lts) as CompactDFA<I>
        return CompactDetLTS(det)
    }


    /**
     * Computes the cartesian product:
     *   Q X αT X Q
     * Presumably T is some LTS whose state space is Q, but this is not required.
     */
    fun <T> product(src: Collection<T>, alphabet: Set<String>, dst: Collection<T>): MutableSet<Triple<T, String, T>> {
        val perturbations: MutableSet<Triple<T, String, T>> = mutableSetOf()
        for (s in src) {
            for (a in alphabet) {
                for (d in dst) {
                    perturbations.add(Triple(s, a, d))
                }
            }
        }
        return perturbations
    }

    fun <I> dfaToNfa(lts: DetLTS<Int, I>): CompactLTS<I> {
        val newNFA = AutomatonBuilders.newNFA(lts.alphabet())
            .withInitial(lts.initialState)
            .create()
        for (s in lts.states) {
            if (lts.initialState == s) {
                newNFA.setAccepting(s, lts.isAccepting(s))
            } else {
                newNFA.addState(lts.isAccepting(s))
            }
        }
        for (t in ltsTransitions(lts)) {
            newNFA.addTransition(t.first, t.second, t.third)
        }
        return newNFA.asLTS()
    }

    fun parallelRestrict(ref: NFA<Int, String>, restr: NFA<Int, String>): NFA<Int, String> {
        val transitionsToKeep = mutableSetOf<Pair<Int, String>>()
        val comp = NFAParallelComposition(ref, restr)
        val nfaComp = parallel(ref, restr)
        for (state in comp.getStates(nfaComp.alphabet())) {
            val refState = state.first
            val restrState = state.second
            for (a in ref.alphabet()) {
                if (!(a in restr.alphabet() && restr.getTransitions(restrState, a).isEmpty())) {
                    transitionsToKeep.add(Pair(refState, a))
                }
            }
        }

        val newNFA = AutomatonBuilders.newNFA(ref.alphabet()).create()
        for (s in ref.states) {
            if (ref.initialStates.contains(s)) {
                newNFA.addInitialState(ref.isAccepting(s))
            } else {
                newNFA.addState(ref.isAccepting(s))
            }
        }
        for (t in ltsTransitions(ref)) {
            if (Pair(t.first, t.second) in transitionsToKeep) {
                newNFA.addTransition(t.first, t.second, t.third)
            }
        }
        return newNFA.asLTS()
    }
}