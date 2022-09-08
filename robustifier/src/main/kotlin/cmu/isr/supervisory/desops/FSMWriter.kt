package cmu.isr.supervisory.desops

import cmu.isr.supervisory.SupervisoryDFA
import net.automatalib.words.Alphabet
import java.io.OutputStream

// TODO: add support for NFA
fun <S, I> write(output: OutputStream, dfa: SupervisoryDFA<S, I>, inputs: Alphabet<I>) {
  val writer = output.bufferedWriter()
  val states = dfa.states.toMutableList()
  // Remove initial and insert it into the beginning
  states.remove(dfa.initialState)
  states.add(0, dfa.initialState)

  writer.appendLine(dfa.size().toString()).appendLine()
  for (state in states) {
    writer
      .append(state.toString())
      .append('\t')
      .append("${if (dfa.isAccepting(state)) 1 else 0}")
      .append('\t')
    val builder = StringBuilder()
    var counter = 0
    for (input in inputs) {
      val trans = dfa.getTransition(state, input)
      if (trans != null) {
        counter++
        builder
          .append(input)
          .append('\t')
          .append(dfa.getSuccessor(trans))
          .append('\t')
          .append(if (input in dfa.controllable) "c" else "uc")
          .append('\t')
          .appendLine(if (input in dfa.observable) "o" else "uo")
      }
    }
    writer.appendLine(counter.toString())
    writer.appendLine(builder.toString())
  }
  writer.flush()
}
