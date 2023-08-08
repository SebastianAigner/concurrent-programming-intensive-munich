package day2

import day1.*
import java.util.concurrent.atomic.*
import kotlin.math.*

class FAABasedQueueSimplified<E> : Queue<E> {
    private val infiniteArray = AtomicReferenceArray<Any?>(1024) // conceptually infinite array
    private val enqIdx = AtomicLong(0)
    private val deqIdx = AtomicLong(0)

    override fun enqueue(element: E) {
        while (true) {
            // TODO: Increment the counter atomically via Fetch-and-Add.
            // TODO: Use `getAndIncrement()` function for that.
            val i = enqIdx.getAndIncrement()
            // TODO: Atomically install the element into the cell
            // TODO: if the cell is not poisoned.
            if (infiniteArray.compareAndSet(i.toInt(), null, element)) {
                // inserted properly
                return
            }
        }
    }

    var deqInvoq = 0

    @Suppress("UNCHECKED_CAST")

    // *********
    // My initial implementation: Opportunistically attempts to read from the Array,
    // if it's non-null, everything is great.
    // Otherwise, things may have happened before
    // *********
//    override fun dequeue(): E? {
//        while(true) {
//            // Is this queue empty?
//            if (deqIdx.get() >= enqIdx.get()) return null
//            // TODO: Increment the counter atomically via Fetch-and-Add.
//            // TODO: Use `getAndIncrement()` function for that.
//            val i = deqIdx.getAndIncrement()
//            // TODO: Try to retrieve an element if the cell contains an
//            // TODO: element, poisoning the cell if it is empty.
//            val retVal = infiniteArray.get(i.toInt())
//            if (retVal == null) {
//                if (infiniteArray.compareAndSet(i.toInt(), null, POISONED)) {
//                    // the poisoning succeeded: we need to restart
//                    continue
//                } else {
//                    // we read a null, but a value was written in the meantime
//                    // "the poisoning failed"
//                    // that means we do have a value tho
//                    val lateRetVal = infiniteArray.get(i.toInt())
//                    infiniteArray.set(i.toInt(), null)
//                    return lateRetVal as E
//                }
//            }
//            infiniteArray.set(i.toInt(), null)
//            return retVal as E
//        }
//    }

    override fun dequeue(): E? {
        while (true) {
            // Is this queue empty?
            if (deqIdx.get() >= enqIdx.get()) return null
            // TODO: Increment the counter atomically via Fetch-and-Add.
            // TODO: Use `getAndIncrement()` function for that.
            val i = deqIdx.getAndIncrement()
            // TODO: Try to retrieve an element if the cell contains an
            // TODO: element, poisoning the cell if it is empty.
            val isPoisoned = infiniteArray.compareAndSet(i.toInt(), null, POISONED)
            if (isPoisoned) {
                continue
            } else {
                // we read a null, but a value was written in the meantime
                // "the poisoning failed"
                // that means we do have a value tho
                val retval = infiniteArray.get(i.toInt())
                infiniteArray.set(i.toInt(), null)
                return retval as E
            }
        }
    }

    override fun validate() {
        for (i in 0 until min(deqIdx.get().toInt(), enqIdx.get().toInt())) {
            check(infiniteArray[i] == null || infiniteArray[i] == POISONED) {
                "`infiniteArray[$i]` must be `null` or `POISONED` with `deqIdx = ${deqIdx.get()}` at the end of the execution"
            }
        }
        for (i in max(deqIdx.get().toInt(), enqIdx.get().toInt()) until infiniteArray.length()) {
            check(infiniteArray[i] == null || infiniteArray[i] == POISONED) {
                "`infiniteArray[$i]` must be `null` or `POISONED` with `enqIdx = ${enqIdx.get()}` at the end of the execution"
            }
        }
    }
}

// TODO: poison cells with this value.
private val POISONED = Any()
