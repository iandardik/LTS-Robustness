package cmu.isr.robustify.simple

import cmu.isr.dfa.parallelComposition
import cmu.isr.robustify.BaseRobustifier
import cmu.isr.robustify.oasis.controlledEvents
import cmu.isr.robustify.supervisory.acceptsSubWord
import cmu.isr.robustify.supervisory.asSupDFA
import cmu.isr.robustify.supervisory.makeProgress
import cmu.isr.robustify.supremica.SupremicaRunner
import cmu.isr.utils.pretty
import net.automatalib.automata.fsa.DFA
import net.automatalib.automata.fsa.impl.compact.CompactDFA
import net.automatalib.words.Alphabet
import net.automatalib.words.Word
import org.slf4j.LoggerFactory
import java.time.Duration

class SimpleRobustifier(
  sys: DFA<*, String>,
  sysInputs: Alphabet<String>,
  devEnv: DFA<*, String>,
  envInputs: Alphabet<String>,
  safety: DFA<*, String>,
  safetyInputs: Alphabet<String>,
  progress: Collection<String>,
  val preferred: Collection<Word<String>>
) : BaseRobustifier<Int, String>(sys, sysInputs, devEnv, envInputs, safety, safetyInputs)
{

  private val logger = LoggerFactory.getLogger(javaClass)
  private val plant = parallelComposition(sys, sysInputs, devEnv, envInputs)
  private val prop: CompactDFA<String>
  private val synthesizer = SupremicaRunner()

  override var numberOfSynthesis: Int = 0

  init {
    val progressProp = progress.map { makeProgress(it) }
    var c = safety as CompactDFA<String>
    for (p in progressProp) {
      c = parallelComposition(c, c.inputAlphabet, p, p.inputAlphabet)
    }
    prop = c
  }

  override fun synthesize(): CompactDFA<String>? {
    return synthesize(sysInputs, sysInputs)
  }

  fun synthesize(controllable: Collection<String>, observable: Collection<String>): CompactDFA<String>? {
    if (!observable.containsAll(controllable))
      error("The controllable events should be a subset of the observable events.")

    logger.info("Number of controllable events: ${controllable.size}")
    logger.info("Controllable: $controllable")
    logger.info("Number of observable events: ${observable.size}")
    logger.info("Observable: $observable")

    val startTime = System.currentTimeMillis()
    val g = plant.asSupDFA(controllable, observable)
    val p = prop.asSupDFA(
      prop.inputAlphabet intersect controllable.toSet(),
      prop.inputAlphabet intersect observable.toSet()
    )
    val sup = synthesizer.synthesize(g, g.inputAlphabet, p, p.inputAlphabet)

    numberOfSynthesis++
    if (sup != null) {
      val ctrlPlant = parallelComposition(g, g.inputAlphabet, sup, sup.inputAlphabet)
        .asSupDFA(sup.controllable, sup.observable)

      logger.info("Found solution!")
      logger.info("Controlled events: ${controlledEvents(g, ctrlPlant, g.inputAlphabet)}")

      val satisfiedPreferred = preferred.filter {
        val (r, how) = acceptsSubWord(ctrlPlant, ctrlPlant.inputAlphabet, it)
        logger.debug("Preferred behavior [$it] is satisfied by $how")
        r
      }
      if (satisfiedPreferred.isEmpty()) {
        logger.info("No preferred behaviors are satisfied!")
      } else {
        logger.info("Number of satisfied preferred behavior: ${satisfiedPreferred.size}")
        logger.info("Satisfied preferred behaviors:")
        satisfiedPreferred.forEach { logger.info("\t$it") }
      }
    } else {
      logger.info("No solution found!")
    }
    logger.info("Termination time: ${Duration.ofMillis(System.currentTimeMillis() - startTime).pretty()}")

    return sup
  }

}