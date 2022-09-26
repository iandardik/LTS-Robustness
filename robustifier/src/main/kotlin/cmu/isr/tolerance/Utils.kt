import cmu.isr.ts.LTS
import cmu.isr.ts.MutableDetLTS
import cmu.isr.ts.MutableLTS
import cmu.isr.ts.alphabet
import cmu.isr.ts.lts.CompactDetLTS
import cmu.isr.ts.lts.asLTS
import cmu.isr.ts.lts.CompactLTS
import cmu.isr.ts.lts.checkSafety
import cmu.isr.ts.lts.makeErrorState
import cmu.isr.ts.nfa.determinise
import net.automatalib.automata.fsa.impl.compact.CompactDFA
import net.automatalib.util.automata.builders.AutomatonBuilders
import net.automatalib.words.Alphabet
import java.util.*

/**
 * Finds all transitions in T
 */
fun ltsTransitions(T : LTS<Int, String>) : Set<Triple<Int,String,Int>> {
    val init = T.initialStates
    if (init.isEmpty()) {
        return emptySet()
    }

    // BFS for all transitions
    val transitions = mutableSetOf<Triple<Int,String,Int>>()
    val visited : MutableSet<Int> = mutableSetOf()
    val q : Queue<Int> = LinkedList()
    q.addAll(init)
    while (q.isNotEmpty()) {
        val state = q.remove()
        if (visited.contains(state)) {
            continue
        }
        visited.add(state)
        for (a in T.alphabet()) {
            val adjacent = T.getTransitions(state, a)
            for (dst in adjacent) {
                transitions.add(Triple(state, a, dst))
                q.add(dst)
            }
        }
    }

    return transitions
}

/**
 * It's ridiculous how tough they make it to copy.
 * The algorithm makes a key assumption: that initial states are the lowest state ID's, and will be added to newLTS
 * with the same lowest state IDs.
 */
fun copyLTS(T : CompactDetLTS<String>) : CompactDetLTS<String> {
    val newLTS = AutomatonBuilders.newDFA(T.inputAlphabet)
        .withInitial(T.initialStates)
        .create()
        .asLTS()
    for (s in T.states) {
        if (T.initialStates.contains(s)) {
            newLTS.setAccepting(s, T.isAccepting(s))
        }
        else {
            newLTS.addState(T.isAccepting(s))
        }
    }
    for (t in ltsTransitions(T)) {
        newLTS.addTransition(t.first, t.second, t.third)
    }
    return newLTS
}

/**
 * See @copyLTS above
 */
fun copyLTS(T : CompactLTS<String>) : CompactLTS<String> {
    val newLTS = AutomatonBuilders.newNFA(T.inputAlphabet)
        .withInitial(T.initialStates)
        .create()
        .asLTS()
    for (s in T.states) {
        if (T.initialStates.contains(s)) {
            newLTS.setAccepting(s, T.isAccepting(s))
        }
        else {
            newLTS.addState(T.isAccepting(s))
        }
    }
    for (t in ltsTransitions(T)) {
        newLTS.addTransition(t.first, t.second, t.third)
    }
    return newLTS
}

/**
 * Turns an NFA (T) into a DFA
 */
fun toDeterministic(T : CompactLTS<String>) : MutableDetLTS<Int, String> {
    val det = determinise(T) as CompactDFA<String>
    val detLts = CompactDetLTS(det)
    return detLts as MutableDetLTS<Int, String>
}

/**
 * returns whether:
 * T |= P
 */
fun satisfies(T : MutableLTS<Int, String>, P : MutableDetLTS<Int,String>) : Boolean {
    val pFixed = makeErrorState(P)
    val result = checkSafety(T, pFixed)
    return !result.violation
}

/**
 * Computes the cartesian product:
 *   Q X αT X Q
 * Presumably T is some LTS whose state space is Q, but this is not required.
 */
fun product(src : Collection<Int>, alphabet : Set<String>, dst : Collection<Int>) : MutableSet<Triple<Int, String, Int>> {
    val perturbations : MutableSet<Triple<Int,String,Int>> = mutableSetOf()
    for (s in src) {
        for (a in alphabet) {
            for (d in dst) {
                perturbations.add(Triple(s,a,d))
            }
        }
    }
    return perturbations
}

/**
 * Transforms an Alphabet into a mutable set of strings
 */
fun alphabetToSet(alphabet: Alphabet<String>) : MutableSet<String> {
    val set = mutableSetOf<String>()
    for (a in alphabet) {
        set.add(a)
    }
    return set
}
