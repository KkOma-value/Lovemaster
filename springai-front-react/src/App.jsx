import { BrowserRouter as Router, Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { AnimatePresence } from 'framer-motion';
import { GoogleOAuthProvider } from '@react-oauth/google';
import AppLayout from './components/Layout/AppLayout';
import Navbar from './components/Navbar/Navbar';
import HomePage from './pages/Home/HomePage';
import ChatPage from './pages/Chat/ChatPage';
import LoginPage from './pages/Auth/LoginPage';
import RegisterPage from './pages/Auth/RegisterPage';
import SetPasswordPage from './pages/Auth/SetPasswordPage';
import ProtectedRoute from './components/Auth/ProtectedRoute';
import { AuthProvider } from './contexts/AuthContext';

const GOOGLE_CLIENT_ID = '59540883835-shhhvumlokiqd24eb2jnjq7o6dt9rohk.apps.googleusercontent.com';

function AnimatedRoutes() {
  const location = useLocation();
  return (
    <AnimatePresence mode="wait">
      <Routes location={location} key={location.pathname}>
        <Route path="/" element={<HomePage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        
        <Route element={<ProtectedRoute />}>
          <Route path="/chat" element={<ChatPage />} />
          <Route path="/chat/:type" element={<ChatPage />} />
          <Route path="/set-password" element={<SetPasswordPage />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </AnimatePresence>
  );
}

function App() {
  return (
    <GoogleOAuthProvider clientId={GOOGLE_CLIENT_ID}>
      <Router>
        <AuthProvider>
          <Navbar />
          <AppLayout>
            <AnimatedRoutes />
          </AppLayout>
        </AuthProvider>
      </Router>
    </GoogleOAuthProvider>
  );
}

export default App;
