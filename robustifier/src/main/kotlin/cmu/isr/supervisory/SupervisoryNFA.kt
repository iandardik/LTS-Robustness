package cmu.isr.supervisory

import net.automatalib.automata.fsa.NFA
import net.automatalib.automata.fsa.impl.compact.CompactNFA

interface SupervisoryNFA<S, I> : NFA<S, I> {

  val controllable: Collection<I>

  val observable: Collection<I>

}

class CompactSupNFA<I>(
  nfa: CompactNFA<I>,
  override val controllable: Collection<I>,
  override val observable: Collection<I>
) : CompactNFA<I>(nfa.inputAlphabet, nfa), SupervisoryNFA<Int, I> {

  override fun getSuccessor(transition: Int?): Int {
    return super<CompactNFA>.getSuccessor(transition)
  }

  override fun getTransitionProperty(transition: Int?): Void? {
    return super<CompactNFA>.getTransitionProperty(transition)
  }

}

fun <I> CompactNFA<I>.asSupNFA(controllable: Collection<I>, observable: Collection<I>): CompactSupNFA<I> {
  if (!inputAlphabet.containsAll(controllable) || !inputAlphabet.containsAll(observable))
    error("controllable and observable should be subsets of the alphabet")
  return CompactSupNFA(this, controllable, observable)
}