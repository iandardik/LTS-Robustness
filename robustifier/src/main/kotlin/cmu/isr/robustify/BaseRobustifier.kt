package cmu.isr.robustify

import net.automatalib.automata.fsa.DFA
import net.automatalib.words.Alphabet


/**
 * @param sys The LTS of the system specification.
 * @param devEnv The deviated environment.
 * @param safety The LTS of the safety property. It does not need to be complete.
 */
abstract class BaseRobustifier<S, I>(
  val sys: DFA<*, I>,
  val sysInputs: Alphabet<I>,
  val devEnv: DFA<*, I>,
  val envInputs: Alphabet<I>,
  val safety: DFA<*, I>,
  val safetyInputs: Alphabet<I>
) {

  /**
   * Synthesize a new system model such that it satisfies the safety property under the deviated environment.
   */
  abstract fun synthesize(): DFA<S, I>?
}
