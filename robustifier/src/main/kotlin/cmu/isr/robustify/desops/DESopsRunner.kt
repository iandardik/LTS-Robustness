package cmu.isr.robustify.desops


import cmu.isr.robustify.supervisory.CompactSupDFA
import cmu.isr.robustify.supervisory.SupervisoryDFA
import cmu.isr.robustify.supervisory.SupervisorySynthesizer
import cmu.isr.utils.pretty
import net.automatalib.words.Alphabet
import org.slf4j.LoggerFactory
import java.io.FileOutputStream
import java.time.Duration


class DESopsRunner<I>(val transformer: (String) -> I) : SupervisorySynthesizer<Int, I> {

  private val logger = LoggerFactory.getLogger(javaClass)
  private val desops = ClassLoader.getSystemResource("scripts/desops.py")?.readBytes() ?: error("Cannot find desops.py")

  init {
    val out = FileOutputStream("./desops.py")
    out.write(desops)
    out.close()
  }

  override fun synthesize(
    plant: SupervisoryDFA<*, I>, inputs1: Alphabet<I>,
    prop: SupervisoryDFA<*, I>, inputs2: Alphabet<I>
  ): CompactSupDFA<I>? {
    // check alphabets
    if (inputs1 != inputs2)
      throw Error("The plant and the property should have the same alphabet")
    if (plant.controllable != prop.controllable)
      throw Error("The plant and the property should have the same controllable")
    if (plant.observable != prop.observable)
      throw Error("The plant and the property should have the same observable")

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
        sup
      }
      255 -> null
      else -> throw Error(
        "Exit code: ${process.exitValue()}. Caused by: " + process.errorStream.bufferedReader().readText()
      )
    }
  }
}