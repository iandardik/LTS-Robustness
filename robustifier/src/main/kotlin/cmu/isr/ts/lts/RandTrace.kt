package cmu.isr.ts.lts

import cmu.isr.ts.DetLTS
import cmu.isr.ts.LTS
import cmu.isr.ts.MutableDetLTS
import cmu.isr.ts.alphabet
import net.automatalib.commons.util.Holder
import net.automatalib.util.ts.traversal.TSTraversal
import net.automatalib.util.ts.traversal.TSTraversalAction
import net.automatalib.util.ts.traversal.TSTraversalVisitor
import net.automatalib.words.Word

class RandTraceVisitor<S, I>(
    private val lts: LTS<S, I>,
    private var targetSize: Int
) : TSTraversalVisitor<S, I, S, Word<I>> {
    var result: Word<I> = Word.epsilon()
    private val visited = mutableSetOf<S>()

    override fun processInitial(state: S, outData: Holder<Word<I>>?): TSTraversalAction {
        outData!!.value = Word.epsilon()
        return TSTraversalAction.EXPLORE
    }

    override fun startExploration(state: S, data: Word<I>): Boolean {
        return if (state !in visited && data.size() < targetSize) {
            visited.add(state)
            true
        } else {
            false
        }
    }

    override fun processTransition(
        source: S,
        srcData: Word<I>,
        input: I,
        transition: S,
        succ: S,
        outData: Holder<Word<I>>?
    ): TSTraversalAction {
        outData!!.value = srcData.append(input)
        if (outData.value.size() == targetSize) {
            result = outData.value
            return TSTraversalAction.ABORT_TRAVERSAL
        }
        return TSTraversalAction.EXPLORE
    }

}

object RandTraceUtils {
    fun <I> randTrace(lts: LTS<Int, I>, size: Int): Word<I> {
        val vis = RandTraceVisitor(lts, size)
        TSTraversal.breadthFirst(lts, lts.alphabet(), vis)
        return vis.result
    }
}
