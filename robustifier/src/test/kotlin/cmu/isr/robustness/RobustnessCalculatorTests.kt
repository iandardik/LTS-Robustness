package cmu.isr.robustness

import cmu.isr.ts.lts.ltsa.LTSACall
import cmu.isr.ts.lts.ltsa.LTSACall.asDetLTS
import cmu.isr.ts.lts.ltsa.LTSACall.compose
import net.automatalib.words.Word
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RobustnessCalculatorTests {

  @Test
  fun testABP() {
    val sys = LTSACall.compile(ClassLoader.getSystemResource("specs/abp/perfect.lts").readText())
      .compose().asDetLTS()
    val env = LTSACall.compile(ClassLoader.getSystemResource("specs/abp/abp_env.lts").readText())
      .compose().asDetLTS()
    val safety = LTSACall.compile(ClassLoader.getSystemResource("specs/abp/p.lts").readText())
      .compose().asDetLTS()

    val cal = BaseCalculator(sys, env, safety)
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
  fun testTherac() {
    val sys = LTSACall.compile(ClassLoader.getSystemResource("specs/therac25/sys.lts").readText())
      .compose().asDetLTS()
    val env = LTSACall.compile(ClassLoader.getSystemResource("specs/therac25/env_perfect.lts").readText())
      .compose().asDetLTS()
    val safety = LTSACall.compile(ClassLoader.getSystemResource("specs/therac25/p.lts").readText())
      .compose().asDetLTS()

    val cal = BaseCalculator(sys, env, safety)
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
}