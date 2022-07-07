package cmu.isr.robustify.supervisory

import cmu.isr.lts.CompactDetLTS
import cmu.isr.lts.DetLTS
import cmu.isr.robustify.BaseRobustifier
import cmu.isr.robustify.desops.CompactSupDFA
import cmu.isr.robustify.desops.DESopsRunner
import cmu.isr.robustify.desops.asSupDFA
import cmu.isr.robustify.desops.observer
import net.automatalib.automata.fsa.impl.compact.CompactDFA
import net.automatalib.automata.simple.SimpleDeterministicAutomaton
import net.automatalib.words.Alphabet
import net.automatalib.words.Word
import org.slf4j.LoggerFactory
import cmu.isr.lts.parallelComposition as parallelLTS
import cmu.isr.robustify.desops.parallelComposition as parallelDFA

enum class Priority { P0, P1, P2, P3 }

enum class Algorithms { Pareto, Fast }

class WeightsMap(
  val preferred: Map<Word<String>, Int>,
  val controllable: Map<String, Int>,
  val observable: Map<String, Int>
  )


class SupervisoryRobustifier(
  sys: DetLTS<*, String, *>,
  sysInputs: Alphabet<String>,
  devEnv: DetLTS<*, String, *>,
  envInputs: Alphabet<String>,
  safety: DetLTS<*, String, *>,
  safetyInputs: Alphabet<String>,
  progress: Collection<String>,
  val preferredMap: Map<Priority, Collection<Word<String>>>,
  val controllableMap: Map<Priority, Collection<String>>,
  val observableMap: Map<Priority, Collection<String>>,
) : BaseRobustifier<Int, String, Int>(sys, devEnv, safety)
{
  private val logger = LoggerFactory.getLogger(javaClass)
  private val desops = DESopsRunner()
  private val plant: CompactDetLTS<String> = parallelLTS(sys, sysInputs, devEnv, envInputs)
  private val prop: CompactDFA<String>

  init {
    val extendedSafety = extendAlphabet(safety, safetyInputs, plant.inputAlphabet)
    val progressProp = progress.map { makeProgress(it) }
    var c = extendedSafety as CompactDFA<String>
    for (p in progressProp) {
      c = parallelDFA(c, c.inputAlphabet, p, p.inputAlphabet)
    }
    prop = c
  }

  override fun synthesize(): CompactDetLTS<String>? {
    return synthesize(Algorithms.Pareto).firstOrNull()
  }

  fun synthesize(alg: Algorithms, deadlockFree: Boolean = false): Iterable<CompactDetLTS<String>> {
    return SolutionIterator(this, alg, deadlockFree)
  }

  fun supervisorySynthesize(controllable: Collection<String>, observable: Collection<String>): CompactSupDFA<String>? {
    val g = plant.asSupDFA(controllable, observable)
    val p = prop.asSupDFA(controllable, observable)
    return desops.synthesize(g, g.inputAlphabet, p, p.inputAlphabet)
  }

  /**
   * Given the priority ranking that the user provides, compute the positive utilities for preferred behavior
   * and the negative cost for making certain events controllable and/or observable.
   * @return dictionary with this information.
   */
  fun computeWeights(): WeightsMap {
    val preferred = mutableMapOf<Word<String>, Int>()
    val controllable = mutableMapOf<String, Int>()
    val observable = mutableMapOf<String, Int>()

    var totalWeight = 0
    // compute new weight in order to maintain hierarchy by sorting absolute value sum of previous weights
    for (p in listOf(Priority.P0, Priority.P1, Priority.P2, Priority.P3)) {
      val curWeight = totalWeight + 1
      if (p in preferredMap) {
        for (word in preferredMap[p]!!) {
          if (p == Priority.P0) {
            preferred[word] = 0
          } else {
            preferred[word] = curWeight
            totalWeight += curWeight
          }
        }
      }
      if (p in controllableMap) {
        for (a in controllableMap[p]!!) {
          if (p == Priority.P0) {
            controllable[a] = 0
          } else {
            controllable[a] = -curWeight
            totalWeight += curWeight
          }
        }
      }
      if (p in observableMap) {
        for (a in observableMap[p]!!) {
          if (p == Priority.P0) {
            observable[a] = 0
          } else {
            observable[a] = -curWeight
            totalWeight += curWeight
          }
        }
      }
    }

    return WeightsMap(preferred, controllable, observable)
  }

  fun checkPreferred(sup: CompactSupDFA<String>, preferred: Collection<Word<String>>): Collection<Word<String>> {
    return preferred.filter { acceptsSubWord(sup, sup.inputAlphabet, it) }
  }

  fun removeUnnecessary(sup: CompactSupDFA<String>): CompactSupDFA<String> {
    val control = constructSupervisor(sup)
    val makeUc = control.observable.toMutableSet()
    for (state in control) {
      for (a in control.observable) {
        if (control.getTransition(state, a) == null)
          makeUc.remove(a)
      }
    }
    val makeUo = makeUc.toMutableSet()
    for (state in control) {
      for (a in makeUc) {
        if (control.getSuccessor(state, a) != state)
          makeUo.remove(a)
      }
    }

    return observer(
      CompactDFA(control).asSupDFA(
        (control.controllable - makeUc) union (controllableMap[Priority.P0]?: emptySet()),
        (control.observable - makeUo) union (observableMap[Priority.P0]?: emptySet())
      ),
      control.inputAlphabet
    )
  }

  private fun constructSupervisor(sup: CompactSupDFA<String>): CompactSupDFA<String> {
    val supQueue = java.util.ArrayDeque<Int>()
    val plantQueue = java.util.ArrayDeque<Int>()
    val out = CompactDFA(sup).asSupDFA(sup.controllable, sup.observable)
    val visited = mutableSetOf<Int>()

    supQueue.offer(sup.initialState!!)
    plantQueue.offer(plant.initialState!!)

    while (supQueue.isNotEmpty()) {
      val supState = supQueue.poll()
      val plantState = plantQueue.poll()

      if (supState in visited)
        continue
      visited.add(supState)

      for (a in sup.inputAlphabet) {
        val supSucc = sup.getSuccessor(supState, a)
        val plantSucc = sup.getSuccessor(plantState, a)
        if (supSucc != SimpleDeterministicAutomaton.IntAbstraction.INVALID_STATE) {
          supQueue.offer(supSucc)
          plantQueue.offer(plantSucc)
        } else if (a in sup.observable) {
          // uncontrollable event, make admissible
          if (a !in sup.controllable)
            out.addTransition(supState, a, supState, null)
          // controllable but not defined in plant, make redundant
          else if (plantSucc == SimpleDeterministicAutomaton.IntAbstraction.INVALID_STATE)
            out.addTransition(supState, a, supState, null)
        }
      }
    }

    return observer(out, out.inputAlphabet)
  }


}