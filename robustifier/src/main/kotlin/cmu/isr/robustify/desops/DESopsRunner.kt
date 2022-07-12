package cmu.isr.robustify.desops


import cmu.isr.robustify.supervisory.*
import cmu.isr.utils.pretty
import net.automatalib.words.Alphabet
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.FileOutputStream
import java.net.Socket
import java.time.Duration


class DESopsRunner<I>(private val transformer: (String) -> I) : SupervisorySynthesizer<Int, I> {

  private val logger = LoggerFactory.getLogger(javaClass)
  private val desops = ClassLoader.getSystemResource("scripts/desops.py")?.readBytes() ?: error("Cannot find desops.py")
  private val process: Process
  private val processStdErr: BufferedReader
  private val port = 5000

  init {
    val out = FileOutputStream("./desops.py")
    out.write(desops)
    out.close()

    val processBuilder = ProcessBuilder("python", "desops.py")
    process = processBuilder.start()
    processStdErr = process.errorStream.bufferedReader()

    Thread.sleep(500) // Wait the DESops process to start
  }

  private fun synthesizeRaw(
    plant: SupervisoryDFA<*, I>, inputs1: Alphabet<I>,
    prop: SupervisoryDFA<*, I>, inputs2: Alphabet<I>
  ): CompactSupDFA<I>? {
    // check alphabets
    if (inputs1 != inputs2)
      error("The plant and the property should have the same alphabet")
    checkAlphabets(plant, inputs1, prop, inputs2)

    if (!process.isAlive) {
      error("The DESops process has terminated. Caused by: ${processStdErr.readText()}")
    }

    val startTime = System.currentTimeMillis()
    val socket = Socket("127.0.0.1", port)

    val outStream = socket.getOutputStream()
    write(outStream, plant, inputs1)
    write(outStream, prop, inputs2)

    val inStream = socket.getInputStream()
    val reader = inStream.bufferedReader()
    val sup = when (reader.readLine()) {
      "0" -> {
        val sup = parse(reader, inputs1, plant.controllable, plant.observable, transformer)
        logger.debug("Synthesis spent ${Duration.ofMillis(System.currentTimeMillis() - startTime).pretty()}")
        observer(sup, sup.inputAlphabet)
      }
      "1" -> null
      else -> error(processStdErr.readLine())
    }

    socket.close()
    return sup
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

  override fun close() {
    process.destroy()
  }
}