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
        val config = HikariConfig()
        
        // Get configuration from application.conf with fallbacks
        val databaseUrl = environment.config.propertyOrNull("database.url")?.getString() 
            ?: "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1"
        val databaseDriver = environment.config.propertyOrNull("database.driver")?.getString() 
            ?: "org.h2.Driver"
        val databaseUser = environment.config.propertyOrNull("database.user")?.getString() 
            ?: "sa"
        val databasePassword = environment.config.propertyOrNull("database.password")?.getString() 
            ?: ""
        val maxPoolSize = environment.config.propertyOrNull("database.maxPoolSize")?.getString()?.toIntOrNull() 
            ?: 10
        
        log.info("Attempting to connect to database: ${databaseUrl.replace(Regex("://[^@]+@"), "://***:***@")}")
        
        // Configure HikariCP for production-grade connection pooling
        config.apply {
            jdbcUrl = databaseUrl
            driverClassName = databaseDriver
            username = databaseUser
            password = databasePassword
            maximumPoolSize = maxPoolSize
            
            // Security settings
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_READ_COMMITTED"
            
            // Connection validation
            connectionTestQuery = "SELECT 1"
            validationTimeout = 3000
            
            // Performance settings
            connectionTimeout = 30000 // 30 seconds
            idleTimeout = 300000 // 5 minutes
            maxLifetime = 900000 // 15 minutes
            leakDetectionThreshold = 60000 // 1 minute
            
            // For PostgreSQL SSL (if using PostgreSQL in production)
            if (jdbcUrl.contains("postgresql")) {
                // Add SSL properties for PostgreSQL
                addDataSourceProperty("sslMode", "prefer")
                addDataSourceProperty("sslCert", System.getenv("DB_SSL_CERT") ?: "")
                addDataSourceProperty("sslKey", System.getenv("DB_SSL_KEY") ?: "")
                addDataSourceProperty("sslRootCert", System.getenv("DB_SSL_ROOT_CERT") ?: "")
            }
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