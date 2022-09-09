package cmu.isr.supervisory

import cmu.isr.ts.alphabet
import net.automatalib.automata.fsa.NFA

open class SupervisoryNFA<S, I>(
  private val nfa: NFA<S, I>,
  val controllable: Collection<I>,
  val observable: Collection<I>
) : NFA<S, I> by nfa {
  fun asNFA(): NFA<S, I> = nfa
}

fun <S, I> NFA<S, I>.asSupNFA(controllable: Collection<I>, observable: Collection<I>): SupervisoryNFA<S, I> {
  if (!alphabet().containsAll(controllable) || !alphabet().containsAll(observable))
    error("controllable and observable should be subsets of the alphabet")
  return SupervisoryNFA(this, controllable, observable)
}