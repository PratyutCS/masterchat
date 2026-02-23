const jwt = require('jsonwebtoken');

/**
 * Auth Middleware
 * 
 * Expects header: Authorization: Bearer <token>
 * On success, attaches req.user = { id, username } and calls next().
 * On failure, returns 401.
 */
const authMiddleware = (req, res, next) => {
    try {
        // Get token from Authorization header
        const authHeader = req.headers.authorization;
        if (!authHeader || !authHeader.startsWith('Bearer ')) {
            return res.status(401).json({ error: 'No token provided' });
        }

        const token = authHeader.split(' ')[1];

        // Verify token
        const decoded = jwt.verify(token, process.env.JWT_SECRET);

        // Attach user info to request
        req.user = {
            id: decoded.id,
            username: decoded.username,
        };

        next();
    } catch (error) {
        return res.status(401).json({ error: 'Invalid or expired token' });
    }
};

module.exports = authMiddleware;
