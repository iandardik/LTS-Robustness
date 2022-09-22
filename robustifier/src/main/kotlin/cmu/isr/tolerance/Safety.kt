package cmu.isr.tolerance

import cmu.isr.ts.MutableDetLTS
import cmu.isr.ts.alphabet
import cmu.isr.ts.lts.CompactDetLTS
import cmu.isr.ts.lts.asLTS
import cmu.isr.ts.lts.checkSafety
import cmu.isr.ts.lts.ltsa.write
import cmu.isr.ts.lts.makeErrorState
import cmu.isr.ts.parallel
import net.automatalib.util.automata.builders.AutomatonBuilders
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

fun satisfies(m : CompactDetLTS<String>, p : CompactDetLTS<String>) : Boolean {
    val pFixed = makeErrorState(p as MutableDetLTS<Int, String>)
    val result = checkSafety(m, pFixed)
    return !result.violation
}


fun main() {
    val m = AutomatonBuilders.newDFA(Alphabets.fromArray("a", "b"))
        .withInitial(0)
        .from(0).on("a").to(1)
        .from(1).on("b").to(0)
        .withAccepting(0, 1)
        .create()
        .asLTS()
    val p = AutomatonBuilders.newDFA(Alphabets.fromArray("a", "b"))
        .withInitial(0)
        .from(0).on("a").to(1)
        .from(1).on("b").to(0)
        .withAccepting(0, 1)
        .create()
        .asLTS()

    val sat = satisfies(m, p)
    println("M |= P $sat")


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