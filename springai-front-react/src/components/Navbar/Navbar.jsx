import { useState, useRef, useCallback } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { Heart } from 'lucide-react';
import { useAuth } from '../../contexts/AuthContext';
import styles from './Navbar.module.css';

const Navbar = () => {
    const { user, isAuthenticated, logout } = useAuth();
    const navigate = useNavigate();
    const location = useLocation();
    const [visible, setVisible] = useState(false);
    const hideTimer = useRef(null);

    const showNav = useCallback(() => {
        clearTimeout(hideTimer.current);
        setVisible(true);
    }, []);

    const scheduleHide = useCallback(() => {
        hideTimer.current = setTimeout(() => setVisible(false), 400);
    }, []);

    const handleLogout = async () => {
        await logout();
        navigate('/');
    };

    // Don't render on login/register/chat pages — they have their own flow
    if (location.pathname === '/login' || location.pathname === '/register' || location.pathname.startsWith('/chat')) {
        return null;
    }

    return (
        <>
            {/* Invisible trigger zone at the very top */}
            <div
                className={styles.hoverTrigger}
                onMouseEnter={showNav}
                aria-hidden="true"
            />

            <nav
                className={`${styles.navWrapper} ${visible ? styles.visible : ''}`}
                onMouseEnter={showNav}
                onMouseLeave={scheduleHide}
                aria-label="主导航"
            >
                <div className={styles.navbar}>
                    {/* Brand */}
                    <a
                        href="/"
                        className={styles.brand}
                        onClick={(e) => { e.preventDefault(); navigate('/'); }}
                    >
                        <div className={styles.brandIcon}>
                            <Heart size={17} />
                        </div>
                        <span className={styles.brandName}>Love Master</span>
                    </a>

                    {/* Actions */}
                    <div className={styles.actions}>
                        {isAuthenticated ? (
                            <div className={styles.userSection}>
                                {user?.avatarUrl ? (
                                    <img
                                        src={user.avatarUrl}
                                        alt={user.nickname || user.username}
                                        className={styles.avatar}
                                    />
                                ) : (
                                    <div className={styles.avatarPlaceholder}>
                                        {(user?.nickname || user?.username || '?')[0].toUpperCase()}
                                    </div>
                                )}
                                <span className={styles.userName}>
                                    {user?.nickname || user?.username}
                                </span>
                                <button
                                    className={styles.chatBtn}
                                    onClick={() => navigate('/chat/loveapp')}
                                >
                                    开始聊天
                                </button>
                                <button
                                    className={styles.logoutBtn}
                                    onClick={handleLogout}
                                >
                                    退出
                                </button>
                            </div>
                        ) : (
                            <>
                                <button
                                    className={styles.loginBtn}
                                    onClick={() => navigate('/login')}
                                >
                                    登录
                                </button>
                                <button
                                    className={styles.registerBtn}
                                    onClick={() => navigate('/register')}
                                >
                                    注册
                                </button>
                            </>
                        )}
                    </div>
                </div>
            </nav>
        </>
    );
};

export default Navbar;
