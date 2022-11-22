package cmu.isr.tolerance

import addPerturbations
import cmu.isr.tolerance.delta.DeltaDFS
import cmu.isr.tolerance.delta.DeltaDFSEnvProp
import cmu.isr.tolerance.delta.DeltaDFSRand
import cmu.isr.tolerance.postprocess.*
import cmu.isr.tolerance.utils.*
import cmu.isr.ts.MutableDetLTS
import cmu.isr.ts.alphabet
import cmu.isr.ts.lts.CompactDetLTS
import cmu.isr.ts.lts.ltsa.write
import cmu.isr.ts.lts.makeErrorState
import cmu.isr.ts.parallel
import net.automatalib.automata.fsa.impl.compact.CompactDFA
import satisfies
import java.io.File

fun <I> makeErrorStateCopy(rawProp : CompactDetLTS<I>) : CompactDetLTS<I> {
    val prop = copyLTS(rawProp)
    for (s in prop.states) {
        if (prop.isErrorState(s))
            continue
        for (a in prop.inputAlphabet) {
            if (prop.getTransition(s, a) == null) {
                prop.addTransition(s, a, prop.errorState, null)
            }
        }
    }
    return prop
}

fun main(args : Array<String>) {
    if (args.size < 4) {
        println("usage: tolerance <alg> <env> <ctrl> <prop> [<env_props>]")
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
    val envPropList = args
        .drop(4)
        .map { fspToDFA(it) }

    val envProp =
        if (envPropList.isEmpty()) {
            if (alg == "4" || alg == "6") {
                error("envPropList is empty")
            }
            prop
        }
        else if (envPropList.size == 1) {
            envPropList.first()
        }
        else {
            parallel(*envPropList.toTypedArray()) as CompactDetLTS<String>
        }

    if (!satisfies(parallel(env,ctrl), prop)) {
        println("E||C does not satisfy P")
        return
    }
    if (!satisfies(env, envProp)) {
        if (alg == "4" || alg == "6") {
            println("E does not satisfy P_env")
            return
        }
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
            "4" -> DeltaDFSEnvProp(env, ctrl, prop, envProp).compute()
            "5" -> deltaNaiveRand(env, ctrl, prop)
            "6" -> deltaNaiveRandEnvProp(env, ctrl, prop, envProp)
            else -> {
                println("Invalid algorithm")
                return
            }
        }

    println("#delta: ${delta.size}")
    //printDelta(delta, 5)
    printDOT(delta, env, ctrl, 3)
    //printFSP(delta, env, 3)

    soundnessCheck(delta, env, ctrl, prop)
    //maximalityCheck(delta, env, ctrl, prop)

    if (alg == "4" || alg == "6") {
        for (d in delta) {
            val envD = addPerturbations(env, d)
            if (!satisfies(envD, envProp)) {
                println("Found violation for overall Ed |= P_env")
                //println("Property:")
                //write(System.out, envProp, envProp.alphabet())
                println("envD:")
                write(System.out, envD, envD.alphabet())
                println()
            }
            for (p in envPropList) {
                if (!satisfies(envD, p)) {
                    println("Found violation for individual Ed |= P_env")
                }
            }
        }
    }
}
