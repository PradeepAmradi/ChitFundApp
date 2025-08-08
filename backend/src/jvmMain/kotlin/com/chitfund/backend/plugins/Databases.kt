package com.chitfund.backend.plugins

import io.ktor.server.application.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import com.chitfund.backend.db.*
import com.chitfund.backend.services.MockDataService
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

fun Application.configureDatabases() {
    try {
        val databaseUrl = System.getenv("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/chitfund"
        val databaseUser = System.getenv("DATABASE_USER") ?: "chitfund"
        val databasePassword = System.getenv("DATABASE_PASSWORD") ?: "password"
        val maxPoolSize = System.getenv("DATABASE_POOL_SIZE")?.toIntOrNull() ?: 10
        
        log.info("Attempting to connect to database: ${databaseUrl.replace(Regex("://[^@]+@"), "://***:***@")}")
        
        // Configure HikariCP for secure database connections
        val config = HikariConfig().apply {
            jdbcUrl = databaseUrl
            username = databaseUser
            password = databasePassword
            driverClassName = "org.postgresql.Driver"
            
            // Security settings
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_READ_COMMITTED"
            maximumPoolSize = maxPoolSize
            minimumIdle = 2
            connectionTimeout = 30000
            idleTimeout = 600000
            maxLifetime = 1800000
            
            // SSL settings for production
            if (System.getenv("DATABASE_SSL_MODE") != null) {
                addDataSourceProperty("sslmode", System.getenv("DATABASE_SSL_MODE"))
            }
            
            // Connection validation
            connectionTestQuery = "SELECT 1"
            validationTimeout = 5000
        }
        
        val dataSource = HikariDataSource(config)
        val database = Database.connect(dataSource)
        
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