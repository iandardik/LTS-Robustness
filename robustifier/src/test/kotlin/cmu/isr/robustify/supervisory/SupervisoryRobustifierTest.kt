package cmu.isr.robustify.supervisory

import cmu.isr.ltsa.LTSACall
import cmu.isr.ltsa.LTSACall.asDetLTS
import cmu.isr.ltsa.LTSACall.compose
import cmu.isr.robustify.desops.DESopsRunner
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

    val sys = LTSACall.compile(sysSpec).compose().asDetLTS()
    val env = LTSACall.compile(envSpec).compose().asDetLTS()
    val safety = LTSACall.compile(pSpec).compose().asDetLTS()
    val back = Word.fromSymbols("select", "back")

    return SupervisoryRobustifier(
      sys, sys.inputAlphabet,
      env, env.inputAlphabet,
      safety, safety.inputAlphabet,
      progress = listOf("confirm"),
      preferredMap = mapOf(Priority.P3 to listOf(back)),
      controllableMap = mapOf(
        Priority.P0 to listOf("back", "confirm", "password", "select", "vote"),
        Priority.P3 to listOf("eo.enter", "eo.exit", "v.enter", "v.exit")
      ),
      observableMap = mapOf(
        Priority.P0 to listOf("back", "confirm", "password", "select", "vote"),
        Priority.P2 to listOf("eo.enter", "eo.exit", "v.enter", "v.exit")
      ),
      synthesizer = DESopsRunner() { it },
      maxIter = 1
    )
  }

  private fun loadTherac(): SupervisoryRobustifier {
    val sysSpec =
      ClassLoader.getSystemResource("specs/therac25/sys.lts")?.readText() ?: error("Cannot find therac25/sys.lts")
    val envSpec =
      ClassLoader.getSystemResource("specs/therac25/env.lts")?.readText() ?: error("Cannot find therac25/env.lts")
    val pSpec = ClassLoader.getSystemResource("specs/therac25/p.lts")?.readText() ?: error("Cannot find therac25/p.lts")
    val back1 = Word.fromSymbols("x", "up")
    val back2 = Word.fromSymbols("e", "up")
    val back3 = Word.fromSymbols("enter", "up")

    val sys = LTSACall.compile(sysSpec).compose().asDetLTS()
    val env = LTSACall.compile(envSpec).compose().asDetLTS()
    val safety = LTSACall.compile(pSpec).compose().asDetLTS()

    return SupervisoryRobustifier(
      sys, sys.inputAlphabet,
      env, env.inputAlphabet,
      safety, safety.inputAlphabet,
      progress = listOf("fire_xray", "fire_ebeam"),
      preferredMap = mapOf(Priority.P3 to listOf(back1, back2), Priority.P2 to listOf(back3)),
      controllableMap = mapOf(
        Priority.P1 to listOf("fire_xray", "fire_ebeam"),
        Priority.P2 to listOf("setMode"),
        Priority.P3 to listOf("x", "e", "enter", "up", "b")
      ),
      observableMap = mapOf(
        Priority.P0 to listOf("x", "e", "enter", "up", "b", "fire_xray", "fire_ebeam"),
        Priority.P1 to listOf("setMode")
      ),
      synthesizer = SupremicaRunner(),
      maxIter = 1
    )
  }

  private fun loadPump(): SupervisoryRobustifier {
    val powerSpec =
      ClassLoader.getSystemResource("specs/pump/power.lts")?.readText() ?: error("Cannot find pump/power.lts")
    val linesSpec =
      ClassLoader.getSystemResource("specs/pump/lines.lts")?.readText() ?: error("Cannot find pump/lines.lts")
    val alarmSpec =
      ClassLoader.getSystemResource("specs/pump/alarm.lts")?.readText() ?: error("Cannot find pump/alarm.lts")
    val envSepc =
      ClassLoader.getSystemResource("specs/pump/deviation.lts")?.readText() ?: error("Cannot find pump/deviation.lts")
    val pSpec =
      ClassLoader.getSystemResource("specs/pump/p.lts")?.readText() ?: error("Cannot find pump/p.lts")

    val power = LTSACall.compile(powerSpec).compose().asDetLTS()
    val lines = LTSACall.compile(linesSpec).compose().asDetLTS()
    val alarm = LTSACall.compile(alarmSpec).compose().asDetLTS()
    var sys = cmu.isr.lts.parallelComposition(power, power.inputAlphabet, lines, lines.inputAlphabet)
    sys = cmu.isr.lts.parallelComposition(sys, sys.inputAlphabet, alarm, alarm.inputAlphabet)
    val env = LTSACall.compile(envSepc).compose().asDetLTS()
    val safety = LTSACall.compile(pSpec).compose().asDetLTS()

    val ideal = Word.fromSymbols("plug_in", "battery_charge", "battery_charge", "turn_on", "line.1.dispense_main_med_flow", "line.1.flow_complete")
    val recover = Word.fromSymbols("plug_in", "line.1.start_dispense", "line.1.dispense_main_med_flow", "line.1.dispense_main_med_flow",
      "power_failure", "plug_in", "line.1.start_dispense", "line.1.dispense_main_med_flow")

    return SupervisoryRobustifier(
      sys, sys.inputAlphabet,
      env, env.inputAlphabet,
      safety, safety.inputAlphabet,
      progress = listOf("line.1.flow_complete"),
      preferredMap = mapOf(Priority.P3 to listOf(ideal, recover)),
      controllableMap = mapOf(
        Priority.P0 to listOf(
          "line.1.start_dispense",
          "line.1.dispense_main_med_flow",
          "line.1.flow_complete",
        ),
        Priority.P1 to listOf(
          "line.1.change_settings",
          "line.1.clear_rate",
          "line.1.confirm_settings",
          "line.1.set_rate",
        ),
        Priority.P3 to listOf(
          "line.1.erase_and_unlock_line",
          "line.1.lock_line",
          "line.1.lock_unit",
          "line.1.unlock_unit",
        )
      ),
      observableMap = mapOf(
        Priority.P0 to listOf(
          "line.1.start_dispense",
          "line.1.dispense_main_med_flow",
          "line.1.flow_complete",
          "line.1.change_settings",
          "line.1.clear_rate",
          "line.1.confirm_settings",
          "line.1.set_rate",
        ),
        Priority.P2 to listOf(
          // Events of the line that are related to the physical world have moderate cost to observe
          "line.1.erase_and_unlock_line",
          "line.1.lock_line",
          "line.1.lock_unit",
          "line.1.unlock_unit",
          // Has moderate cost to observe some system events in other modules
          "alarm_silence",
          "enable_alarm",
        ),
        Priority.P3 to listOf(
          // Has high cost to observe some events of other modules related to the physical world.
          "battery_charge",
          "battery_spent",
          "plug_in",
          "unplug",
          "turn_off",
          "turn_on",
        )
      ),
      synthesizer = DESopsRunner() { it },
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

    robustifier.close()
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
    robustifier.close()
  }

  @Test
  fun testTherac() {
    val robustifier = loadTherac()
    robustifier.synthesize(Algorithms.Pareto).toList()
    robustifier.synthesize(Algorithms.Fast).toList()
    robustifier.close()
  }

  @Test
  fun testPump() {
    val robustifier = loadPump()
    robustifier.synthesize(Algorithms.Pareto).toList()
    robustifier.synthesize(Algorithms.Fast).toList()
    robustifier.close()
  }
}