package cmu.isr.assumption

import cmu.isr.ts.alphabet
import cmu.isr.ts.lts.asLTS
import cmu.isr.ts.lts.ltsa.LTSACall
import cmu.isr.ts.lts.ltsa.LTSACall.asDetLTS
import cmu.isr.ts.lts.ltsa.LTSACall.compose
import cmu.isr.ts.lts.ltsa.write
import net.automatalib.util.automata.Automata
import net.automatalib.util.automata.builders.AutomatonBuilders
import net.automatalib.words.impl.Alphabets
import org.junit.jupiter.api.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class SubsetGenTests {

  @Test
  fun testWA() {
    val a = AutomatonBuilders.newDFA(Alphabets.fromArray("a", "b", "c"))
      .withInitial(0)
      .from(0).on("a").to(1)
      .from(1).on("b").to(2).on("a").to(1)
      .from(2).on("c").to(0)
      .withAccepting(0, 1, 2)
      .create()
      .asLTS()

    val b = AutomatonBuilders.newDFA(Alphabets.fromArray("a", "b"))
      .withInitial(0)
      .from(0).on("a").to(0).on("b").to(0)
      .withAccepting(0)
      .create()
      .asLTS()

    val p = AutomatonBuilders.newDFA(Alphabets.fromArray("a", "c"))
      .withInitial(0)
      .from(0).on("a").to(1)
      .from(1).on("c").to(0)
      .withAccepting(0, 1)
      .create()
      .asLTS()

    val w = SubsetConstructionGenerator(a, b, p).generate()

    val c = AutomatonBuilders.newDFA(Alphabets.fromArray("a", "b"))
      .withInitial(0)
      .from(0).on("a").to(1)
      .from(1).on("b").to(0)
      .withAccepting(0, 1)
      .create()

    assertContentEquals(c.alphabet(), w.alphabet())
    assert(Automata.testEquivalence(c, w, c.alphabet())) {
      write(System.err, w, w.alphabet())
    }
  }

  @Test
  fun testSimpleProtocol() {
    val sys = LTSACall.compile(ClassLoader.getSystemResource("specs/abp/perfect.lts").readText())
      .compose().asDetLTS()
    val env = LTSACall.compile(ClassLoader.getSystemResource("specs/abp/abp_env.lts").readText())
      .compose().asDetLTS()
    val safety = LTSACall.compile(ClassLoader.getSystemResource("specs/abp/p.lts").readText())
      .compose().asDetLTS()

    val w = SubsetConstructionGenerator(sys, env, safety).generate()

    val c = LTSACall.compile(ClassLoader.getSystemResource("specs/abp/perfect_wa.lts").readText())
      .compose().asDetLTS()

    assertEquals(c.alphabet().toSet(), w.alphabet().toSet())
    assert(Automata.testEquivalence(c, w, c.alphabet())) {
      write(System.err, w, w.alphabet())
    }
  }

  @Test
  fun testTherac() {
    val sys = LTSACall.compile(ClassLoader.getSystemResource("specs/therac25/sys.lts").readText())
      .compose().asDetLTS()
    val env = LTSACall.compile(ClassLoader.getSystemResource("specs/therac25/env_perfect.lts").readText())
      .compose().asDetLTS()
    val safety = LTSACall.compile(ClassLoader.getSystemResource("specs/therac25/p.lts").readText())
      .compose().asDetLTS()

    val w = SubsetConstructionGenerator(sys, env, safety).generate()
    write(System.out, w, w.alphabet())
  }

  @Test
  fun testTheracR() {
    val sys = LTSACall.compile(ClassLoader.getSystemResource("specs/therac25/sys_r.lts").readText())
      .compose().asDetLTS()
    val env = LTSACall.compile(ClassLoader.getSystemResource("specs/therac25/env_perfect.lts").readText())
      .compose().asDetLTS()
    val safety = LTSACall.compile(ClassLoader.getSystemResource("specs/therac25/p.lts").readText())
      .compose().asDetLTS()

    val w = SubsetConstructionGenerator(sys, env, safety).generate()
    write(System.out, w, w.alphabet())
  }

}