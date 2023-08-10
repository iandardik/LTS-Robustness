package cmu.isr.ts.lts

import cmu.isr.ts.DetLTS
import cmu.isr.ts.LTS
import cmu.isr.ts.MutableDetLTS
import cmu.isr.ts.alphabet
import net.automatalib.commons.util.Holder
import net.automatalib.util.ts.traversal.TSTraversal
import net.automatalib.util.ts.traversal.TSTraversalAction
import net.automatalib.util.ts.traversal.TSTraversalVisitor
import net.automatalib.words.Word


class SafetyResult<I> {
  var violation: Boolean = false
  var trace: Word<I>? = null

  override fun toString(): String {
    return if (violation) "Safety violation: $trace" else "No safety violation"
  }
}


class SafetyVisitor<S, I>(
  private val lts: LTS<S, I>,
  private val result: SafetyResult<I>
) : TSTraversalVisitor<S, I, S, Word<I>> {
  private val visited = mutableSetOf<S>()

  override fun processInitial(state: S, outData: Holder<Word<I>>?): TSTraversalAction {
    outData!!.value = Word.epsilon()
    return TSTraversalAction.EXPLORE
  }

  override fun startExploration(state: S, data: Word<I>?): Boolean {
    return if (state !in visited) {
      visited.add(state)
      true
    } else {
      false
    }
  }

  override fun processTransition(
    source: S,
    srcData: Word<I>?,
    input: I,
    transition: S,
    succ: S,
    outData: Holder<Word<I>>?
  ): TSTraversalAction {
    outData!!.value = srcData!!.append(input)
    if (lts.isErrorState(succ)) {
      result.violation = true
      result.trace = outData.value
      return TSTraversalAction.ABORT_TRAVERSAL
    }
    return TSTraversalAction.EXPLORE
  }

}


/**
 * Check the safety property of an LTS. The safety property is modeled as a complete LTS where unsafe transitions
 * lead to the error state.
 */
fun <I> checkSafety(lts: LTS<*, I>, prop: DetLTS<*, I>): SafetyResult<I> {
  val c = parallelComposition(lts, prop)
  val result = SafetyResult<I>()
  val vis = SafetyVisitor(c, result)
  TSTraversal.breadthFirst(c, c.alphabet(), vis)
  return result
}


/**
 * Given a safety property LTS, make it into a complete LTS where all unsafe transitions lead to the error state.
 */
fun <S, I> makeErrorState(prop: MutableDetLTS<S, I>): DetLTS<S, I> {
  for (s in prop.states) {
    if (prop.isErrorState(s))
      continue
    for (a in prop.alphabet()) {
      if (prop.getTransition(s, a) == null) {
        prop.addTransition(s, a, prop.errorState, null)
      }
    }
  }
  return prop
}
