package cmu.isr.robustify.desops

import net.automatalib.words.Alphabet
import java.io.FileOutputStream


class DESopsRunner {

  private val desops = ClassLoader.getSystemResource("scripts/desops.py")?.readBytes() ?: error("Cannot find desops.py")

  init {
    val out = FileOutputStream("./desops.py")
    out.write(desops)
    out.close()
  }

  fun synthesize(
    plant: SupervisoryDFA<*, String>, inputs1: Alphabet<String>,
    prop: SupervisoryDFA<*, String>, inputs2: Alphabet<String>
  ): CompactSupDFA<String>? {
    return synthesize(plant, inputs1, prop, inputs2) { it }
  }

  fun <I> synthesize(
    plant: SupervisoryDFA<*, I>, inputs1: Alphabet<I>,
    prop: SupervisoryDFA<*, I>, inputs2: Alphabet<I>,
    transformer: (String) -> I
  ): CompactSupDFA<I>? {
    // check alphabets
    if (inputs1 != inputs2)
      throw Error("The plant and the property should have the same alphabet")
    if (plant.controllable != prop.controllable)
      throw Error("The plant and the property should have the same controllable")
    if (plant.observable != prop.observable)
      throw Error("The plant and the property should have the same observable")

    val processBuilder = ProcessBuilder("python", "desops.py")
    val process = processBuilder.start()

    write(process.outputStream, plant, inputs1)
    write(process.outputStream, prop, inputs2)
    process.waitFor()
    return when (process.exitValue()) {
      0 -> parse(process.inputStream, inputs1, plant.controllable, plant.observable, transformer)
      -1 -> null
      else -> throw Error(
        "Exit code: ${process.exitValue()}. Caused by: " + process.errorStream.bufferedReader().readText()
      )
    }
  }
}