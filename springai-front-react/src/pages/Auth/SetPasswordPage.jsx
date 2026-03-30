import { useState } from 'react';
import { useNavigate, Navigate } from 'react-router-dom';
// eslint-disable-next-line no-unused-vars
import { motion } from 'framer-motion';
import { useAuth } from '../../contexts/AuthContext';
import styles from './AuthPage.module.css';

export default function SetPasswordPage() {
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const navigate = useNavigate();
  const { setPassword: setPasswordFn, needsPassword, isAuthenticated } = useAuth();

  // If not authenticated, redirect to login
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  // If password already set, redirect to chat
  if (!needsPassword) {
    return <Navigate to="/chat" replace />;
  }

  const getPasswordStrength = (pwd) => {
    if (!pwd) return 0;
    let strength = 0;
    if (pwd.length >= 8) strength++;
    if (/[A-Z]/.test(pwd)) strength++;
    if (/[0-9]/.test(pwd)) strength++;
    if (/[^A-Za-z0-9]/.test(pwd)) strength++;
    return strength;
  };

  const strength = getPasswordStrength(password);
  const strengthColors = ['#DC2626', '#DC2626', '#D97706', '#3A8B7F', '#2A6B5F'];
  const strengthWidths = ['0%', '25%', '50%', '75%', '100%'];

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!password || !confirmPassword) {
      setError('Please fill in all fields');
      return;
    }
    if (password.length < 8) {
      setError('Password must be at least 8 characters long');
      return;
    }
    if (password !== confirmPassword) {
      setError('Passwords do not match');
      return;
    }

    setIsLoading(true);
    setError('');

    try {
      await setPasswordFn(password);
      navigate('/chat');
    } catch (err) {
      console.error(err);
      setError(err.response?.data?.error || err.message || 'Failed to set password. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className={styles.authContainer}>
      <motion.div
        className={styles.authCard}
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4, ease: "easeOut" }}
      >
        <div className={styles.logoContainer}>
          <h1 className={styles.logoTitle}>Lovemaster</h1>
        </div>

        <p className={styles.subtitle}>
          Set a local password to secure your account
        </p>

        {error && <div className={styles.errorMessage}>{error}</div>}

        <form onSubmit={handleSubmit}>
          <div className={styles.formGroup}>
            <label htmlFor="password">Password</label>
            <input
              id="password"
              type="password"
              className={styles.authInput}
              placeholder="Min 8 characters"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              disabled={isLoading}
              required
            />
            {password && (
              <div className={styles.strengthBar}>
                <div
                  className={styles.strengthFill}
                  style={{
                    width: strengthWidths[strength],
                    background: strengthColors[strength]
                  }}
                />
              </div>
            )}
          </div>

          <div className={styles.formGroup}>
            <label htmlFor="confirmPassword">Confirm Password</label>
            <input
              id="confirmPassword"
              type="password"
              className={styles.authInput}
              placeholder="Re-enter your password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              disabled={isLoading}
              required
            />
          </div>

          <button
            type="submit"
            className={styles.authButton}
            disabled={isLoading}
            aria-busy={isLoading}
          >
            {isLoading ? 'Setting password...' : 'Set Password'}
          </button>
        </form>
      </motion.div>
    </div>
  );
}
