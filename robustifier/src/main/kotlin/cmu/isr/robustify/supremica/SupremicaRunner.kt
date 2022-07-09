package cmu.isr.robustify.supremica

import cmu.isr.robustify.supervisory.CompactSupDFA
import cmu.isr.robustify.supervisory.SupervisoryDFA
import cmu.isr.robustify.supervisory.SupervisorySynthesizer
import net.automatalib.words.Alphabet
import org.supremica.automata.Automata
import org.supremica.automata.AutomatonType
import org.supremica.automata.algorithms.AutomataSynthesizer
import org.supremica.automata.algorithms.SynchronizationOptions
import org.supremica.automata.algorithms.SynthesisAlgorithm
import org.supremica.automata.algorithms.SynthesisType
import org.supremica.automata.algorithms.SynthesizerOptions

class SupremicaRunner : SupervisorySynthesizer<Int, String> {

    private val synthOptions = SynthesizerOptions.getDefaultSynthesizerOptions()
    private val syncOptions = SynchronizationOptions.getDefaultSynthesisOptions()

    init {
        synthOptions.dialogOK = false
        synthOptions.oneEventAtATime = false
        synthOptions.addOnePlantAtATime = false
        synthOptions.synthesisType = SynthesisType.NONBLOCKING_CONTROLLABLE_NORMAL
        synthOptions.synthesisAlgorithm = SynthesisAlgorithm.MONOLITHIC_WATERS
        synthOptions.setPurge(true)
        synthOptions.setRename(true)
        synthOptions.removeUnecessarySupervisors = false
        synthOptions.maximallyPermissive = true
        synthOptions.maximallyPermissiveIncremental = true
        synthOptions.reduceSupervisors = false
        synthOptions.localizeSupervisors = false
    }

    override fun synthesize(
        plant: SupervisoryDFA<*, String>,
        inputs1: Alphabet<String>,
        prop: SupervisoryDFA<*, String>,
        inputs2: Alphabet<String>
    ): CompactSupDFA<String>? {
        assert(synthOptions.isValid)
        checkAlphabets(plant, inputs1, prop, inputs2)

        val theAutomata = Automata()
        val plantAutomaton = write(plant, inputs1, "plant")
        val specAutomaton = write(prop, inputs2, "spec")
        plantAutomaton.type = AutomatonType.PLANT
        specAutomaton.type = AutomatonType.SPECIFICATION
        theAutomata.addAutomaton(plantAutomaton)
        theAutomata.addAutomaton(specAutomaton)

        val synthesizer = AutomataSynthesizer(theAutomata, syncOptions, synthOptions)
        val sup = synthesizer.execute().firstAutomaton

        return if (sup.nbrOfStates() == 0) null else read(sup)
    }

}