package cmu.isr.ts.dfa

import cmu.isr.utils.forEachSetBit
import net.automatalib.automata.fsa.DFA
import net.automatalib.automata.fsa.impl.compact.CompactDFA
import net.automatalib.words.Alphabet
import net.automatalib.words.impl.Alphabets
import java.util.*

fun <S, I> reachableSet(dfa: DFA<S, I>, hidden: Collection<I>): Map<Int, BitSet> {
  val reachable = mutableMapOf<Int, BitSet>()
  val stateIDs = dfa.stateIDs()
  for (state in dfa) {
    val id = stateIDs.getStateId(state)
    reachable[id] = BitSet()
    for (uo in hidden) {
      val succ = dfa.getSuccessor(state, uo)
      if (succ != null) {
        reachable[id]!!.set(stateIDs.getStateId(succ))
      }
    }
  }

  var fixpoint = false
  while (!fixpoint) {
    fixpoint = true
    for (bs in reachable.values) {
      val oldSize = bs.cardinality()
      var i = bs.nextSetBit(0)
      while (i >= 0) {
        if (i == Int.MAX_VALUE)
          break
        bs.or(reachable[i]!!)
        i = bs.nextSetBit(i+1)
      }
      fixpoint = fixpoint && bs.cardinality() == oldSize
    }
  }

  return reachable
}


fun <S, I> hide(dfa: DFA<S, I>, inputs: Alphabet<I>, hidden: Collection<I>): CompactDFA<I> {
  val observable = inputs - hidden.toSet()
  val out = CompactDFA(Alphabets.fromCollection(observable))
  val outStateMap = mutableMapOf<BitSet, Int>()
  val reachable = reachableSet(dfa, hidden)
  val stateIDs = dfa.stateIDs()
  val stack = ArrayDeque<BitSet>()

  // create initial bitset
  val initStateId = stateIDs.getStateId(dfa.initialState)
  val initBs = BitSet()
  initBs.set(initStateId)
  initBs.or(reachable[initStateId]!!)

  // create output initial state
  var initAccept = true
  initBs.forEachSetBit { initAccept = initAccept && dfa.isAccepting(stateIDs.getState(it)) }
  val initOut = out.addInitialState(initAccept)

  outStateMap[initBs] = initOut

  stack.push(initBs)

  while (stack.isNotEmpty()) {
    val currBs = stack.pop()

    for (a in observable) {
      val succBs = BitSet()

      currBs.forEachSetBit {
        val succState = dfa.getSuccessor(stateIDs.getState(it), a)
        if (succState != null) {
          val succStateId = stateIDs.getStateId(succState)
          succBs.set(succStateId)
          succBs.or(reachable[succStateId]!!)
        }
      }

      if (!succBs.isEmpty) {
        var outSucc = outStateMap[succBs]
        if (outSucc == null) {
          var outSuccAccept = true
          succBs.forEachSetBit { outSuccAccept = outSuccAccept && dfa.isAccepting(stateIDs.getState(it)) }
          outSucc = out.addState(outSuccAccept)
          outStateMap[succBs] = outSucc
          stack.push(succBs)
        }
        out.addTransition(outStateMap[currBs], a, outSucc, null)
      }
    }
  }

  return out
}