package cmu.isr.tolerance

import net.automatalib.automata.fsa.NFA
import net.automatalib.commons.util.Holder
import net.automatalib.util.ts.traversal.TSTraversal
import net.automatalib.util.ts.traversal.TSTraversalAction
import net.automatalib.util.ts.traversal.TSTraversalVisitor
import net.automatalib.words.Alphabet
import java.io.OutputStream

fun <S, I> writeDOT(output : OutputStream, lts : NFA<S, I>, inputs : Alphabet<I>) {
    val labels = mutableMapOf<Pair<S,S>, List<I>>()
    TSTraversal.breadthFirst(lts, inputs, DOTVisitor(labels))

    val writer = output.writer()
    writer.appendLine("digraph G {")
    labels.forEach { (s, el) ->
        val src = s.first
        val dst = s.second
        if (lts.isAccepting(src) && lts.isAccepting(dst)) {
            val labels = el.joinToString()
            writer.appendLine("  $src -> $dst [label=\"$labels\"]")
        }
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
