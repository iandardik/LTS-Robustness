package cmu.isr.lts

import net.automatalib.automata.MutableDeterministic
import net.automatalib.automata.fsa.impl.compact.CompactDFA
import net.automatalib.ts.UniversalDTS


interface DetLTS<S, I, T> : UniversalDTS<S, I, T, Boolean, Void?> {

  val errorState: S

  fun isErrorState(state: S): Boolean

}


interface MutableDetLTS<S, I, T> : DetLTS<S, I, T>, MutableDeterministic<S, I, T, Boolean, Void?>


/**
 * A MutableDetLTS implementation using CompactDFA such that all the states are acceptable except the
 * error state. The default constructor takes a CompactDFA as the input which should meet the
 * aforementioned requirement. If there's no error state (the only one unacceptable state), then the
 * constructor will add one unreachable state as the error state.
 */
class CompactDetLTS<I>(dfa: CompactDFA<I>) : CompactDFA<I>(dfa), MutableDetLTS<Int, I, Int> {

  private val _errorState: Int

  init {
    // Check that there should be at most one error state, that is marked as unacceptable.
    val unacceptable = states.filter { !isAccepting(it) }
    if (unacceptable.size > 1)
      throw Error("There should be one error state in LTS which might be unreachable.")
    _errorState = if (unacceptable.isEmpty())
      addState(false)
    else
      unacceptable[0]
  }

  override val errorState: Int
    get() = _errorState

  override fun isErrorState(state: Int): Boolean {
    return !isAccepting(state)
  }

}


/**
 * Convert a CompactDFA to a CompactDetLTS.
 */
fun <I> CompactDFA<I>.asLTS(): CompactDetLTS<I> {
  return CompactDetLTS(this)
}
