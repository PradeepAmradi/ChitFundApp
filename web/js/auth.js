// Authentication module
class AuthManager {
    constructor() {
        this.setupAuthEventListeners();
        this.checkOAuthCallback();      // handle returning from OAuth redirect
        this.probeOAuthProviders();     // enable/disable OAuth buttons
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

        // Resend OTP link
        document.getElementById('resend-otp')?.addEventListener('click', (e) => {
            e.preventDefault();
            document.getElementById('loginForm')?.dispatchEvent(new Event('submit'));
        });

        // ── OTP digit-box behaviour ──────────────────────────────────────
        this.setupOtpDigitBoxes();
    }

    /** Wire up OTP individual digit inputs with auto-advance, backspace & paste. */
    setupOtpDigitBoxes() {
        const digits = document.querySelectorAll('.otp-digit');
        if (!digits.length) return;

        const syncHidden = () => {
            const val = Array.from(digits).map(d => d.value).join('');
            const hidden = document.getElementById('otp');
            if (hidden) hidden.value = val;
            // Mark filled state
            digits.forEach(d => d.classList.toggle('filled', d.value !== ''));
            // Auto-submit when all 6 filled
            if (val.length === 6) {
                document.getElementById('otpForm')?.dispatchEvent(new Event('submit'));
            }
        };

        digits.forEach((input, idx) => {
            input.addEventListener('input', (e) => {
                // Keep only last digit
                input.value = input.value.replace(/\D/g, '').slice(-1);
                syncHidden();
                if (input.value && idx < digits.length - 1) {
                    digits[idx + 1].focus();
                }
            });

            input.addEventListener('keydown', (e) => {
                if (e.key === 'Backspace' && !input.value && idx > 0) {
                    digits[idx - 1].focus();
                    digits[idx - 1].value = '';
                    syncHidden();
                }
                if (e.key === 'ArrowLeft' && idx > 0) digits[idx - 1].focus();
                if (e.key === 'ArrowRight' && idx < digits.length - 1) digits[idx + 1].focus();
            });

            // Select content on focus for easy overwrite
            input.addEventListener('focus', () => input.select());
        });

        // Handle paste on any digit
        digits[0]?.addEventListener('paste', (e) => {
            e.preventDefault();
            const pasted = (e.clipboardData.getData('text') || '').replace(/\D/g, '').slice(0, 6);
            pasted.split('').forEach((ch, i) => {
                if (digits[i]) digits[i].value = ch;
            });
            syncHidden();
            const nextEmpty = Array.from(digits).findIndex(d => !d.value);
            (digits[nextEmpty >= 0 ? nextEmpty : 5])?.focus();
        });
    }

    // ── OAuth callback detection ──────────────────────────────────────────

    /**
     * Called on page load. Checks the URL hash for an OAuth callback or error.
     * Hash format:  #oauth-callback=<one-time-code>
     *           or  #oauth-error=<encoded-message>
     */
    checkOAuthCallback() {
        const hash = window.location.hash;

        if (hash.startsWith('#oauth-callback=')) {
            const code = hash.substring('#oauth-callback='.length);
            // Clear the hash immediately so it's not leaked on refresh/bookmarks
            history.replaceState(null, '', window.location.pathname);
            this.exchangeOAuthCode(code);
        } else if (hash.startsWith('#oauth-error=')) {
            const msg = decodeURIComponent(hash.substring('#oauth-error='.length));
            history.replaceState(null, '', window.location.pathname);
            setTimeout(() => Utils.showError(msg), 300);
        }
    }

    /**
     * Exchange the one-time OAuth code for JWT tokens, then log in.
     */
    async exchangeOAuthCode(code) {
        // Show a full-screen loading overlay during exchange
        this.showOAuthLoading('Completing sign-in…');

        try {
            // Force live mode for the exchange call (OAuth is always live)
            const savedMock = API_CONFIG.useMockData;
            ConfigManager.setMockData(false);

            const response = await authAPI.exchangeOAuthCode(code);

            if (response.success) {
                // Store JWT
                authAPI.setToken(response.token);
                chitAPI.setToken(response.token);
                if (response.refreshToken) {
                    localStorage.setItem('refreshToken', response.refreshToken);
                }

                // Update app state
                if (window.app) {
                    window.app.currentUser = response.user;
                    window.app.isAuthenticated = true;
                    window.app.showMainApp();
                    window.app.loadDashboard();
                }
                Utils.showSuccess(response.message || 'Signed in successfully!');
            } else {
                // Restore mock mode if exchange failed
                ConfigManager.setMockData(savedMock);
                Utils.showError(response.message || 'OAuth sign-in failed');
            }
        } catch (error) {
            console.error('OAuth exchange error:', error);
            Utils.showError(`Sign-in failed: ${error.message}`);
        } finally {
            this.hideOAuthLoading();
        }
    }

    // ── OAuth provider probe ──────────────────────────────────────────────

    /**
     * Ask the backend which OAuth providers are configured and
     * enable/disable the corresponding buttons.
     */
    async probeOAuthProviders() {
        const googleBtn    = document.getElementById('google-signin-btn');
        const microsoftBtn = document.getElementById('microsoft-signin-btn');
        if (!googleBtn && !microsoftBtn) return;

        try {
            // Always call the real backend (not mock) for provider info
            const baseUrl = ConfigManager.resolveBackendBaseUrl();

            const controller = new AbortController();
            const timeoutId  = setTimeout(() => controller.abort(), 4000);

            const resp = await fetch(`${baseUrl}/api/v1/auth/oauth/providers`, {
                signal: controller.signal
            });
            clearTimeout(timeoutId);

            if (!resp.ok) throw new Error('Backend unreachable');
            const json = await resp.json();

            if (json.success && Array.isArray(json.data)) {
                for (const p of json.data) {
                    if (p.name === 'google'    && googleBtn)    googleBtn.disabled    = !p.enabled;
                    if (p.name === 'microsoft' && microsoftBtn) microsoftBtn.disabled = !p.enabled;
                }

                // Show hint if none are configured
                const anyEnabled = json.data.some(p => p.enabled);
                if (!anyEnabled) {
                    const hint = document.createElement('p');
                    hint.className = 'oauth-hint';
                    hint.textContent = 'OAuth providers not configured on the server.';
                    document.getElementById('oauth-buttons')?.appendChild(hint);
                }
            }
        } catch (e) {
            // Backend not reachable — disable OAuth buttons silently
            if (googleBtn)    googleBtn.disabled = true;
            if (microsoftBtn) microsoftBtn.disabled = true;
        }
    }

    // ── OAuth login initiation ────────────────────────────────────────────

    /**
     * Called when the user clicks a "Sign in with ..." button.
     * Navigates the browser to the backend, which redirects to the provider.
     */
    handleOAuthLogin(provider) {
        switch (provider) {
            case 'google':
                authAPI.startGoogleOAuth();
                break;
            case 'microsoft':
                authAPI.startMicrosoftOAuth();
                break;
            default:
                Utils.showError('Unknown OAuth provider');
        }
    }

    /** Mock-mode OAuth simulation so the demo still works. */
    simulateMockOAuth(provider) {
        this.showOAuthLoading(`Signing in with ${provider}…`);
        setTimeout(() => {
            authAPI.setToken('demo-token-123');
            if (window.app) {
                window.app.currentUser = MOCK_DATA.user;
                window.app.isAuthenticated = true;
                window.app.showMainApp();
                window.app.loadDashboard();
            }
            this.hideOAuthLoading();
            Utils.showSuccess(`Signed in with ${provider} (mock mode)`);
        }, 1000);
    }

    // ── OAuth loading overlay ─────────────────────────────────────────────

    showOAuthLoading(message) {
        let overlay = document.getElementById('oauth-loading-overlay');
        if (!overlay) {
            overlay = document.createElement('div');
            overlay.id = 'oauth-loading-overlay';
            overlay.className = 'oauth-loading';
            overlay.innerHTML = `<div class="spinner"></div><p>${message}</p>`;
            document.body.appendChild(overlay);
        } else {
            overlay.querySelector('p').textContent = message;
            overlay.classList.remove('hidden');
        }
    }

    hideOAuthLoading() {
        const overlay = document.getElementById('oauth-loading-overlay');
        if (overlay) overlay.remove();
    }

    // ── Existing OTP methods ──────────────────────────────────────────────

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
        
        // Clear forms
        document.getElementById('loginForm').reset();
        document.getElementById('otpForm').reset();
        // Clear OTP digit boxes
        document.querySelectorAll('.otp-digit').forEach(d => { d.value = ''; d.classList.remove('filled'); });
        const hidden = document.getElementById('otp');
        if (hidden) hidden.value = '';
        
        // Focus on email input
        document.getElementById('email').focus();
    }

    showOtpForm(email) {
        this.currentEmail = email;
        document.getElementById('login-form').classList.add('hidden');
        document.getElementById('otp-form').classList.remove('hidden');
        
        // Update message
        document.getElementById('otp-message').textContent = `We sent a 6-digit code to ${email}`;
        
        // Clear and focus first OTP digit
        document.querySelectorAll('.otp-digit').forEach(d => { d.value = ''; d.classList.remove('filled'); });
        const hidden = document.getElementById('otp');
        if (hidden) hidden.value = '';
        const firstDigit = document.querySelector('.otp-digit[data-index="0"]');
        if (firstDigit) firstDigit.focus();
    }
}

// Initialize authentication manager
document.addEventListener('DOMContentLoaded', () => {
    window.authManager = new AuthManager();
});
