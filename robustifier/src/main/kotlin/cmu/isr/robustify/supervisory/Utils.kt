package cmu.isr.robustify.supervisory

import cmu.isr.supervisory.SupervisoryDFA
import cmu.isr.supervisory.asSupDFA
import cmu.isr.ts.alphabet
import cmu.isr.ts.nfa.hide
import net.automatalib.automata.fsa.DFA
import net.automatalib.automata.fsa.impl.compact.CompactDFA
import net.automatalib.commons.util.Holder
import net.automatalib.ts.UniversalDTS
import net.automatalib.util.automata.builders.AutomatonBuilders
import net.automatalib.util.ts.copy.TSCopy
import net.automatalib.util.ts.traversal.TSTraversal
import net.automatalib.util.ts.traversal.TSTraversalAction
import net.automatalib.util.ts.traversal.TSTraversalMethod
import net.automatalib.util.ts.traversal.TSTraversalVisitor
import net.automatalib.words.Alphabet
import net.automatalib.words.impl.Alphabets


fun <S, I, T> extendAlphabet(lts: UniversalDTS<S, I, T, Boolean, Void?>, old: Alphabet<I>, extended: Alphabet<I>): CompactDFA<I> {
  val out = CompactDFA(extended)
  TSCopy.copy(TSTraversalMethod.DEPTH_FIRST, lts, TSTraversal.NO_LIMIT, old, out)

  for (state in out) {
    for (a in extended - old) {
      out.addTransition(state, a, state, null)
    }
  }
  return out
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


/**
 * @param dfa the DFA to be observed
 * @param inputs the alphabet of the DFA
 */
fun <S, I> observer(dfa: SupervisoryDFA<S, I>, inputs: Alphabet<I>): SupervisoryDFA<Int, I> {
  val out = hide(dfa, inputs - dfa.observable.toSet())
  return out.asSupDFA(dfa.controllable intersect dfa.observable.toSet(), dfa.observable)
}


fun <S, I> DFA<S, I>.numOfTransitions(): Int {

  class TransitionVisitor(val result: Array<Int>): TSTraversalVisitor<S, I, S, Void?> {
    private val visited = mutableSetOf<S>()
    override fun processInitial(state: S, outData: Holder<Void?>): TSTraversalAction {
      result[0] = 0
      return TSTraversalAction.EXPLORE
    }

    override fun startExploration(state: S, data: Void?): Boolean {
      return if (state !in visited) {
        visited.add(state)
        true
      } else {
        false
      }
    }

    override fun processTransition(
      source: S,
      srcData: Void?,
      input: I,
      transition: S,
      succ: S,
      outData: Holder<Void?>
    ): TSTraversalAction {
      result[0]++
      return TSTraversalAction.EXPLORE
    }

  }

  val result = arrayOf(0)
  TSTraversal.depthFirst(this, this.alphabet(), TransitionVisitor(result))
  return result[0]
}