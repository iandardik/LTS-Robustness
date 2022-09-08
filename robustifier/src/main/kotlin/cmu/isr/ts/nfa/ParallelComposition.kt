package cmu.isr.ts.nfa

import net.automatalib.automata.fsa.NFA
import net.automatalib.automata.fsa.impl.compact.CompactNFA
import net.automatalib.ts.UniversalTransitionSystem
import net.automatalib.util.ts.copy.TSCopy
import net.automatalib.util.ts.traversal.TSTraversal
import net.automatalib.util.ts.traversal.TSTraversalMethod
import net.automatalib.words.Alphabet
import net.automatalib.words.impl.Alphabets

class NFAParallelComposition<S1, S2, I>(
  private val nfa1: NFA<S1, I>,
  private val inputs1: Alphabet<I>,
  private val nfa2: NFA<S2, I>,
  private val inputs2: Alphabet<I>
) : UniversalTransitionSystem<Pair<S1, S2>, I, Pair<S1, S2>, Boolean, Void?> {

  override fun getInitialStates(): Set<Pair<S1, S2>> {
    return nfa1.initialStates.flatMap { s1 -> nfa2.initialStates.map { s2 -> Pair(s1, s2) } }.toSet()
  }

  override fun getTransitionProperty(transition: Pair<S1, S2>?): Void? {
    return null
  }

  override fun getStateProperty(state: Pair<S1, S2>): Boolean {
    return nfa1.getStateProperty(state.first) && nfa2.getStateProperty(state.second)
  }

  override fun getSuccessor(transition: Pair<S1, S2>): Pair<S1, S2> {
    return transition
  }

  override fun getTransitions(state: Pair<S1, S2>, input: I): Collection<Pair<S1, S2>> {
    val s1 = state.first
    val s2 = state.second
    return when {
      (input in inputs1 && input in inputs2) -> {
        val t1s = nfa1.getTransitions(s1, input)
        val t2s = nfa2.getTransitions(s2, input)
        if (t1s.isEmpty() || t2s.isEmpty())
          emptyList()
        else
          t1s.flatMap { t1 -> t2s.map { t2 -> Pair(t1, t2) } }
      }
      (input in inputs1) -> {
        val t1s = nfa1.getTransitions(s1, input)
        if (t1s.isEmpty())
          emptyList()
        else
          t1s.map { t1 -> Pair(t1, s2) }
      }
      else -> {
        val t2s = nfa2.getTransitions(s2, input)
        if (t2s.isEmpty())
          emptyList()
        else
          t2s.map { t2 -> Pair(s1, t2) }
      }
    }
  }

}

fun <S1, S2, I> parallelComposition(nfa1: NFA<S1, I>, inputs1: Alphabet<I>,
                                    nfa2: NFA<S2, I>, inputs2: Alphabet<I>): CompactNFA<I> {
  val inputs = Alphabets.fromCollection(inputs1 union inputs2)
  val out = CompactNFA(inputs)
  val composition = NFAParallelComposition(nfa1, inputs1, nfa2, inputs2)

  TSCopy.copy(TSTraversalMethod.DEPTH_FIRST, composition, TSTraversal.NO_LIMIT, inputs, out)
  return out
}

fun <I> parallelComposition(vararg nfas: CompactNFA<I>): CompactNFA<I> {
  if (nfas.isEmpty())
    error("Should provide at least one model")
  if (nfas.size == 1)
    return nfas[0]
  var c = nfas[0]
  for (i in 1 until nfas.size)
    c = parallelComposition(c, c.inputAlphabet, nfas[i], nfas[i].inputAlphabet)
  return c
}