package cmu.isr.tolerance

import addPerturbations
import cmu.isr.assumption.SubsetConstructionGenerator
import cmu.isr.tolerance.delta.DeltaDFS
import cmu.isr.tolerance.delta.DeltaDFSRand
import cmu.isr.tolerance.utils.*
import cmu.isr.ts.MutableDetLTS
import cmu.isr.ts.parallel
import parallelRestrict
import product
import satisfies
import toDeterministic
import java.io.File

fun main(args : Array<String>) {
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
    val env = stripTauTransitions(fspToNFA(args[1]))
    val ctrl = stripTauTransitions(fspToNFA(args[2]))
    val prop = fspToDFA(args[3])

    if (!satisfies(parallel(env,ctrl), prop)) {
        //val waGen = SubsetConstructionGenerator(C, env, P)
        //val wa = waGen.generate()
        //println("WA:")
        //write(System.out, wa, wa.alphabet())
        //println(wa.alphabet())
        println("env||C does not satisfy P")
        return
    }

    //println("env:")
    //writeDOT(System.out, env, env.alphabet())

    if (alg == "t") {
        val nTrials = 1000
        for (naiveRandN in setOf(2,3,4,5,6,7,8,9,10,15,19)) {
            var nSuccess = 0
            var nFail = 0
            for (i in 0..nTrials) {
                val delta = deltaNaiveRand(env, ctrl, prop, naiveRandN)
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
            "1" -> deltaNaiveBruteForce(env, ctrl, prop)
            "2" -> DeltaDFS(env, ctrl, prop).compute()
            "3" -> DeltaDFSRand(env, ctrl, prop).compute()
            "4" -> deltaNaiveRand(env, ctrl, prop)
            else -> {
                println("Invalid algorithm")
                return
            }
        }

    println("#delta: ${delta.size}")
    /*
    delta = delta
        .map { it.filter { env.isAccepting(it.first) && env.isAccepting(it.third) }.toSet() }
        .associateWith { 0 }
        .keys.toSet()
    println("#delta no err states: ${delta.size}")
     */

    val A = product(env.states, env.inputAlphabet.toSet(), env.states)
        .filter { env.isAccepting(it.first) && env.isAccepting(it.third) }
        .toSet()
    val Re = ltsTransitions(env)
        .filter { env.isAccepting(it.first) && env.isAccepting(it.third) }
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

    //delta = filterControlledDuplicates(delta, env, C)
    //println("#(filtered delta): ${delta.size}")

    /*
    val controlledBehBuckets = bucketControlledDuplicates(delta, env, C)
    val cbbMin = controlledBehBuckets.map { it.size }.min()
    val cbbMax = controlledBehBuckets.map { it.size }.max()
    println("#controlledBehBuckets: ${controlledBehBuckets.size}")
    println("min # controlledBehBuckets: $cbbMin")
    println("max # controlledBehBuckets: $cbbMax")

    val intersects = controlledBehBuckets
        .map {
                s -> s.fold(s.first()) { acc,d -> acc intersect d }
        }
        .fold(DeltaBuilder(env,C,P)) { acc,i -> acc.add(i); acc }
        .toSet()
    println("# Max Intersects: ${intersects.size}")
     */
    /*
    println("Max Intersects (sample):")
    for (d in intersects.take(3)) {
        val Ed = addPerturbations(env d)
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
        val Ed = addPerturbations(env d)
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
        val Ed = copyLTSAcceptingOnly(addPerturbations(env, d))
        //println()
        //write(System.out, Ed, Ed.alphabet())
    }

    // print the FSP for each Ed || C
    for (d in delta) {
        val Ed = addPerturbations(env, d)
        val EdC = copyLTSAcceptingOnly(parallel(Ed, ctrl))
        //println()
        //write(System.out, EdC, EdC.alphabet())
    }

    // print the DOT for each Ed || C
    for (d in delta) {
        val Ed = addPerturbations(env, d)
        val EdRestrictedToC = parallelRestrict(Ed, ctrl)
        //println()
        //writeDOT(System.out, EdRestrictedToC, EdRestrictedToC.alphabet())
    }

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
    /*
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
     */
}
