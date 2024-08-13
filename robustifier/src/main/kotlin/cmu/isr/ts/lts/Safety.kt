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

object SafetyUtils {
  /**
   * Returns whether or not the given LTS is safe, i.e. whether or not an
   * error state is reachable is the LTS. This method assumes that the given
   * LTS already has error states made.
   */
  fun <I> ltsIsSafe(lts: LTS<Int, I>): Boolean {
    val hasInitErrorState = lts.initialStates.contains(lts.errorState)
    if (hasInitErrorState) {
      return false
    }
    val result = SafetyResult<I>()
    val vis = SafetyVisitor(lts, result)
    TSTraversal.breadthFirst(lts, lts.alphabet(), vis)
    return !result.violation
  }

  fun <I> hasErrInitState(lts: LTS<Int, I>): Boolean {
    val numErrInitStates = lts.initialStates
      .filter { s -> lts.isErrorState(s) }
      .size
    return numErrInitStates > 0
  }

  /**
   * Returns whether lts |= prop, but assumes that prop already contains an
   * error state.
   */
  fun <I> satisfiesNotProp(lts: LTS<Int, I>, prop: LTS<Int, I>): Boolean {
    val c = parallelComposition(lts, prop)
    val result = SafetyResult<I>()
    val vis = SafetyVisitor(c, result)
    TSTraversal.breadthFirst(c, c.alphabet(), vis)
    //TSTraversal.depthFirst(c, c.alphabet(), vis)
    return !result.violation
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

  fun <I> findErrorTrace(lts: LTS<Int, I>): Word<I> {
    val hasInitErrorState = lts.initialStates.contains(lts.errorState)
    if (hasInitErrorState) {
      return Word.epsilon()
    }
    val result = SafetyResult<I>()
    val vis = SafetyVisitor(lts, result)
    TSTraversal.breadthFirst(lts, lts.alphabet(), vis)
    return result.trace ?: error("No error found!")
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
}
