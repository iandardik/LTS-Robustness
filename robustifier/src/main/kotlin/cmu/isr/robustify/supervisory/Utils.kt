package cmu.isr.robustify.supervisory

import cmu.isr.lts.CompactDetLTS
import cmu.isr.lts.DetLTS
import cmu.isr.lts.asLTS
import net.automatalib.automata.fsa.impl.compact.CompactDFA
import net.automatalib.util.automata.builders.AutomatonBuilders
import net.automatalib.util.ts.copy.TSCopy
import net.automatalib.util.ts.traversal.TSTraversal
import net.automatalib.util.ts.traversal.TSTraversalMethod
import net.automatalib.words.Alphabet
import net.automatalib.words.impl.Alphabets


fun <S, I, T> extendAlphabet(lts: DetLTS<S, I, T>, old: Alphabet<I>, extended: Alphabet<I>): CompactDetLTS<I> {
  val out = CompactDFA(extended)
  TSCopy.copy(TSTraversalMethod.DEPTH_FIRST, lts, TSTraversal.NO_LIMIT, old, out)

  val outLTS = out.asLTS()
  for (state in outLTS) {
    if (outLTS.isErrorState(state))
      continue
    for (a in extended - old) {
      outLTS.addTransition(state, a, state, null)
    }
  }
  return outLTS
}


fun <I> makeProgress(input: I): CompactDFA<I> {
  val inputs = Alphabets.fromArray(input)
  return AutomatonBuilders.newDFA(inputs)
    .withInitial(0)
    .from(0).on(input).to(1)
    .from(1).on(input).to(1)
    .withAccepting(1)
    .create()
}