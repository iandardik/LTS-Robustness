package cmu.isr.tolerance.postprocess

enum class ComparisonResult {
    equal,
    strictlyMoreRobust,
    strictlyLessRobust,
    incomparable
}

fun atLeastAsRobust(delta1 : Set<Set<Triple<Int, String, Int>>>, delta2: Set<Set<Triple<Int, String, Int>>>) : Boolean {
    for (d2 in delta2) {
        val existsD1ContainsD2 = delta1.fold(false) { acc, d1 -> acc || d1.containsAll(d2) }
        if (!existsD1ContainsD2) {
            return false
        }
    }
    return true
}

fun compare(delta1 : Set<Set<Triple<Int, String, Int>>>, delta2: Set<Set<Triple<Int, String, Int>>>) : ComparisonResult {
    if (delta1 == delta2) {
        return ComparisonResult.equal
    }
    if (atLeastAsRobust(delta1, delta2)) {
        return ComparisonResult.strictlyMoreRobust
    }
    if (atLeastAsRobust(delta2, delta1)) {
        return ComparisonResult.strictlyLessRobust
    }
    return ComparisonResult.incomparable
}
