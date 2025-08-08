package com.chitfund.backend.plugins

import io.ktor.server.application.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import com.chitfund.backend.db.*
import com.chitfund.backend.services.MockDataService

fun Application.configureDatabases() {
    try {
        // Enhanced configuration supporting both environment variables and application.conf
        val databaseUrl = System.getenv("DATABASE_URL") 
            ?: environment.config.propertyOrNull("database.url")?.getString() 
            ?: "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1"
        val databaseUser = System.getenv("DATABASE_USER") 
            ?: environment.config.propertyOrNull("database.user")?.getString() 
            ?: "sa"
        val databasePassword = System.getenv("DATABASE_PASSWORD") 
            ?: environment.config.propertyOrNull("database.password")?.getString() 
            ?: ""
        val maxPoolSize = System.getenv("DATABASE_POOL_SIZE")?.toIntOrNull() 
            ?: environment.config.propertyOrNull("database.maxPoolSize")?.getString()?.toIntOrNull() 
            ?: 10
        
        log.info("Attempting to connect to database: ${databaseUrl.replace(Regex("://[^@]+@"), "://***:***@")}")
        
        // Configure HikariCP for production-grade connection pooling
        val config = HikariConfig().apply {
            jdbcUrl = databaseUrl
            username = databaseUser
            password = databasePassword
            
            // Auto-detect driver based on URL
            driverClassName = when {
                databaseUrl.contains("postgresql") -> "org.postgresql.Driver"
                databaseUrl.contains("h2") -> "org.h2.Driver"
                else -> environment.config.propertyOrNull("database.driver")?.getString() 
                    ?: "org.h2.Driver"
            }
            
            // Security and performance settings
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
        log.info("Active connections: ${dataSource.hikariPoolMXBean.activeConnections}")
        
        // Initialize mock data only if it's development (H2 database)
        if (databaseUrl.contains("h2")) {
            try {
                val mockDataService = MockDataService()
                mockDataService.initializeMockData()
                log.info("Mock data initialization completed")
            } catch (e: Exception) {
                log.warn("Failed to initialize mock data: ${e.message}")
            }
        }
        
    } catch (e: Exception) {
        log.error("Failed to connect to database", e)
        // Don't fail the application startup - allow it to run without database for basic health checks
        log.warn("Application will continue without database connection")
    }
}