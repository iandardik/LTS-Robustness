package cmu.isr.dfa

import net.automatalib.automata.fsa.DFA
import net.automatalib.automata.fsa.impl.compact.CompactDFA
import net.automatalib.commons.util.Holder
import net.automatalib.util.ts.traversal.TSTraversal
import net.automatalib.util.ts.traversal.TSTraversalAction
import net.automatalib.util.ts.traversal.TSTraversalVisitor
import net.automatalib.words.Alphabet
import net.automatalib.words.impl.Alphabets


class DFAParallelComposition<S1, S2, I>(
  private val dfa1: DFA<S1, I>,
  private val inputs1: Alphabet<I>,
  private val dfa2: DFA<S2, I>,
  private val inputs2: Alphabet<I>
) : DFA<Pair<S1, S2>, I> {

  override fun getStates(): MutableCollection<Pair<S1, S2>> {
    error("Cannot generate all states without traversal.")
//    return dfa1.states.flatMap { s1 -> dfa2.states.map { s2 -> Pair(s1, s2) } }.toMutableList()
  }

  override fun getInitialState(): Pair<S1, S2> {
    return Pair(dfa1.initialState!!, dfa2.initialState!!)
  }

  override fun isAccepting(state: Pair<S1, S2>): Boolean {
    return dfa1.isAccepting(state.first) && dfa2.isAccepting(state.second)
  }

  override fun getTransition(state: Pair<S1, S2>, input: I): Pair<S1, S2>? {
    val s1 = state.first
    val s2 = state.second
    return when {
      (input in inputs1 && input in inputs2) -> {
        val t1 = dfa1.getTransition(s1, input)
        val t2 = dfa2.getTransition(s2, input)
        if (t1 == null || t2 == null) null else Pair(t1, t2)
      }
      (input in inputs1) -> {
        val t1 = dfa1.getTransition(s1, input)
        if (t1 == null) null else Pair(t1, s2)
      }
      else -> {
        val t2 = dfa2.getTransition(s2, input)
        if (t2 == null) null else Pair(s1, t2)
      }
    }
  }

}

private class DFAParallelCompositionVisitor<S, I>(
  val comp: DFA<S, I>,
  val out: CompactDFA<I>
) : TSTraversalVisitor<S, I, S, Int> {

  private val visited = mutableSetOf<S>()
  private val stateMapping = mutableMapOf<S, Int>()

  override fun processInitial(state: S, outData: Holder<Int>): TSTraversalAction {
    val initState = out.addInitialState(comp.isAccepting(state))
    stateMapping[state] = initState
    outData.value = initState
    return TSTraversalAction.EXPLORE
  }

  override fun startExploration(state: S, data: Int): Boolean {
    return if (state !in visited) {
      visited.add(state)
      true
    } else {
      false
    }
  }

  override fun processTransition(
    source: S,
    srcData: Int,
    input: I,
    transition: S,
    succ: S,
    outData: Holder<Int>
  ): TSTraversalAction {
    val succState = if (succ in stateMapping) {
      stateMapping[succ]!!
    } else {
      val s = out.addState(comp.isAccepting(succ))
      stateMapping[succ] = s
      s
    }
    outData.value = succState
    out.addTransition(srcData, input, succState, null)
    return TSTraversalAction.EXPLORE
  }

}

fun <S1, S2, I> parallelComposition(dfa1: DFA<S1, I>, inputs1: Alphabet<I>,
                                    dfa2: DFA<S2, I>, inputs2: Alphabet<I>): CompactDFA<I> {
  val inputs = Alphabets.fromCollection(inputs1 union inputs2)
  val out = CompactDFA(inputs)
  val composition = DFAParallelComposition(dfa1, inputs1, dfa2, inputs2)

  TSTraversal.depthFirst(composition, inputs, DFAParallelCompositionVisitor(composition, out))
  return out
}