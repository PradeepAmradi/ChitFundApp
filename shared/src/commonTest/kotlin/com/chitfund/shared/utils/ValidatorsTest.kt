package com.chitfund.shared.utils

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals

class ValidatorsTest {
    
    @Test
    fun testEmailValidation() {
        assertTrue(Validators.isValidEmail("test@example.com"))
        assertTrue(Validators.isValidEmail("user.name@domain.co.in"))
        assertFalse(Validators.isValidEmail("invalid-email"))
        assertFalse(Validators.isValidEmail("@domain.com"))
        assertFalse(Validators.isValidEmail("user@"))
    }
    
    @Test
    fun testMobileValidation() {
        assertTrue(Validators.isValidMobile("+1234567890"))
        assertTrue(Validators.isValidMobile("9876543210"))
        assertFalse(Validators.isValidMobile("123"))
        assertFalse(Validators.isValidMobile("0123456789"))
        assertFalse(Validators.isValidMobile(""))
    }
    
    @Test
    fun testChitAmountValidation() {
        assertTrue(Validators.isValidChitAmount(1000000000L)) // ₹1L
        assertTrue(Validators.isValidChitAmount(5000000000000L)) // ₹50L
        assertFalse(Validators.isValidChitAmount(500000000L)) // ₹0.5L
        assertFalse(Validators.isValidChitAmount(6000000000000L)) // ₹60L
        assertFalse(Validators.isValidChitAmount(0L))
    }
    
    @Test
    fun testTenureValidation() {
        assertTrue(Validators.isValidTenure(12))
        assertTrue(Validators.isValidTenure(24))
        assertFalse(Validators.isValidTenure(11))
        assertFalse(Validators.isValidTenure(25))
    }
    
    @Test
    fun testMemberCountValidation() {
        assertTrue(Validators.isValidMemberCount(10))
        assertTrue(Validators.isValidMemberCount(25))
        assertFalse(Validators.isValidMemberCount(9))
        assertFalse(Validators.isValidMemberCount(13))
        assertFalse(Validators.isValidMemberCount(26))
    }
}

class DateUtilsTest {
    
    @Test
    fun testCalculateEndMonth() {
        assertEquals("2024-12", DateUtils.calculateEndMonth("2024-01", 12))
        assertEquals("2025-12", DateUtils.calculateEndMonth("2024-01", 24))
        assertEquals("2024-06", DateUtils.calculateEndMonth("2024-01", 6))
    }
    
    @Test
    fun testFormatCurrency() {
        assertEquals("₹10.00", DateUtils.formatCurrency(1000L))
        assertEquals("₹100.50", DateUtils.formatCurrency(10050L))
    }
    
    @Test
    fun testFormatLargeCurrency() {
        assertEquals("₹1.0L", DateUtils.formatLargeCurrency(1000000000L))
        assertEquals("₹50.0L", DateUtils.formatLargeCurrency(5000000000000L))
    }
}