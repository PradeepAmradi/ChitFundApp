// Main App Controller
class App {
    constructor() {
        this.currentUser = null;
        this.currentPage = 'dashboard';
        this.isAuthenticated = false;
        this.init();
    }

    init() {
        this.setupEventListeners();
        this.checkAuthStatus();
    }

    setupEventListeners() {
        // Navigation
        document.addEventListener('click', (e) => {
            if (e.target.classList.contains('nav-btn')) {
                const page = e.target.dataset.page;
                if (page) {
                    this.navigateToPage(page);
                }
            }
        });

        // Logout
        document.getElementById('logout-btn')?.addEventListener('click', () => {
            this.logout();
        });

        // Window events
        window.addEventListener('popstate', (e) => {
            const page = e.state?.page || 'dashboard';
            this.navigateToPage(page, false);
        });
    }

    async checkAuthStatus() {
        const token = localStorage.getItem('authToken');
        if (token) {
            authAPI.setToken(token);
            if (API_CONFIG.useMockData) {
                // For mock data, skip the profile check and use stored user
                this.currentUser = MOCK_DATA.user;
                this.isAuthenticated = true;
                this.showMainApp();
                this.loadDashboard();
                return;
            }
            
            try {
                const response = await authAPI.getUserProfile();
                if (response.success) {
                    this.currentUser = response.data;
                    this.isAuthenticated = true;
                    this.showMainApp();
                    this.loadDashboard();
                } else {
                    this.logout();
                }
            } catch (error) {
                console.error('Auth check failed:', error);
                this.logout();
            }
        } else {
            this.showLogin();
        }
    }

    showLogin() {
        document.getElementById('loading').classList.add('hidden');
        document.getElementById('login-section').classList.remove('hidden');
        document.getElementById('main-section').classList.add('hidden');
        this.isAuthenticated = false;
    }

    showMainApp() {
        document.getElementById('loading').classList.add('hidden');
        document.getElementById('login-section').classList.add('hidden');
        document.getElementById('main-section').classList.remove('hidden');
        this.isAuthenticated = true;
    }

    navigateToPage(page, updateHistory = true) {
        if (!this.isAuthenticated) {
            this.showLogin();
            return;
        }

        // Update active nav button
        document.querySelectorAll('.nav-btn').forEach(btn => {
            btn.classList.remove('active');
        });
        document.querySelector(`[data-page="${page}"]`)?.classList.add('active');

        // Update current page
        this.currentPage = page;

        // Update URL
        if (updateHistory) {
            history.pushState({ page }, '', `#${page}`);
        }

        // Load page content
        this.loadPageContent(page);
    }

    async loadPageContent(page) {
        const contentArea = document.getElementById('content-area');
        
        // Show loading
        contentArea.innerHTML = `
            <div class="loading-content">
                <div class="spinner"></div>
                <p>Loading...</p>
            </div>
        `;

        try {
            switch (page) {
                case 'dashboard':
                    await this.loadDashboard();
                    break;
                case 'chits':
                    await this.loadChitList();
                    break;
                case 'create-chit':
                    await this.loadCreateChit();
                    break;
                default:
                    contentArea.innerHTML = '<h2>Page not found</h2>';
            }
        } catch (error) {
            console.error('Error loading page:', error);
            contentArea.innerHTML = `
                <div class="error-message">
                    <p>Error loading page: ${error.message}</p>
                    <button class="btn btn-primary" onclick="location.reload()">
                        <i class="fas fa-refresh"></i> Reload
                    </button>
                </div>
            `;
        }
    }

    async loadDashboard() {
        const contentArea = document.getElementById('content-area');
        
        try {
            // Load user's chits for dashboard stats
            const chitsResponse = await chitAPI.getChits();
            const chits = chitsResponse.success ? chitsResponse.data : [];
            
            const activeChits = chits.filter(chit => chit.status === 'ACTIVE').length;
            const totalChits = chits.length;
            const openChits = chits.filter(chit => chit.status === 'OPEN').length;

            contentArea.innerHTML = `
                <div class="dashboard">
                    <h2>Dashboard</h2>
                    <div class="dashboard-stats">
                        <div class="stat-card">
                            <i class="fas fa-list"></i>
                            <h3>${totalChits}</h3>
                            <p>Total Chits</p>
                        </div>
                        <div class="stat-card">
                            <i class="fas fa-play-circle"></i>
                            <h3>${activeChits}</h3>
                            <p>Active Chits</p>
                        </div>
                        <div class="stat-card">
                            <i class="fas fa-clock"></i>
                            <h3>${openChits}</h3>
                            <p>Open Chits</p>
                        </div>
                    </div>
                    
                    <div class="card">
                        <h3>Welcome to ChitFund Management</h3>
                        <p>Manage your chit funds, track payments, and invite members.</p>
                        
                        <div style="margin-top: 20px;">
                            <button class="btn btn-primary" onclick="app.navigateToPage('chits')">
                                <i class="fas fa-list"></i> View My Chits
                            </button>
                            <button class="btn btn-success" onclick="app.navigateToPage('create-chit')">
                                <i class="fas fa-plus"></i> Create New Chit
                            </button>
                        </div>
                    </div>
                    
                    ${totalChits > 0 ? `
                        <div class="card">
                            <h3>Recent Chits</h3>
                            <div class="chit-grid">
                                ${chits.slice(0, 3).map(chit => this.createChitCard(chit)).join('')}
                            </div>
                            ${totalChits > 3 ? `
                                <div style="text-align: center; margin-top: 20px;">
                                    <button class="btn btn-secondary" onclick="app.navigateToPage('chits')">
                                        View All Chits
                                    </button>
                                </div>
                            ` : ''}
                        </div>
                    ` : ''}
                </div>
            `;
        } catch (error) {
            console.error('Error loading dashboard:', error);
            contentArea.innerHTML = `
                <div class="error-message">
                    <p>Error loading dashboard: ${error.message}</p>
                </div>
            `;
        }
    }

    async loadChitList() {
        const contentArea = document.getElementById('content-area');
        
        try {
            const response = await chitAPI.getChits();
            const chits = response.success ? response.data : [];
            
            contentArea.innerHTML = `
                <div class="chit-list">
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 30px;">
                        <h2>My Chits</h2>
                        <button class="btn btn-success" onclick="app.navigateToPage('create-chit')">
                            <i class="fas fa-plus"></i> Create New Chit
                        </button>
                    </div>
                    
                    ${chits.length === 0 ? `
                        <div class="empty-state">
                            <i class="fas fa-coins"></i>
                            <h3>No Chits Found</h3>
                            <p>You haven't created any chits yet.</p>
                            <button class="btn btn-primary" onclick="app.navigateToPage('create-chit')">
                                <i class="fas fa-plus"></i> Create Your First Chit
                            </button>
                        </div>
                    ` : `
                        <div class="chit-grid">
                            ${chits.map(chit => this.createChitCard(chit)).join('')}
                        </div>
                    `}
                </div>
            `;
        } catch (error) {
            console.error('Error loading chits:', error);
            contentArea.innerHTML = `
                <div class="error-message">
                    <p>Error loading chits: ${error.message}</p>
                </div>
            `;
        }
    }

    async loadCreateChit() {
        const contentArea = document.getElementById('content-area');
        
        contentArea.innerHTML = `
            <div class="create-chit">
                <h2>Create New Chit</h2>
                <div class="card">
                    <form id="createChitForm">
                        <div class="form-group">
                            <label for="chitName">Chit Name</label>
                            <input type="text" id="chitName" name="chitName" required>
                        </div>
                        
                        <div class="form-group">
                            <label for="fundAmount">Fund Amount (₹)</label>
                            <input type="number" id="fundAmount" name="fundAmount" 
                                   min="100000" max="5000000" step="1000" required>
                            <small>Amount must be between ₹1L and ₹50L</small>
                        </div>
                        
                        <div class="form-group">
                            <label for="tenure">Tenure (months)</label>
                            <select id="tenure" name="tenure" required>
                                <option value="">Select tenure</option>
                                ${Array.from({length: 13}, (_, i) => i + 12).map(i => 
                                    `<option value="${i}">${i} months</option>`
                                ).join('')}
                            </select>
                        </div>
                        
                        <div class="form-group">
                            <label for="memberCount">Member Count</label>
                            <select id="memberCount" name="memberCount" required>
                                <option value="">Select member count</option>
                                ${[10, 15, 20, 25].map(count => 
                                    `<option value="${count}">${count} members</option>`
                                ).join('')}
                            </select>
                        </div>
                        
                        <div class="form-group">
                            <label for="startMonth">Start Month</label>
                            <input type="month" id="startMonth" name="startMonth" 
                                   min="${Utils.getCurrentMonth()}" required>
                        </div>
                        
                        <div class="form-group">
                            <label for="payoutMethod">Payout Method</label>
                            <select id="payoutMethod" name="payoutMethod" required>
                                <option value="RANDOM">Random</option>
                                <option value="VOTING">Voting</option>
                            </select>
                        </div>
                        
                        <div style="display: flex; gap: 10px;">
                            <button type="submit" class="btn btn-success">
                                <span class="btn-text">
                                    <i class="fas fa-plus"></i> Create Chit
                                </span>
                                <span class="btn-loading hidden">
                                    <i class="fas fa-spinner fa-spin"></i> Creating...
                                </span>
                            </button>
                            <button type="button" class="btn btn-secondary" onclick="app.navigateToPage('dashboard')">
                                <i class="fas fa-arrow-left"></i> Back to Dashboard
                            </button>
                        </div>
                    </form>
                    
                    <div id="create-chit-error" class="error-message hidden"></div>
                    <div id="create-chit-success" class="success-message hidden"></div>
                </div>
            </div>
        `;
        
        // Setup form submission
        document.getElementById('createChitForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            await this.handleCreateChit(e);
        });
    }

    async handleCreateChit(e) {
        const form = e.target;
        const formData = new FormData(form);
        const data = Object.fromEntries(formData.entries());
        
        // Validate form
        if (!this.validateChitForm(data)) {
            return;
        }
        
        Utils.setLoading('createChitForm', true);
        Utils.hideMessages();
        
        try {
            const chitData = {
                name: data.chitName,
                fundAmount: parseInt(data.fundAmount) * 100, // Convert to paisa
                tenure: parseInt(data.tenure),
                memberCount: parseInt(data.memberCount),
                startMonth: data.startMonth,
                payoutMethod: data.payoutMethod
            };
            
            const response = await chitAPI.createChit(chitData);
            
            if (response.success) {
                Utils.showSuccess('Chit created successfully!', 'create-chit-success');
                form.reset();
                setTimeout(() => {
                    this.navigateToPage('chits');
                }, 2000);
            } else {
                Utils.showError(response.message || 'Failed to create chit', 'create-chit-error');
            }
        } catch (error) {
            console.error('Error creating chit:', error);
            Utils.showError(`Error: ${error.message}`, 'create-chit-error');
        } finally {
            Utils.setLoading('createChitForm', false);
        }
    }

    validateChitForm(data) {
        if (!data.chitName || data.chitName.trim() === '') {
            Utils.showError('Chit name is required', 'create-chit-error');
            return false;
        }
        
        if (!Utils.validateChitAmount(data.fundAmount)) {
            Utils.showError('Fund amount must be between ₹1L and ₹50L', 'create-chit-error');
            return false;
        }
        
        if (!Utils.validateTenure(data.tenure)) {
            Utils.showError('Tenure must be between 12 and 24 months', 'create-chit-error');
            return false;
        }
        
        if (!Utils.validateMemberCount(data.memberCount)) {
            Utils.showError('Member count must be between 10 and 25 (multiples of 5)', 'create-chit-error');
            return false;
        }
        
        if (!data.startMonth) {
            Utils.showError('Start month is required', 'create-chit-error');
            return false;
        }
        
        return true;
    }

    createChitCard(chit) {
        return `
            <div class="chit-card">
                <h4>${chit.name}</h4>
                <div class="chit-info">
                    <label>Amount:</label>
                    <span>${Utils.formatIndianCurrency(chit.fundAmount)}</span>
                </div>
                <div class="chit-info">
                    <label>Tenure:</label>
                    <span>${chit.tenure} months</span>
                </div>
                <div class="chit-info">
                    <label>Members:</label>
                    <span>${chit.members ? chit.members.length : 0}/${chit.memberCount}</span>
                </div>
                <div class="chit-info">
                    <label>Status:</label>
                    <span class="status-badge status-${chit.status.toLowerCase()}">${chit.status}</span>
                </div>
                <div class="chit-info">
                    <label>Start:</label>
                    <span>${Utils.formatMonth(chit.startMonth)}</span>
                </div>
                <div class="chit-info">
                    <label>End:</label>
                    <span>${Utils.formatMonth(chit.endMonth)}</span>
                </div>
                <div style="margin-top: 20px;">
                    <button class="btn btn-primary" onclick="app.viewChitDetails('${chit.id}')">
                        <i class="fas fa-eye"></i> View Details
                    </button>
                </div>
            </div>
        `;
    }

    async viewChitDetails(chitId) {
        // For now, just show an alert. In a full implementation, 
        // this would navigate to a detailed view
        alert(`Viewing details for chit ID: ${chitId}`);
    }

    logout() {
        authAPI.clearToken();
        this.currentUser = null;
        this.isAuthenticated = false;
        this.showLogin();
        history.pushState(null, '', '/');
    }
}

// Initialize the app when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.app = new App();
    
    // Hide loading after a minimum time
    setTimeout(() => {
        document.getElementById('loading').classList.add('hidden');
    }, 1000);
});