import cmu.isr.tolerance.utils.copyLTS
import cmu.isr.ts.lts.CompactLTS

fun addPerturbations(lts : CompactLTS<String>, d : Set<Triple<Int,String,Int>>) : CompactLTS<String> {
    val ltsD = copyLTS(lts)
    for (t in d) {
        ltsD.addTransition(t.first, t.second, t.third)
    }
    return ltsD
}

/**
 * The order of the args may be a bit misleading, but essentially we're asking:
 *      is d2 <= d1 ?
 * Or equivalently,
 *      is d1 at least as powerful as d2?
 */
fun atLeastAsPowerful(E : CompactLTS<String>, d2 : Set<Triple<Int,String,Int>>, d1 : Set<Triple<Int,String,Int>>) : Boolean {
    return d1.containsAll(d2)

    /*
    val Td2 = addPerturbations(E, d2)
    val Td1 = addPerturbations(E, d1)
    val Td1Det = toDeterministic(Td1)

    if (satisfies(Td2, Td1Det)) {
        val Td2Det = toDeterministic(Td2)
        return if (satisfies(Td1, Td2Det)) {
            // Runs(Td2) = Runs(Td1)
            d1.containsAll(d2)
        }
        else {
            // Runs(Td2) ⊂ Runs(Td1)
            true
        }
    }

    return false
     */
}
