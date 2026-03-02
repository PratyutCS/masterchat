const mongoose = require('mongoose');

/**
 * User Schema
 * 
 * Fields:
 *   - username:     Unique display name
 *   - email:        Unique email address
 *   - passwordHash: bcrypt-hashed password
 *   - lastSeen:     Timestamp of last activity (updated on disconnect)
 *   - createdAt:    Auto-set by Mongoose timestamps
 */
const userSchema = new mongoose.Schema(
    {
        username: {
            type: String,
            required: true,
            unique: true,
            trim: true,
            minlength: 3,
            maxlength: 30,
        },
        email: {
            type: String,
            required: true,
            unique: true,
            trim: true,
            lowercase: true,
        },
        passwordHash: {
            type: String,
            required: true,
        },
        lastSeen: {
            type: Date,
            default: Date.now,
        },
        fcmToken: {
            type: String,
            default: null,
        },
    },
    {
        timestamps: true, // adds createdAt and updatedAt automatically
    }
);

// Never return passwordHash in JSON responses
userSchema.methods.toJSON = function () {
    const user = this.toObject();
    delete user.passwordHash;
    return user;
};

module.exports = mongoose.model('User', userSchema);
