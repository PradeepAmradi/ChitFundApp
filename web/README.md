# ChitFund Web Application

A modern, responsive web application for managing chit funds with full API integration.

## Features

- **Authentication**: Email-based OTP login system
- **Dashboard**: Overview of chits and statistics
- **Chit Management**: Create, view, and manage chit funds
- **Member Management**: Invite and manage chit members
- **Responsive Design**: Works on desktop, tablet, and mobile devices
- **Real-time API Integration**: Connects to the ChitFund backend API

## Technology Stack

- **Frontend**: HTML5, CSS3, Vanilla JavaScript
- **UI**: Modern responsive design with CSS Grid and Flexbox
- **API**: REST API integration with fetch
- **Icons**: Font Awesome 6
- **Development Server**: Python HTTP server

## Getting Started

### Prerequisites

1. ChitFund backend server running on port 8080
2. Python 3.x installed (for development server)

### Running the Web Application

1. **Start the Backend Server** (in the project root):
   ```bash
   ./gradlew :backend:run
   ```

2. **Start the Web Server** (in a new terminal):
   ```bash
   cd web
   python3 server.py
   ```

3. **Open your browser** and navigate to:
   ```
   http://localhost:3000
   ```

## File Structure

```
web/
├── index.html              # Main HTML file
├── css/
│   └── styles.css          # All CSS styles
├── js/
│   ├── app.js              # Main application logic
│   ├── api.js              # API client and utilities
│   ├── auth.js             # Authentication handling
│   ├── chits.js            # Chit management functionality
│   └── utils.js            # Utility functions
├── pages/                  # Additional HTML pages (if needed)
├── server.py               # Development HTTP server
└── README.md               # This file
```

## Features in Detail

### Authentication
- Email-based login with OTP verification
- Secure token storage and management
- Auto-logout on token expiration

### Dashboard
- Overview of user's chits
- Quick statistics and metrics
- Easy navigation to key features

### Chit Management
- Create new chits with validation
- View list of all user's chits
- Detailed chit information
- Member invitation system

### User Experience
- Responsive design for all screen sizes
- Loading states and error handling
- Success/error message feedback
- Intuitive navigation

## API Integration

The web application integrates with the following backend endpoints:

### Authentication
- `POST /api/v1/auth/login` - Login with email
- `POST /api/v1/auth/verify-otp` - Verify OTP
- `GET /api/v1/users/profile` - Get user profile

### Chit Management
- `GET /api/v1/chits` - Get user's chits
- `POST /api/v1/chits` - Create new chit
- `GET /api/v1/chits/{id}` - Get chit details
- `POST /api/v1/chits/{id}/invite` - Invite member
- `POST /api/v1/chits/{id}/join` - Join chit

## Development

### Adding New Features

1. **New Pages**: Add HTML templates and corresponding JavaScript handlers
2. **API Endpoints**: Extend the API client in `js/api.js`
3. **UI Components**: Add reusable components in the appropriate JS files
4. **Styling**: Use the existing CSS utility classes or add new ones

### Code Organization

- **Modular JavaScript**: Each feature has its own module
- **Consistent Styling**: Utility-first CSS approach
- **Error Handling**: Centralized error handling and user feedback
- **Responsive Design**: Mobile-first approach

## Browser Support

- Chrome 80+
- Firefox 75+
- Safari 13+
- Edge 80+

## Production Deployment

For production deployment:

1. **Serve with a proper web server** (Nginx, Apache, etc.)
2. **Update API configuration** to point to production backend
3. **Implement proper HTTPS**
4. **Add CSP headers** for security
5. **Minify and bundle assets**

## Security Considerations

- API tokens are stored in localStorage (consider upgrading to httpOnly cookies for production)
- CORS is configured in the backend
- Input validation on both client and server
- XSS protection through proper escaping

## Contributing

1. Follow the existing code style
2. Add comments for complex logic
3. Test on multiple browsers
4. Ensure responsive design works on all screen sizes