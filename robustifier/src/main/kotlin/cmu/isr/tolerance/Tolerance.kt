package cmu.isr.tolerance

import addPerturbations
import cmu.isr.assumption.SubsetConstructionGenerator
import cmu.isr.tolerance.delta.*
import cmu.isr.tolerance.postprocess.*
import cmu.isr.tolerance.utils.*
import cmu.isr.ts.DetLTS
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
import errorTrace
import net.automatalib.automata.fsa.impl.compact.CompactDFA
import net.automatalib.automata.fsa.impl.compact.CompactNFA
import satisfies
import toDeterministic
import java.io.File

fun main(args : Array<String>) {
    //var prop : DetLTS<Int, String> = toDeterministic(fspToNFA(args[0]))
    var prop : DetLTS<Int, String> = fspToDFA(args[0])
    for (i in 1 until args.size) {
        val file = args[i]
        println("iteration $i: $file")
        val comp = stripTauTransitions(fspToNFA(file))

        // if the component satisfies the property (i.e. wa is TRUE) then return success
        if (satisfies(comp, prop)) {
            println("Property satisfied")
            return
        }
        // otherwise, generate the WA and recurse
        else {
            val waGen = SubsetConstructionGenerator(comp, prop)
            prop = waGen.generate()
        }
    }
    println("Property may be violated")
}
