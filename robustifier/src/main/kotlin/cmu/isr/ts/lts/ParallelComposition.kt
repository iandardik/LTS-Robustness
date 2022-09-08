package cmu.isr.ts.lts

import net.automatalib.automata.fsa.impl.compact.CompactDFA
import net.automatalib.util.ts.copy.TSCopy
import net.automatalib.util.ts.traversal.TSTraversal
import net.automatalib.util.ts.traversal.TSTraversalMethod
import net.automatalib.words.Alphabet
import net.automatalib.words.impl.Alphabets


/**
 * A helper class for composition. If the transition is not null, the LTS should follow the transition;
 * if the transition is null, the LTS should take a self-loop at the current state (loop state).
 */
private class TransOrLoop<S, T>(val loop: S, val trans: T?)


/**
 * Model the composition of two LTS's as a special automaton where the state is S1xS2.
 */
private class DetLTSParallelComposition<S1, S2, I, T1, T2, A1, A2>(
  private val ts1: A1,
  private val inputs1: Alphabet<I>,
  private val ts2: A2,
  private val inputs2: Alphabet<I>,
) : DetLTS<Pair<S1, S2>, I, Pair<TransOrLoop<S1, T1>, TransOrLoop<S2, T2>>>
    where A1 : DetLTS<S1, I, T1>,
          A2 : DetLTS<S2, I, T2>
{
  override val errorState: Pair<S1, S2>
    get() = Pair(ts1.errorState, ts2.errorState)

  override fun isErrorState(state: Pair<S1, S2>): Boolean {
    return ts1.isErrorState(state.first) || ts2.isErrorState(state.second)
  }

  override fun getSuccessor(transition: Pair<TransOrLoop<S1, T1>, TransOrLoop<S2, T2>>): Pair<S1, S2> {
    val t1 = transition.first
    val t2 = transition.second
    val succ = Pair(
      if (t1.trans == null) t1.loop else ts1.getSuccessor(t1.trans),
      if (t2.trans == null) t2.loop else ts2.getSuccessor(t2.trans)
    )
    // Merge all error states into one
    if (isErrorState(succ))
      return errorState
    return succ
  }

  override fun getStateProperty(state: Pair<S1, S2>): Boolean {
    return !isErrorState(state)
  }

  override fun getTransitionProperty(transition: Pair<TransOrLoop<S1, T1>, TransOrLoop<S2, T2>>): Void? {
    return null
  }

  override fun getInitialState(): Pair<S1, S2> {
    return Pair(ts1.initialState!!, ts2.initialState!!)
  }

  override fun getTransition(state: Pair<S1, S2>, input: I): Pair<TransOrLoop<S1, T1>, TransOrLoop<S2, T2>>? {
    if (isErrorState(state))
      return null
    val s1 = state.first
    val s2 = state.second
    return when {
      (input in inputs1 && input in inputs2) -> {
        val t1 = ts1.getTransition(s1, input)
        val t2 = ts2.getTransition(s2, input)
        if (t1 == null || t2 == null) null else Pair(TransOrLoop(s1, t1), TransOrLoop(s2, t2))
      }
      (input in inputs1) -> {
        val t1 = ts1.getTransition(s1, input)
        if (t1 == null) null else Pair(TransOrLoop(s1, t1), TransOrLoop(s2, null))
      }
      else -> {
        val t2 = ts2.getTransition(s2, input)
        if (t2 == null) null else Pair(TransOrLoop(s1, null), TransOrLoop(s2, t2))
      }
    }
  }

}


/**
 * Perform parallel composition of two deterministic LTS.
 */
fun <I> parallelComposition(lts1: DetLTS<*, I, *>, inputs1: Alphabet<I>,
                            lts2: DetLTS<*, I, *>, inputs2: Alphabet<I>): CompactDetLTS<I> {
  val inputs = Alphabets.fromCollection(inputs1.union(inputs2))
  val out = CompactDFA(inputs)
  val composition = DetLTSParallelComposition(lts1, inputs1, lts2, inputs2)

  TSCopy.copy(TSTraversalMethod.BREADTH_FIRST, composition, TSTraversal.NO_LIMIT, inputs, out)
  return out.asLTS()
}
