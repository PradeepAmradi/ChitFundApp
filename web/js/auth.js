// Authentication module
class AuthManager {
    constructor() {
        this.setupAuthEventListeners();
    }

    setupAuthEventListeners() {
        // Login form submission
        document.getElementById('loginForm')?.addEventListener('submit', async (e) => {
            e.preventDefault();
            await this.handleLogin(e);
        });

        // OTP form submission
        document.getElementById('otpForm')?.addEventListener('submit', async (e) => {
            e.preventDefault();
            await this.handleOtpVerification(e);
        });

        // Back to login button
        document.getElementById('back-to-login')?.addEventListener('click', () => {
            this.showLoginForm();
        });

        // Auto-focus on OTP input when shown
        const otpInput = document.getElementById('otp');
        if (otpInput) {
            otpInput.addEventListener('input', (e) => {
                // Auto-submit when 6 digits are entered
                if (e.target.value.length === 6) {
                    document.getElementById('otpForm').dispatchEvent(new Event('submit'));
                }
            });
        }
    }

    async handleLogin(e) {
        const form = e.target;
        const formData = new FormData(form);
        const email = formData.get('email').trim();

        // Validate email
        if (!Utils.validateEmail(email)) {
            Utils.showError('Please enter a valid email address');
            return;
        }

        // Show loading state
        const submitButton = form.querySelector('button[type="submit"]');
        Utils.setLoading('send-otp-btn', true);
        Utils.hideMessages();

        try {
            const response = await authAPI.login(email);
            
            if (response.success) {
                this.showOtpForm(email);
                Utils.showSuccess(response.message || 'OTP sent to your email!');
            } else {
                Utils.showError(response.message || 'Failed to send OTP');
            }
        } catch (error) {
            console.error('Login error:', error);
            Utils.showError(`Network error: ${error.message}`);
        } finally {
            Utils.setLoading('send-otp-btn', false);
        }
    }

    async handleOtpVerification(e) {
        const form = e.target;
        const formData = new FormData(form);
        const otp = formData.get('otp').trim();
        const email = this.currentEmail;

        // Validate OTP
        if (!Utils.validateOtp(otp)) {
            Utils.showError('Please enter a valid 6-digit OTP');
            return;
        }

        // Show loading state
        const submitButton = form.querySelector('button[type="submit"]');
        Utils.setLoading('verify-otp-btn', true);
        Utils.hideMessages();

        try {
            const response = await authAPI.verifyOtp(email, otp);
            
            if (response.success) {
                // Store auth token
                authAPI.setToken(response.token);
                
                // Update app state
                if (window.app) {
                    window.app.currentUser = response.user;
                    window.app.isAuthenticated = true;
                    window.app.showMainApp();
                    window.app.loadDashboard();
                }
                
                Utils.showSuccess('Login successful!');
            } else {
                Utils.showError(response.message || 'Invalid OTP');
            }
        } catch (error) {
            console.error('OTP verification error:', error);
            Utils.showError(`Network error: ${error.message}`);
        } finally {
            Utils.setLoading('verify-otp-btn', false);
        }
    }

    showLoginForm() {
        document.getElementById('login-form').classList.remove('hidden');
        document.getElementById('otp-form').classList.add('hidden');
        Utils.hideMessages();
        
        // Clear form
        document.getElementById('loginForm').reset();
        document.getElementById('otpForm').reset();
        
        // Focus on email input
        document.getElementById('email').focus();
    }

    showOtpForm(email) {
        this.currentEmail = email;
        document.getElementById('login-form').classList.add('hidden');
        document.getElementById('otp-form').classList.remove('hidden');
        
        // Update message
        document.getElementById('otp-message').textContent = `OTP has been sent to: ${email}`;
        
        // Focus on OTP input
        document.getElementById('otp').focus();
    }
}

// Initialize authentication manager
document.addEventListener('DOMContentLoaded', () => {
    window.authManager = new AuthManager();
});