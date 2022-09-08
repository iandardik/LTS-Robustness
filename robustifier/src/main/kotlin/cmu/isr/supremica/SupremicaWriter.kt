package cmu.isr.supremica

import cmu.isr.robustify.supervisory.SupervisoryDFA
import net.automatalib.words.Alphabet
import org.supremica.automata.Arc
import org.supremica.automata.Automaton
import org.supremica.automata.LabeledEvent
import org.supremica.automata.State


fun <S, I> write(dfa: SupervisoryDFA<S, I>, inputs: Alphabet<I>, name: String): Automaton {
  val automaton = Automaton(name)
  val alphabetMap = mutableMapOf<I, LabeledEvent>()
  val stateMap = mutableMapOf<S, State>()

  fun getSupremicaState(state: S): State {
    if (state in stateMap)
      return stateMap[state]!!

    val s = automaton.createUniqueState(state.toString())
    s.isInitial = state == dfa.initialState
    s.isAccepting = dfa.isAccepting(state)
    s.isForbidden = false
    automaton.addState(s)
    stateMap[state] = s

    return s
  }

  for (input in inputs) {
    val label = LabeledEvent(input.toString())
    label.isControllable = input in dfa.controllable
    label.isObservable = input in dfa.observable
    label.isPrioritized = true
    automaton.alphabet.addEvent(label)
    alphabetMap[input] = label
  }

  for (state in dfa) {
    for (input in inputs) {
      val succ = dfa.getSuccessor(state, input)
      if (succ != null) {
        val arc = Arc(getSupremicaState(state), getSupremicaState(succ), alphabetMap[input])
        automaton.addArc(arc)
      }
    }
  }

  return automaton
}
