// API Configuration
const API_CONFIG = {
    baseURL: 'http://localhost:8080/api/v1',
    timeout: 10000,
    get useMockData() {
        // Get from localStorage, default to true if not set
        const saved = localStorage.getItem('useMockData');
        return saved !== null ? JSON.parse(saved) : true;
    },
    set useMockData(value) {
        localStorage.setItem('useMockData', JSON.stringify(value));
    }
};

// Shared token management
class TokenManager {
    static getToken() {
        return localStorage.getItem('authToken');
    }
    
    static setToken(token) {
        localStorage.setItem('authToken', token);
    }
    
    static clearToken() {
        localStorage.removeItem('authToken');
    }
}

// Configuration management
class ConfigManager {
    static getConfig() {
        return {
            baseURL: localStorage.getItem('apiBaseURL') || API_CONFIG.baseURL,
            timeout: parseInt(localStorage.getItem('apiTimeout')) || API_CONFIG.timeout,
            useMockData: API_CONFIG.useMockData
        };
    }
    
    static setBaseURL(url) {
        localStorage.setItem('apiBaseURL', url);
        API_CONFIG.baseURL = url;
    }
    
    static setTimeout(timeout) {
        localStorage.setItem('apiTimeout', timeout.toString());
        API_CONFIG.timeout = timeout;
    }
    
    static toggleMockData() {
        API_CONFIG.useMockData = !API_CONFIG.useMockData;
        // Dispatch event for UI updates
        window.dispatchEvent(new CustomEvent('configChanged', {
            detail: { useMockData: API_CONFIG.useMockData }
        }));
        return API_CONFIG.useMockData;
    }
    
    static setMockData(value) {
        API_CONFIG.useMockData = value;
        window.dispatchEvent(new CustomEvent('configChanged', {
            detail: { useMockData: API_CONFIG.useMockData }
        }));
    }
}

// Mock data for development/demo purposes
const MOCK_DATA = {
    user: {
        id: "user-123",
        email: "demo@chitfund.com",
        mobile: "+91 9876543210",
        name: "Demo User",
        isEmailVerified: true,
        isMobileVerified: true,
        createdAt: "2024-01-01T00:00:00Z"
    },
    chits: [
        {
            id: "chit-1",
            name: "Family Savings Chit",
            fundAmount: 100000000000, // ₹1L in paisa (corrected)
            tenure: 12,
            memberCount: 10,
            startMonth: "2024-01",
            endMonth: "2024-12",
            payoutMethod: "RANDOM",
            moderatorId: "user-123",
            status: "ACTIVE",
            members: [
                {
                    userId: "user-123",
                    userName: "Demo User",
                    email: "demo@chitfund.com",
                    joinedAt: "2024-01-01T00:00:00Z",
                    status: "APPROVED",
                    payments: []
                },
                {
                    userId: "user-124",
                    userName: "John Doe",
                    email: "john.doe@example.com",
                    joinedAt: "2024-01-02T00:00:00Z",
                    status: "APPROVED",
                    payments: []
                },
                {
                    userId: "user-125",
                    userName: "Jane Smith",
                    email: "jane.smith@example.com",
                    joinedAt: "2024-01-03T00:00:00Z",
                    status: "APPROVED",
                    payments: []
                },
                {
                    userId: "user-126",
                    userName: "Raj Patel",
                    email: "raj.patel@example.com",
                    joinedAt: "2024-01-04T00:00:00Z",
                    status: "APPROVED",
                    payments: []
                },
                {
                    userId: "user-127",
                    userName: "Priya Sharma",
                    email: "priya.sharma@example.com",
                    joinedAt: "2024-01-05T00:00:00Z",
                    status: "APPROVED",
                    payments: []
                },
                {
                    userId: "user-128",
                    userName: "Amit Kumar",
                    email: "amit.kumar@example.com",
                    joinedAt: "2024-01-06T00:00:00Z",
                    status: "APPROVED",
                    payments: []
                },
                {
                    userId: "user-129",
                    userName: "Sunita Reddy",
                    email: "sunita.reddy@example.com",
                    joinedAt: "2024-01-07T00:00:00Z",
                    status: "APPROVED",
                    payments: []
                },
                {
                    userId: "user-130",
                    userName: "Vikram Singh",
                    email: "vikram.singh@example.com",
                    joinedAt: "2024-01-08T00:00:00Z",
                    status: "APPROVED",
                    payments: []
                },
                {
                    userId: "user-131",
                    userName: "Meera Joshi",
                    email: "meera.joshi@example.com",
                    joinedAt: "2024-01-09T00:00:00Z",
                    status: "APPROVED",
                    payments: []
                },
                {
                    userId: "user-132",
                    userName: "Arjun Nair",
                    email: "arjun.nair@example.com",
                    joinedAt: "2024-01-10T00:00:00Z",
                    status: "APPROVED",
                    payments: []
                }
            ],
            createdAt: "2024-01-01T00:00:00Z"
        },
        {
            id: "chit-2",
            name: "Business Expansion Fund",
            fundAmount: 500000000000, // ₹5L in paisa (corrected)
            tenure: 24,
            memberCount: 20,
            startMonth: "2024-02",
            endMonth: "2026-01",
            payoutMethod: "VOTING",
            moderatorId: "user-123",
            status: "OPEN",
            members: [],
            createdAt: "2024-01-15T00:00:00Z"
        }
    ]
};

// API utility functions
class ApiClient {
    constructor() {
        // Always get the latest token from storage
        this.getToken = () => TokenManager.getToken();
    }

    async request(endpoint, options = {}) {
        // Use mock data if configured
        if (API_CONFIG.useMockData) {
            return this.handleMockRequest(endpoint, options);
        }

        const config = ConfigManager.getConfig();
        const url = `${config.baseURL}${endpoint}`;
        const requestConfig = {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                ...options.headers
            },
            ...options
        };

        // Add auth token if available
        const token = this.getToken();
        if (token) {
            requestConfig.headers['Authorization'] = `Bearer ${token}`;
        }

        try {
            const controller = new AbortController();
            const timeoutId = setTimeout(() => controller.abort(), config.timeout);

            const response = await fetch(url, {
                ...requestConfig,
                signal: controller.signal
            });

            clearTimeout(timeoutId);

            if (!response.ok) {
                // Handle specific HTTP errors
                if (response.status === 401) {
                    throw new Error('Unauthorized - Please login again');
                } else if (response.status === 403) {
                    throw new Error('Access forbidden');
                } else if (response.status === 404) {
                    throw new Error('Resource not found');
                } else if (response.status === 500) {
                    throw new Error('Server error - Please try again later');
                } else {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }
            }

            return await response.json();
        } catch (error) {
            if (error.name === 'AbortError') {
                throw new Error('Request timeout - Check your connection or try mock data mode');
            } else if (error.name === 'TypeError') {
                // Network error (e.g., server not running)
                throw new Error('Cannot connect to server - Please check if the backend is running or switch to mock data mode');
            }
            throw error;
        }
    }

    async handleMockRequest(endpoint, options = {}) {
        // Simulate network delay
        await new Promise(resolve => setTimeout(resolve, 500 + Math.random() * 500));

        const method = options.method || 'GET';
        
        if (endpoint === '/auth/login' && method === 'POST') {
            return {
                success: true,
                message: "OTP sent successfully! (Demo: any 6 digits will work)"
            };
        }
        
        if (endpoint === '/auth/verify-otp' && method === 'POST') {
            return {
                success: true,
                token: "demo-token-123",
                user: MOCK_DATA.user,
                message: "Login successful!"
            };
        }
        
        if (endpoint === '/users/profile' && method === 'GET') {
            const token = this.getToken();
            if (!token) {
                throw new Error('Unauthorized');
            }
            return {
                success: true,
                data: MOCK_DATA.user
            };
        }
        
        if (endpoint === '/chits' && method === 'GET') {
            const token = this.getToken();
            if (!token) {
                throw new Error('Unauthorized');
            }
            return {
                success: true,
                data: MOCK_DATA.chits
            };
        }
        
        if (endpoint === '/chits' && method === 'POST') {
            const token = this.getToken();
            if (!token) {
                throw new Error('Unauthorized');
            }
            const newChit = {
                id: `chit-${Date.now()}`,
                ...JSON.parse(options.body),
                moderatorId: MOCK_DATA.user.id,
                status: "OPEN",
                members: [],
                createdAt: new Date().toISOString(),
                endMonth: this.calculateEndMonth(JSON.parse(options.body).startMonth, JSON.parse(options.body).tenure)
            };
            MOCK_DATA.chits.push(newChit);
            return {
                success: true,
                data: newChit,
                message: "Chit created successfully!"
            };
        }
        
        if (endpoint.startsWith('/chits/') && endpoint.includes('/invite') && method === 'POST') {
            return {
                success: true,
                message: "Member invitation sent successfully!"
            };
        }
        
        if (endpoint.startsWith('/chits/') && method === 'GET' && !endpoint.includes('/invite') && !endpoint.includes('/join')) {
            const chitId = endpoint.split('/')[2];
            const chit = MOCK_DATA.chits.find(c => c.id === chitId);
            if (chit) {
                return {
                    success: true,
                    data: chit
                };
            } else {
                throw new Error('Chit not found');
            }
        }
        
        throw new Error(`Mock endpoint not implemented: ${method} ${endpoint}`);
    }

    calculateEndMonth(startMonth, tenure) {
        const [year, month] = startMonth.split('-').map(Number);
        const endDate = new Date(year, month - 1 + tenure);
        return `${endDate.getFullYear()}-${String(endDate.getMonth() + 1).padStart(2, '0')}`;
    }

    async get(endpoint, params = {}) {
        const queryString = new URLSearchParams(params).toString();
        const url = queryString ? `${endpoint}?${queryString}` : endpoint;
        return this.request(url);
    }

    async post(endpoint, data = {}) {
        return this.request(endpoint, {
            method: 'POST',
            body: JSON.stringify(data)
        });
    }

    async put(endpoint, data = {}) {
        return this.request(endpoint, {
            method: 'PUT',
            body: JSON.stringify(data)
        });
    }

    async delete(endpoint) {
        return this.request(endpoint, {
            method: 'DELETE'
        });
    }

    setToken(token) {
        TokenManager.setToken(token);
    }

    clearToken() {
        TokenManager.clearToken();
    }
}

// Authentication API
class AuthAPI extends ApiClient {
    async login(email) {
        return this.post('/auth/login', { email });
    }

    async verifyOtp(email, otp) {
        return this.post('/auth/verify-otp', { email, otp });
    }

    async getUserProfile() {
        return this.get('/users/profile');
    }
}

// Chit API
class ChitAPI extends ApiClient {
    async getChits() {
        return this.get('/chits');
    }

    async createChit(chitData) {
        return this.post('/chits', chitData);
    }

    async getChitDetails(chitId) {
        return this.get(`/chits/${chitId}`);
    }

    async inviteMember(chitId, email) {
        return this.post(`/chits/${chitId}/invite`, { chitId, email });
    }

    async joinChit(chitId) {
        return this.post(`/chits/${chitId}/join`, { chitId });
    }
}

// Export API instances
const authAPI = new AuthAPI();
const chitAPI = new ChitAPI();

// Make instances available globally for token synchronization
window.authAPI = authAPI;
window.chitAPI = chitAPI;