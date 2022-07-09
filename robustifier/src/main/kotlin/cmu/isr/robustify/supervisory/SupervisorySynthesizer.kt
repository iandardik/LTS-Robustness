package cmu.isr.robustify.supervisory

import net.automatalib.words.Alphabet

interface SupervisorySynthesizer<S, I> {
  fun synthesize(
    plant: SupervisoryDFA<*, I>, inputs1: Alphabet<I>,
    prop: SupervisoryDFA<*, I>, inputs2: Alphabet<I>
  ): SupervisoryDFA<S, I>?

  fun checkAlphabets(plant: SupervisoryDFA<*, I>, inputs1: Alphabet<I>,
                     prop: SupervisoryDFA<*, I>, inputs2: Alphabet<I>) {
    val common = inputs1 intersect inputs2
    for (input in common) {
      if (!((input !in plant.controllable || input in prop.controllable) && // plant => prop
          (input !in prop.controllable || input in plant.controllable))) // prop => plant
        error("The plant and the property should have the same controllable")
      if (!((input !in plant.observable || input in prop.observable) && // plant => prop
        (input !in prop.observable || input in plant.observable))) // prop => plant
        error("The plant and the property should have the same observable")
    }
  }
}