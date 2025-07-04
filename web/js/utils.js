// Utility functions
class Utils {
    static formatCurrency(amount) {
        // Convert from paisa to rupees
        const rupees = amount / 100;
        return new Intl.NumberFormat('en-IN', {
            style: 'currency',
            currency: 'INR',
            maximumFractionDigits: 0
        }).format(rupees);
    }

    static formatIndianCurrency(amount) {
        // Convert from paisa to rupees
        const rupees = amount / 100;
        
        if (rupees >= 10000000) {
            return `₹${(rupees / 10000000).toFixed(1)}Cr`;
        } else if (rupees >= 100000) {
            return `₹${(rupees / 100000).toFixed(1)}L`;
        } else if (rupees >= 1000) {
            return `₹${(rupees / 1000).toFixed(1)}K`;
        } else {
            return `₹${rupees.toFixed(0)}`;
        }
    }

    static formatDate(dateString) {
        const date = new Date(dateString);
        return date.toLocaleDateString('en-IN', {
            year: 'numeric',
            month: 'short',
            day: 'numeric'
        });
    }

    static formatMonth(monthString) {
        const [year, month] = monthString.split('-');
        const date = new Date(year, month - 1);
        return date.toLocaleDateString('en-IN', {
            year: 'numeric',
            month: 'long'
        });
    }

    static validateEmail(email) {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    }

    static validateOtp(otp) {
        return /^\d{6}$/.test(otp);
    }

    static validateChitAmount(amount) {
        const numAmount = parseFloat(amount);
        return numAmount >= 100000 && numAmount <= 5000000; // ₹1L to ₹50L
    }

    static validateTenure(tenure) {
        const numTenure = parseInt(tenure);
        return numTenure >= 12 && numTenure <= 24;
    }

    static validateMemberCount(count) {
        const numCount = parseInt(count);
        return numCount >= 10 && numCount <= 25 && numCount % 5 === 0;
    }

    static showError(message, containerId = 'error-message') {
        const errorDiv = document.getElementById(containerId);
        if (errorDiv) {
            errorDiv.textContent = message;
            errorDiv.classList.remove('hidden');
            setTimeout(() => {
                errorDiv.classList.add('hidden');
            }, 5000);
        }
    }

    static showSuccess(message, containerId = 'success-message') {
        const successDiv = document.getElementById(containerId);
        if (successDiv) {
            successDiv.textContent = message;
            successDiv.classList.remove('hidden');
            setTimeout(() => {
                successDiv.classList.add('hidden');
            }, 5000);
        }
    }

    static hideMessages() {
        const errorDiv = document.getElementById('error-message');
        const successDiv = document.getElementById('success-message');
        if (errorDiv) errorDiv.classList.add('hidden');
        if (successDiv) successDiv.classList.add('hidden');
    }

    static setLoading(buttonId, isLoading = true) {
        const button = document.getElementById(buttonId);
        if (button) {
            const textSpan = button.querySelector('.btn-text');
            const loadingSpan = button.querySelector('.btn-loading');
            
            if (isLoading) {
                if (textSpan) textSpan.classList.add('hidden');
                if (loadingSpan) loadingSpan.classList.remove('hidden');
                button.disabled = true;
            } else {
                if (textSpan) textSpan.classList.remove('hidden');
                if (loadingSpan) loadingSpan.classList.add('hidden');
                button.disabled = false;
            }
        }
    }

    static createElement(tag, className = '', innerHTML = '') {
        const element = document.createElement(tag);
        if (className) element.className = className;
        if (innerHTML) element.innerHTML = innerHTML;
        return element;
    }

    static debounce(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    }

    static getCurrentMonth() {
        const now = new Date();
        const year = now.getFullYear();
        const month = String(now.getMonth() + 1).padStart(2, '0');
        return `${year}-${month}`;
    }

    static getNextMonth(monthString, months = 1) {
        const [year, month] = monthString.split('-').map(Number);
        const date = new Date(year, month - 1 + months);
        return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
    }

    static calculateEndMonth(startMonth, tenure) {
        return this.getNextMonth(startMonth, tenure);
    }

    static copyToClipboard(text) {
        navigator.clipboard.writeText(text).then(() => {
            this.showSuccess('Copied to clipboard!');
        }).catch(() => {
            this.showError('Failed to copy to clipboard');
        });
    }

    static generateId() {
        return Math.random().toString(36).substr(2, 9);
    }

    static isValidDate(dateString) {
        const date = new Date(dateString);
        return date instanceof Date && !isNaN(date);
    }

    static formatFileSize(bytes) {
        if (bytes === 0) return '0 Bytes';
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }
}