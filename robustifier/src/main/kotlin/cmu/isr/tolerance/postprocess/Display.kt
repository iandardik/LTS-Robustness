package cmu.isr.tolerance.postprocess

import addPerturbations
import cmu.isr.assumption.SubsetConstructionGenerator
import cmu.isr.tolerance.utils.copyLTSAcceptingOnly
import cmu.isr.tolerance.utils.isMaximalAccepting
import cmu.isr.tolerance.utils.ltsTransitions
import cmu.isr.ts.DetLTS
import cmu.isr.ts.MutableDetLTS
import cmu.isr.ts.alphabet
import cmu.isr.ts.lts.CompactDetLTS
import cmu.isr.ts.lts.CompactLTS
import cmu.isr.ts.lts.ltsa.write
import cmu.isr.ts.nfa.determinise
import cmu.isr.ts.parallel
import net.automatalib.automata.fsa.impl.compact.CompactDFA
import parallelRestrict
import product
import satisfies
import toDeterministic

fun printDelta(delta : Set<Set<Triple<Int, String, Int>>>, maxNum : Int = -1) {
    val deltaTrimmed = if (maxNum < 0) { delta } else { delta.take(maxNum) }
    for (d in deltaTrimmed) {
        val sortedD = d.sortedWith { a, b ->
            if (a.second != b.second) {
                a.second.compareTo(b.second)
            }
            else if (a.first != b.first) {
                a.first - b.first
            }
            else {
                a.third - b.third
            }
        }
        println("  {${sortedD.joinToString()}}")
    }
}

fun printLargestDeltaSz(delta : Set<Set<Triple<Int, String, Int>>>) {
    if (delta.isEmpty()) {
        println("Delta is empty and has no largest element.")
        return
    }
    val largestElem = delta.fold(delta.first()) { acc, d -> if (acc.size >= d.size) acc else d }
    val sz = largestElem.size
    println("Size of largest delta element: $sz")
}

fun printFSP(delta : Set<Set<Triple<Int, String, Int>>>,
             env : CompactLTS<String>,
             maxNum : Int = -1) {
    val deltaTrimmed = if (maxNum < 0) { delta } else { delta.take(maxNum) }
    for (d in deltaTrimmed) {
        val envD = copyLTSAcceptingOnly(addPerturbations(env, d))
        println()
        write(System.out, envD, envD.alphabet())
    }
}

fun printClosedLoopFSP(delta : Set<Set<Triple<Int, String, Int>>>,
                       env : CompactLTS<String>,
                       ctrl : CompactLTS<String>,
                       maxNum : Int = -1) {
    // print the FSP for each Ed || C
    val deltaTrimmed = if (maxNum < 0) { delta } else { delta.take(maxNum) }
    for (d in deltaTrimmed) {
        val envD = addPerturbations(env, d)
        val envDCtrl = copyLTSAcceptingOnly(parallel(envD, ctrl))
        println()
        write(System.out, envDCtrl, envDCtrl.alphabet())
    }
}

fun printDOT(delta : Set<Set<Triple<Int, String, Int>>>,
             env : CompactLTS<String>,
             ctrl : CompactLTS<String>,
             maxNum : Int = -1) {
    // print the DOT for each Ed || C
    val deltaTrimmed = if (maxNum < 0) { delta } else { delta.take(maxNum) }
    for (d in deltaTrimmed) {
        val envD = addPerturbations(env, d)
        //val envDRCtrl = parallelRestrict(envD, ctrl)
        println()
        writeDOT(System.out, envD, envD.alphabet())
        //writeDOT(System.out, envDRCtrl, envDRCtrl.alphabet())
    }
}

fun compareBehToWA(delta : Set<Set<Triple<Int, String, Int>>>,
                   env : CompactLTS<String>,
                   ctrl : CompactLTS<String>,
                   prop : CompactDetLTS<String>,
                   maxNum : Int = -1) {
    // print the DOT for each Ed || C
    val deltaTrimmed = if (maxNum < 0) { delta } else { delta.take(maxNum) }
    val waGen = SubsetConstructionGenerator(ctrl, env, prop)
    val wa = waGen.generate()
    for (d in deltaTrimmed) {
        val envD = addPerturbations(env, d)
        val envDProp =  CompactDetLTS(determinise(envD) as CompactDFA<String>)
        val equivToWA = satisfies(wa, envDProp)
        if (equivToWA) {
            println("Equiv to WA")
        }
        else {
            println("NOT equiv to WA")
        }
    }
}

fun compareBehPairs(delta : Set<Set<Triple<Int, String, Int>>>,
                    env : CompactLTS<String>,
                    ctrl : CompactLTS<String>,
                    prop : CompactDetLTS<String>,
                    maxNum : Int = -1) {
    // print the DOT for each Ed || C
    val deltaTrimmed = if (maxNum < 0) { delta } else { delta.take(maxNum) }
    val deltaList = deltaTrimmed.toList()
    for (i in 0 until deltaList.size) {
        for (j in (i+1)until deltaList.size) {
            val envDI = addPerturbations(env, deltaList[i])
            val envDJ = addPerturbations(env, deltaList[j])
            val envDIProp = CompactDetLTS(determinise(envDI) as CompactDFA<String>)
            val envDJProp = CompactDetLTS(determinise(envDJ) as CompactDFA<String>)
            val iStronger = satisfies(envDI, envDJProp)
            val jStronger = satisfies(envDJ, envDIProp)
            if (iStronger && jStronger) {
                println("$i and $j are equiv")
            }
            else if (iStronger) {
                println("$i is stronger than $j")
            }
            else if (jStronger) {
                println("$j is stronger than $i")
            }
            else {
                println("$i and $j are incomparable")
            }
        }
    }
}

fun edgesToErr(delta : Set<Set<Triple<Int, String, Int>>>, env : CompactLTS<String>) {
    val Re = ltsTransitions(env)
        .filter { env.isAccepting(it.first) && env.isAccepting(it.third) }
        .toSet()
    val maxPertSize = delta.map { it.size }.max()
    val minPertSize = delta.map { it.size }.min()
    val minEdgesToErr = minPertSize+1 - Re.size
    println("maxPertSize: $maxPertSize")
    println("minPertSize: $minPertSize")
    println("minEdgesToErr: $minEdgesToErr")
}

fun soundnessCheck(delta : Set<Set<Triple<Int, String, Int>>>,
                   env : CompactLTS<String>,
                   ctrl : CompactLTS<String>,
                   prop : CompactDetLTS<String>) {
    // checks to make sure the solution is sound
    var sound = true
    for (d in delta) {
        val Ed = addPerturbations(env, d)
        val EdComposeC = parallel(Ed, ctrl)
        if (!satisfies(EdComposeC, prop)) {
            sound = false
            println("Found violation for Ed||P |= P")
            //println("Violation for Ed||P |= P: $d")
        }
    }
    if (sound) {
        println()
        println("Solution is sound")
    }
}

fun maximalityCheck(delta : Set<Set<Triple<Int, String, Int>>>,
                    env : CompactLTS<String>,
                    ctrl : CompactLTS<String>,
                    prop : CompactDetLTS<String>) {
    // checks to make sure every member of delta is maximal
    var maximal = true
    for (d in delta) {
        val envD = addPerturbations(env, d)
        if (!isMaximalAccepting(envD, ctrl, prop)) {
            maximal = false
            println("Found non-maximal d")
            //println("Found non-maximal d: $d")
        }
    }
    if (maximal) {
        println("All solutions are maximal")
    }
}

fun missingBehaviors(delta : Set<Set<Triple<Int, String, Int>>>,
                     env : CompactLTS<String>,
                     ctrl : CompactLTS<String>,
                     prop : CompactDetLTS<String>) {
    val waGen = SubsetConstructionGenerator(ctrl, env, prop)
    val wa = waGen.generate()
    //println()
    //println("WA:")
    //write(System.out, wa, wa.alphabet())

    var equiv = mutableSetOf<Set<Triple<Int,String,Int>>>()
    for (d in delta) {
        val Ed = addPerturbations(env, d)
        val EdDet = toDeterministic(Ed)
        if (satisfies(wa, EdDet)) {
            equiv.add(d)
        }
        if (!satisfies(Ed, wa as MutableDetLTS<Int, String>)) {
            println("WA ERROR!!")
        }
    }

    val A = product(env.states, env.inputAlphabet.toSet(), env.states)
        .filter { env.isAccepting(it.first) && env.isAccepting(it.third) }
        .toSet()
    val Re = ltsTransitions(env)
        .filter { env.isAccepting(it.first) && env.isAccepting(it.third) }
        .toSet()

    println("#equiv: ${equiv.size}")
    println("#missing: ${delta.size - equiv.size}")
    val equivIntersect = equiv
        .fold(A) { acc,d -> acc intersect d }
        .toSet() - Re
    println("#Equiv intersect: ${equivIntersect.size}")
}

fun topBottomEdges(delta : Set<Set<Triple<Int, String, Int>>>, env : CompactLTS<String>, tNum : Int = 5) {
    val A = product(env.states, env.inputAlphabet.toSet(), env.states)
        .filter { env.isAccepting(it.first) && env.isAccepting(it.third) }
        .toSet()
    val Re = ltsTransitions(env)
        .filter { env.isAccepting(it.first) && env.isAccepting(it.third) }
        .toSet()
    val useFrequency = (A - Re)
        .associateWith { e -> delta.filter { it.contains(e) }.size.toDouble() / delta.size.toDouble() * 100.0 }
        .entries
        .sortedByDescending { it.value }

    println("Top $tNum edges:")
    useFrequency
        .take(tNum)
        .forEach { (e,f) -> println("$e: $f %") }
    println("Bottom $tNum edges:")
    useFrequency
        .takeLast(tNum)
        .forEach { (e,f) -> println("$e: $f %") }
}
