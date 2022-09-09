package cmu.isr.assumption

import cmu.isr.ts.*
import cmu.isr.ts.lts.hide
import cmu.isr.ts.lts.makeErrorState

class SubsetConstructionGenerator<I>(
  private val sys: LTS<*, I>,
  private val env: LTS<*, I>,
  private val safety: DetLTS<*, I>
) {
  private val assumptionInputs: Collection<I>

  init {
    val common = sys.alphabet() intersect env.alphabet()
    val internal = sys.alphabet() - common
    assumptionInputs = common union (safety.alphabet() - internal.toSet())
  }

  fun generate(sink: Boolean = false): DetLTS<Int, I> {
    // 1. compose sys || safety_err
    val comp = parallel(sys, makeErrorState(safety as MutableDetLTS) as LTS<*, I>) as MutableLTS
    // 2. prune the error state by backtracking from the initial error state
    val predecessors = Predecessors(comp)
    val queue = ArrayDeque<Int>()
    val hidden = comp.alphabet().toSet() - assumptionInputs.toSet()

    for (input in hidden)
      queue.addAll(predecessors.getPredecessors(comp.errorState, input).map { it.source })

    while (queue.isNotEmpty()) {
      // make this state an error state
      val state = queue.removeFirst()
      if (state in comp.initialStates)
        error("Initial state becomes the error state, no environment can prevent the system from reaching error state")

      // Remove out-going transitions
      comp.removeAllTransitions(state)
      // For all predecessors of this state, redirect them to the error state
      for (input in comp.alphabet()) {
        for ((transition, source) in predecessors.getPredecessors(state, input)) {
          comp.removeTransition(source, input, transition)
          comp.addTransition(source, input, comp.errorState, null)

          if (input in hidden)
            queue.addLast(source)
        }
      }
    }
    // 3. hide and determinise
    val wa = hide(comp, hidden) as MutableDetLTS
    // 4. make sink
    if (sink) {
      val theta = wa.addState(true)
      for (state in wa) {
        if (wa.isErrorState(state))
          continue
        for (input in wa.alphabet()) {
          if (wa.getSuccessor(state, input) == null)
            wa.addTransition(state, input, theta, null)
        }
      }
    }
    // 5. remove error state
    val waPredecessors = Predecessors(wa)
    for (input in wa.alphabet()) {
      for ((transition, source) in waPredecessors.getPredecessors(wa.errorState, input)) {
        wa.removeTransition(source, input, transition)
      }
    }

    return wa
  }
}