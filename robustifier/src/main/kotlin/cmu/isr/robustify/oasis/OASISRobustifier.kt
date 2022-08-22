package cmu.isr.robustify.oasis

import cmu.isr.dfa.hide
import cmu.isr.dfa.parallelComposition
import cmu.isr.robustify.BaseRobustifier
import cmu.isr.robustify.supervisory.acceptsSubWord
import cmu.isr.robustify.supervisory.asSupDFA
import cmu.isr.robustify.supervisory.makeProgress
import cmu.isr.robustify.supremica.SupremicaRunner
import cmu.isr.utils.combinations
import cmu.isr.utils.pretty
import net.automatalib.automata.fsa.DFA
import net.automatalib.automata.fsa.impl.compact.CompactDFA
import net.automatalib.words.Alphabet
import net.automatalib.words.Word
import org.slf4j.LoggerFactory
import java.time.Duration

class OASISRobustifier(
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
    val iter = OrderedPowerSetIterator((controllable union observable).toList())
    for (abs in iter) {
      logger.info("Abstract the system by $abs")
      val abstracted = abstracter(abs)
      assert(abstracted.inputAlphabet.toSet() == sysInputs.toSet())

      val g = parallelComposition(abstracted, sysInputs, devEnv, envInputs).asSupDFA(controllable, observable)
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

        val satisfiedPreferred = preferred.filter { acceptsSubWord(ctrlPlant, ctrlPlant.inputAlphabet, it) }
        if (satisfiedPreferred.isEmpty()) {
          logger.info("No preferred behaviors are satisfied!")
        } else {
          logger.info("Number of satisfied preferred behavior: ${satisfiedPreferred.size}")
          logger.info("Satisfied preferred behaviors:")
          satisfiedPreferred.forEach { logger.info("\t$it") }
        }

        logger.info("Termination time: ${Duration.ofMillis(System.currentTimeMillis() - startTime).pretty()}")
        return sup
      }
    }

    logger.info("Termination time: ${Duration.ofMillis(System.currentTimeMillis() - startTime).pretty()}")
    return null
  }

  private fun abstracter(abs: Collection<String>): CompactDFA<String> {
    val m = hide(sys, sysInputs, abs)
    val n = hide(sys, sysInputs, sysInputs - abs.toSet())
    return parallelComposition(m, m.inputAlphabet, n, n.inputAlphabet)
  }

}

class OrderedPowerSetIterator(val inputs: List<String>) : Iterator<Collection<String>> {
  private var k = 0
  private val queue = ArrayDeque<Collection<String>>()

  override fun hasNext(): Boolean {
    while (queue.isEmpty() && k <= inputs.size) {
      queue.addAll(inputs.combinations(k))
      k++
    }
    return queue.isNotEmpty()
  }

  override fun next(): Collection<String> {
    return queue.removeFirst()
  }

}