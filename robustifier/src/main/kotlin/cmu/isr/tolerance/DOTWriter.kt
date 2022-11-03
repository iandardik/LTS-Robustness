package cmu.isr.tolerance

import net.automatalib.automata.fsa.DFA
import net.automatalib.automata.fsa.NFA
import net.automatalib.commons.util.Holder
import net.automatalib.util.ts.traversal.TSTraversal
import net.automatalib.util.ts.traversal.TSTraversalAction
import net.automatalib.util.ts.traversal.TSTraversalVisitor
import net.automatalib.words.Alphabet
import java.io.OutputStream

fun <S, I> writeDOT(output: OutputStream, dfa: DFA<S, I>, inputs: Alphabet<I>) {
    val builder = StringBuilder()
    TSTraversal.breadthFirst(dfa, inputs, DOTWriterVisitor(builder, dfa, inputs))

    val writer = output.writer()
    writer.appendLine("digraph G {")
    writer.write(builder.toString())
    writer.appendLine("}")
    writer.flush()
}

private class DOTWriterVisitor<S, I>(
    val builder: StringBuilder,
    val dfa: DFA<S, I>,
    val inputs: Alphabet<I>
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
        builder.appendLine("$source -> $succ")
        return TSTraversalAction.EXPLORE
    }

}

fun <S, I> writeDOT(output: OutputStream, nfa: NFA<S, I>, inputs: Alphabet<I>) {
    val builder = StringBuilder()
    TSTraversal.breadthFirst(nfa, inputs, NFADOTWriterVisitor(builder, nfa, inputs))

    val writer = output.writer()
    writer.appendLine("digraph G {")
    writer.write(builder.toString())
    writer.appendLine("}")
    writer.flush()
}

private class NFADOTWriterVisitor<S, I>(
    val builder: StringBuilder,
    val dfa: NFA<S, I>,
    val inputs: Alphabet<I>
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
        builder.appendLine("$source -> $succ")
        return TSTraversalAction.EXPLORE
    }

}
