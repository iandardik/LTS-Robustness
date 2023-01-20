package cmu.isr.robustness

import cmu.isr.robustness.explanation.BaseExplanationGenerator
import cmu.isr.robustness.explanation.ExplanationGenerator
import cmu.isr.ts.alphabet
import cmu.isr.ts.lts.ltsa.LTSACall
import cmu.isr.ts.lts.ltsa.LTSACall.asDetLTS
import cmu.isr.ts.lts.ltsa.LTSACall.asLTS
import cmu.isr.ts.lts.ltsa.LTSACall.compose
import net.automatalib.words.Word
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RobustnessCalculatorTests {

  private fun buildABP(): Pair<RobustnessCalculator<Int, String>, ExplanationGenerator<String>> {
    val sys = LTSACall
      .compile(ClassLoader.getSystemResource("specs/abp/abp.lts").readText())
      .compose()
      .asLTS()
    val env = LTSACall
      .compile(ClassLoader.getSystemResource("specs/abp/abp_env.lts").readText())
      .compose()
      .asLTS()
    val safety = LTSACall
      .compile(ClassLoader.getSystemResource("specs/abp/p.lts").readText())
      .compose()
      .asDetLTS()
    val dev = LTSACall
      .compile(ClassLoader.getSystemResource("specs/abp/abp_env_lossy.lts").readText())
      .compose()
      .asLTS()

    return Pair(BaseCalculator(sys, env, safety), BaseExplanationGenerator(sys, dev))
  }

  private fun buildSimpleProtocol(): Pair<RobustnessCalculator<Int, String>, ExplanationGenerator<String>> {
    val sys = LTSACall
      .compile(ClassLoader.getSystemResource("specs/abp/perfect.lts").readText())
      .compose()
      .asLTS()
    val env = LTSACall
      .compile(ClassLoader.getSystemResource("specs/abp/abp_env.lts").readText())
      .compose()
      .asLTS()
    val safety = LTSACall
      .compile(ClassLoader.getSystemResource("specs/abp/p.lts").readText())
      .compose()
      .asDetLTS()
    val dev = LTSACall
      .compile(ClassLoader.getSystemResource("specs/abp/abp_env_lossy.lts").readText())
      .compose()
      .asLTS()

    return Pair(BaseCalculator(sys, env, safety), BaseExplanationGenerator(sys, dev))
  }

  @Test
  fun testSimpleProtocol() {
    val (cal, explain) = buildSimpleProtocol()
    val expected = setOf(
      Word.fromSymbols("send.0", "rec.0", "ack.1", "getack.0"),
      Word.fromSymbols("send.0", "rec.0", "ack.0", "getack.1"),
      Word.fromSymbols("send.1", "rec.1", "ack.0", "getack.1"),
      Word.fromSymbols("send.1", "rec.1", "ack.1", "getack.0"),
      Word.fromSymbols("send.0", "rec.1"),
      Word.fromSymbols("send.1", "rec.0")
    )
    val actual = cal.computeRobustness().values.flatten().toSet()
    assertEquals(expected = expected, actual = actual)

    val expectedExplain = setOf(
      Word.fromSymbols("input", "send.0", "rec.0", "output", "ack.1", "ack.corrupt", "getack.0"),
      Word.fromSymbols("input", "send.0", "rec.0", "output", "ack.0", "ack.corrupt", "getack.1"),
      Word.fromSymbols("input", "send.1", "rec.1", "output", "ack.0", "ack.corrupt", "getack.1"),
      Word.fromSymbols("input", "send.1", "rec.1", "output", "ack.1", "ack.corrupt", "getack.0"),
      Word.fromSymbols("input", "send.0", "trans.corrupt", "rec.1"),
      Word.fromSymbols("input", "send.1", "trans.corrupt", "rec.0")
    )
    assertEquals(expectedExplain, actual.map { explain.generate(it, cal.weakestAssumption.alphabet()) }.toSet())
  }

  @Test
  fun testABP() {
    val (cal, explain) = buildABP()
    val actual = cal.computeRobustness().values.flatten().toSet()
    for (t in actual) {
      println(explain.generate(t, cal.weakestAssumption.alphabet()))
    }
  }

  private fun buildTherac(): Pair<RobustnessCalculator<Int, String>, ExplanationGenerator<String>> {
    val sys = LTSACall
      .compile(ClassLoader.getSystemResource("specs/therac25/sys.lts").readText())
      .compose()
      .asLTS()
    val env = LTSACall
      .compile(ClassLoader.getSystemResource("specs/therac25/env_perfect.lts").readText())
      .compose()
      .asLTS()
    val safety = LTSACall
      .compile(ClassLoader.getSystemResource("specs/therac25/p.lts").readText())
      .compose()
      .asDetLTS()
    val dev = LTSACall
      .compile(ClassLoader.getSystemResource("specs/therac25/env.lts").readText())
      .compose()
      .asLTS()

    return Pair(BaseCalculator(sys, env, safety), BaseExplanationGenerator(sys, dev))
  }

  private fun buildTheracR(): Pair<RobustnessCalculator<Int, String>, ExplanationGenerator<String>> {
    val sys = LTSACall
      .compile(ClassLoader.getSystemResource("specs/therac25/sys_r.lts").readText())
      .compose()
      .asLTS()
    val env = LTSACall
      .compile(ClassLoader.getSystemResource("specs/therac25/env_perfect.lts").readText())
      .compose()
      .asLTS()
    val safety = LTSACall
      .compile(ClassLoader.getSystemResource("specs/therac25/p.lts").readText())
      .compose()
      .asDetLTS()
    val dev = LTSACall
      .compile(ClassLoader.getSystemResource("specs/therac25/env.lts").readText())
      .compose()
      .asLTS()

    return Pair(BaseCalculator(sys, env, safety), BaseExplanationGenerator(sys, dev))
  }

  @Test
  fun testTherac() {
    val (cal, _) = buildTherac()
    val actual = cal.computeRobustness().values.flatten().toSet()
    val expected = setOf(
      Word.fromSymbols("x", "up"),
      Word.fromSymbols("e", "up"),
      Word.fromSymbols("x", "enter", "up"),
      Word.fromSymbols("e", "enter", "up"),
      Word.fromSymbols("x", "enter", "b", "enter", "e", "up"),
      Word.fromSymbols("e", "enter", "b", "enter", "x", "up"),
      Word.fromSymbols("x", "enter", "b", "enter", "e", "enter", "up"),
      Word.fromSymbols("e", "enter", "b", "enter", "x", "enter", "up"),
    )
    assertEquals(expected = expected, actual = actual)
  }

  @Test
  fun testTheracR() {
    val (cal, _) = buildTheracR()
    val actual = cal.computeRobustness().values.flatten().toSet()
    val expected = setOf(
      Word.fromSymbols("x", "up"),
      Word.fromSymbols("e", "up"),
      Word.fromSymbols("x", "enter", "up"),
      Word.fromSymbols("e", "enter", "up"),
      Word.fromSymbols("x", "enter", "b", "enter", "e", "up"),
      Word.fromSymbols("e", "enter", "b", "enter", "x", "up"),
    )
    assertEquals(expected = expected, actual = actual)
  }

  @Test
  fun testCompareTherac() {
    val (cal1, _) = buildTherac()
    val (cal2, _) = buildTheracR()
    assert(cal1.compare(cal2).isEmpty())
    assertEquals(
      setOf(Word.fromSymbols("x", "up", "e", "enter", "b")),
      cal2.compare(cal1).values.flatten().toSet()
    )
  }
}