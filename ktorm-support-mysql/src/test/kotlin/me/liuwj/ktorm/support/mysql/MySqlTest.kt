package me.liuwj.ktorm.support.mysql

import me.liuwj.ktorm.BaseTest
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.entity.*
import me.liuwj.ktorm.logging.ConsoleLogger
import me.liuwj.ktorm.logging.LogLevel
import org.junit.ClassRule
import org.junit.Test
import org.testcontainers.containers.MySQLContainer
import java.time.LocalDate

/**
 * Created by vince on Dec 12, 2018.
 */
class MySqlTest : BaseTest() {

    companion object {
        class KMySqlContainer : MySQLContainer<KMySqlContainer>()

        @ClassRule
        @JvmField
        val mysql = KMySqlContainer()
    }

    override fun init() {
        db = Database.connect(
            url = mysql.jdbcUrl,
            driver = mysql.driverClassName,
            user = mysql.username,
            password = mysql.password,
            logger = ConsoleLogger(threshold = LogLevel.TRACE)
        )

        execSqlScript("init-mysql-data.sql")
    }

    @Test
    fun testLimit() {
        val query = db.from(Employees).select().orderBy(Employees.id.desc()).limit(0, 2)
        assert(query.totalRecords == 4)

        val ids = query.map { it[Employees.id] }
        assert(ids[0] == 4)
        assert(ids[1] == 3)
    }

    @Test
    fun testBulkInsert() {
        db.bulkInsert(Employees) {
            item {
                it.name to "jerry"
                it.job to "trainee"
                it.managerId to 1
                it.hireDate to LocalDate.now()
                it.salary to 50
                it.departmentId to 1
            }
            item {
                it.name to "linda"
                it.job to "assistant"
                it.managerId to 3
                it.hireDate to LocalDate.now()
                it.salary to 100
                it.departmentId to 2
            }
        }

        assert(db.sequenceOf(Employees).count() == 6)
    }

    @Test
    fun testInsertOrUpdate() {
        db.insertOrUpdate(Employees) {
            it.id to 1
            it.name to "vince"
            it.job to "engineer"
            it.salary to 1000
            it.hireDate to LocalDate.now()
            it.departmentId to 1

            onDuplicateKey {
                it.salary to it.salary + 900
            }
        }
        db.insertOrUpdate(Employees) {
            it.id to 5
            it.name to "vince"
            it.job to "engineer"
            it.salary to 1000
            it.hireDate to LocalDate.now()
            it.departmentId to 1

            onDuplicateKey {
                it.salary to it.salary + 900
            }
        }

        assert(db.sequenceOf(Employees).find { it.id eq 1 }!!.salary == 1000L)
        assert(db.sequenceOf(Employees).find { it.id eq 5 }!!.salary == 1000L)
    }

    @Test
    fun testNaturalJoin() {
        val query = Employees.naturalJoin(Departments).select()
        assert(query.count() == 0)
    }

    @Test
    fun testPagingSql() {
        var query = db.from(Employees)
            .leftJoin(Departments, on = Employees.departmentId eq Departments.id)
            .select()
            .orderBy(Departments.id.desc())
            .limit(0, 1)

        assert(query.totalRecords == 4)

        query = db.from(Employees)
            .select(Employees.name)
            .orderBy((Employees.id + 1).desc())
            .limit(0, 1)

        assert(query.totalRecords == 4)

        query = db.from(Employees)
            .select(Employees.departmentId, avg(Employees.salary))
            .groupBy(Employees.departmentId)
            .limit(0, 1)

        assert(query.totalRecords == 2)

        query = db.from(Employees)
            .selectDistinct(Employees.departmentId)
            .limit(0, 1)

        assert(query.totalRecords == 2)

        query = db.from(Employees)
            .select(max(Employees.salary))
            .limit(0, 1)

        assert(query.totalRecords == 1)

        query = db.from(Employees)
            .select(Employees.name)
            .limit(0, 1)

        assert(query.totalRecords == 4)
    }

    @Test
    fun testDrop() {
        val employees = db.sequenceOf(Employees).drop(1).drop(1).drop(1).toList()
        assert(employees.size == 1)
        assert(employees[0].name == "penny")
    }

    @Test
    fun testTake() {
        val employees = db.sequenceOf(Employees).take(2).take(1).toList()
        assert(employees.size == 1)
        assert(employees[0].name == "vince")
    }

    @Test
    fun testElementAt() {
        val employee = db.sequenceOf(Employees).drop(2).elementAt(1)

        assert(employee.name == "penny")
        assert(db.sequenceOf(Employees).elementAtOrNull(4) == null)
    }

    @Test
    fun testMapColumns3() {
        db.sequenceOf(Employees)
            .filter { it.departmentId eq 1 }
            .mapColumns3 { Triple(it.id, it.name, dateDiff(LocalDate.now(), it.hireDate)) }
            .forEach { (id, name, days) ->
                println("$id:$name:$days")
            }
    }

    @Test
    fun testMatchAgainst() {
        val employees = db.sequenceOf(Employees).filterTo(ArrayList()) {
            match(it.name, it.job).against("vince", SearchModifier.IN_NATURAL_LANGUAGE_MODE)
        }

        employees.forEach { println(it) }
    }

    @Test
    fun testReplace() {
        val names = db.sequenceOf(Employees).mapColumns { it.name.replace("vince", "VINCE") }
        println(names)
    }
}