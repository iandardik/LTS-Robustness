package cmu.isr.robustify.supervisory

import cmu.isr.lts.asLTS
import net.automatalib.util.automata.Automata
import net.automatalib.util.automata.builders.AutomatonBuilders
import net.automatalib.words.Word
import net.automatalib.words.impl.Alphabets
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class UtilsTest {

  @Test
  fun testExtendAlphabet() {
    val a = AutomatonBuilders.newDFA(Alphabets.fromArray('a', 'b'))
      .withInitial(0)
      .from(0).on('a').to(1)
      .from(1).on('b').to(0)
      .withAccepting(0, 1)
      .create()
      .asLTS()
    val inputs = Alphabets.fromArray('a', 'b', 'c')

    val extended = extendAlphabet(a, a.inputAlphabet, inputs)
    assertEquals(inputs, extended.inputAlphabet)

    val b = AutomatonBuilders.newDFA(Alphabets.fromArray('a', 'b', 'c'))
      .withInitial(0)
      .from(0).on('a').to(1).on('c').to(0)
      .from(1).on('b').to(0).on('c').to(1)
      .withAccepting(0, 1)
      .create()
      .asLTS()

    assert(Automata.testEquivalence(extended, b, inputs)) { println(Automata.findSeparatingWord(extended, b, inputs)) }
  }

  @Test
  fun testParallelComposition() {
    val a = AutomatonBuilders.newDFA(Alphabets.fromArray('a', 'b'))
      .withInitial(0)
      .from(0).on('a').to(1)
      .from(1).on('b').to(0)
      .withAccepting(0, 1)
      .create()

    val b = AutomatonBuilders.newDFA(Alphabets.fromArray('c'))
      .withInitial(0)
      .from(0).on('c').to(1)
      .from(1).on('c').to(1)
      .withAccepting(1)
      .create()

    val c = parallelComposition(a, a.inputAlphabet, b, b.inputAlphabet)

    val d = AutomatonBuilders.newDFA(Alphabets.fromArray('a', 'b', 'c'))
      .withInitial(0)
      .from(0).on('c').to(1).on('a').to(3)
      .from(1).on('c').to(1).on('a').to(2)
      .from(2).on('c').to(2).on('b').to(1)
      .from(3).on('c').to(2).on('b').to(0)
      .withAccepting(1, 2)
      .create()

    assertEquals(Alphabets.fromArray('a', 'b', 'c'), c.inputAlphabet)
    assert(Automata.testEquivalence(c, d, d.inputAlphabet))
  }

  @Test
  fun testReachableSet() {
    val a = AutomatonBuilders.newDFA(Alphabets.fromArray('a', 'b', 'c'))
      .withInitial(0)
      .from(0).on('a').to(1)
      .from(1).on('b').to(2)
      .from(2).on('c').to(0)
      .withAccepting(0, 1, 2)
      .create()
      .asSupDFA(listOf('a', 'b', 'c'), listOf('a', 'b'))

    val reachable = reachableSet(a, a.inputAlphabet)
    assertEquals(BitSet(), reachable[0])
    assertEquals(BitSet(), reachable[1])
    assertEquals(let { val s = BitSet(); s.set(0); s }, reachable[2])
  }

  @Test
  fun testReachableSet2() {
    val a = AutomatonBuilders.newDFA(Alphabets.fromArray('a', 'b', 'c'))
      .withInitial(0)
      .from(0).on('a').to(1)
      .from(1).on('b').to(2)
      .from(2).on('c').to(3).on('b').to(1)
      .from(3).on('a').to(1)
      .create()
      .asSupDFA(listOf('a', 'b', 'c'), listOf('a', 'c'))

    val reachable = reachableSet(a, a.inputAlphabet)
    assertEquals(BitSet(), reachable[0])
    assertEquals(let { val s = BitSet(); s.set(2); s.set(1); s }, reachable[1])
    assertEquals(let { val s = BitSet(); s.set(1); s.set(2); s }, reachable[2])
    assertEquals(BitSet(), reachable[3])
  }

  @Test
  fun testObserver() {
    val a = AutomatonBuilders.newDFA(Alphabets.fromArray('a', 'b', 'c'))
      .withInitial(0)
      .from(0).on('a').to(1)
      .from(1).on('b').to(2)
      .from(2).on('c').to(0)
      .withAccepting(0, 1, 2)
      .create()
      .asSupDFA(listOf('a', 'b', 'c'), listOf('a', 'b'))

    val b = AutomatonBuilders.newDFA(Alphabets.fromArray('a', 'b'))
      .withInitial(0)
      .from(0).on('a').to(1)
      .from(1).on('b').to(0)
      .withAccepting(0, 1)
      .create()
      .asSupDFA(listOf('a', 'b'), listOf('a', 'b'))

    val observed = observer(a, a.inputAlphabet)
    assertContentEquals(b.controllable, observed.controllable)
    assertContentEquals(b.observable, observed.observable)
    assert(Automata.testEquivalence(b, observed, b.inputAlphabet))
  }

  @Test
  fun testAcceptsSubWord() {
    val a = AutomatonBuilders.newDFA(Alphabets.fromArray('a', 'b', 'c'))
      .withInitial(0)
      .from(0).on('a').to(1)
      .from(1).on('b').to(2)
      .from(2).on('c').to(0)
      .withAccepting(0, 1, 2)
      .create()
    val word = Word.fromSymbols('a', 'c')
    assertEquals(true, acceptsSubWord(a, a.inputAlphabet, word))
  }

  @Test
  fun testAcceptsSubWord2() {
    val a = AutomatonBuilders.newDFA(Alphabets.fromArray('a', 'b', 'c', 'd'))
      .withInitial(0)
      .from(0).on('a').to(1)
      .from(1).on('b').to(2)
      .from(2).on('c').to(3)
      .from(3).on('d').to(0)
      .create()
    val word = Word.fromSymbols('b', 'c')
    assertEquals(true, acceptsSubWord(a, a.inputAlphabet, word))
  }

  @Test
  fun testAcceptsSubWord3() {
    val a = AutomatonBuilders.newDFA(Alphabets.fromArray('a', 'b', 'c'))
      .withInitial(0)
      .from(0).on('a').to(1)
      .from(1).on('b').to(2)
      .from(2).on('c').to(0)
      .withAccepting(0, 1, 2)
      .create()
    val word = Word.fromSymbols('b', 'd', 'c')
    assertEquals(true, acceptsSubWord(a, a.inputAlphabet, word))
  }

  @Test
  fun testCombinations() {
    val l = listOf('a', 'b', 'c', 'd')
    val cs = l.combinations(2)
    assertContentEquals(
      listOf(
        listOf('a', 'b'),
        listOf('a', 'c'),
        listOf('a', 'd'),
        listOf('b', 'c'),
        listOf('b', 'd'),
        listOf('c', 'd')
      ),
      cs
    )
  }

  @Test
  fun testCombinations2() {
    val l = listOf('a')
    val cs = l.combinations(1)
    assertContentEquals(
      listOf(listOf('a')),
      cs
    )
  }

  @Test
  fun testCombinations3() {
    val l = listOf('a')
    val cs = l.combinations(0)
    assertContentEquals(
      listOf(emptyList()),
      cs
    )
  }

}