package cmu.isr.tolerance

import cmu.isr.tolerance.delta.DeltaDFS
import cmu.isr.tolerance.delta.DeltaDFSRand
import cmu.isr.tolerance.postprocess.maximalityCheck
import cmu.isr.tolerance.postprocess.printDelta
import cmu.isr.tolerance.postprocess.soundnessCheck
import cmu.isr.tolerance.utils.*
import cmu.isr.ts.parallel
import satisfies
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
        println("env||C does not satisfy P")
        return
    }

    /* Functionality for empirically estimating % of delta the Naive Rand algo will produce as a function of
       # of trials. We can only truly estimate this % if we know the real size of delta.
     */
    if (alg == "t") {
        val trueDeltaSize = 3 // for the classic toy example
        val nTrials = 1000
        for (naiveRandN in setOf(2,3,4,5,6,7,8,9,10,15,19)) {
            var nSuccess = 0
            var nFail = 0
            for (i in 0..nTrials) {
                val delta = deltaNaiveRand(env, ctrl, prop, naiveRandN)
                if (delta.size == trueDeltaSize) {
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

    // calc delta based on the user's choice of algorithm
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
    printDelta(delta, 5)

    soundnessCheck(delta, env, ctrl, prop)
    maximalityCheck(delta, env, ctrl, prop)
}
