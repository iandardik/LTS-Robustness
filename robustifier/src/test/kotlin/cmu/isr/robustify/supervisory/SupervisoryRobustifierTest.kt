package cmu.isr.robustify.supervisory

import cmu.isr.lts.checkDeadlock
import cmu.isr.ltsa.LTSACall
import cmu.isr.ltsa.LTSACall.asDetLTS
import cmu.isr.ltsa.LTSACall.compose
import cmu.isr.robustify.supremica.SupremicaRunner
import net.automatalib.words.Word
import org.junit.jupiter.api.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class SupervisoryRobustifierTest {

  private fun loadVoting(): SupervisoryRobustifier {
    val sysSpec =
      ClassLoader.getSystemResource("specs/voting/sys.lts")?.readText() ?: error("Cannot find voting/sys.lts")
    val envSpec =
      ClassLoader.getSystemResource("specs/voting/env2.lts")?.readText() ?: error("Cannot find voting/env2.lts")
    val pSpec = ClassLoader.getSystemResource("specs/voting/p.lts")?.readText() ?: error("Cannot find voting/p.lts")
    val backSpec =
      ClassLoader.getSystemResource("specs/voting/back.lts")?.readText() ?: error("Cannot find voting/back.lts")

    val sys = LTSACall.compile(sysSpec).compose().asDetLTS()
    val env = LTSACall.compile(envSpec).compose().asDetLTS()
    val safety = LTSACall.compile(pSpec).compose().asDetLTS()
    val back = LTSACall.compile(backSpec).compose().asDetLTS()
    val backTrace = checkDeadlock(back, back.inputAlphabet).trace!!

    return SupervisoryRobustifier(
      sys, sys.inputAlphabet,
      env, env.inputAlphabet,
      safety, safety.inputAlphabet,
      progress = listOf("confirm"),
      preferredMap = mapOf(Priority.P3 to listOf(backTrace)),
      controllableMap = mapOf(
        Priority.P0 to listOf("back", "confirm", "password", "select", "vote"),
        Priority.P3 to listOf("eo.enter", "eo.exit", "v.enter", "v.exit")
      ),
      observableMap = mapOf(
        Priority.P0 to listOf("back", "confirm", "password", "select", "vote"),
        Priority.P2 to listOf("eo.enter", "eo.exit", "v.enter", "v.exit")
      ),
      synthesizer = SupremicaRunner(),
      maxIter = 1
    )
  }

  @Test
  fun testComputeWeights() {
    val robustifier = loadVoting()
    val weights = robustifier.computeWeights()
    assertEquals(0, weights.controllable["back"])
    assertEquals(0, weights.controllable["confirm"])
    assertEquals(0, weights.controllable["password"])
    assertEquals(0, weights.controllable["select"])
    assertEquals(0, weights.controllable["vote"])

    assertEquals(0, weights.observable["back"])
    assertEquals(0, weights.observable["confirm"])
    assertEquals(0, weights.observable["password"])
    assertEquals(0, weights.observable["select"])
    assertEquals(0, weights.observable["vote"])

    assertEquals(-1, weights.observable["eo.enter"])
    assertEquals(-1, weights.observable["eo.exit"])
    assertEquals(-1, weights.observable["v.enter"])
    assertEquals(-1, weights.observable["v.exit"])

    assertEquals(-5, weights.controllable["eo.enter"])
    assertEquals(-5, weights.controllable["eo.exit"])
    assertEquals(-5, weights.controllable["v.enter"])
    assertEquals(-5, weights.controllable["v.exit"])

    assertEquals(5, weights.preferred[Word.fromSymbols("select", "back")])
  }

  @Test
  fun testPreferredBehIterator() {
    val a = Word.fromSymbols('a', 'b')
    val preferred = mapOf(Priority.P1 to listOf(a))
    val iter = PreferredBehIterator(preferred).asSequence().toList()
    assertContentEquals(
      listOf(listOf(emptyList()), listOf(listOf(a))),
      iter
    )
  }

  @Test
  fun testPreferredBehIterator2() {
    val a = Word.fromSymbols('a', 'b')
    val b = Word.fromSymbols('c', 'd')
    val c = Word.fromSymbols('e', 'f')
    val preferred = mapOf(Priority.P1 to listOf(a), Priority.P2 to listOf(b), Priority.P3 to listOf(c))
    val iter = PreferredBehIterator(preferred).asSequence().toList()
    assertContentEquals(
      listOf(
        listOf(emptyList()),
        listOf(listOf(a)),
        listOf(listOf(b)),
        listOf(listOf(a, b)),
        listOf(listOf(c)),
        listOf(listOf(a, c)),
        listOf(listOf(b, c)),
        listOf(listOf(a, b, c))
      ),
      iter
    )
  }

  @Test
  fun testPreferredBehIterator3() {
    val a = Word.fromSymbols('a', 'b')
    val b = Word.fromSymbols('c', 'd')
    val c = Word.fromSymbols('e', 'f')
    val preferred = mapOf(Priority.P1 to listOf(a, b), Priority.P3 to listOf(c))
    val iter = PreferredBehIterator(preferred).asSequence().toList()
    assertContentEquals(
      listOf(
        listOf(emptyList()),
        listOf(listOf(a), listOf(b)),
        listOf(listOf(a, b)),
        listOf(listOf(c)),
        listOf(listOf(a, c), listOf(b, c)),
        listOf(listOf(a, b, c)),
      ),
      iter
    )
  }

  @Test
  fun testVoting() {
    val robustifier = loadVoting()
    robustifier.synthesize(Algorithms.Pareto).toList()
    robustifier.synthesize(Algorithms.Fast).toList()
  }

}