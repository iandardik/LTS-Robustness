package cmu.isr.tolerance

import addPerturbations
import cmu.isr.assumption.SubsetConstructionGenerator
import cmu.isr.tolerance.delta.DeltaBackwardDFS
import cmu.isr.tolerance.delta.DeltaDFS
import cmu.isr.tolerance.delta.DeltaDFSRand
import cmu.isr.ts.LTS
import cmu.isr.ts.MutableDetLTS
import cmu.isr.ts.alphabet
import cmu.isr.ts.lts.*
import cmu.isr.ts.lts.ltsa.write
import cmu.isr.ts.nfa.determinise
import cmu.isr.ts.parallel
import copyLTSAcceptingOnly
import fspToDFA
import fspToNFA
import isMaximalAccepting
import ltsTransitions
import net.automatalib.automata.fsa.impl.compact.CompactDFA
import parallelRestrict
import product
import satisfies
import stripTauTransitions
import toDeterministic
import java.io.File
import java.util.*



fun filterControlledDuplicates(delta : Set<Set<Triple<Int,String,Int>>>,
                               E : CompactLTS<String>,
                               C : CompactLTS<String>)
        : Set<Set<Triple<Int,String,Int>>> {
    val controlledDelta = delta.associateWith { parallel(addPerturbations(E,it), C) }
    val ls = delta.toMutableList()
    val toRemove = mutableSetOf<Int>()
    for (i in 0 until ls.size) {
        if (i in toRemove) {
            continue
        }
        val di = ls[i]
        val cdi = controlledDelta[di] ?: throw RuntimeException("cdi bug")
        val cdiDet = CompactDetLTS(determinise(cdi) as CompactDFA<String>)
        for (j in i+1 until ls.size) {
            if (j in toRemove) {
                continue
            }
            val dj = ls[j]
            val cdj = controlledDelta[dj] ?: throw RuntimeException("cdj bug")
            val cdjDet = CompactDetLTS(determinise(cdj) as CompactDFA<String>)
            if (satisfies(cdi, cdjDet)) {
                toRemove.add(j)
            }
            else if (satisfies(cdj, cdiDet)) {
                toRemove.add(i)
            }

            /*
            if (satisfies(cdi, cdjDet) && satisfies(cdj, cdiDet)) {
                toRemove.add(j)
            }
             */
            // only keep maximal behaviors, JDEDS style
            /*
            else if (satisfies(cdi, cdjDet)) {
                toRemove.add(i)
            }
            else if (satisfies(cdj, cdiDet)) {
                toRemove.add(j)
            }
             */
        }
    }
    return ls.filterIndexedTo(HashSet()) { i,_ -> !toRemove.contains(i) }
}

fun bucketControlledDuplicates(delta : Set<Set<Triple<Int,String,Int>>>,
                               E : CompactLTS<String>,
                               C : CompactLTS<String>)
                               : Set<Set<Set<Triple<Int,String,Int>>>> {
    val controlledDelta = delta.associateWith { parallel(addPerturbations(E,it), C) }
    val buckets = mutableMapOf<Pair<LTS<Int,String>,CompactDetLTS<String>>, Set<Set<Triple<Int,String,Int>>>>()
    for (d in delta) {
        val cd = controlledDelta[d] ?: throw RuntimeException("cdi bug")
        val cdDet = CompactDetLTS(determinise(cd) as CompactDFA<String>)
        var foundBucket = false
        for (k in buckets.keys) {
            val dk = k.first
            val dkDet = k.second
            if (satisfies(cd, dkDet) && satisfies(dk, cdDet)) {
                val bucketk = buckets[k] ?: throw RuntimeException("bucket error")
                buckets[k] = bucketk union setOf(d)
                foundBucket = true
                break
            }
        }
        if (!foundBucket) {
            val k = Pair(cd,cdDet)
            buckets[k] = setOf(d)
        }
    }
    return buckets.values.toSet()
}



fun main(args : Array<String>) {
    /*
    val T = AutomatonBuilders.newNFA(Alphabets.fromArray("a"))
        .withInitial(0)
        .from(0).on("a").to(1)
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
    write(System.out, P, P.alphabet())
    */

    if (args.size < 4) {
        println("usage: tolerance <alg> <env> <ctrl> <prop>")
        return
    }

    for (i in 1..3) {
        val path = args[i]
        if (!File(path).exists()) {
            println("File \"$path\" does not exist")
            return
        }
    }

    val alg = args[0]
    val E = stripTauTransitions(fspToNFA(args[1]))
    val C = stripTauTransitions(fspToNFA(args[2]))
    val P = fspToDFA(args[3])

    if (!satisfies(parallel(E,C), P)) {
        //val waGen = SubsetConstructionGenerator(C, E, P)
        //val wa = waGen.generate()
        //println("WA:")
        //write(System.out, wa, wa.alphabet())
        //println(wa.alphabet())
        println("E||C does not satisfy P")
        return
    }

    //println("E:")
    //writeDOT(System.out, E, E.alphabet())

    if (alg == "t") {
        val nTrials = 1000
        for (naiveRandN in setOf(2,3,4,5,6,7,8,9,10,15,19)) {
            var nSuccess = 0
            var nFail = 0
            for (i in 0..nTrials) {
                val delta = deltaNaiveRand(E, C, P, naiveRandN)
                if (delta.size == 3) {
                    ++nSuccess
                } else {
                    ++nFail
                }
            }
            val successRate = nSuccess.toDouble() / nTrials.toDouble()
            val failRate = nFail.toDouble() / nTrials.toDouble()
            println("naiveRandN: $naiveRandN")
            println("success %: ${successRate * 100.0}")
            println("fail %: ${failRate * 100.0}")
            println()
        }
        return
    }

    var delta =
        when (alg) {
            "0" -> emptySet()
            "1" -> deltaNaiveBruteForce(E, C, P)
            "2" -> deltaBruteForce(E, C, P)
            "3" -> DeltaDFS(E, C, P).compute()
            "4" -> DeltaBackwardDFS(E, C, P).compute()
            "5" -> DeltaDFSRand(E, C, P).compute()
            "6" -> deltaNaiveRand(E, C, P)
            else -> {
                println("Invalid algorithm")
                return
            }
        }

    println("#delta: ${delta.size}")
    /*
    delta = delta
        .map { it.filter { E.isAccepting(it.first) && E.isAccepting(it.third) }.toSet() }
        .associateWith { 0 }
        .keys.toSet()
    println("#delta no err states: ${delta.size}")
     */

    val A = product(E.states, E.inputAlphabet.toSet(), E.states)
        .filter { E.isAccepting(it.first) && E.isAccepting(it.third) }
        .toSet()
    val Re = ltsTransitions(E)
        .filter { E.isAccepting(it.first) && E.isAccepting(it.third) }
        .toSet()

    /*
    val maxPertSize = delta.map { it.size }.max()
    val minPertSize = delta.map { it.size }.min()
    val minEdgesToErr = minPertSize+1 - Re.size
    println("maxPertSize: $maxPertSize")
    println("minPertSize: $minPertSize")
    println("minEdgesToErr: $minEdgesToErr")

    val lcd = delta.fold(delta.first()) { acc, d -> acc intersect d }
    val lcdPerts = lcd - Re
    val Elcd = addPerturbations(E, lcdPerts)
    val ElcdRestr = parallelRestrict(Elcd, C)
    println("lcdPerts: $lcdPerts")
    println("#lcdPerts: ${lcdPerts.size}")
    println("Ecld:")
    writeDOT(System.out, Elcd, Elcd.alphabet())
    println("EcldRestr:")
    writeDOT(System.out, ElcdRestr, ElcdRestr.alphabet())
    println()
     */

    //delta = filterControlledDuplicates(delta, E, C)
    //println("#(filtered delta): ${delta.size}")

    /*
    val controlledBehBuckets = bucketControlledDuplicates(delta, E, C)
    val cbbMin = controlledBehBuckets.map { it.size }.min()
    val cbbMax = controlledBehBuckets.map { it.size }.max()
    println("#controlledBehBuckets: ${controlledBehBuckets.size}")
    println("min # controlledBehBuckets: $cbbMin")
    println("max # controlledBehBuckets: $cbbMax")

    val intersects = controlledBehBuckets
        .map {
                s -> s.fold(s.first()) { acc,d -> acc intersect d }
        }
        .fold(DeltaBuilder(E,C,P)) { acc,i -> acc.add(i); acc }
        .toSet()
    println("# Max Intersects: ${intersects.size}")
     */
    /*
    println("Max Intersects (sample):")
    for (d in intersects.take(3)) {
        val Ed = addPerturbations(E, d)
        writeDOT(System.out, Ed, Ed.alphabet())
    }
     */

    /*
    val sampleIntersects = controlledBehBuckets
        .filter { it.size > 1 }
        .take(5)
        .map {
            s -> s.fold(s.first()) { acc,d -> acc intersect d }
        }
        .toSet()
    println("Samples Intersects:")
    for (d in sampleIntersects) {
        val Ed = addPerturbations(E, d)
        writeDOT(System.out, Ed, Ed.alphabet())
    }
     */

    /*
    val tb = controlledBehBuckets
        .first { it.size > 1 }
        .take(3)
        .toSet()
    println("Samples:")
    for (d in tb) {
        val Ed = addPerturbations(E, d)
        writeDOT(System.out, Ed, Ed.alphabet())
    }
     */


    // prints delta
    //println("#delta: ${delta.size}")
    for (d in delta) {
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
        //println("  {${sortedD.joinToString()}}")
    }

    // print the FSP for each Ed
    for (d in delta) {
        val Ed = copyLTSAcceptingOnly(addPerturbations(E, d))
        //println()
        //write(System.out, Ed, Ed.alphabet())
    }

    // print the FSP for each Ed || C
    for (d in delta) {
        val Ed = addPerturbations(E, d)
        val EdC = copyLTSAcceptingOnly(parallel(Ed, C))
        //println()
        //write(System.out, EdC, EdC.alphabet())
    }

    // print the DOT for each Ed || C
    for (d in delta) {
        val Ed = addPerturbations(E, d)
        val EdRestrictedToC = parallelRestrict(Ed, C)
        //println()
        //writeDOT(System.out, EdRestrictedToC, EdRestrictedToC.alphabet())
    }

    // checks to make sure the solution is sound
    var sound = true
    for (d in delta) {
        val Ed = addPerturbations(E, d)
        val EdComposeC = parallel(Ed, C)
        if (!satisfies(EdComposeC, P)) {
            sound = false
            println("Violation for Ed||P |= P: $d")
        }
    }
    if (sound) {
        println()
        println("Solution is sound")
    }

    // checks to make sure every member of delta is maximal
    var maximal = true
    for (d in delta) {
        if (!isMaximalAccepting(E, d, C, P)) {
            maximal = false
            println("Found non-maximal d")
            //println("Found non-maximal d: $d")
        }
    }
    if (maximal) {
        println("All solutions are maximal")
    }

    /*
    val oneSol = deltaBruteForce(E, C, P)
    val correct = delta == oneSol
    println("Correct compared to 1? $correct")
    if (!correct) {
        println("Num missing from choice: ${(delta - oneSol).size}")
        println("Num missing from one sol: ${(oneSol - delta).size}")
    }
     */

    println("calc'ing WA")
    val waGen = SubsetConstructionGenerator(C, E, P)
    val wa = waGen.generate()
    //println()
    //println("WA:")
    //write(System.out, wa, wa.alphabet())

    var equiv = mutableSetOf<Set<Triple<Int,String,Int>>>()
    for (d in delta) {
        val Ed = addPerturbations(E, d)
        val EdDet = toDeterministic(Ed)
        if (satisfies(wa, EdDet)) {
            equiv.add(d)
        }
        if (!satisfies(Ed, wa as MutableDetLTS<Int, String>)) {
            println("WA ERROR!!")
        }
    }
    println("#equiv: ${equiv.size}")
    println("#missing: ${delta.size - equiv.size}")
    val equivIntersect = equiv
        .fold(A) { acc,d -> acc intersect d }
        .toSet() - Re
    println("#Equiv intersect: ${equivIntersect.size}")
    /*
    println("Equiv intersect: $equivIntersect")
    println("Equiv extras:")
    equiv.forEach {
        val extra = it - equivIntersect
        println("  $extra")
    }
    println()
     */
    /*
    for (d in equiv.take(1)) {
        val Ed = parallelRestrict(addPerturbations(E, d), C)
        println("equiv:")
        write(System.out, Ed, Ed.alphabet())
    }
     */

    // top/bottom used edges
    val useFrequency = (A - Re)
        .associateWith { e -> delta.filter { it.contains(e) }.size.toDouble() / delta.size.toDouble() * 100.0 }
        .entries
        .sortedByDescending { it.value }

    val tNum = 5
    println("Top $tNum edges:")
    useFrequency
        .take(tNum)
        .forEach { (e,f) -> println("$e: $f %") }
    println("Bottom $tNum edges:")
    useFrequency
        .takeLast(tNum)
        .forEach { (e,f) -> println("$e: $f %") }
}
