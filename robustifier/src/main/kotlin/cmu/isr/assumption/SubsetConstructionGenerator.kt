package cmu.isr.assumption

import cmu.isr.ts.*
import cmu.isr.ts.ParallelComposition.parallel
import cmu.isr.ts.lts.SafetyUtils.makeErrorState
import cmu.isr.ts.lts.hide

class SubsetConstructionGenerator<I>(
  private val sys: LTS<*, I>,
  private val safety: LTS<*, I>
) {
  private val assumptionInputs: Collection<I>

  init {
    val common = sys.alphabet()
    val internal = sys.alphabet() - common
    assumptionInputs = common union (safety.alphabet() - internal.toSet())
  }

  fun generate(sink: Boolean = false): LTS<Int, I> {
    // 1. compose sys || safety_err
    //val comp = parallel(sys, makeErrorState(safety as MutableDetLTS) as LTS<*, I>) as MutableLTS
    val comp = parallel(sys, safety) as MutableLTS
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
    //val wa = hide(comp, hidden) as MutableDetLTS
    val wa = comp
    // 4. make sink
    if (sink) {
      val theta = wa.addState(true)
      for (state in wa) {
        if (wa.isErrorState(state))
          continue
        for (input in wa.alphabet()) {
          if (wa.getSuccessors(state, input).size == 0)
            wa.addTransition(state, input, theta, null)
        }
      }
    }
    /*
    // 5. remove error state
    val waPredecessors = Predecessors(wa)
    for (input in wa.alphabet()) {
      for ((transition, source) in waPredecessors.getPredecessors(wa.errorState, input)) {
        wa.removeTransition(source, input, transition)
      }
    }
     */

    return wa
  }
}

object WAHelper {
  fun makeWA(inpLts : MutableDetLTS<Int, String>): DetLTS<Int, String> {
    val comp = makeErrorState(inpLts) as MutableDetLTS

    // 2. prune the error state by backtracking from the initial error state
    val predecessors = Predecessors(comp)
    val queue = ArrayDeque<Int>()
    val hidden = emptySet<String>()

    // 3. hide and determinise
    val wa = hide(comp, hidden) as MutableDetLTS
    // 4. make sink
    val theta = wa.addState(true)
    for (state in wa) {
      if (wa.isErrorState(state))
        continue
      for (input in wa.alphabet()) {
        if (wa.getSuccessor(state, input) == null)
          wa.addTransition(state, input, theta, null)
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

  /**
   * This method assumes that lts is a "property" LTS, i.e. it has an error state
   * with all expected transitions going to the error state.
   */
  fun addTheta(lts : MutableDetLTS<Int, String>): DetLTS<Int, String> {
    // add theta
    val theta = lts.addState(true)
    for (state in lts) {
      if (lts.isErrorState(state))
        continue
      for (input in lts.alphabet()) {
        if (lts.getSuccessor(state, input) == null)
          lts.addTransition(state, input, theta, null)
      }
    }

    // remove error state
    val waPredecessors = Predecessors(lts)
    for (input in lts.alphabet()) {
      for ((transition, source) in waPredecessors.getPredecessors(lts.errorState, input)) {
        lts.removeTransition(source, input, transition)
      }
    }

    return lts
  }

  fun addThetaNonDeterministic(lts : MutableLTS<Int, String>): LTS<Int, String> {
    // add theta
    val theta = lts.addState(true)
    for (state in lts) {
      if (lts.isErrorState(state))
        continue
      for (input in lts.alphabet()) {
        if (lts.getSuccessors(state, input).size == 0)
          lts.addTransition(state, input, theta, null)
      }
    }
    return lts
  }
}