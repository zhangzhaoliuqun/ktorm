package me.liuwj.ktorm.dsl

import me.liuwj.ktorm.BaseTest
import me.liuwj.ktorm.entity.*
import org.junit.Test

/**
 * Created by vince on Dec 09, 2018.
 */
class AggregationTest : BaseTest() {

    @Test
    fun testCount() {
        val count = db.sequenceOf(Employees).count { it.departmentId eq 1 }
        assert(count == 2)
    }

    @Test
    fun testCountAll() {
        val count = db.sequenceOf(Employees).count()
        assert(count == 4)
    }

    @Test
    fun testSum() {
        val sum = db.sequenceOf(Employees).sumBy { it.salary + 1 }
        assert(sum == 454L)
    }

    @Test
    fun testMax() {
        val max = db.sequenceOf(Employees).maxBy { it.salary - 1 }
        assert(max == 199L)
    }

    @Test
    fun testMin() {
        val min = db.sequenceOf(Employees).minBy { it.salary }
        assert(min == 50L)
    }

    @Test
    fun testAvg() {
        val avg = db.sequenceOf(Employees).averageBy { it.salary }
        println(avg)
    }

    @Test
    fun testNone() {
        assert(db.sequenceOf(Employees).none { it.salary greater 200L })
    }

    @Test
    fun testAny() {
        assert(!db.sequenceOf(Employees).any { it.salary greater 200L })
    }

    @Test
    fun testAll() {
        assert(db.sequenceOf(Employees).all { it.salary greater 0L })
    }

    @Test
    fun testAggregate() {
        val result = db.sequenceOf(Employees).aggregateColumns { max(it.salary) - min(it.salary) }
        println(result)
        assert(result == 150L)
    }

    @Test
    fun testAggregate2() {
        val (max, min) = db.sequenceOf(Employees).aggregateColumns2 { Pair(max(it.salary), min(it.salary)) }
        assert(max == 200L)
        assert(min == 50L)
    }

    @Test
    fun testGroupAggregate3() {
        db.sequenceOf(Employees)
            .groupingBy { it.departmentId }
            .aggregateColumns2 { Pair(max(it.salary), min(it.salary)) }
            .forEach { departmentId, (max, min) ->
                println("$departmentId:$max:$min")
            }
    }
}