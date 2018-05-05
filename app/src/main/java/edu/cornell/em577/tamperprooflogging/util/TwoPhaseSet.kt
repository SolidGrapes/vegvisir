package edu.cornell.em577.tamperprooflogging.util

/**
 * A set abstraction where removing an element removes if forever from a set, even if it has not
 * been added yet.
 */
class TwoPhaseSet<E> {

    private val addSet = HashSet<E>()

    private val removeSet = HashSet<E>()

    /** Check whether the provided element exists in the set. */
    fun lookup(element: E): Boolean {
        return addSet.contains(element) && !removeSet.contains(element)
    }

    /**
     * Add an element to the set. A lookup following an element that was previously removed but
     * later added will yield false.
     */
    fun add(elementToAdd: E) {
        addSet.add(elementToAdd)
    }

    /**
     * Remove an element forever from the set. The element does not need to already be in the
     * set.
     */
    fun remove(elementToRemove: E) {
        removeSet.add(elementToRemove)
    }

    /** Return the elements currently in the set and the removed set of elements. */
    fun toList(): Pair<List<E>, List<E>> {
        return Pair(addSet.minus(removeSet).toList(), removeSet.toList())
    }
}