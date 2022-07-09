package cmu.isr.robustify.supervisory

import net.automatalib.automata.fsa.DFA
import net.automatalib.commons.util.Holder
import net.automatalib.util.automata.builders.AutomatonBuilders
import net.automatalib.util.ts.traversal.TSTraversal
import net.automatalib.util.ts.traversal.TSTraversalAction
import net.automatalib.util.ts.traversal.TSTraversalVisitor
import net.automatalib.words.Alphabet
import net.automatalib.words.Word
import net.automatalib.words.impl.Alphabets


/**
 *
 */
fun <I> acceptsSubWord(sup: DFA<*, I>, inputs: Alphabet<I>, word: Word<I>): Boolean {
  // build automata from the word
  val builder = AutomatonBuilders.newDFA(Alphabets.fromCollection(word.distinct()))
    .withInitial(0)
    .withAccepting(0)
  var s = 0
  for (input in word) {
    builder.from(s).on(input).to(++s).withAccepting(s)
  }
  val wordDFA = builder.create()

  val composition = DFAParallelComposition(sup, inputs, wordDFA, wordDFA.inputAlphabet)
  val result = booleanArrayOf(false)
  TSTraversal.depthFirst(composition, inputs + wordDFA.inputAlphabet, AcceptsSubWordVisitor(wordDFA, result))

  return result[0]
}


private class AcceptsSubWordVisitor<S1, S2, I, T>(
  private val wordDFA: DFA<S2, I>,
  private val result: BooleanArray
) : TSTraversalVisitor<Pair<S1, S2>, I, T, Void> {

  private val visited = mutableSetOf<Pair<S1, S2>>()
  private val visitedS2 = mutableSetOf<S2>()

  override fun processInitial(state: Pair<S1, S2>, outData: Holder<Void>): TSTraversalAction {
    return TSTraversalAction.EXPLORE
  }

  override fun startExploration(state: Pair<S1, S2>, data: Void?): Boolean {
    return if (state !in visited) {
      visited.add(state)
      visitedS2.add(state.second)
      true
    } else {
      false
    }
  }

  override fun processTransition(
    source: Pair<S1, S2>,
    srcData: Void?,
    input: I,
    transition: T,
    succ: Pair<S1, S2>,
    outData: Holder<Void>
  ): TSTraversalAction {
    visitedS2.add(succ.second)
    return if (visitedS2.size == wordDFA.size()) {
      result[0] = true
      TSTraversalAction.ABORT_TRAVERSAL
    } else {
      TSTraversalAction.EXPLORE
    }
  }

}