package day4

import day1.*
import java.util.concurrent.*
import java.util.concurrent.atomic.*

class FlatCombiningQueue<E> : Queue<E> {
    private val queue = ArrayDeque<E>() // sequential queue
    private val combinerLock = AtomicBoolean(false) // unlocked initially
    private val tasksForCombiner = AtomicReferenceArray<Any?>(TASKS_FOR_COMBINER_SIZE)

    private fun AtomicBoolean.tryLock(): Boolean {
        return compareAndSet(false, true)
    }

    private fun AtomicBoolean.unlock() {
        set(false)
    }

    private fun performCombinerTask(op: Any): Result<E?>? {
        when (op) {
            is Dequeue -> return performDequeue()
            is Result<*> -> return null
            else -> return performEnqueue(op)
        }
    }

    private fun performEnqueue(op: Any): Result<E?> {
        // Enqueue
        queue.addLast(op as E)
        return Result(op)
    }

    private fun performDequeue(): Result<E?> {
        val elem = queue.removeFirstOrNull()
        return Result(elem)
    }

    enum class Operation {
        ENQUEUE,
        DEQUEUE
    }

    private fun performOperation(operation: Operation, value: E?): Any? {
        val isCombiner = combinerLock.tryLock()
        if(isCombiner) {
            val returnValue = when(operation) {
                Operation.ENQUEUE -> {
                    queue.addLast(value!!)
                }
                Operation.DEQUEUE -> {
                    queue.removeFirstOrNull()
                }
            }
            combinerLock.unlock()
            return returnValue
        }

        // we couldn't obtain the lock

        // TODO: 2b. If the lock is already acquired, announce this operation in
        // TODO:     `tasksForCombiner` by replacing a random cell state from
        // TODO:      `null` with the element.
        val itemToPutInCell = when(operation) {
            Operation.ENQUEUE -> value
            Operation.DEQUEUE -> Dequeue
        }
        val cellIndex = putInRandomCell(itemToPutInCell!!)


        while (true) {
            val cellValue = tasksForCombiner.get(cellIndex)
            if (cellValue is Result<*>) {
                return when(operation) {
                    Operation.ENQUEUE -> {
                        // successful enqueue
                        cleanCell(cellIndex)
                        // we're done
                    }
                    Operation.DEQUEUE -> {
                        val retVal = cellValue.value as E
                        cleanCell(cellIndex)
                        retVal
                    }
                }
            }
            // TODO: or `combinerLock` becomes available to acquire.
            val gotLock = combinerLock.tryLock()
            if (gotLock) {
                val cellValue = tasksForCombiner.get(cellIndex)
                if (cellValue is Result<*>) {
                    return when(operation) {
                        Operation.ENQUEUE -> {
                            // successful enqueue
                            cleanCell(cellIndex)
                            // we're done
                            combinerLock.unlock()
                        }
                        Operation.DEQUEUE -> {
                            val retVal = cellValue.value as E
                            cleanCell(cellIndex)
                            combinerLock.unlock()
                            retVal
                        }
                    }
                }
                cleanCell(cellIndex)
                // TODO: 2a. On success, apply this operation
                val retVal = when(operation) {
                    Operation.ENQUEUE -> {
                        queue.add(value!!)
                    }
                    Operation.DEQUEUE -> {
                        val retVal = queue.removeFirstOrNull()
                        retVal
                    }
                }
                // TODO: and help others by traversing
                // TODO:     `tasksForCombiner`, performing the announced operations, and
                // TODO:      updating the corresponding cells to `Result`.
                performAllCombinerTasks()
                combinerLock.unlock()
                return retVal
            }
        }
    }

    override fun enqueue(element: E) {
        performOperation(Operation.ENQUEUE, element)
    }

    fun performAllCombinerTasks() {
        // TODO: and help others by traversing
        // TODO:     `tasksForCombiner`, performing the announced operations, and
        // TODO:      updating the corresponding cells to `Result`.
        repeat(TASKS_FOR_COMBINER_SIZE) { index ->
            val task = tasksForCombiner[index]
            if (task != null) {
                val value = performCombinerTask(task)
                if(value != null) {
                    tasksForCombiner[index] = value
                }
            }
        }
    }


    override fun dequeue(): E? {
        return performOperation(Operation.DEQUEUE, null) as E?
    }

    private fun cleanCell(index: Int) {
        tasksForCombiner.set(index, null)
    }

    private fun putInRandomCell(element: Any): Int {
        while (true) {
            val cellIndex = randomCellIndex()
            val hasAdded = tasksForCombiner.compareAndSet(cellIndex, null, element)
            if (hasAdded) return cellIndex
        }
    }

    private fun randomCellIndex(): Int =
        ThreadLocalRandom.current().nextInt(tasksForCombiner.length())
}

private const val TASKS_FOR_COMBINER_SIZE = 3 // Do not change this constant!

// TODO: Put this token in `tasksForCombiner` for dequeue().
// TODO: enqueue()-s should put the inserting element.
private object Dequeue

// TODO: Put the result wrapped with `Result` when the operation in `tasksForCombiner` is processed.
private class Result<V>(
    val value: V
)