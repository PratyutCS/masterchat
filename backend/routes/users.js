const express = require('express');
const User = require('../models/User');
const auth = require('../middleware/auth');

const router = express.Router();

/**
 * GET /api/users
 * 
 * Returns all users except the currently authenticated user.
 * Used to display a list of people to chat with.
 * Protected route — requires JWT.
 */
router.get('/', auth, async (req, res) => {
    try {
        // Find all users except self, exclude passwordHash
        const users = await User.find({ _id: { $ne: req.user.id } })
            .select('-passwordHash')
            .sort({ username: 1 });

        res.json(users);
    } catch (error) {
        console.error('Get users error:', error.message);
        res.status(500).json({ error: 'Server error' });
    }
});

/**
 * GET /api/users/:id
 * 
 * Returns a single user's profile.
 * Protected route — requires JWT.
 */
router.get('/:id', auth, async (req, res) => {
    try {
        const user = await User.findById(req.params.id).select('-passwordHash');
        if (!user) {
            return res.status(404).json({ error: 'User not found' });
        }
        res.json(user);
    } catch (error) {
        console.error('Get user error:', error.message);
        res.status(500).json({ error: 'Server error' });
    }
});

module.exports = router;
