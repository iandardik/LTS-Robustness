package cmu.isr.tolerance

import addPerturbations
import cmu.isr.tolerance.delta.*
import cmu.isr.tolerance.postprocess.*
import cmu.isr.tolerance.utils.*
import cmu.isr.ts.MutableDetLTS
import cmu.isr.ts.alphabet
import cmu.isr.ts.lts.CompactDetLTS
import cmu.isr.ts.lts.CompactLTS
import cmu.isr.ts.lts.ltsa.write
import cmu.isr.ts.lts.makeErrorState
import cmu.isr.ts.nfa.determinise
import cmu.isr.ts.parallel
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.int
import net.automatalib.automata.fsa.impl.compact.CompactDFA
import satisfies
import java.io.File

fun oldMain(args : Array<String>) {
    // just for convenience
    if (args.size == 1) {
        //val m = stripTauTransitions(fspToNFA(args[0]))
        val m = fspToDFA(args[0])
        writeDOT(System.out, m, m.alphabet())
        return
    }

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
            "2" -> DeltaDFS(env, ctrl, prop, true).compute()
            "3" -> DeltaDFSRand(env, ctrl, prop).compute()
            "4" -> DeltaDFSEnvProp(env, ctrl, prop, envProp, true).compute()
            "5" -> deltaNaiveRand(env, ctrl, prop)
            "6" -> deltaNaiveRandEnvProp(env, ctrl, prop, envProp)
            else -> {
                println("Invalid algorithm")
                return
            }
        }

    println("#delta: ${delta.size}")
    //printDelta(delta, 5)
    //printDOT(delta, env, ctrl, 3)
    // TODO also check envPropList to make sure each contains all of env's transitions
    // TODO under which conditions will we always get a single delta elem back? and can we prove it??
    //printDOT(filterEnvPropSubsets(delta, envPropList), env, ctrl, 3)
    //compareBehToWA(delta, env, ctrl, prop, 3)
    compareBehPairs(delta, env, ctrl, prop, 3)
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
    if (alg == "4") {
        var deltaContainsAll = true
        val deltaRand = deltaNaiveRandEnvProp(env, ctrl, prop, envProp)
        for (dr in deltaRand) {
            var deltaContains = false
            for (d in delta) {
                if (d.containsAll(dr)) {
                    deltaContains = true
                    break
                }
            }
            if (!deltaContains) {
                deltaContainsAll = false
                println("delta missing from rand: $dr")
            }
        }
        if (deltaContainsAll) {
            println("delta contains all rand solutions")
        }
    }
}

class ToleranceApp : CliktCommand() {
    val envFile by option("--env", help="FSP file for the environment.").required()
    val ctrlFile by option("--ctrl", help="FSP file for the controller.").multiple()
    val propFile by option("--prop", help="FSP file for the property.").multiple()
    val envPropFile by option("--env-prop", help="FSP file for an environment property. This arg is allowed multiple times.")
        .multiple()
    val bruteForce by option("--bf", help="Will run brute-force version of the algorithm.")
        .flag(default = false)
    val naiveBruteForce by option("--naive-bf", help="Will run naive brute-force version of the algorithm.")
        .flag(default = false)
    val random by option("--rand", help="Will run randomized version of the algorithm.")
        .flag(default = false)
    val randInters by option("--rand-iters", help="Number of iterations to run in randomized mode.")
        .int()
        .default(1000)
    val silent by option("--silent", help="Will not print results to stdout. Mutually exclusive with verbose mode.")
        .flag(default = false)
    val printDOT by option("--print-dot", help="Print the contents of delta in DOT format. Default is set format.")
        .flag(default = false)
    val printFSP by option("--print-fsp", help="Print the contents of delta in FSP format. Default is set format.")
        .flag(default = false)
    val printLargestDeltaSize by option("--largest-delta-size", help="Print the size of the largest element of delta.")
        .flag(default = false)
    val numPrint by option("--num-print", help="Number of items in delta to print. Default is 3.")
        .int()
        .default(3)
    val verbose by option("--verbose", help="Prints extra information about the run, including |W|. Mutually exclusive with silent mode.")
        .flag(default = false)
    val compareCtrl by option("--compare-ctrl", help="Compares the robustness of two controllers. Mutually exclusive with property comparison.")
        .flag(default = false)
    val compareProp by option("--compare-prop", help="Compares the robustness w/respect to two properties. Mutually exclusive with controller comparison.")
        .flag(default = false)

    fun calcDelta(env : CompactLTS<String>,
                  ctrl : CompactLTS<String>,
                  prop : CompactDetLTS<String>)
                  : Set<Set<Triple<Int, String, Int>>>? {
        if (!satisfies(parallel(env,ctrl), prop)) {
            println("Error: ~(E||C |= P)")
            return null
        }

        if (verbose) {
            println("# states in E: ${env.states.size}")
            println("# states in C: ${ctrl.states.size}")
            println("# states in P: ${prop.states.size}")
        }

        val delta =
            if (envPropFile.isEmpty()) {
                if (random) {
                    if (!silent) {
                        println("Running randomized algorithm with $randInters iterations")
                    }
                    deltaNaiveRand(env, ctrl, prop, randInters)
                } else if (bruteForce) {
                    deltaBruteForce(env, ctrl, prop)
                } else if (naiveBruteForce) {
                    deltaNaiveBruteForce(env, ctrl, prop)
                } else {
                    DeltaDFS(env, ctrl, prop, verbose).compute()
                }
            }
            else {
                //val envPropList = envPropFile.map { fspToDFA(it) }
                val envPropList = envPropFile.map { CompactDetLTS(determinise(stripTauTransitions(fspToNFA(it))) as CompactDFA) }
                val envProp =
                    if (envPropList.size == 1) {
                        envPropList.first()
                    }
                    else {
                        parallel(*envPropList.toTypedArray()) as CompactDetLTS<String>
                    }
                if (!satisfies(env, envProp)) {
                    println("Error: ~(E |= envProp)")
                    return null
                }
                if (random) {
                    if (!silent) {
                        println("Running randomized algorithm with $randInters iterations")
                    }
                    deltaNaiveRandEnvProp(env, ctrl, prop, envProp, randInters)
                } else if (bruteForce) {
                    deltaBruteForceEnvProp(env, ctrl, prop, envProp)
                } else if (naiveBruteForce) {
                    deltaNaiveBruteForceEnvProp(env, ctrl, prop, envProp)
                } else {
                    DeltaDFSEnvProp(env, ctrl, prop, envProp, verbose).compute()
                }
            }
        return delta
    }

    override fun run() {
        if (silent && verbose) {
            println("Cannot run in silent and verbose mode.")
            return
        }
        if (compareCtrl && compareProp) {
            println("Cannot compare controllers and properties.")
            return
        }
        if (random && bruteForce || random && naiveBruteForce || bruteForce && naiveBruteForce) {
            println("Cannot mix random / brute-force / naive brute-force modes.")
            return
        }

        /* compare robustness of two different controllers */
        if (compareCtrl) {
            if (ctrlFile.size != 2) {
                println("Exactly two controller files are required.")
                return
            }
            if (propFile.size != 1) {
                println("Exactly one property file is required for comparison.")
                return
            }
            val env = stripTauTransitions(fspToNFA(envFile))
            val ctrl1 = stripTauTransitions(fspToNFA(ctrlFile[0]))
            val ctrl2 = stripTauTransitions(fspToNFA(ctrlFile[1]))
            val prop = fspToDFA(propFile[0])

            val delta1 = calcDelta(env, ctrl1, prop)
            val delta2 = calcDelta(env, ctrl2, prop)
            if (delta1 == null || delta2 == null) {
                // not the cleanest, but it'll do for now
                return
            }

            val result =
                when (compare(delta1, delta2)) {
                    ComparisonResult.equal -> "equal"
                    ComparisonResult.strictlyMoreRobust -> "delta1 strictly MORE robust than delta2"
                    ComparisonResult.strictlyLessRobust -> "delta1 strictly LESS robust than delta2"
                    ComparisonResult.incomparable -> "delta1 is incomparable to delta2"
                }
            if (!silent) {
                println("Comparison result: $result")
            }
        }

        /* compare robustness for a controller to an env w/respect to two different props */
        else if (compareProp) {
            if (ctrlFile.size != 1) {
                println("Exactly one controller file is required.")
                return
            }
            if (propFile.size != 2) {
                println("Exactly two property files are required for comparison.")
                return
            }
            val env = stripTauTransitions(fspToNFA(envFile))
            val ctrl = stripTauTransitions(fspToNFA(ctrlFile[0]))
            val prop1 = fspToDFA(propFile[0])
            val prop2 = fspToDFA(propFile[1])

            val delta1 = calcDelta(env, ctrl, prop1)
            val delta2 = calcDelta(env, ctrl, prop2)
            if (delta1 == null || delta2 == null) {
                // not the cleanest, but it'll do for now
                return
            }

            val result =
                when (compare(delta1, delta2)) {
                    ComparisonResult.equal -> "equal"
                    ComparisonResult.strictlyMoreRobust -> "delta1 strictly MORE robust than delta2"
                    ComparisonResult.strictlyLessRobust -> "delta1 strictly LESS robust than delta2"
                    ComparisonResult.incomparable -> "delta1 is incomparable to delta2"
                }
            if (!silent) {
                println("Comparison result: $result")
            }
        }

        /* calculate robustness for a single env, ctrl, prop */
        else {
            if (ctrlFile.size != 1) {
                println("Exactly one controller file is required.")
                return
            }
            if (propFile.size != 1) {
                println("Exactly one property file is required.")
                return
            }
            val env = stripTauTransitions(fspToNFA(envFile))
            val ctrl = stripTauTransitions(fspToNFA(ctrlFile[0]))
            val prop = fspToDFA(propFile[0])

            val delta = calcDelta(env, ctrl, prop)
            if (delta == null) {
                // not the cleanest, but it'll do for now
                return
            }

            if (!silent) {
                println("#delta: ${delta.size}")
                if (printDOT) {
                    printDOT(delta, env, ctrl, numPrint)
                }
                else if (printFSP) {
                    printFSP(delta, env, numPrint)
                }
                else if (printLargestDeltaSize) {
                    printLargestDeltaSz(delta)
                }
                else {
                    printDelta(delta, numPrint)
                }
            }
        }
    }
}

fun main(args : Array<String>) = ToleranceApp().main(args)
