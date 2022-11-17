package cmu.isr.tolerance.postprocess

import cmu.isr.tolerance.utils.ltsTransitions
import cmu.isr.ts.alphabet
import net.automatalib.automata.fsa.NFA
import net.automatalib.commons.util.Holder
import net.automatalib.util.ts.traversal.TSTraversal
import net.automatalib.util.ts.traversal.TSTraversalAction
import net.automatalib.util.ts.traversal.TSTraversalVisitor
import net.automatalib.words.Alphabet
import java.io.OutputStream

fun <S, I> writeDOT(output : OutputStream, lts : NFA<S, I>, inputs : Alphabet<I>) {
    // TSTraversal only looks at reachable states
    //val labels = mutableMapOf<Pair<S,S>, List<I>>()
    //TSTraversal.breadthFirst(lts, inputs, DOTVisitor(labels))
    val labels = mutableMapOf<Pair<String,String>, MutableSet<I>>()
    for (edge in ltsTransitions(lts)) {
        val src = edge.first
        val dst = edge.third
        val key =
            if (lts.isAccepting(src) && lts.isAccepting(dst)) {
                Pair("$src", "$dst")
            }
            else if (lts.isAccepting(src)) {
                Pair("$src", "err")
            }
            else {
                continue
            }
        val newVal = edge.second
        if (key !in labels) {
            labels[key] = mutableSetOf()
        }
        labels[key]?.add(newVal)
    }

    val writer = output.writer()
    writer.appendLine("digraph G {")
    labels.forEach { (s, el) ->
        val src = s.first
        val dst = s.second
        val labels = el.joinToString()
        writer.appendLine("  \"$src\" -> \"$dst\" [label=\"$labels\"]")
    }
    writer.appendLine("}")
    writer.flush()
}

private class DOTVisitor<S, I>(
    val labels: MutableMap<Pair<S,S>, List<I>>
) : TSTraversalVisitor<S, I, S, Void?> {
    private val visited = mutableSetOf<S>()

    override fun processInitial(state: S, outData: Holder<Void?>): TSTraversalAction {
        return TSTraversalAction.EXPLORE
    }

    override fun startExploration(state: S, data: Void?): Boolean {
        val cont = state !in visited
        visited.add(state)
        return cont
    }

    override fun processTransition(
        source: S,
        srcData: Void?,
        input: I,
        transition: S,
        succ: S,
        outData: Holder<Void?>?
    ): TSTraversalAction {
        val key = Pair(source,succ)
        val ls = labels[key] ?: mutableListOf()
        labels[key] = ls + listOf(input)
        return TSTraversalAction.EXPLORE
    }

}
