package cmu.isr.ts.dfa

import cmu.isr.ts.nfa.tauElimination
import net.automatalib.automata.fsa.DFA
import net.automatalib.automata.fsa.impl.compact.CompactDFA
import net.automatalib.words.Alphabet
import net.automatalib.words.impl.Alphabets
import java.util.*

fun <S, I> reachableSet(dfa: DFA<S, I>, hidden: Collection<I>): Map<Int, BitSet> {
  return cmu.isr.ts.nfa.reachableSet(dfa, hidden)
}


fun <S, I> hide(dfa: DFA<S, I>, inputs: Alphabet<I>, hidden: Collection<I>): CompactDFA<I> {
  val observable = inputs - hidden.toSet()
  return tauElimination(dfa, hidden, observable, CompactDFA(Alphabets.fromCollection(observable))) as CompactDFA<I>
}