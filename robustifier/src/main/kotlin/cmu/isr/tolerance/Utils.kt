import cmu.isr.ts.LTS
import cmu.isr.ts.MutableDetLTS
import cmu.isr.ts.MutableLTS
import cmu.isr.ts.alphabet
import cmu.isr.ts.lts.CompactDetLTS
import cmu.isr.ts.lts.asLTS
import cmu.isr.ts.lts.CompactLTS
import cmu.isr.ts.lts.checkSafety
import cmu.isr.ts.lts.makeErrorState
import cmu.isr.ts.nfa.NFAParallelComposition
import cmu.isr.ts.nfa.determinise
import net.automatalib.automata.fsa.impl.compact.CompactDFA
import net.automatalib.util.automata.builders.AutomatonBuilders
import net.automatalib.words.Alphabet
import java.util.*

fun QfProjE(src : Pair<Int, Int>,
            E : LTS<Int, String>,
            F : NFAParallelComposition<Int, Int, String>)
        : Set<Pair<Int,Int>> {
    val proj = mutableSetOf<Pair<Int,Int>>()
    for (a in E.alphabet()) {
        for (dst in F.getStates(E.alphabet())) {
            if (F.getTransitions(src, a).contains(dst) && E.getTransitions(src.first, a).contains(dst.first))  {
                proj.add(dst)
            }
        }
    }
    return proj
}

fun safe(E : LTS<Int, String>,
         F : NFAParallelComposition<Int, Int, String>,
         CPre_iminus1 : Set<Pair<Int, Int>>)
        : Set<Pair<Int, Int>> {
    val CPre_i = F.getStates(E.alphabet()).filter { CPre_iminus1.containsAll(QfProjE(it, E, F)) }.toSet()
    //val CPre_i = CPre_iminus1.filter { CPre_iminus1.containsAll(QfProjE(it, E, F)) }.toSet()
    return if (CPre_i == CPre_iminus1) {
        CPre_i
    }
    else {
        safe(E, F, CPre_i).intersect(CPre_iminus1)
    }
}

/**
 * Finds all transitions in T
 */

fun ltsTransitions(T : LTS<Int, String>) : Set<Triple<Int,String,Int>> {
    val transitions = mutableSetOf<Triple<Int,String,Int>>()
    for (src in T) {
        for (a in T.alphabet()) {
            for (dst in T.getTransitions(src, a)) {
                transitions.add(Triple(src, a, dst))
            }
        }
    }
    return transitions

    /*
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
    */
}

fun ltsTransitions(T : NFAParallelComposition<Int, Int, String>, alph : Alphabet<String>) : Set<Triple<Pair<Int,Int>,String,Pair<Int,Int>>> {
    val transitions = mutableSetOf<Triple<Pair<Int,Int>,String,Pair<Int,Int>>>()
    for (src in T.getStates(alph)) {
        for (a in alph) {
            for (dst in T.getTransitions(src, a)) {
                transitions.add(Triple(src, a, dst))
            }
        }
    }
    return transitions
}

/**
 * It's ridiculous how tough they make it to copy.
 * The algorithm makes a key assumption: that the initial state in a DFA has the lowest state ID (presumably 0), and it
 * will be added to newLTS with the same lowest state ID.
 */
fun copyLTS(T : CompactDetLTS<String>) : CompactDetLTS<String> {
    val newDFA = AutomatonBuilders.newDFA(T.inputAlphabet)
        .withInitial(T.initialState)
        .create()
    for (s in T.states) {
        if (T.initialState == s) {
            newDFA.setAccepting(s, T.isAccepting(s))
        }
        else {
            newDFA.addState(T.isAccepting(s))
        }
    }
    for (t in ltsTransitions(T)) {
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

fun <T> powerset(s : Set<T>) : Set<Set<T>> {
    val ps = mutableSetOf<Set<T>>(emptySet())
    for (e in s) {
        val toAdd = mutableSetOf<Set<T>>()
        for (p in ps) {
            toAdd.add(p + e)
        }
        ps += toAdd
    }
    return ps
}
