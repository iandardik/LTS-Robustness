package cmu.isr.tolerance

import addPerturbations
import cmu.isr.ts.alphabet
import cmu.isr.ts.lts.CompactDetLTS
import cmu.isr.ts.lts.CompactLTS
import cmu.isr.ts.parallel
import net.automatalib.words.Alphabet
import product
import satisfies

fun <T> allPerturbations(states : Collection<T>, alphabet : Alphabet<String>) : Set<Set<Triple<T, String, T>>> {
    fun pertHelper(perturbations : MutableSet<MutableSet<Triple<T,String,T>>>,
                   powerset : Set<Triple<T,String,T>>) {
        if (powerset.isNotEmpty()) {
            val elem = powerset.first()
            pertHelper(perturbations, powerset - elem)

            val dPlusElems = mutableSetOf<MutableSet<Triple<T, String, T>>>()
            for (d in perturbations) {
                dPlusElems += (d + elem) as MutableSet<Triple<T, String, T>>
            }
            perturbations += dPlusElems
        }
    }
    val perturbations : MutableSet<MutableSet<Triple<T,String,T>>> = mutableSetOf(mutableSetOf())
    val powerset = product(states, alphabet.toMutableSet(), states)
    pertHelper(perturbations, powerset)
    return perturbations
}

fun deltaNaiveBruteForce(env : CompactLTS<String>,
                         ctrl : CompactLTS<String>,
                         prop : CompactDetLTS<String>
)
        : Set<Set<Triple<Int,String,Int>>> {
    val delta = DeltaBuilder(env, ctrl, prop)
    val allPerts = allPerturbations(env.states, env.alphabet())

    for (d in allPerts) {
        val envD = addPerturbations(env, d)
        val envDCompC = parallel(envD, ctrl)
        if (satisfies(envDCompC, prop)) {
            delta += d
        }
    }

    return delta.toSet()
}
