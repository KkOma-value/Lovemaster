/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useState, useEffect } from 'react';
import { authApi } from '../services/authApi';

const AuthContext = createContext();

export function useAuth() {
  return useContext(AuthContext);
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [accessToken, setAccessToken] = useState(null);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [needsPassword, setNeedsPassword] = useState(false);

  useEffect(() => {
    // Attempt to restore session
    const initAuth = async () => {
      const storedToken = localStorage.getItem('accessToken');
      if (storedToken) {
        try {
          // Verify with server using getMe or just assume valid until API call fails
          const me = await authApi.getMe(storedToken);
          setAccessToken(storedToken);
          setUser(me);
          setIsAuthenticated(true);
        } catch (error) {
          console.error('Session restoration failed:', error);
          localStorage.removeItem('accessToken');
        }
      }
      setIsLoading(false);
    };

    initAuth();
  }, []);

  const login = async (credentials) => {
    const data = await authApi.login(credentials);
    const token = data.accessToken || data.token; // Adapt to backend spec
    setAccessToken(token);
    setUser(data.user);
    setIsAuthenticated(true);
    localStorage.setItem('accessToken', token);
  };

  const register = async (userData) => {
    const data = await authApi.register(userData);
    const token = data.accessToken || data.token;
    setAccessToken(token);
    setUser(data.user);
    setIsAuthenticated(true);
    localStorage.setItem('accessToken', token);
  };

  const googleLogin = async (credential) => {
    const data = await authApi.googleAuth(credential);
    const token = data.accessToken || data.token;
    setAccessToken(token);
    setUser(data.user);
    setIsAuthenticated(true);
    setNeedsPassword(!!data.needsPassword);
    localStorage.setItem('accessToken', token);
    return data;
  };

  const setPasswordFn = async (password) => {
    await authApi.setPassword(password, accessToken);
    setNeedsPassword(false);
  };

  const logout = async () => {
    try {
      await authApi.logout();
    } catch (e) {
      console.error('Logout error:', e);
    } finally {
      setAccessToken(null);
      setUser(null);
      setIsAuthenticated(false);
      localStorage.removeItem('accessToken');
    }
  };

  const updateAvatarUrl = (avatarUrl) => {
    setUser((prev) => prev ? { ...prev, avatarUrl } : prev);
  };

  const value = {
    user,
    accessToken,
    isAuthenticated,
    isLoading,
    needsPassword,
    login,
    register,
    googleLogin,
    setPassword: setPasswordFn,
    logout,
    updateAvatarUrl,
  };

  return (
    <AuthContext.Provider value={value}>
      {!isLoading && children}
    </AuthContext.Provider>
  );
}
