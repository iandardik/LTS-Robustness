package cmu.isr.robustify.supervisory

import cmu.isr.dfa.parallelComposition
import cmu.isr.ltsa.LTSACall
import cmu.isr.ltsa.LTSACall.asDetLTS
import cmu.isr.ltsa.LTSACall.compose
import cmu.isr.supremica.SupremicaRunner
import net.automatalib.words.Word
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.config.Configurator
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled
class PumpBenchmarkTests {

  private fun loadPump(): SupervisoryRobustifier {
    val powerSpec =
      ClassLoader.getSystemResource("specs/pump2/power.lts")?.readText() ?: error("Cannot find pump2/power.lts")
    val linesSpec =
      ClassLoader.getSystemResource("specs/pump2/lines.lts")?.readText() ?: error("Cannot find pump2/lines.lts")
    val alarmSpec =
      ClassLoader.getSystemResource("specs/pump2/alarm.lts")?.readText() ?: error("Cannot find pump2/alarm.lts")
    val envSepc =
      ClassLoader.getSystemResource("specs/pump2/deviation.lts")?.readText() ?: error("Cannot find pump2/deviation.lts")
    val pSpec =
      ClassLoader.getSystemResource("specs/pump2/p.lts")?.readText() ?: error("Cannot find pump2/p.lts")

    val power = LTSACall.compile(powerSpec).compose().asDetLTS()
    val lines = LTSACall.compile(linesSpec).compose().asDetLTS()
    val alarm = LTSACall.compile(alarmSpec).compose().asDetLTS()
    var sys = parallelComposition(power, power.inputAlphabet, lines, lines.inputAlphabet)
    sys = parallelComposition(sys, sys.inputAlphabet, alarm, alarm.inputAlphabet)
    val env = LTSACall.compile(envSepc).compose().asDetLTS()
    val safety = LTSACall.compile(pSpec).compose().asDetLTS()

    val ideal = Word.fromSymbols("plug_in", "battery_charge", "battery_charge", "turn_on", "line.1.dispense_main_med_flow", "line.1.flow_complete")
    val ideal2 = Word.fromSymbols("plug_in", "battery_charge", "battery_charge", "turn_on", "line.2.dispense_main_med_flow", "line.2.flow_complete")
    val recover = Word.fromSymbols("plug_in", "line.1.start_dispense", "line.1.dispense_main_med_flow", "line.1.dispense_main_med_flow",
      "power_failure", "plug_in", "line.1.start_dispense", "line.1.dispense_main_med_flow")
    val recover2 = Word.fromSymbols("plug_in", "line.2.start_dispense", "line.2.dispense_main_med_flow", "line.2.dispense_main_med_flow",
      "power_failure", "plug_in", "line.2.start_dispense", "line.2.dispense_main_med_flow")

    return SupervisoryRobustifier(
      sys, sys.inputAlphabet,
      env, env.inputAlphabet,
      safety, safety.inputAlphabet,
      progress = listOf("line.1.flow_complete", "line.2.flow_complete"),
      preferredMap = mapOf(Priority.P3 to listOf(ideal, ideal2, recover, recover2)),
      controllableMap = mapOf(
        Priority.P0 to (1..2).flatMap {
          listOf(
            "line.${it}.start_dispense",
            "line.${it}.dispense_main_med_flow",
            "line.${it}.flow_complete"
          ) },
        Priority.P1 to (1..2).flatMap {
          listOf(
            "line.${it}.change_settings",
            "line.${it}.clear_rate",
            "line.${it}.confirm_settings",
            "line.${it}.set_rate",
          )
        },
        Priority.P3 to (1..2).flatMap {
          listOf(
            "line.${it}.erase_and_unlock_line",
            "line.${it}.lock_line",
            "line.${it}.lock_unit",
            "line.${it}.unlock_unit",
          )
        }
      ),
      observableMap = mapOf(
        Priority.P0 to (1..2).flatMap {
          listOf(
            "line.${it}.start_dispense",
            "line.${it}.dispense_main_med_flow",
            "line.${it}.flow_complete",
            "line.${it}.change_settings",
            "line.${it}.clear_rate",
            "line.${it}.confirm_settings",
            "line.${it}.set_rate",
          )
        },
        Priority.P2 to (1..2).flatMap {
          listOf(
            // Events of the line that are related to the physical world have moderate cost to observe
            "line.${it}.erase_and_unlock_line",
            "line.${it}.lock_line",
            "line.${it}.lock_unit",
            "line.${it}.unlock_unit",
          )
        } + listOf(
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
      synthesizer = SupremicaRunner(),
      maxIter = 1
    )
  }

  @Test
  fun testPump() {
    Configurator.setAllLevels(LogManager.getRootLogger().name, Level.DEBUG)
    val robustifier = loadPump()
    robustifier.use {
      it.synthesize(Algorithms.Fast).toList()
    }
  }

}