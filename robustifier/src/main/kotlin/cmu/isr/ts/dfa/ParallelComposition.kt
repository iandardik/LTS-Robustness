package cmu.isr.ts.dfa

import cmu.isr.ts.nfa.NFAParallelComposition
import net.automatalib.automata.fsa.DFA
import net.automatalib.automata.fsa.impl.compact.CompactDFA
import net.automatalib.util.ts.copy.TSCopy
import net.automatalib.util.ts.traversal.TSTraversal
import net.automatalib.util.ts.traversal.TSTraversalMethod
import net.automatalib.words.Alphabet
import net.automatalib.words.impl.Alphabets

fun <S1, S2, I> parallelComposition(dfa1: DFA<S1, I>, inputs1: Alphabet<I>,
                                    dfa2: DFA<S2, I>, inputs2: Alphabet<I>): CompactDFA<I> {
  val inputs = Alphabets.fromCollection(inputs1 union inputs2)
  val out = CompactDFA(inputs)
  val composition = NFAParallelComposition(dfa1, inputs1, dfa2, inputs2)

  TSCopy.copy(TSTraversalMethod.DEPTH_FIRST, composition, TSTraversal.NO_LIMIT, inputs, out)
  return out
}

fun <I> parallelComposition(vararg dfas: CompactDFA<I>): CompactDFA<I> {
  if (dfas.isEmpty())
    error("Should provide at least one model")
  if (dfas.size == 1)
    return dfas[0]
  var c = dfas[0]
  for (i in 1 until dfas.size)
    c = parallelComposition(c, c.inputAlphabet, dfas[i], dfas[i].inputAlphabet)
  return c
}