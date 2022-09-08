package cmu.isr.robustify.supervisory

import cmu.isr.ltsa.LTSACall
import cmu.isr.ltsa.LTSACall.asDetLTS
import cmu.isr.ltsa.LTSACall.compose
import cmu.isr.supremica.SupremicaRunner
import net.automatalib.words.Word
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled
class VotingBenchmarkTests {

  private fun generateEnv(voters: Int, officials: Int): String = """
    range N_VOTER = 1..${voters}
    range N_EO = 1..${officials}

    ENV = (v[i:N_VOTER].enter -> VOTER[i] | eo[j:N_EO].enter -> EO[j]),
    VOTER[i:N_VOTER] = (password -> VOTER[i] | select -> VOTER[i] | vote -> VOTER[i] | confirm -> v[i].done -> v[i].exit -> ENV | back -> VOTER[i] | v[i].exit -> ENV),
    EO[j:N_EO] = (select -> EO[j] | vote -> EO[j] | confirm -> eo[j].exit -> ENV | back -> EO[j] | eo[j].exit -> ENV).
  """.trimIndent()

  private fun generateProp(voters: Int, officials: Int): String = """
    const NoBody = 0
    const VOTERS = $voters

    range N_VOTER = 1..${voters}
    range N_EO = 1..${officials}

    range WHO = NoBody..${officials+voters}

    P = VOTE[NoBody][NoBody][NoBody],
    VOTE[in:WHO][sel:WHO][v:WHO] = (
          v[i:N_VOTER].enter -> VOTE[i][sel][v] | eo[j:N_EO].enter -> VOTE[VOTERS+j][sel][v]
        | password -> VOTE[in][sel][in]
        | select -> VOTE[in][in][v]
        | when (in > 0 && in <= VOTERS && in == sel && sel == v) confirm -> VOTE[in][NoBody][NoBody]
        | when (in > VOTERS && sel == v) confirm -> VOTE[in][NoBody][NoBody]
    ).
  """.trimIndent()

  private fun generateTest(voters: Int, officials: Int): SupervisoryRobustifier {
    val sysSpec =
      ClassLoader.getSystemResource("specs/voting/sys.lts")?.readText() ?: error("Cannot find voting/sys.lts")
    val envSpec = generateEnv(voters, officials)
    val pSpec = generateProp(voters, officials)

    val sys = LTSACall.compile(sysSpec).compose().asDetLTS()
    val env = LTSACall.compile(envSpec).compose().asDetLTS()
    val safety = LTSACall.compile(pSpec).compose().asDetLTS()
    val back = Word.fromSymbols("select", "back")
//    val back = Word.fromSymbols("select", "back", "select")
//    val back2 = Word.fromSymbols("select", "vote", "back", "back", "select")

    val dones = (1..voters).map { "v.${it}.done" }
    val entries = (1..voters).map { "v.${it}.enter" } +
        (1..voters).map { "v.${it}.exit" } +
        (1..officials).map { "eo.${it}.enter" } +
        (1..officials).map { "eo.${it}.exit" }

    return SupervisoryRobustifier(
      sys, sys.inputAlphabet,
      env, env.inputAlphabet,
      safety, safety.inputAlphabet,
      progress = dones,
      preferredMap = mapOf(Priority.P3 to listOf(back)),
      controllableMap = mapOf(
        Priority.P0 to listOf("back", "confirm", "password", "select", "vote"),
        Priority.P3 to entries
      ),
      observableMap = mapOf(
        Priority.P0 to listOf("back", "confirm", "password", "select", "vote") + dones,
        Priority.P2 to entries
      ),
      synthesizer = SupremicaRunner(),
      maxIter = 1
    )
  }

  @Test
  fun testVoting() {
    val robustifier = generateTest(2, 1)
    robustifier.use {
      robustifier.synthesize(Algorithms.Pareto).toList()
    }
  }
}