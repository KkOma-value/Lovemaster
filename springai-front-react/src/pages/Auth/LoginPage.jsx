import { useState } from 'react';
import { useNavigate, Link, Navigate } from 'react-router-dom';
// eslint-disable-next-line no-unused-vars
import { motion } from 'framer-motion';
import { GoogleLogin } from '@react-oauth/google';
import { useAuth } from '../../contexts/AuthContext';
import styles from './AuthPage.module.css';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  
  const navigate = useNavigate();
  const { login, googleLogin, isAuthenticated } = useAuth();

  // If already logged in, redirect to chat
  if (isAuthenticated) {
    return <Navigate to="/chat" replace />;
  }

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!email || !password) {
      setError('Please fill in all fields');
      return;
    }
    
    setIsLoading(true);
    setError('');
    
    try {
      await login({ email, password });
      navigate('/chat');
    } catch (err) {
      console.error(err);
      setError(err.message || 'Login failed. Please check your credentials.');
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
              placeholder="••••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
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
            {isLoading ? 'Logging in...' : 'Log In'}
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
                setError(err.response?.data?.error || 'Google login failed. Please try again.');
              } finally {
                setIsLoading(false);
              }
            }}
            onError={() => {
              setError('Google login failed. Please try again.');
            }}
            theme="outline"
            size="large"
            shape="pill"
            width="320"
            text="signin_with"
          />
        </div>

        <div className={styles.linkContainer}>
          Don't have an account?
          <Link to="/register" className={styles.authLink}>
            Register
          </Link>
        </div>
      </motion.div>
    </div>
  );
}
