package cmu.isr.robustness

import cmu.isr.ts.lts.ltsa.LTSACall
import cmu.isr.ts.lts.ltsa.LTSACall.asDetLTS
import cmu.isr.ts.lts.ltsa.LTSACall.compose
import net.automatalib.words.Word
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RobustnessCalculatorTests {

  private fun buildABP(): RobustnessCalculator<Int, String> {
    val sys = LTSACall.compile(ClassLoader.getSystemResource("specs/abp/abp.lts").readText())
      .compose().asDetLTS()
    val env = LTSACall.compile(ClassLoader.getSystemResource("specs/abp/abp_env.lts").readText())
      .compose().asDetLTS()
    val safety = LTSACall.compile(ClassLoader.getSystemResource("specs/abp/p.lts").readText())
      .compose().asDetLTS()

    return BaseCalculator(sys, env, safety)
  }

  private fun buildSimpleProtocol(): RobustnessCalculator<Int, String> {
    val sys = LTSACall.compile(ClassLoader.getSystemResource("specs/abp/perfect.lts").readText())
      .compose().asDetLTS()
    val env = LTSACall.compile(ClassLoader.getSystemResource("specs/abp/abp_env.lts").readText())
      .compose().asDetLTS()
    val safety = LTSACall.compile(ClassLoader.getSystemResource("specs/abp/p.lts").readText())
      .compose().asDetLTS()

    return BaseCalculator(sys, env, safety)
  }

  @Test
  fun testSimpleProtocol() {
    val cal = buildSimpleProtocol()
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
  }

  @Test
  fun testABP() {
    val cal = buildABP()
    val actual = cal.computeRobustness().values.flatten().toSet()
    for (t in actual) {
      println(t)
    }
  }

  private fun buildTherac(): RobustnessCalculator<Int, String> {
    val sys = LTSACall.compile(ClassLoader.getSystemResource("specs/therac25/sys.lts").readText())
      .compose().asDetLTS()
    val env = LTSACall.compile(ClassLoader.getSystemResource("specs/therac25/env_perfect.lts").readText())
      .compose().asDetLTS()
    val safety = LTSACall.compile(ClassLoader.getSystemResource("specs/therac25/p.lts").readText())
      .compose().asDetLTS()

    return BaseCalculator(sys, env, safety)
  }

  private fun buildTheracR(): RobustnessCalculator<Int, String> {
    val sys = LTSACall.compile(ClassLoader.getSystemResource("specs/therac25/sys_r.lts").readText())
      .compose().asDetLTS()
    val env = LTSACall.compile(ClassLoader.getSystemResource("specs/therac25/env_perfect.lts").readText())
      .compose().asDetLTS()
    val safety = LTSACall.compile(ClassLoader.getSystemResource("specs/therac25/p.lts").readText())
      .compose().asDetLTS()

    return BaseCalculator(sys, env, safety)
  }

  @Test
  fun testTherac() {
    val cal = buildTherac()
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
    val cal = buildTheracR()
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
    val cal1 = buildTherac()
    val cal2 = buildTheracR()
    assert(cal1.compare(cal2).isEmpty())
    assertEquals(
      setOf(Word.fromSymbols("x", "up", "e", "enter", "b")),
      cal2.compare(cal1).values.flatten().toSet()
    )
  }
}