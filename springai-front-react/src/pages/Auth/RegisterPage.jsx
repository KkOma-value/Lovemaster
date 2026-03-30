import { useState } from 'react';
import { useNavigate, Link, Navigate } from 'react-router-dom';
// eslint-disable-next-line no-unused-vars
import { motion } from 'framer-motion';
import { GoogleLogin } from '@react-oauth/google';
import { useAuth } from '../../contexts/AuthContext';
import styles from './AuthPage.module.css';

export default function RegisterPage() {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  
  const navigate = useNavigate();
  const { register, googleLogin, isAuthenticated } = useAuth();

  // If already logged in, redirect to chat
  if (isAuthenticated) {
    return <Navigate to="/chat" replace />;
  }

  const getPasswordStrength = (pwd) => {
    if (!pwd) return 0;
    let strength = 0;
    if (pwd.length >= 8) strength++;
    if (/[A-Z]/.test(pwd)) strength++;
    if (/[0-9]/.test(pwd)) strength++;
    if (/[^A-Za-z0-9]/.test(pwd)) strength++;
    return strength; // 0 to 4
  };

  const strength = getPasswordStrength(password);
  const strengthColors = ['#DC2626', '#DC2626', '#D97706', '#3A8B7F', '#2A6B5F'];
  const strengthWidths = ['0%', '25%', '50%', '75%', '100%'];

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!name || !email || !password || !confirmPassword) {
      setError('Please fill in all fields');
      return;
    }
    
    if (password !== confirmPassword) {
      setError('Passwords do not match');
      return;
    }

    if (password.length < 8) {
      setError('Password must be at least 8 characters long');
      return;
    }
    
    setIsLoading(true);
    setError('');
    
    try {
      await register({ name, email, password });
      navigate('/chat');
    } catch (err) {
      console.error(err);
      setError(err.message || 'Registration failed. Please try again.');
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

        {error && <div className={styles.errorMessage}>{error}</div>}

        <form onSubmit={handleSubmit}>
          <div className={styles.formGroup}>
            <label htmlFor="name">Nickname</label>
            <input
              id="name"
              type="text"
              className={styles.authInput}
              placeholder="Your Nickname"
              value={name}
              onChange={(e) => setName(e.target.value)}
              disabled={isLoading}
              required
            />
          </div>

          <div className={styles.formGroup}>
            <label htmlFor="email">Email</label>
            <input
              id="email"
              type="email"
              className={styles.authInput}
              placeholder="your@email.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              disabled={isLoading}
              required
            />
          </div>

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
              placeholder="••••••••"
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
            {isLoading ? 'Registering...' : 'Register'}
          </button>
        </form>

        <div className={styles.divider} role="separator" aria-hidden="true">
          <span className={styles.dividerText}>or</span>
        </div>

        <div className={styles.googleButtonContainer}>
          <GoogleLogin
            onSuccess={async (credentialResponse) => {
              setIsLoading(true);
              setError('');
              try {
                const data = await googleLogin(credentialResponse.credential);
                if (data.needsPassword) {
                  navigate('/set-password');
                } else {
                  navigate('/chat');
                }
              } catch (err) {
                console.error(err);
                setError(err.response?.data?.error || 'Google registration failed. Please try again.');
              } finally {
                setIsLoading(false);
              }
            }}
            onError={() => {
              setError('Google registration failed. Please try again.');
            }}
            theme="outline"
            size="large"
            shape="pill"
            width="320"
            text="signup_with"
          />
        </div>

        <div className={styles.linkContainer}>
          Already have an account?
          <Link to="/login" className={styles.authLink}>
            Login
          </Link>
        </div>
      </motion.div>
    </div>
  );
}
