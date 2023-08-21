package cmu.isr.ts

import cmu.isr.ts.nfa.NFAParallelComposition
import net.automatalib.automata.concepts.InputAlphabetHolder
import net.automatalib.automata.fsa.NFA
import net.automatalib.words.Alphabet

/*
fun <I> NFA<*, I>.alphabet(): Alphabet<I> {
  if (this is InputAlphabetHolder<*>) {
    return this.inputAlphabet as Alphabet<I>
  } else if (this is NFAParallelComposition<*,*,I>) {
      return (this as NFAParallelComposition<*,*,I>).alphabet()
  } else {
    error("Instance '${this.javaClass}' does not support getting alphabet")
  }
}
 */

fun <I> NFA<*, I>.alphabet(): Alphabet<I> {
  if (this is InputAlphabetHolder<*>) {
    return this.inputAlphabet as Alphabet<I>
  } else {
    error("Instance '${this.javaClass}' does not support getting alphabet")
  }
}