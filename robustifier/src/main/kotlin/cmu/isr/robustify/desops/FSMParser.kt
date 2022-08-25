package cmu.isr.robustify.desops

import cmu.isr.robustify.supervisory.CompactSupDFA
import cmu.isr.robustify.supervisory.asSupDFA
import net.automatalib.util.automata.builders.AutomatonBuilders
import net.automatalib.words.Alphabet
import net.automatalib.words.impl.Alphabets
import java.io.BufferedReader


fun <I> parse(reader: BufferedReader, alphabets: Alphabet<I>, controllable: Collection<I>,
              observable: Collection<I>, transformer: (String) -> I): CompactSupDFA<I> {
  val states = mutableListOf<Pair<String, Boolean>>()
  val stateTransitions = mutableMapOf<String, List<Pair<String, String>>>()
//  val alphabetsRead = mutableMapOf<String, Pair<Boolean, Boolean>>() // event -> <controllable, observable>
  var isDFA = true

  val numStateLine = readNonEmptyLine(reader)
  try {
    val numStates = numStateLine.toInt()
    for (i in 0 until numStates) {
      val stateLine = readNonEmptyLine(reader)
      val stateTuple = stateLine.split('\t')
      if (stateTuple.size < 3)
        throw Error("Missing argument at '${stateLine}'. States are in the format: SOURCE_STATE\\tMARKED\\t#TRANSITIONS")
      states.add(Pair(stateTuple[0], stateTuple[1] == "1"))

      try {
        val transitions = mutableListOf<Pair<String, String>>()
        val events = mutableSetOf<String>()
        for (j in 0 until stateTuple[2].toInt()) {
          val transLine = readNonEmptyLine(reader)
          val transTuple = transLine.split('\t')
          if (transTuple.size > 5 || transTuple.size < 4)
            throw Error("ERROR: Wrong arguments at '${transLine}'. Transitions are in the format: EVENT\tTARGET_STATE\tc/uc\to/uo\tprob(optional)")
          // Add transition
          transitions.add(Pair(transTuple[0], transTuple[1]))
          // Decide NFA
          if (isDFA) {
            if (transTuple[0] !in events)
              events.add(transTuple[0])
            else {
              isDFA = false
              // TODO: currently doesn't support NFA
              throw Error("Currently doesn't support NFA")
            }
          }
          // Add event to alphabet and decide controllable and observable
//          val attr = Pair(transTuple[2] == "c", transTuple[3] == "o")
//          if (transTuple[0] !in alphabetsRead)
//            alphabetsRead[transTuple[0]] = attr
//          else if (alphabetsRead[transTuple[0]] != attr)
//            throw Error("Inconsistent controllability and observability of event '${transTuple[0]}'")
        }
        stateTransitions[stateTuple[0]] = transitions
      } catch (e: NumberFormatException) {
        throw Error("Need number of transitions at '${stateLine}'")
      }
    }
  } catch (e: NumberFormatException) {
    throw Error("Need number of states, get '${numStateLine}'")
  }

  // Builder automaton
  val builder = AutomatonBuilders.newDFA(alphabets).withInitial(states[0].first)
  for (e in stateTransitions) {
    for (trans in e.value) {
      builder.from(e.key).on(transformer(trans.first)).to(trans.second)
    }
  }
  for (state in states) {
    if (state.second)
      builder.withAccepting(state.first)
  }
  return builder.create().asSupDFA(controllable, observable)
//    alphabetsRead.keys.filter { alphabetsRead[it]!!.first }.map(transformer),
//    alphabetsRead.keys.filter { alphabetsRead[it]!!.second }.map(transformer)
}

fun parse(reader: BufferedReader, alphabets: Alphabet<String>,
          controllable: Collection<String>, observable: Collection<String>): CompactSupDFA<String> {
  return parse(reader, alphabets, controllable, observable) { it }
}


fun parse(reader: BufferedReader): CompactSupDFA<String> {
  val states = mutableListOf<Pair<String, Boolean>>()
  val stateTransitions = mutableMapOf<String, List<Pair<String, String>>>()
  val alphabetsRead = mutableMapOf<String, Pair<Boolean, Boolean>>() // event -> <controllable, observable>
  var isDFA = true

  val numStateLine = readNonEmptyLine(reader)
  try {
    val numStates = numStateLine.toInt()
    for (i in 0 until numStates) {
      val stateLine = readNonEmptyLine(reader)
      val stateTuple = stateLine.split('\t')
      if (stateTuple.size < 3)
        throw Error("Missing argument at '${stateLine}'. States are in the format: SOURCE_STATE\\tMARKED\\t#TRANSITIONS")
      states.add(Pair(stateTuple[0], stateTuple[1] == "1"))

      try {
        val transitions = mutableListOf<Pair<String, String>>()
        val events = mutableSetOf<String>()
        for (j in 0 until stateTuple[2].toInt()) {
          val transLine = readNonEmptyLine(reader)
          val transTuple = transLine.split('\t')
          if (transTuple.size > 5 || transTuple.size < 4)
            throw Error("ERROR: Wrong arguments at '${transLine}'. Transitions are in the format: EVENT\tTARGET_STATE\tc/uc\to/uo\tprob(optional)")
          // Add transition
          transitions.add(Pair(transTuple[0], transTuple[1]))
          // Decide NFA
          if (isDFA) {
            if (transTuple[0] !in events)
              events.add(transTuple[0])
            else {
              isDFA = false
              // TODO: currently doesn't support NFA
              throw Error("Currently doesn't support NFA")
            }
          }
          // Add event to alphabet and decide controllable and observable
          val attr = Pair(transTuple[2] == "c", transTuple[3] == "o")
          if (transTuple[0] !in alphabetsRead)
            alphabetsRead[transTuple[0]] = attr
          else if (alphabetsRead[transTuple[0]] != attr)
            throw Error("Inconsistent controllability and observability of event '${transTuple[0]}'")
        }
        stateTransitions[stateTuple[0]] = transitions
      } catch (e: NumberFormatException) {
        throw Error("Need number of transitions at '${stateLine}'")
      }
    }
  } catch (e: NumberFormatException) {
    throw Error("Need number of states, get '${numStateLine}'")
  }

  // Builder automaton
  val builder = AutomatonBuilders.newDFA(Alphabets.fromCollection(alphabetsRead.keys)).withInitial(states[0].first)
  for (e in stateTransitions) {
    for (trans in e.value) {
      builder.from(e.key).on(trans.first).to(trans.second)
    }
  }
  for (state in states) {
    if (state.second)
      builder.withAccepting(state.first)
  }
  return builder.create().asSupDFA(
    alphabetsRead.keys.filter { alphabetsRead[it]!!.first },
    alphabetsRead.keys.filter { alphabetsRead[it]!!.second }
  )
}


private fun readNonEmptyLine(reader: BufferedReader): String {
  var line: String?
  while (true) {
    line = reader.readLine()
    if (line == null)
      throw Error("Unexpected end of the input stream")
    if (line.isNotEmpty())
      return line
  }
}
