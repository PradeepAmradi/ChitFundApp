package com.chitfund.backend.plugins

import io.ktor.server.application.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import com.chitfund.backend.db.*
import com.chitfund.backend.services.MockDataService

fun Application.configureDatabases() {
    try {
        val driverClassName = "org.postgresql.Driver"
        val jdbcURL = System.getenv("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/chitfund"
        
        log.info("Attempting to connect to database: ${jdbcURL.replace(Regex("://[^@]+@"), "://***:***@")}")
        
        val database = Database.connect(jdbcURL, driverClassName)
        
        transaction(database) {
            SchemaUtils.create(Users, Chits, ChitMembers, Payments, Payouts)
        }
        
        log.info("Database connection established and schema created successfully")
        
        // Initialize mock data
        try {
            val mockDataService = MockDataService()
            mockDataService.initializeMockData()
            log.info("Mock data initialization completed")
        } catch (e: Exception) {
            log.warn("Failed to initialize mock data: ${e.message}")
        }
        
    } catch (e: Exception) {
        log.error("Failed to connect to database", e)
        // Don't fail the application startup - allow it to run without database for basic health checks
        log.warn("Application will continue without database connection")
    }
}