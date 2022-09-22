package cmu.isr.tolerance

class NFA<A : Any> {
    private val _nodes: MutableSet<Node> = mutableSetOf<Node>()
    private val _edges: MutableSet<Edge<A>> = mutableSetOf<Edge<A>>()

    fun addState(n: String) {
        _nodes.add(Node(n))
    }

    fun addInitialState(n: String) {
        _nodes.add(Node(n, true))
    }

    fun addErrorState(n: String) {
        _nodes.add(Node(n, false, true))
    }

    fun addTransition(src: String, a: A, dst: String) {
        val nsrc = _nodes.find { it.name == src }
        val ndst = _nodes.find { it.name == dst }
        if (nsrc != null && ndst != null) {
            _edges.add(Edge(nsrc, a, ndst))
        } else {
            error("Cannot add specified transition: src or dst node DNE")
        }
    }

    fun getStates(): Set<String> {
        return (_nodes.map { it.name }).toSet()
    }

    fun getInitialStates(): Set<String> {
        return getInitialNodes().map { it.name }.toSet()
    }

    fun getErrorStates(): Set<String> {
        return getErrorNodes().map { it.name }.toSet()
    }

    fun getTransitions(): Set<Triple<String, A, String>> {
        return _edges.map { Triple(it.src.name, it.act, it.dst.name) }.toSet()
    }

    fun getTransitionsFrom(c: Collection<String> = getStates()): Set<Triple<String, A, String>> {
        var correspondingEdges = _edges.filter { c.contains(it.src.name) }
        return correspondingEdges.map { Triple(it.src.name, it.act, it.dst.name) }.toSet()
    }

    private fun getTransitionEdgesFrom(c: Collection<Node>): Set<Edge<A>> {
        return _edges.filter { c.contains(it.src) }.toSet()
    }

    /**
     * Finds the first error trace (of actions) if one exists. If no error trace exists then returns null.
     */
    fun errorTrace(): List<A>? {
        fun etHelper(curr: Node, visited: Set<Node>, trace: List<A>): List<A>? {
            if (curr.err) {
                return trace
            }
            if (!visited.contains(curr)) {
                val newVisited = visited + curr
                for (t in getTransitionEdgesFrom(listOf(curr))) {
                    val newTrace = trace + t.act
                    val result = etHelper(t.dst, newVisited, newTrace)
                    if (result != null) {
                        return result
                    }
                }
            }
            return null
        }
        for (n in getInitialNodes()) {
            val result = etHelper(n, emptySet(), emptyList())
            if (result != null) {
                return result
            }
        }
        return null
    }

    private fun getInitialNodes(): Set<Node> {
        return _nodes.filter { it.init }.toSet()
    }

    private fun getErrorNodes(): Set<Node> {
        return _nodes.filter { it.err }.toSet()
    }


    private class Node(name: String, init: Boolean = false, err: Boolean = false) {
        public val name: String = name
        public val init: Boolean = init
        public val err: Boolean = err

        override fun equals(other: Any?): Boolean {
            if (other is Node) {
                return name == other.name
            }
            return false
        }

        override fun hashCode() = name.hashCode()
    }

    private class Edge<A : Any>(src: Node, act: A, dst: Node) {
        public val src: Node = src
        public val act: A = act
        public val dst: Node = dst

        override fun equals(other: Any?): Boolean {
            // * is not ideal..
            if (other is Edge<*>) {
                return src == other.src
                        && act == other.act
                        && dst == other.dst
            }
            return false
        }

        override fun hashCode() = src.hashCode() * act.hashCode() * dst.hashCode()
    }


    companion object {
        /*fun <A : Any> parallelCompose(n1: NFA<A>, n2: NFA<A>): NFA<A> {

        }*/
    }
}