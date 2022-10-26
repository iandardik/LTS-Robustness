package cmu.isr.tolerance

import cmu.isr.ts.DetLTS
import cmu.isr.ts.alphabet

// Tree is no longer an appropriate name for these classes, they're all graphs

class Tree<E,D>(private val data : D) {
    private val children : MutableSet<Pair<E, Tree<E,D>>> = mutableSetOf()

    fun addChild(edgeData : E, nodeData : D) {
        children.add(Pair(edgeData, Tree(nodeData)))
    }

    fun addChild(edgeData : E, child : Tree<E,D>) {
        children.add(Pair(edgeData, child))
    }

    /**
     * Returns all the edge data (actions) that lead to a child in the next level of the tree
     */
    fun currentLevelEdgeData() : Set<E> {
        return children.fold(HashSet<E>() as Set<E>) { acc,c -> acc + c.first }
    }

    /**
     * Returns all the children at the next level of the tree whose edge data (action) is contained in the edgeData
     * parameter
     */
    fun nextLevel(edgeData : Set<E>) : Set<Tree<E,D>> {
        return children
                .filter { edgeData.contains(it.first) }
                .fold(HashSet<Tree<E,D>>() as Set<Tree<E,D>>) { acc,child -> acc + child.second }
    }
}

class WATree {
    private val treesAtLevel : Set<Tree<String,Int>>

    constructor(wa : DetLTS<Int, String>) {
        // since we require wa to be deterministic, the init set always has one tree
        treesAtLevel = setOf(initTree(wa))
    }

    private constructor(root : Set<Tree<String,Int>>) {
        treesAtLevel = root
    }

    fun actionsAtLevel() : Set<String> {
        return treesAtLevel.fold(HashSet<String>() as Set<String>) { acc,t -> acc union t.currentLevelEdgeData() }
    }

    fun nextLevel(actions : Set<String>) : WATree {
        val nextRoot = treesAtLevel
            .fold(HashSet<Tree<String,Int>>() as Set<Tree<String,Int>>) { acc,t -> acc union t.nextLevel(actions) }
        return WATree(nextRoot)
    }

    private fun initTree(wa : DetLTS<Int, String>) : Tree<String,Int> {
        fun initTreeHelper(tree : Tree<String,Int>, state : Int, visited : MutableMap<Int, Tree<String,Int>>) {
            visited[state] = tree
            for (a in wa.alphabet()) {
                for (dst in wa.getTransitions(state, a)) {
                    if (dst != null) {
                        if (dst in visited) {
                            // already explored, don't recurse
                            val prevTree = visited[dst] ?: throw RuntimeException("Cant construct init tree")
                            tree.addChild(a, prevTree)
                        }
                        else {
                            // unexplored, recurse
                            val child = Tree<String, Int>(dst)
                            tree.addChild(a, child)
                            initTreeHelper(child, dst, visited)
                        }
                    }
                }
            }
        }
        val init = wa.initialState ?: throw RuntimeException("Initial state is null")
        val tree = Tree<String, Int>(init)
        val visited = mutableMapOf<Int, Tree<String,Int>>()
        initTreeHelper(tree, init, visited)
        return tree
    }
}