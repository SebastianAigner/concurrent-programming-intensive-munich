package day1

import java.util.concurrent.atomic.AtomicReference

class MSQueue<E> : Queue<E> {
    private val head: AtomicReference<Node<E>>
    private val tail: AtomicReference<Node<E>>

    init {
        val dummy = Node<E>(null)
        head = AtomicReference(dummy)
        tail = AtomicReference(dummy)
    }

    override fun enqueue(element: E) {
        while (true) {
            val node = Node(element)
            val currentTail = tail.get()
            if (currentTail.next.compareAndSet(null, node)) {
                // successfully added, need to move the tail
                tail.compareAndSet(currentTail, node)
                // ^ if this fails, we've been helped
                return
            } else {
                // turns out currentTail.next was not NULL
                // we 'help' by moving the tail one forward
                tail.compareAndSet(currentTail, currentTail.next.get())
            }
        }
    }

    override fun dequeue(): E? {
        while(true) {
            val curHead = head.get() // dummy
            val nextNode = curHead.next.get() // real value
            if(nextNode == null) return null
            val retVal = nextNode.element
            if(head.compareAndSet(curHead, nextNode)) {
                return retVal
            }
        }
    }

    // FOR TEST PURPOSE, DO NOT CHANGE IT.
    override fun validate() {
        check(tail.get().next.get() == null) {
            "At the end of the execution, `tail.next` must be `null`"
        }
        check(head.get().element == null) {
            "At the end of the execution, the dummy node shouldn't store an element"
        }
    }

    private class Node<E>(
        var element: E?
    ) {
        val next = AtomicReference<Node<E>?>(null)
    }
}
