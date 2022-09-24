package cmu.isr.tolerance

import cmu.isr.ts.*
import cmu.isr.ts.lts.*
import cmu.isr.ts.lts.ltsa.write
import cmu.isr.ts.nfa.determinise
import net.automatalib.automata.fsa.impl.compact.CompactDFA
import net.automatalib.util.automata.Automata
import net.automatalib.util.automata.builders.AutomatonBuilders
import net.automatalib.words.Alphabet
import net.automatalib.words.impl.Alphabets
import java.util.*

class Path(path : LinkedList<String> = LinkedList<String>()) {
    val impl : LinkedList<String> = path
}

fun append(path : Path, last : String) : Path {
    val newList = LinkedList<String>()
    for (e in path.impl) {
        newList.add(e)
    }
    newList.add(last)
    return Path(newList)
}

fun findErrors(m : CompactDetLTS<String>) : Path? {
    val init = m.initialState
    checkNotNull(init)
    val alph = m.inputAlphabet

    // BFS for an error state
    val visited : MutableSet<Int> = mutableSetOf()
    val q : Queue<Pair<Int, Path>> = LinkedList<Pair<Int, Path>>()
    q.add(Pair(init, Path()))
    while (q.isNotEmpty()) {
        val (state, path) = q.remove()
        if (visited.contains(state)) {
            continue
        }
        if (m.isErrorState(state)) {
            return path
        }
        // keep searching for an error state
        visited.add(state)
        for (a in alph) {
            val t : Int? = m.getTransition(state, a)
            if (t != null) {
                val newPath = append(path, a)
                q.add(Pair(t, newPath))
            }
        }
    }
    // no error state found
    return null
}

/**
 * As of now, this function will mutate the arguments. However, we *should* put them back to their original
 * state, if I did this correctly.
 */
/*
fun satisfies(m : CompactDetLTS<String>, p : CompactDetLTS<String>) : Boolean {
    p.flipAcceptance()
    val intersect = parallel(m, p) as CompactDetLTS<String>
    val path : Path? = findErrors(intersect)

    // put p back to the way the callee had it
    p.flipAcceptance()
    return path == null
}
 */

fun satisfies(T : MutableLTS<Int,String>, P : MutableDetLTS<Int,String>) : Boolean {
    val pFixed = makeErrorState(P)
    val result = checkSafety(T, pFixed)
    return !result.violation
}

fun allPerturbations(states : Collection<Int>, alphabet : Alphabet<String>) : Set<Set<Triple<Int, String, Int>>> {
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
    fun pertHelper(perturbations : MutableSet<MutableSet<Triple<Int,String,Int>>>,
                   powerset : Set<Triple<Int,String,Int>>) {
        if (powerset.isNotEmpty()) {
            val elem = powerset.random()
            pertHelper(perturbations, powerset - elem)

            val dPlusElems = mutableSetOf<MutableSet<Triple<Int, String, Int>>>()
            for (d in perturbations) {
                dPlusElems += (d + elem) as MutableSet<Triple<Int, String, Int>>
            }
            perturbations += dPlusElems
        }
    }
    fun alphabetToSet(alphabet: Alphabet<String>) : MutableSet<String> {
        val set = mutableSetOf<String>()
        for (a in alphabet) {
            set.add(a)
        }
        return set
    }
    val perturbations : MutableSet<MutableSet<Triple<Int,String,Int>>> = mutableSetOf(mutableSetOf())
    val powerset = product(states, alphabetToSet(alphabet), states)
    pertHelper(perturbations, powerset)
    return perturbations
}

fun ltsTransitions(T : LTS<Int,String>) : Set<Triple<Int,String,Int>> {
    val init = T.initialStates
    if (init.isEmpty()) {
        return emptySet()
    }

    // BFS for all transitions
    val transitions = mutableSetOf<Triple<Int,String,Int>>()
    val visited : MutableSet<Int> = mutableSetOf()
    val q : Queue<Int> = LinkedList<Int>()
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
fun addPerturbations(T : CompactLTS<String>, d : Set<Triple<Int,String,Int>>) : CompactLTS<String> {
    val Td = copyLTS(T)
    for (t in d) {
        Td.addTransition(t.first, t.second, t.third)
    }
    return Td
}

/*
fun perturbationsToAutomaton(perturbations : Set<Triple<Int,String,Int>>) : CompactLTS<String> {
    val alphabet = perturbations.map { it.second }.toTypedArray()
    val aut = AutomatonBuilders.newNFA(Alphabets.fromArray(alphabet)).create().asLTS()

    val srcStates = perturbations.map { it.first }.toSet()
    val dstStates = perturbations.map { it.first }.toSet()
    val states = srcStates union dstStates
    for (s in states) {
        aut.addState()
    }
    for (t in perturbations) {
        aut.addTransition()
    }
}
 */

fun toDeterministic(T : CompactLTS<String>) : MutableDetLTS<Int,String> {
    val det = determinise(T) as CompactDFA<String>
    val detLts = CompactDetLTS(det)
    return detLts as MutableDetLTS<Int, String>
}

/**
 * The order of the args may be a bit misleading, but essentially we're asking:
 *      is d2 <= d1 ?
 * Or equivalently,
 *      is d1 at least as powerful as d2?
 */
fun atLeastAsPowerful(T : CompactLTS<String>, d2 : Set<Triple<Int,String,Int>>, d1 : Set<Triple<Int,String,Int>>) : Boolean {
    //return d1.containsAll(d2)

    val Td2 = addPerturbations(T, d2)
    val Td1 = addPerturbations(T, d1)
    val Td1Det = toDeterministic(Td1)

    if (satisfies(Td2, Td1Det)) {
        val Td2Det = toDeterministic(Td2)
        return if (satisfies(Td1, Td2Det)) {
            // Runs(Td2) = Runs(Td1)
            d1.containsAll(d2)
        }
        else {
            // Runs(Td2) ⊂ Runs(Td1)
            true
        }
    }

    return false
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

    //val cp = copyLTS(P2)
    //println(Automata.testEquivalence(P2, cp, P2.alphabet()))
    //println("The two models are not equivalent by [${Automata.findSeparatingWord(P2, cp, P2.inputAlphabet)}]!")
    //write(System.out, P2, P2.alphabet())
    //println()
    //write(System.out, cp, cp.alphabet())

    /*
    val P = AutomatonBuilders.newDFA(Alphabets.fromArray("a", "b"))
        .withInitial(0)
        .from(0).on("a").to(1)
        .from(1).on("b").to(0)
        .withAccepting(0, 1)
        .create()
        .asLTS()
     */

    val delta = mutableSetOf<Set<Triple<Int,String,Int>>>()
    val QXActXQ = allPerturbations(T.states, T.inputAlphabet)
    //println("#states: ${m.states.size}")
    //println("#alph: ${m.inputAlphabet.size}")
    //println("#prod: ${QXActXQ.size}")

    for (d in QXActXQ) {
        val Td = addPerturbations(T, d)
        if (satisfies(Td, P)) {
            delta += d
        }
    }
    println("#delta before: ${delta.size}")

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

    println("#delta: ${delta.size}")
    for (d in delta) {
        println("  {${d.joinToString()}}")
    }


    //val sat = satisfies(m, p)
    //println("M |= P $sat")

    /*
    val m = AutomatonBuilders.newDFA(Alphabets.fromArray("a", "b", "c"))
        .withInitial(0)
        .from(0).on("a").to(1)
        .from(1).on("b").to(2).on("a").to(1)
        .from(2).on("c").to(0)
        .withAccepting(0, 1, 2)
        .create()
        .asLTS()
    val n = AutomatonBuilders.newDFA(Alphabets.fromArray("a", "b", "c"))
        .withInitial(0)
        .from(0).on("a").to(0)
        .withAccepting(0)
        .create()
        .asLTS()

    var c = parallel(m, n)

    write(System.out, c, c.alphabet())
    */


    /*
    val path : Path? = findErrors(m)
    if (path == null) {
        println("No errors are reachable")
    }
    else {
        println("Error path:")
        for (a in path.impl) {
            print("$a ")
        }
    }
     */


    /*
    val m = NFA<Char>()
    m.addInitialState("1")
    m.addState("2")
    m.addErrorState("3")
    m.addState("4")
    m.addTransition("1", 'b', "1")
    m.addTransition("1", 'a', "2")
    m.addTransition("1", 'a', "4")
    m.addTransition("2", 'b', "1")
    m.addTransition("4", 'b', "2")
    m.addTransition("4", 'a', "3")

    val trace = m.errorTrace()
    if (trace == null) {
        println("no errors")
    }
    else {
        for (c in trace) {
            println(c)
        }
    }
     */
}