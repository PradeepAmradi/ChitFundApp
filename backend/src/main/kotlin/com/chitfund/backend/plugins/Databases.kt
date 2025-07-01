package com.chitfund.backend.plugins

import io.ktor.server.application.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import com.chitfund.backend.db.*

fun Application.configureDatabases() {
    val driverClassName = "org.postgresql.Driver"
    val jdbcURL = System.getenv("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/chitfund"
    val database = Database.connect(jdbcURL, driverClassName)
    
    transaction(database) {
        SchemaUtils.create(Users, Chits, ChitMembers, Payments, Payouts)
    }
}