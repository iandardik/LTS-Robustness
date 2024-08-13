package cmu.isr.ts.lts

import cmu.isr.ts.LTS
import cmu.isr.ts.alphabet
import net.automatalib.commons.util.Holder
import net.automatalib.util.ts.traversal.TSTraversal
import net.automatalib.util.ts.traversal.TSTraversalAction
import net.automatalib.util.ts.traversal.TSTraversalVisitor
import net.automatalib.words.Word


class MultiCexResult<I>(
    val alph: Set<String>
) {
    var traces: Set<List<I>?>? = mutableSetOf()
}


class MultiCexVisitor<S, I>(
    private val lts: LTS<S, I>,
    private val result: MultiCexResult<I>,
    private val maxSize: Int
) : TSTraversalVisitor<S, I, S, Word<I>> {
    private val visited = mutableSetOf<S>()

    override fun processInitial(state: S, outData: Holder<Word<I>>?): TSTraversalAction {
        outData!!.value = Word.epsilon()
        return TSTraversalAction.EXPLORE
    }

    override fun startExploration(state: S, data: Word<I>?): Boolean {
        return if (state !in visited) {
            visited.add(state)
            true
        } else {
            false
        }
    }

    override fun processTransition(
        source: S,
        srcData: Word<I>?,
        input: I,
        transition: S,
        succ: S,
        outData: Holder<Word<I>>?
    ): TSTraversalAction {
        outData!!.value = srcData!!.append(input)
        if (lts.isErrorState(succ)) {
            val trace = srcData.append(input)
            val errTrace = trace?.filter {
                val act : String = it.toString()
                val abstractAct = Regex("\\..*$").replace(act, "")
                result.alph.contains(abstractAct)
            }
            if (errTrace!!.isNotEmpty()) {
                // I do not know why errTrace2 is necessary...
                val errTrace2 = trace?.filter {
                    val act : String = it.toString()
                    val abstractAct = Regex("\\..*$").replace(act, "")
                    result.alph.contains(abstractAct)
                }
                result.traces = result.traces!!.plus(errTrace2)
            }
        }
        return if (result.traces!!.size >= maxSize) {
            TSTraversalAction.ABORT_TRAVERSAL
        } else {
            TSTraversalAction.EXPLORE
        }
    }

}

object MultiTraceCex {
    fun <I> findErrorTraces(lts: LTS<Int, I>, numTraces: Int, alph: Set<String>): Set<List<I>?> {
        val hasInitErrorState = lts.initialStates.contains(lts.errorState)
        if (hasInitErrorState) {
            return setOf(emptyList())
        }
        val result = MultiCexResult<I>(alph)
        val vis = MultiCexVisitor(lts, result, numTraces)
        TSTraversal.breadthFirst(lts, lts.alphabet(), vis)
        return result.traces ?: error("No error found!")
    }
}
