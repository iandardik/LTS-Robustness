package cmu.isr.app

import cmu.isr.robustness.BaseCalculator
import cmu.isr.robustness.RobustnessCalculator
import cmu.isr.robustness.explanation.BaseExplanationGenerator
import cmu.isr.robustness.explanation.ExplanationGenerator
import cmu.isr.ts.DetLTS
import cmu.isr.ts.alphabet
import cmu.isr.ts.lts.ltsa.LTSACall
import cmu.isr.ts.lts.ltsa.LTSACall.asDetLTS
import cmu.isr.ts.lts.ltsa.LTSACall.compose
import cmu.isr.ts.parallel
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.split
import org.slf4j.LoggerFactory
import java.io.File

class Robustness : CliktCommand(help = "Compute the robustness of a system design.") {

  private val sys by option("--sys", "-s", help = "The model of the system.")
  private val env by option("--env", "-e", help = "The model of the environment")
  private val prop by option("--prop", "-p", help = "The model of the safety property.")
  private val dev by option("--dev", "-d", help = "The model of the deviation model for explanation")
  private val unsafe by option("--unsafe", "-u", help = "Generate unsafe behaviors").flag()
  private val jsons by option("--jsons", help = "One or more model config files, separated by ','").split(",")
  private val compare by option("--compare", help = "Compare the robustness of two models").flag()

  private val logger = LoggerFactory.getLogger(javaClass)

  override fun run() {
    val cals = if (jsons != null) {
      jsons!!.map { buildCalculator(it) }
    } else if (sys != null && env != null && prop != null) {
      listOf(buildCalculator())
    } else {
      error("Must provide a JSON config file or specify the '-s, -e, -p' options.")
    }
    if (compare) {
      if (cals.size < 2)
        error("Must provide two configs for robustness comparison.")

      val a = cals[0].first
      val b = cals[1].first

      logger.info("Comparing the robustness of a to b...")
      for ((k, v) in a.compare(b)) {
        logger.info("Equivalence class '$k':")
        for (t in v) {
          logger.info("\t${v}")
        }
      }

      logger.info("Comparing the robustness of b to a...")
      b.compare(a)
      for ((k, v) in b.compare(a)) {
        logger.info("Equivalence class '$k':")
        for (t in v) {
          logger.info("\t${v}")
        }
      }
    } else {
      for ((cal, explain)  in cals) {
        val traces = cal.computeRobustness()
        for ((k, v) in traces) {
          logger.info("Equivalence class '$k':")
          for (t in v) {
            if (explain != null)
              logger.info("\t${v} => ${explain.generate(t, cal.weakestAssumption.alphabet())}")
            else
              logger.info("\t${v}")
          }
        }
      }
    }
  }

  private fun parseFile(path: String): DetLTS<*, String> {
    val f = File(path)
    return when (f.extension) {
      "lts" -> LTSACall.compile(f.readText()).compose().asDetLTS()
      else -> error("Unsupported file type '.${f.extension}'")
    }
  }

  private fun parseFiles(paths: List<String>): DetLTS<*, String> {
    if (paths.isEmpty())
      error("Should provide at least one model file")
    if (paths.size == 1)
      return parseFile(paths[0])
    return parallel(*paths.map { parseFile(it) }.toTypedArray())
  }

  private fun buildCalculator(json: String): Pair<RobustnessCalculator<*, String>, ExplanationGenerator<String>?> {
    val obj = jacksonObjectMapper().readValue(File(json), RobustnessConfigJSON::class.java)
    val sys = parseFiles(obj.sys)
    return Pair(
      BaseCalculator(sys, parseFiles(obj.env), parseFiles(obj.prop)),
      obj.dev?.let { BaseExplanationGenerator(sys, parseFiles(it)) }
    )
  }

  private fun buildCalculator(): Pair<RobustnessCalculator<*, String>, ExplanationGenerator<String>?> {
    val sys = parseFile(sys!!)
    return Pair(
      BaseCalculator(sys, parseFile(env!!), parseFile(prop!!)),
      dev?.let { BaseExplanationGenerator(sys, parseFile(dev!!)) }
    )
  }
}

private data class RobustnessConfigJSON(
  @JsonProperty
  val sys: List<String>,
  @JsonProperty
  val env: List<String>,
  @JsonProperty
  val prop: List<String>,
  @JsonProperty
  val dev: List<String>?,
)