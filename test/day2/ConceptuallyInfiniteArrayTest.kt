package day2

import TestBase
import org.jetbrains.kotlinx.lincheck.annotations.Operation
import org.jetbrains.kotlinx.lincheck.annotations.Param
import org.jetbrains.kotlinx.lincheck.annotations.Validate
import org.jetbrains.kotlinx.lincheck.paramgen.IntGen
import java.util.concurrent.atomic.AtomicReferenceArray

//class ConceptuallyInfiniteArraySequential {
//    val array = AtomicReferenceArray<Any?>(1024)
//    fun compareAndSet(i: Int, expectedValue: Any?, newValue: Any?): Boolean = array.compareAndSet(i, expectedValue, newValue)
//    fun get(i: Int): Any? = array.get(i)
//    fun set(i: Int, value: Any?) = array.set(i, value)
//}

@Param(name = "element", gen = IntGen::class, conf = "0:3")
@Param(name = "index", gen = IntGen::class, conf = "0:10")
class ConceptuallyInfiniteArrayTest : TestBase(
    checkObstructionFreedom = true,
    threads = 2,
    actorsBefore = 5
) {
    private val array = ConceptuallyInfiniteArray()

    @Operation
    fun compareAndSet(@Param(name = "index") i: Int, expectedValue: Int, newValue: Int): Boolean =
        array.compareAndSet(i, expectedValue, newValue)

    @Operation
    fun get(@Param(name = "index") i: Int): Any? = array.get(i)

    @Operation
    fun set(@Param(name = "index") i: Int, value: Int) = array.set(i, value)

//    @Validate
//    fun validate() = array.validate()
}