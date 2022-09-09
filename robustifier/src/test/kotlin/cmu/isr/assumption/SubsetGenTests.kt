package cmu.isr.assumption

import cmu.isr.ts.alphabet
import cmu.isr.ts.lts.asLTS
import cmu.isr.ts.lts.ltsa.write
import net.automatalib.util.automata.builders.AutomatonBuilders
import net.automatalib.words.impl.Alphabets
import org.junit.jupiter.api.Test

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

    val w = SubsetConstructionGenerator(a, b, p).generate(true)
    write(System.out, w, w.alphabet())
  }

}