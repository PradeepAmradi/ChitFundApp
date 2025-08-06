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
        this.setupConfigListeners();
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

    setupConfigListeners() {
        // Mock data toggle
        const mockToggle = document.getElementById('mockDataToggle');
        if (mockToggle) {
            // Set initial state
            mockToggle.checked = API_CONFIG.useMockData;
            
            mockToggle.addEventListener('change', async (e) => {
                // Add visual feedback
                this.addToggleFeedback(e.target);
                await this.handleMockDataToggle(e.target.checked);
            });
        }

        // Initialize toggle labels
        this.updateToggleLabels(API_CONFIG.useMockData);

        // Listen for config changes
        window.addEventListener('configChanged', (e) => {
            this.updateDataModeIndicators(e.detail.useMockData);
            // Update header toggle if needed
            const headerToggle = document.getElementById('mockDataToggle');
            if (headerToggle) {
                headerToggle.checked = e.detail.useMockData;
            }
            // Update toggle labels
            this.updateToggleLabels(e.detail.useMockData);
        });

        // Periodic connection check when in live mode
        setInterval(() => {
            if (!API_CONFIG.useMockData && this.isAuthenticated) {
                this.checkConnectionStatus();
            }
        }, 30000); // Check every 30 seconds
    }

    addToggleFeedback(toggleInput) {
        const slider = toggleInput.nextElementSibling;
        if (slider) {
            slider.style.transform = 'scale(0.95)';
            setTimeout(() => {
                slider.style.transform = 'scale(1)';
            }, 100);
        }
    }

    async checkConnectionStatus() {
        try {
            await this.testBackendConnection();
            // Connection is good
        } catch (error) {
            // Connection failed, show warning
            console.warn('Backend connection lost:', error.message);
            Utils.showError('Lost connection to backend. Consider switching to mock data mode.', 'connection-warning');
        }
    }

    async checkAuthStatus() {
        const token = localStorage.getItem('authToken');
        if (token) {
            // Ensure all API instances have the token
            authAPI.setToken(token);
            chitAPI.setToken(token);
            
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
                case 'settings':
                    await this.loadSettings();
                    break;
                case 'chit-details':
                    if (this.currentChitId) {
                        const chitDetails = await chitManager.getChitDetails(this.currentChitId);
                        await this.loadChitDetailsPage(chitDetails);
                    } else {
                        await this.loadDashboard();
                    }
                    break;
                case 'invite-members':
                    if (this.currentChitId) {
                        await this.loadInviteMembersPage(this.currentChitId);
                    } else {
                        await this.loadDashboard();
                    }
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
                    <div class="data-mode-indicator ${API_CONFIG.useMockData ? 'mock' : 'live'}">
                        <i class="fas ${API_CONFIG.useMockData ? 'fa-flask' : 'fa-server'}"></i>
                        Using ${API_CONFIG.useMockData ? 'Mock' : 'Live'} Data
                    </div>
                    
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
                <div class="dashboard">
                    <div class="data-mode-indicator ${API_CONFIG.useMockData ? 'mock' : 'live'}">
                        <i class="fas ${API_CONFIG.useMockData ? 'fa-flask' : 'fa-server'}"></i>
                        Using ${API_CONFIG.useMockData ? 'Mock' : 'Live'} Data
                    </div>
                    
                    <h2>Dashboard</h2>
                    
                    ${!API_CONFIG.useMockData ? `
                        <div class="empty-state">
                            <i class="fas fa-server"></i>
                            <h3>No Live Data Available</h3>
                            <p>Unable to connect to the backend server or no data found.</p>
                            <div style="margin-top: 20px;">
                                <button class="btn btn-warning" onclick="app.switchToMockData()">
                                    <i class="fas fa-flask"></i> Switch to Mock Data
                                </button>
                                <button class="btn btn-secondary" onclick="app.loadPageContent('dashboard')">
                                    <i class="fas fa-refresh"></i> Retry
                                </button>
                            </div>
                        </div>
                    ` : `
                        <div class="error-message">
                            <p>Error loading dashboard: ${error.message}</p>
                            <button class="btn btn-primary" onclick="app.loadPageContent('dashboard')">
                                <i class="fas fa-refresh"></i> Retry
                            </button>
                        </div>
                    `}
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
                    <div class="data-mode-indicator ${API_CONFIG.useMockData ? 'mock' : 'live'}">
                        <i class="fas ${API_CONFIG.useMockData ? 'fa-flask' : 'fa-server'}"></i>
                        Using ${API_CONFIG.useMockData ? 'Mock' : 'Live'} Data
                    </div>
                    
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
                <div class="chit-list">
                    <div class="data-mode-indicator ${API_CONFIG.useMockData ? 'mock' : 'live'}">
                        <i class="fas ${API_CONFIG.useMockData ? 'fa-flask' : 'fa-server'}"></i>
                        Using ${API_CONFIG.useMockData ? 'Mock' : 'Live'} Data
                    </div>
                    
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 30px;">
                        <h2>My Chits</h2>
                        <button class="btn btn-success" onclick="app.navigateToPage('create-chit')">
                            <i class="fas fa-plus"></i> Create New Chit
                        </button>
                    </div>
                    
                    ${!API_CONFIG.useMockData ? `
                        <div class="empty-state">
                            <i class="fas fa-server"></i>
                            <h3>No Live Data Available</h3>
                            <p>Unable to connect to the backend server or no chits found.</p>
                            <div style="margin-top: 20px;">
                                <button class="btn btn-warning" onclick="app.switchToMockData()">
                                    <i class="fas fa-flask"></i> Switch to Mock Data
                                </button>
                                <button class="btn btn-secondary" onclick="app.loadPageContent('chits')">
                                    <i class="fas fa-refresh"></i> Retry
                                </button>
                                <button class="btn btn-primary" onclick="app.navigateToPage('create-chit')">
                                    <i class="fas fa-plus"></i> Create Your First Chit
                                </button>
                            </div>
                        </div>
                    ` : `
                        <div class="error-message">
                            <p>Error loading chits: ${error.message}</p>
                            <button class="btn btn-primary" onclick="app.loadPageContent('chits')">
                                <i class="fas fa-refresh"></i> Retry
                            </button>
                        </div>
                    `}
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

    // Helper function to determine user's role in a chit
    getUserRoleInChit(chit) {
        const currentUserId = this.currentUser?.id || "user-123";
        
        if (chit.moderatorId === currentUserId) {
            return 'moderator';
        }
        
        // Check if user is a member
        const isMember = chit.members && chit.members.some(member => member.userId === currentUserId);
        if (isMember) {
            return 'member';
        }
        
        return 'none'; // User has no role in this chit
    }

    createChitCard(chit) {
        const userRole = this.getUserRoleInChit(chit);
        const roleClass = `chit-card-${userRole}`;
        const roleInfo = this.getRoleDisplayInfo(userRole);
        
        return `
            <div class="chit-card ${roleClass}">
                <div class="chit-header">
                    <h4>${chit.name}</h4>
                    <div class="role-badge ${roleInfo.badgeClass}">
                        <i class="${roleInfo.icon}"></i>
                        <span>${roleInfo.label}</span>
                    </div>
                </div>
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
                <div class="chit-actions">
                    <button class="btn btn-primary" onclick="app.viewChitDetails('${chit.id}')">
                        <i class="fas fa-eye"></i> View Details
                    </button>
                    ${userRole === 'moderator' && chit.status === 'OPEN' ? `
                        <button class="btn btn-success btn-sm" onclick="app.navigateToInviteMembers('${chit.id}')">
                            <i class="fas fa-user-plus"></i> Invite
                        </button>
                    ` : ''}
                </div>
            </div>
        `;
    }

    getRoleDisplayInfo(role) {
        switch (role) {
            case 'moderator':
                return {
                    label: 'Moderator',
                    icon: 'fas fa-crown',
                    badgeClass: 'role-moderator'
                };
            case 'member':
                return {
                    label: 'Member',
                    icon: 'fas fa-user',
                    badgeClass: 'role-member'
                };
            default:
                return {
                    label: 'Observer',
                    icon: 'fas fa-eye',
                    badgeClass: 'role-observer'
                };
        }
    }

    async viewChitDetails(chitId) {
        try {
            // Load chit details
            const chitDetails = await chitManager.getChitDetails(chitId);
            this.currentPage = 'chit-details';
            this.currentChitId = chitId;
            
            // Load chit details page
            await this.loadChitDetailsPage(chitDetails);
            
            // Update browser history
            history.pushState({ page: 'chit-details', chitId }, '', `/chit/${chitId}`);
        } catch (error) {
            console.error('Error loading chit details:', error);
            Utils.showError(`Failed to load chit details: ${error.message}`);
        }
    }

    async handleMockDataToggle(useMockData) {
        // Show loading overlay
        this.showLoadingOverlay('Switching data mode...');
        
        try {
            ConfigManager.setMockData(useMockData);
            
            // Update toggle labels
            this.updateToggleLabels(useMockData);
            
            // If switching to live data, test connection
            if (!useMockData) {
                await this.testBackendConnection();
            }
            
            // Reload current page content
            await this.loadPageContent(this.currentPage);
            
            Utils.showSuccess(`Switched to ${useMockData ? 'mock' : 'live'} data mode`);
        } catch (error) {
            console.error('Error switching data mode:', error);
            Utils.showError(`Failed to switch to live data: ${error.message}`);
            
            // Revert toggle state
            const mockToggle = document.getElementById('mockDataToggle');
            const settingsToggle = document.getElementById('settingsMockToggle');
            if (mockToggle) {
                mockToggle.checked = true;
            }
            if (settingsToggle) {
                settingsToggle.checked = true;
            }
            ConfigManager.setMockData(true);
            this.updateToggleLabels(true);
        } finally {
            this.hideLoadingOverlay();
        }
    }

    updateToggleLabels(useMockData) {
        // Update header toggle label
        const headerLabel = document.querySelector('#mockDataToggle + .toggle-slider + .toggle-label');
        if (headerLabel) {
            headerLabel.textContent = useMockData ? 'Mock Data' : 'Live Data';
        }
        
        // Update settings toggle label
        const settingsLabel = document.querySelector('#settingsMockToggle + .toggle-slider + .toggle-label');
        if (settingsLabel) {
            settingsLabel.textContent = useMockData ? 'Mock Data' : 'Live Data';
        }
    }

    async testBackendConnection() {
        // If using mock data, always return success (no real connection needed)
        if (API_CONFIG.useMockData) {
            return Promise.resolve();
        }
        
        try {
            // Try a simple endpoint to test connection
            const response = await fetch(`${ConfigManager.getConfig().baseURL.replace('/api/v1', '')}/health`, {
                method: 'GET',
                timeout: 5000
            });
            
            if (!response.ok) {
                throw new Error(`Server responded with status: ${response.status}`);
            }
        } catch (error) {
            if (error.name === 'TypeError') {
                throw new Error('Cannot connect to backend server. Please ensure it is running.');
            }
            throw error;
        }
    }

    showLoadingOverlay(message = 'Loading...') {
        const overlay = document.createElement('div');
        overlay.id = 'loading-overlay';
        overlay.className = 'loading-overlay';
        overlay.innerHTML = `
            <div style="text-align: center; color: white;">
                <div class="spinner"></div>
                <p style="margin-top: 20px;">${message}</p>
            </div>
        `;
        document.body.appendChild(overlay);
    }

    hideLoadingOverlay() {
        const overlay = document.getElementById('loading-overlay');
        if (overlay) {
            overlay.remove();
        }
    }

    updateDataModeIndicators(useMockData) {
        const indicators = document.querySelectorAll('.data-mode-indicator');
        indicators.forEach(indicator => {
            indicator.className = `data-mode-indicator ${useMockData ? 'mock' : 'live'}`;
            indicator.innerHTML = `
                <i class="fas ${useMockData ? 'fa-flask' : 'fa-server'}"></i>
                Using ${useMockData ? 'Mock' : 'Live'} Data
            `;
        });
    }

    async loadSettings() {
        const contentArea = document.getElementById('content-area');
        const config = ConfigManager.getConfig();
        
        contentArea.innerHTML = `
            <div class="settings">
                <div class="data-mode-indicator ${API_CONFIG.useMockData ? 'mock' : 'live'}">
                    <i class="fas ${API_CONFIG.useMockData ? 'fa-flask' : 'fa-server'}"></i>
                    Using ${API_CONFIG.useMockData ? 'Mock' : 'Live'} Data
                </div>
                
                <h2>Settings</h2>
                
                <div class="settings-section">
                    <h3>Data Source</h3>
                    <div class="settings-group">
                        <div class="settings-row">
                            <div class="settings-label">
                                Data Mode
                                <div class="settings-description">
                                    Choose between mock data for testing or live backend data
                                </div>
                            </div>
                            <div class="settings-control">
                                <label class="mock-toggle">
                                    <input type="checkbox" id="settingsMockToggle" ${API_CONFIG.useMockData ? 'checked' : ''}>
                                    <span class="toggle-slider"></span>
                                    <span class="toggle-label">${API_CONFIG.useMockData ? 'Mock Data' : 'Live Data'}</span>
                                </label>
                            </div>
                        </div>
                        
                        <div class="settings-row">
                            <div class="settings-label">
                                Connection Status
                                <div class="settings-description">
                                    Current status of the backend connection
                                </div>
                            </div>
                            <div class="settings-control">
                                <div id="connectionStatus" class="connection-status testing">
                                    <i class="fas fa-circle-notch fa-spin"></i>
                                    Testing...
                                </div>
                                <button class="btn btn-secondary" id="testConnectionBtn" style="margin-left: 10px;">
                                    <i class="fas fa-plug"></i> Test Connection
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
                
                <div class="settings-section">
                    <h3>API Configuration</h3>
                    <div class="settings-group">
                        <div class="settings-row">
                            <div class="settings-label">
                                Backend URL
                                <div class="settings-description">
                                    The base URL for the ChitFund backend API
                                </div>
                            </div>
                            <div class="settings-control">
                                <input type="url" id="backendUrl" class="form-control" 
                                       value="${config.baseURL}" placeholder="http://localhost:8080/api/v1">
                            </div>
                        </div>
                        
                        <div class="settings-row">
                            <div class="settings-label">
                                Request Timeout
                                <div class="settings-description">
                                    Timeout for API requests in milliseconds
                                </div>
                            </div>
                            <div class="settings-control">
                                <input type="number" id="apiTimeout" class="form-control" 
                                       value="${config.timeout}" min="1000" max="30000" step="1000">
                            </div>
                        </div>
                    </div>
                </div>
                
                <div class="settings-section">
                    <div style="display: flex; gap: 10px;">
                        <button class="btn btn-success" id="saveSettings">
                            <i class="fas fa-save"></i> Save Settings
                        </button>
                        <button class="btn btn-secondary" id="resetSettings">
                            <i class="fas fa-undo"></i> Reset to Defaults
                        </button>
                        <button class="btn btn-secondary" onclick="app.navigateToPage('dashboard')">
                            <i class="fas fa-arrow-left"></i> Back to Dashboard
                        </button>
                    </div>
                </div>
                
                <div id="settings-error" class="error-message hidden"></div>
                <div id="settings-success" class="success-message hidden"></div>
            </div>
        `;
        
        // Setup settings event listeners
        this.setupSettingsEventListeners();
        
        // Test initial connection
        this.testConnectionStatus();
    }

    setupSettingsEventListeners() {
        // Settings mock toggle
        const settingsToggle = document.getElementById('settingsMockToggle');
        if (settingsToggle) {
            settingsToggle.addEventListener('change', async (e) => {
                // Add visual feedback
                this.addToggleFeedback(e.target);
                await this.handleMockDataToggle(e.target.checked);
                // Label will be updated by handleMockDataToggle
            });
        }

        // Test connection button
        document.getElementById('testConnectionBtn')?.addEventListener('click', () => {
            this.testConnectionStatus();
        });

        // Save settings
        document.getElementById('saveSettings')?.addEventListener('click', () => {
            this.saveSettings();
        });

        // Reset settings
        document.getElementById('resetSettings')?.addEventListener('click', () => {
            this.resetSettings();
        });
    }

    async testConnectionStatus() {
        const statusEl = document.getElementById('connectionStatus');
        if (!statusEl) return;
        
        statusEl.className = 'connection-status testing';
        statusEl.innerHTML = '<i class="fas fa-circle-notch fa-spin"></i> Testing...';
        
        try {
            await this.testBackendConnection();
            statusEl.className = 'connection-status connected';
            statusEl.innerHTML = '<i class="fas fa-check-circle"></i> Connected';
        } catch (error) {
            statusEl.className = 'connection-status disconnected';
            statusEl.innerHTML = '<i class="fas fa-times-circle"></i> Disconnected';
        }
    }

    saveSettings() {
        const backendUrl = document.getElementById('backendUrl')?.value;
        const apiTimeout = document.getElementById('apiTimeout')?.value;
        
        if (backendUrl) {
            ConfigManager.setBaseURL(backendUrl);
        }
        
        if (apiTimeout) {
            ConfigManager.setTimeout(parseInt(apiTimeout));
        }
        
        Utils.showSuccess('Settings saved successfully!', 'settings-success');
        
        // Test connection with new settings
        setTimeout(() => {
            this.testConnectionStatus();
        }, 1000);
    }

    resetSettings() {
        document.getElementById('backendUrl').value = 'http://localhost:8080/api/v1';
        document.getElementById('apiTimeout').value = '10000';
        
        ConfigManager.setBaseURL('http://localhost:8080/api/v1');
        ConfigManager.setTimeout(10000);
        
        Utils.showSuccess('Settings reset to defaults!', 'settings-success');
        
        // Test connection with default settings
        setTimeout(() => {
            this.testConnectionStatus();
        }, 1000);
    }

    async switchToMockData() {
        ConfigManager.setMockData(true);
        const mockToggle = document.getElementById('mockDataToggle');
        if (mockToggle) {
            mockToggle.checked = true;
        }
        
        // Reload current page
        await this.loadPageContent(this.currentPage);
        Utils.showSuccess('Switched to mock data mode');
    }

    async loadChitDetailsPage(chit) {
        const contentArea = document.getElementById('content-area');
        const currentUserId = this.currentUser?.id || "user-123";
        const userRole = this.getUserRoleInChit(chit);
        const roleInfo = this.getRoleDisplayInfo(userRole);
        const isModeratorAndOpen = userRole === 'moderator' && chit.status === 'OPEN';
        
        contentArea.innerHTML = `
            <div class="chit-details">
                <div class="data-mode-indicator ${API_CONFIG.useMockData ? 'mock' : 'live'}">
                    <i class="fas ${API_CONFIG.useMockData ? 'fa-flask' : 'fa-server'}"></i>
                    Using ${API_CONFIG.useMockData ? 'Mock' : 'Live'} Data
                </div>
                
                <div class="page-header">
                    <div>
                        <h2><i class="fas fa-coins"></i> ${chit.name}</h2>
                        <p class="breadcrumb">
                            <button class="btn-link" onclick="app.navigateToPage('chits')">My Chits</button>
                            <i class="fas fa-chevron-right"></i>
                            <span>Chit Details</span>
                        </p>
                    </div>
                    <div class="page-actions">
                        <div class="role-badge ${roleInfo.badgeClass}" style="margin-right: 15px;">
                            <i class="${roleInfo.icon}"></i>
                            <span>Your Role: ${roleInfo.label}</span>
                        </div>
                        <button class="btn btn-secondary" onclick="app.navigateToPage('chits')">
                            <i class="fas fa-arrow-left"></i> Back to Chits
                        </button>
                    </div>
                </div>

                <div class="chit-details-content">
                    <div class="chit-overview-card chit-card-${userRole}">
                        <div class="chit-header">
                            <div class="chit-title">
                                <h3>${chit.name}</h3>
                                <span class="status-badge status-${chit.status.toLowerCase()}">${chit.status}</span>
                            </div>
                            ${isModeratorAndOpen ? `
                                <button class="btn btn-success" onclick="app.navigateToInviteMembers('${chit.id}')">
                                    <i class="fas fa-user-plus"></i> Invite Members
                                </button>
                            ` : userRole === 'moderator' ? `
                                <div class="moderator-actions">
                                    <span class="moderator-label">
                                        <i class="fas fa-crown"></i> Moderator Controls
                                    </span>
                                </div>
                            ` : `
                                <div class="member-info">
                                    <span class="member-label">
                                        <i class="fas fa-user"></i> Member View
                                    </span>
                                </div>
                            `}
                        </div>
                        
                        ${chitManager.generateChitSummary(chit)}
                        
                        <div class="chit-progress">
                            ${chitManager.generateChitProgressBar(
                                chit.members ? chit.members.length : 0, 
                                chit.memberCount
                            )}
                        </div>
                    </div>

                    <div class="chit-sections">
                        <div class="chit-section">
                            ${chitManager.generateMembersList(chit.members)}
                        </div>
                        
                        <div class="chit-section">
                            ${chitManager.generatePaymentHistory(chit.payments || [])}
                        </div>
                    </div>
                </div>
            </div>
        `;
    }

    async loadInviteMembersPage(chitId) {
        const contentArea = document.getElementById('content-area');
        
        try {
            // Get current chit details
            const chit = await chitManager.getChitDetails(chitId);
            
            // Get contacts list (mock data for now)
            const contacts = await this.getContactsList();
            
            contentArea.innerHTML = `
                <div class="invite-members">
                    <div class="data-mode-indicator ${API_CONFIG.useMockData ? 'mock' : 'live'}">
                        <i class="fas ${API_CONFIG.useMockData ? 'fa-flask' : 'fa-server'}"></i>
                        Using ${API_CONFIG.useMockData ? 'Mock' : 'Live'} Data
                    </div>
                    
                    <div class="page-header">
                        <div>
                            <h2><i class="fas fa-user-plus"></i> Invite Members</h2>
                            <p class="breadcrumb">
                                <button class="btn-link" onclick="app.navigateToPage('chits')">My Chits</button>
                                <i class="fas fa-chevron-right"></i>
                                <button class="btn-link" onclick="app.viewChitDetails('${chitId}')">${chit.name}</button>
                                <i class="fas fa-chevron-right"></i>
                                <span>Invite Members</span>
                            </p>
                        </div>
                        <button class="btn btn-secondary" onclick="app.viewChitDetails('${chitId}')">
                            <i class="fas fa-arrow-left"></i> Back to Details
                        </button>
                    </div>

                    <div class="invite-content">
                        <div class="chit-info-card">
                            <h3>${chit.name}</h3>
                            <p>Select contacts to invite to this chit fund.</p>
                            <div class="chit-quick-info">
                                <span><strong>Fund Amount:</strong> ${Utils.formatIndianCurrency(chit.fundAmount)}</span>
                                <span><strong>Current Members:</strong> ${chit.members ? chit.members.length : 0}/${chit.memberCount}</span>
                                <span><strong>Available Slots:</strong> ${chit.memberCount - (chit.members ? chit.members.length : 0)}</span>
                            </div>
                        </div>

                        <div class="contacts-section">
                            <div class="section-header">
                                <h4>Select Contacts to Invite</h4>
                                <div class="contacts-actions">
                                    <button class="btn btn-secondary" onclick="app.selectAllContacts()">
                                        <i class="fas fa-check-square"></i> Select All
                                    </button>
                                    <button class="btn btn-secondary" onclick="app.clearAllContacts()">
                                        <i class="fas fa-square"></i> Clear All
                                    </button>
                                </div>
                            </div>
                            
                            <div class="contacts-grid" id="contacts-grid">
                                ${this.generateContactsGrid(contacts, chit.members)}
                            </div>
                            
                            <div class="invite-actions">
                                <div class="selected-count">
                                    <span id="selected-count">0</span> contacts selected
                                </div>
                                <button class="btn btn-primary" id="send-invites-btn" onclick="app.sendInvitations('${chitId}')" disabled>
                                    <i class="fas fa-envelope"></i> Send Invitations
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            `;
            
            // Initialize contact selection handlers
            this.initializeContactSelection();
        } catch (error) {
            console.error('Error loading invite members page:', error);
            contentArea.innerHTML = `
                <div class="error-message">
                    <p>Error loading invite page: ${error.message}</p>
                    <button class="btn btn-primary" onclick="app.viewChitDetails('${chitId}')">
                        <i class="fas fa-arrow-left"></i> Back to Details
                    </button>
                </div>
            `;
        }
    }

    navigateToInviteMembers(chitId) {
        this.currentPage = 'invite-members';
        this.currentChitId = chitId;
        this.loadPageContent('invite-members');
        history.pushState({ page: 'invite-members', chitId }, '', `/chit/${chitId}/invite`);
    }

    async getContactsList() {
        // For now, return mock contacts. In a real app, this would fetch from the contacts API
        return [
            { id: 'contact-1', name: 'Alice Johnson', email: 'alice@example.com', phone: '+91 9876543210' },
            { id: 'contact-2', name: 'Bob Smith', email: 'bob@example.com', phone: '+91 9876543211' },
            { id: 'contact-3', name: 'Carol Davis', email: 'carol@example.com', phone: '+91 9876543212' },
            { id: 'contact-4', name: 'David Wilson', email: 'david@example.com', phone: '+91 9876543213' },
            { id: 'contact-5', name: 'Eve Brown', email: 'eve@example.com', phone: '+91 9876543214' },
            { id: 'contact-6', name: 'Frank Miller', email: 'frank@example.com', phone: '+91 9876543215' },
            { id: 'contact-7', name: 'Grace Lee', email: 'grace@example.com', phone: '+91 9876543216' },
            { id: 'contact-8', name: 'Henry Garcia', email: 'henry@example.com', phone: '+91 9876543217' }
        ];
    }

    generateContactsGrid(contacts, existingMembers) {
        const existingEmails = new Set((existingMembers || []).map(member => member.email || ''));
        
        return contacts.map(contact => {
            const isAlreadyMember = existingEmails.has(contact.email);
            
            return `
                <div class="contact-item ${isAlreadyMember ? 'already-member' : ''}" data-contact-id="${contact.id}">
                    <div class="contact-checkbox">
                        <input type="checkbox" id="contact-${contact.id}" 
                               ${isAlreadyMember ? 'disabled' : ''} 
                               onchange="app.updateSelectedCount()">
                    </div>
                    <div class="contact-info">
                        <div class="contact-name">${contact.name}</div>
                        <div class="contact-details">
                            <span class="contact-email">${contact.email}</span>
                            <span class="contact-phone">${contact.phone}</span>
                        </div>
                        ${isAlreadyMember ? '<span class="member-badge">Already Member</span>' : ''}
                    </div>
                </div>
            `;
        }).join('');
    }

    initializeContactSelection() {
        this.updateSelectedCount();
    }

    selectAllContacts() {
        const checkboxes = document.querySelectorAll('#contacts-grid input[type="checkbox"]:not([disabled])');
        checkboxes.forEach(checkbox => {
            checkbox.checked = true;
        });
        this.updateSelectedCount();
    }

    clearAllContacts() {
        const checkboxes = document.querySelectorAll('#contacts-grid input[type="checkbox"]:not([disabled])');
        checkboxes.forEach(checkbox => {
            checkbox.checked = false;
        });
        this.updateSelectedCount();
    }

    updateSelectedCount() {
        const checkboxes = document.querySelectorAll('#contacts-grid input[type="checkbox"]:checked');
        const count = checkboxes.length;
        const countElement = document.getElementById('selected-count');
        const sendButton = document.getElementById('send-invites-btn');
        
        if (countElement) {
            countElement.textContent = count;
        }
        
        if (sendButton) {
            sendButton.disabled = count === 0;
        }
    }

    async sendInvitations(chitId) {
        const selectedContacts = [];
        const checkboxes = document.querySelectorAll('#contacts-grid input[type="checkbox"]:checked');
        
        checkboxes.forEach(checkbox => {
            const contactId = checkbox.id.replace('contact-', '');
            const contactItem = document.querySelector(`[data-contact-id="${contactId}"]`);
            const name = contactItem.querySelector('.contact-name').textContent;
            const email = contactItem.querySelector('.contact-email').textContent;
            selectedContacts.push({ id: contactId, name, email });
        });

        if (selectedContacts.length === 0) {
            Utils.showError('Please select at least one contact to invite');
            return;
        }

        try {
            // Show loading
            const sendButton = document.getElementById('send-invites-btn');
            const originalText = sendButton.innerHTML;
            sendButton.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Sending Invitations...';
            sendButton.disabled = true;

            // Send invitations
            let successCount = 0;
            const results = [];

            for (const contact of selectedContacts) {
                try {
                    const result = await chitManager.inviteMember(chitId, contact.email);
                    results.push({ contact, success: true, message: result.message });
                    successCount++;
                } catch (error) {
                    results.push({ contact, success: false, message: error.message });
                }
            }

            // Show results
            if (successCount === selectedContacts.length) {
                Utils.showSuccess(`Successfully sent ${successCount} invitation(s)!`);
                // Navigate back to chit details after a delay
                setTimeout(() => {
                    this.viewChitDetails(chitId);
                }, 2000);
            } else {
                const failedCount = selectedContacts.length - successCount;
                Utils.showError(`Sent ${successCount} invitation(s). ${failedCount} failed.`);
            }

            // Reset button
            sendButton.innerHTML = originalText;
            sendButton.disabled = false;

        } catch (error) {
            console.error('Error sending invitations:', error);
            Utils.showError(`Failed to send invitations: ${error.message}`);
            
            // Reset button
            const sendButton = document.getElementById('send-invites-btn');
            sendButton.innerHTML = '<i class="fas fa-envelope"></i> Send Invitations';
            sendButton.disabled = false;
        }
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
    // Force reload CSS to avoid caching issues
    const cssLink = document.querySelector('link[href*="styles.css"]');
    if (cssLink) {
        const href = cssLink.href;
        cssLink.href = href.split('?')[0] + '?v=' + Date.now();
    }
    
    window.app = new App();
    
    // Hide loading after a minimum time
    setTimeout(() => {
        document.getElementById('loading').classList.add('hidden');
    }, 1000);
});