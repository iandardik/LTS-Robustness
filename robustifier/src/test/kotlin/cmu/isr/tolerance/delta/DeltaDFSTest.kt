package cmu.isr.tolerance.delta

import addPerturbations
import cmu.isr.tolerance.utils.fspStringToDFA
import cmu.isr.tolerance.utils.fspStringToNFA
import cmu.isr.tolerance.utils.isMaximalAccepting
import cmu.isr.tolerance.utils.stripTauTransitions
import cmu.isr.ts.parallel
import org.junit.jupiter.api.Test
import satisfies

class DeltaDFSTest {

    @Test
    fun toyTest() {
        val envFSP = "S0 = (a -> b -> STOP)."
        val ctrlFSP = "S0 = (a -> S0) + {b}."
        val propFSP = "S0 = (a -> a -> a -> ERROR)."
        val env = stripTauTransitions(fspStringToNFA(envFSP))
        val ctrl = stripTauTransitions(fspStringToNFA(ctrlFSP))
        val prop = fspStringToDFA(propFSP)

        val delta = DeltaDFS(env, ctrl, prop).compute()

        // checks to make sure the solution is sound
        for (d in delta) {
            val envD = addPerturbations(env, d)
            val EdComposeC = parallel(envD, ctrl)
            assert(satisfies(EdComposeC, prop)) { "Violation for Ed||P |= P: $d" }
        }

        // checks to make sure every member of delta is maximal
        for (d in delta) {
            val envD = addPerturbations(env, d)
            assert(isMaximalAccepting(envD, ctrl, prop)) { "Found non-maximal member of delta" }
        }

        // checks to make sure delta has 3 elements
        assert(delta.size == 3) { "Delta is missing elements, should have 3 but found ${delta.size}" }
    }

    @Test
    fun romuloVotingTest() {
        val envFSP = "S0 = (eo.enter -> S3 | v.enter -> S1),\n" +
            "S1 = (v.exit -> S0 | pw -> S2 | sel -> S1 | cfm -> S1 | vote -> S1),\n" +
            "S2 = (cfm -> S1 | pw -> S2 | sel -> S2 | vote -> S2),\n" +
            "S3 = (eo.exit -> S0 | sel -> S3 | cfm -> S3 | vote -> S3)."
        val ctrlFSP = "S0 = (pw -> S1),\n" +
                "S1 = (sel -> S2),\n" +
                "S2 = (vote -> S3 | back -> S1),\n" +
                "S3 = (cfm -> S0 | back -> S2)."
        val propFSP = "S0 = (eo.enter -> S1 | v.enter -> S0 | v.exit -> S0 | cfm -> S0),\n" +
                "S1 = (eo.exit -> S0 | v.enter -> S1 | v.exit -> S1)."
        val env = stripTauTransitions(fspStringToNFA(envFSP))
        val ctrl = stripTauTransitions(fspStringToNFA(ctrlFSP))
        val prop = fspStringToDFA(propFSP)

        val delta = DeltaDFS(env, ctrl, prop).compute()

        // checks to make sure the solution is sound
        for (d in delta) {
            val envD = addPerturbations(env, d)
            val EdComposeC = parallel(envD, ctrl)
            assert(satisfies(EdComposeC, prop)) { "Violation for Ed||P |= P: $d" }
        }

        // checks to make sure every member of delta is maximal
        for (d in delta) {
            val envD = addPerturbations(env, d)
            assert(isMaximalAccepting(envD, ctrl, prop)) { "Found non-maximal member of delta" }
        }

        // checks to make sure delta has 3 elements
        assert(delta.size == 2) { "Delta is missing elements, should have 2 but found ${delta.size}" }
    }
}
