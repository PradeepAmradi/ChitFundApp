// API Configuration
const API_CONFIG = {
    baseURL: 'http://localhost:8080/api/v1',
    timeout: 10000,
    useMockData: true // Set to false when backend is available
};

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
            fundAmount: 10000000, // ₹1L in paisa
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
                    joinedAt: "2024-01-01T00:00:00Z",
                    status: "APPROVED",
                    payments: []
                },
                {
                    userId: "user-124",
                    userName: "John Doe",
                    joinedAt: "2024-01-02T00:00:00Z",
                    status: "APPROVED",
                    payments: []
                }
            ],
            createdAt: "2024-01-01T00:00:00Z"
        },
        {
            id: "chit-2",
            name: "Business Expansion Fund",
            fundAmount: 50000000, // ₹5L in paisa
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
        this.token = localStorage.getItem('authToken');
    }

    async request(endpoint, options = {}) {
        // Use mock data if configured
        if (API_CONFIG.useMockData) {
            return this.handleMockRequest(endpoint, options);
        }

        const url = `${API_CONFIG.baseURL}${endpoint}`;
        const config = {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                ...options.headers
            },
            ...options
        };

        // Add auth token if available
        if (this.token) {
            config.headers['Authorization'] = `Bearer ${this.token}`;
        }

        try {
            const controller = new AbortController();
            const timeoutId = setTimeout(() => controller.abort(), API_CONFIG.timeout);

            const response = await fetch(url, {
                ...config,
                signal: controller.signal
            });

            clearTimeout(timeoutId);

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            if (error.name === 'AbortError') {
                throw new Error('Request timeout');
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
            if (!this.token) {
                throw new Error('Unauthorized');
            }
            return {
                success: true,
                data: MOCK_DATA.user
            };
        }
        
        if (endpoint === '/chits' && method === 'GET') {
            if (!this.token) {
                throw new Error('Unauthorized');
            }
            return {
                success: true,
                data: MOCK_DATA.chits
            };
        }
        
        if (endpoint === '/chits' && method === 'POST') {
            if (!this.token) {
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
        
        if (endpoint.startsWith('/chits/') && !endpoint.includes('/') && method === 'GET') {
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
        this.token = token;
        localStorage.setItem('authToken', token);
    }

    clearToken() {
        this.token = null;
        localStorage.removeItem('authToken');
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