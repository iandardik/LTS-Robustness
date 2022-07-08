package cmu.isr.robustify.supervisory

import net.automatalib.words.Alphabet

interface SupervisorySynthesizer<S, I> {
  fun synthesize(
    plant: SupervisoryDFA<*, I>, inputs1: Alphabet<I>,
    prop: SupervisoryDFA<*, I>, inputs2: Alphabet<I>
  ): SupervisoryDFA<S, I>?
}