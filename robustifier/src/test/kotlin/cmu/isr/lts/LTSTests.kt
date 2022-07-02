package cmu.isr.lts

import net.automatalib.util.automata.Automata
import net.automatalib.util.automata.builders.AutomatonBuilders
import net.automatalib.words.Word
import net.automatalib.words.impl.Alphabets
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class LTSTests {

  @Test
  fun testParallelComposition() {
    val a = AutomatonBuilders.newDFA(Alphabets.fromArray('a', 'b', 'c'))
      .withInitial(0)
      .from(0).on('a').to(1)
      .from(1)
      .on('b').to(0)
      .on('c').to(2)
      .from(2).on('b').to(0)
      .withAccepting(0, 1, 2)
      .create()
      .asLTS()

    val b = AutomatonBuilders.newDFA(Alphabets.fromArray('a', 'b', 'c'))
      .withInitial(0)
      .from(0).on('a').to(1)
      .from(1)
      .on('b').to(-1)
      .on('c').to(-1)
      .withAccepting(0, 1)
      .create()
      .asLTS()

    val c = parallelComposition(a, a.inputAlphabet, b, b.inputAlphabet)

    val d = AutomatonBuilders.newDFA(Alphabets.fromArray('a', 'b', 'c'))
      .withInitial(0)
      .from(0).on('a').to(1)
      .from(1)
      .on('b').to(-1)
      .on('c').to(-1)
      .withAccepting(0, 1)
      .create()
      .asLTS()

    assertEquals(true, c.isAccepting(0))
    assertEquals(true, c.isAccepting(1))
    assertEquals(false, c.isAccepting(2))
    assert(Automata.testEquivalence(c, d, c.inputAlphabet))
  }

  @Test
  fun testParallelComposition2() {
    val a = AutomatonBuilders.newDFA(Alphabets.fromArray('a', 'b'))
      .withInitial(0)
      .from(0).on('a').to(1)
      .from(1).on('b').to(0)
      .withAccepting(0, 1)
      .create()
      .asLTS()

    val b = AutomatonBuilders.newDFA(Alphabets.fromArray('b', 'c'))
      .withInitial(0)
      .from(0).on('b').to(1)
      .from(1).on('c').to(0)
      .withAccepting(0, 1)
      .create()
      .asLTS()

    val c = parallelComposition(a, a.inputAlphabet, b, b.inputAlphabet)

    val d = AutomatonBuilders.newDFA(Alphabets.fromArray('a', 'b', 'c'))
      .withInitial(0)
      .from(0).on('a').to(1)
      .from(1).on('b').to(2)
      .from(2)
        .on('c').to(0)
        .on('a').to(3)
      .from(3).on('c').to(1)
      .withAccepting(0, 1, 2, 3)
      .create()
      .asLTS()

    assertEquals(c.inputAlphabet, d.inputAlphabet)
    assert(Automata.testEquivalence(c, d, d.inputAlphabet))
  }

  @Test
  fun testDeadlock() {
    val p = AutomatonBuilders.newDFA(Alphabets.fromArray('a', 'b'))
      .withInitial(0)
      .from(0).on('a').to(1)
      .from(1).on('b').to(0)
      .withAccepting(0, 1)
      .create()
      .asLTS()

    val a = AutomatonBuilders.newDFA(Alphabets.characters('a', 'c'))
      .withInitial(0)
      .from(0).on('a').to(1)
      .from(1)
      .on('b').to(2)
      .on('a').to(1)
      .from(2).on('c').to(0)
      .withAccepting(0, 1, 2)
      .create()
      .asLTS()

//    AUTWriter.writeAutomaton(p, p.inputAlphabet, System.out)
    val p_err = makeErrorState(p, p.inputAlphabet)
//    AUTWriter.writeAutomaton(p_err, p.inputAlphabet, System.out)

    val c = parallelComposition(a, a.inputAlphabet, p_err, p.inputAlphabet)
    val res = checkDeadlock(c, c.inputAlphabet)
    assertEquals(false, res.violation)
  }

  @Test
  fun testDeadlock2() {
    val p = AutomatonBuilders.newDFA(Alphabets.fromArray('a', 'b'))
      .withInitial(0)
      .from(0).on('a').to(1)
      .from(1).on('b').to(0)
      .withAccepting(0, 1)
      .create()
      .asLTS()

    val a = AutomatonBuilders.newDFA(Alphabets.characters('a', 'c'))
      .withInitial(0)
      .from(0).on('a').to(1)
      .from(1)
      .on('b').to(2)
      .on('a').to(1)
      .from(2).on('c').to(3)
      .withAccepting(0, 1, 2, 3)
      .create()
      .asLTS()

//    AUTWriter.writeAutomaton(p, p.inputAlphabet, System.out)
    val p_err = makeErrorState(p, p.inputAlphabet)
//    AUTWriter.writeAutomaton(p_err, p.inputAlphabet, System.out)

    val c = parallelComposition(a, a.inputAlphabet, p_err, p.inputAlphabet)
    val res = checkDeadlock(c, c.inputAlphabet)
    assertEquals(true, res.violation)
    assertEquals(Word.fromSymbols('a', 'b', 'c'), res.trace)
  }

  @Test
  fun testSafety() {
    val p = AutomatonBuilders.newDFA(Alphabets.fromArray('a', 'b'))
      .withInitial(0)
      .from(0).on('a').to(1)
      .from(1).on('b').to(0)
      .withAccepting(0, 1)
      .create()
      .asLTS()

    val a = AutomatonBuilders.newDFA(Alphabets.characters('a', 'c'))
      .withInitial(0)
      .from(0).on('a').to(1)
      .from(1)
      .on('b').to(2)
      .on('a').to(1)
      .from(2).on('c').to(0)
      .withAccepting(0, 1, 2)
      .create()
      .asLTS()

    val p_err = makeErrorState(p, p.inputAlphabet)
    val res = checkSafety(a, a.inputAlphabet, p_err, p.inputAlphabet)
    assertEquals(true, res.violation)
    assertEquals(Word.fromSymbols('a', 'a'), res.trace)
  }
}