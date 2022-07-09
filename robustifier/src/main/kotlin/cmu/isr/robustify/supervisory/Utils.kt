package cmu.isr.robustify.supervisory

import net.automatalib.automata.fsa.impl.compact.CompactDFA
import net.automatalib.ts.UniversalDTS
import net.automatalib.util.automata.builders.AutomatonBuilders
import net.automatalib.util.ts.copy.TSCopy
import net.automatalib.util.ts.traversal.TSTraversal
import net.automatalib.util.ts.traversal.TSTraversalMethod
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


fun <E> List<E>.combinations(k: Int): Collection<Collection<E>> {
  if (k > this.size)
    error("k should not be bigger than the size of this list")

  val l = mutableListOf<Collection<E>>()
  val c = (0 until k).toMutableList()

  while (true) {
    l.add(c.map { this[it] })

    var i = k - 1
    while (i >= 0 && c[i] == this.size - k + i)
      i--
    if (i < 0)
      break
    c[i]++
    for (j in i+1 until k)
      c[j] = c[j-1] + 1
  }

  return l
}