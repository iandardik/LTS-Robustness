package cmu.isr.tolerance.utils

import cmu.isr.ts.lts.CompactDetLTS
import cmu.isr.ts.lts.asLTS
import cmu.isr.ts.lts.CompactLTS
import cmu.isr.ts.lts.ltsa.LTSACall
import cmu.isr.ts.lts.ltsa.LTSACall.asDetLTS
import cmu.isr.ts.lts.ltsa.LTSACall.asLTS
import cmu.isr.ts.lts.ltsa.LTSACall.compose
import net.automatalib.util.automata.builders.AutomatonBuilders
import net.automatalib.words.impl.Alphabets
import java.io.File

object FspUtils {
    fun stripTauTransitions(lts: CompactLTS<String>): CompactLTS<String> {
        val newAlphabet = Alphabets.fromCollection(lts.inputAlphabet.toSet() - "tau")
        val newNFA = AutomatonBuilders.newNFA(newAlphabet).create()
        for (s in lts.states) {
            if (lts.initialStates.contains(s)) {
                newNFA.addInitialState(lts.isAccepting(s))
            } else {
                newNFA.addState(lts.isAccepting(s))
            }
        }
        for (t in ltsTransitions(lts)) {
            if (t.second != "tau") {
                newNFA.addTransition(t.first, t.second, t.third)
            }
        }
        return newNFA.asLTS()
    }

    fun fspToDFA(path: String): CompactDetLTS<String> {
        val spec = File(path).readText()
        val composite = LTSACall.compile(spec).compose()
        return composite.asDetLTS() as CompactDetLTS
    }

    fun fspToNFA(path: String): CompactLTS<String> {
        val spec = File(path).readText()
        val composite = LTSACall.compile(spec).compose()
        return composite.asLTS() as CompactLTS
    }

    fun fspStringToDFA(spec: String): CompactDetLTS<String> {
        val composite = LTSACall.compile(spec).compose()
        return composite.asDetLTS() as CompactDetLTS
    }

    fun fspStringToNFA(spec: String): CompactLTS<String> {
        val composite = LTSACall.compile(spec).compose()
        return composite.asLTS() as CompactLTS
    }
}