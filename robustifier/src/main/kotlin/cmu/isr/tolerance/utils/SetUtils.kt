package cmu.isr.tolerance.utils

fun <T> powerset(s : Set<T>) : Set<Set<T>> {
    if (s.isEmpty()) {
        return setOf(emptySet())
    }
    val head = s.first()
    val tail = s.drop(1).toSet()
    val subPowerset = powerset(tail)
    val subPowersetWithHead = subPowerset.map { e -> e union setOf(head) }.toSet()
    return subPowerset union subPowersetWithHead
}
