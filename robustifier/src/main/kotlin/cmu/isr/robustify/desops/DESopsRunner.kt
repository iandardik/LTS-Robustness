package cmu.isr.robustify.desops


import cmu.isr.robustify.supervisory.*
import cmu.isr.utils.pretty
import net.automatalib.words.Alphabet
import org.slf4j.LoggerFactory
import java.io.FileOutputStream
import java.time.Duration


class DESopsRunner<I>(private val transformer: (String) -> I) : SupervisorySynthesizer<Int, I> {

  private val logger = LoggerFactory.getLogger(javaClass)
  private val desops = ClassLoader.getSystemResource("scripts/desops.py")?.readBytes() ?: error("Cannot find desops.py")

  init {
    val out = FileOutputStream("./desops.py")
    out.write(desops)
    out.close()
  }

  private fun synthesizeRaw(
    plant: SupervisoryDFA<*, I>, inputs1: Alphabet<I>,
    prop: SupervisoryDFA<*, I>, inputs2: Alphabet<I>
  ): CompactSupDFA<I>? {
    // check alphabets
    if (inputs1 != inputs2)
      error("The plant and the property should have the same alphabet")
    checkAlphabets(plant, inputs1, prop, inputs2)

    val startTime = System.currentTimeMillis()
    val processBuilder = ProcessBuilder("python", "desops.py")
    val process = processBuilder.start()

    write(process.outputStream, plant, inputs1)
    write(process.outputStream, prop, inputs2)
    process.waitFor()

    return when (process.exitValue()) {
      0 -> {
        val sup = parse(process.inputStream, inputs1, plant.controllable, plant.observable, transformer)
        logger.debug("Synthesis spent ${Duration.ofMillis(System.currentTimeMillis() - startTime).pretty()}")
        observer(sup, sup.inputAlphabet)
      }
      255 -> null
      else -> error(
        "Exit code: ${process.exitValue()}. Caused by: " + process.errorStream.bufferedReader().readText()
      )
    }
  }

  override fun synthesize(
    plant: SupervisoryDFA<*, I>, inputs1: Alphabet<I>,
    prop: SupervisoryDFA<*, I>, inputs2: Alphabet<I>
  ): CompactSupDFA<I>? {
    // DESops requires the prop to have the same alphabet as the plant
    if (inputs1 != inputs2) {
      val extendedProp = extendAlphabet(prop, inputs2, inputs1).asSupDFA(
        prop.controllable union plant.controllable, prop.observable union plant.observable)
      return synthesizeRaw(plant, inputs1, extendedProp, extendedProp.inputAlphabet)
    }
    return synthesizeRaw(plant, inputs1, prop, inputs2)
  }
}