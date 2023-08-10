package cmu.isr.tolerance

import cmu.isr.assumption.SubsetConstructionGenerator
import cmu.isr.tolerance.utils.*
import cmu.isr.ts.DetLTS

fun main(args : Array<String>) {
    //var prop : DetLTS<Int, String> = toDeterministic(fspToNFA(args[0]))
    var prop : DetLTS<Int, String> = FspUtils.fspToDFA(args[0])
    for (i in 1 until args.size) {
        val file = args[i]
        println("iteration $i: $file")
        //val comp = FspUtils.stripTauTransitions(FspUtils.fspToNFA(file))
        val comp = FspUtils.fspToNFA(file)

        // if the component satisfies the property (i.e. wa is TRUE) then return success
        if (LtsUtils.satisfies(comp, prop)) {
            println("Property satisfied")
            return
        }
        // otherwise, generate the WA and recurse
        else {
            val waGen = SubsetConstructionGenerator(comp, prop)
            prop = waGen.generate(true)
        }
    }
    println("Property may be violated")
}
