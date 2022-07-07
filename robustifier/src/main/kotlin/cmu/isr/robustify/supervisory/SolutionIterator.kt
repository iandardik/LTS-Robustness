package cmu.isr.robustify.supervisory

import cmu.isr.lts.CompactDetLTS
import cmu.isr.robustify.desops.CompactSupDFA
import org.slf4j.LoggerFactory
import java.time.Duration

class SolutionIterator(
  private val problem: SupervisoryRobustifier,
  private val alg: Algorithms,
  private val deadlockFree: Boolean
) : Iterable<CompactDetLTS<String>>, Iterator<CompactDetLTS<String>> {

  private val logger = LoggerFactory.getLogger(javaClass)
  private lateinit var initSol: CompactSupDFA<String>

  override fun iterator(): Iterator<CompactDetLTS<String>> {
    val startTime = System.currentTimeMillis()
    logger.info("==============================>")
    logger.info("Initializing search by using $alg search...")

    // flatten the preferred behaviors
    val preferred = problem.preferredMap.flatMap { it.value }
    logger.info("Number of preferred behaviors: ${preferred.size}")

    // compute weight map
    val weights = problem.computeWeights()
    // get controllable and observable events
    val controllable = weights.controllable.keys
    val observable = weights.observable.keys
    logger.info("Number of controllable events with cost: ${controllable.size - (problem.controllableMap[Priority.P0]?.size ?: 0)}")
    logger.info("Number of observable events with cost: ${observable.size - (problem.observableMap[Priority.P0]?.size ?: 0)}")

    // synthesize a supervisor with the max controllable and observable events
    val sup = problem.supervisorySynthesize(controllable, observable)
    if (sup == null) {
      logger.warn("No supervisor found with max controllable and observable events.")
      return emptyArray<CompactDetLTS<String>>().iterator()
    }

    // compute the maximum fulfilled preferred behavior under the max controllability and observability
    val maxPreferred = problem.checkPreferred(sup, preferred)
    logger.info("Maximum fulfilled preferred behavior:")
    for (p in maxPreferred)
      logger.info("\t$p")

    // remove those absolutely unused controllable and observable events which generates the initial solution
    initSol = problem.removeUnnecessary(sup)

    logger.info("Initialization completes, time: ${Duration.ofMillis(System.currentTimeMillis() - startTime).pretty()}")
    logger.info("Start search from events:")
    logger.info("Controllable: ${initSol.controllable}")
    logger.info("Observable: ${initSol.observable}")

    return this
  }

  private fun Duration.pretty(): String {
    return "${this.toHours()}:${this.toMinutes()}:${this.seconds}.${this.toMillis()}"
  }

  override fun hasNext(): Boolean {
    TODO("Not yet implemented")
  }

  override fun next(): CompactDetLTS<String> {
    TODO("Not yet implemented")
  }

}