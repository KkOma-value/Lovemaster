import React, { useRef, useState } from 'react';
import { Plus, ChevronLeft, ChevronRight, LogOut, Heart } from 'lucide-react';
// eslint-disable-next-line no-unused-vars
import { motion } from 'framer-motion';
import ChatSidebarItem from './ChatSidebarItem';
import { useAuth } from '../../contexts/AuthContext';
import { useImageUpload } from '../../hooks/useImageUpload';
import styles from './ChatSidebar.module.css';

const listVariants = {
    hidden: { opacity: 0 },
    visible: {
        opacity: 1,
        transition: {
            staggerChildren: 0.06
        }
    }
};

const itemVariants = {
    hidden: { opacity: 0, x: -10 },
    visible: { opacity: 1, x: 0 }
};

const ChatSidebar = ({
    isOpen,
    onToggle,
    onNewChat,
    currentChatId,
    chatList = [],
    onSelectChat,
    onDeleteChat,
    backgroundRuns = {}
}) => {
    const { user, logout, updateAvatarUrl } = useAuth();
    const { compressImage, uploadImage } = useImageUpload();
    const [isUploadingAvatar, setIsUploadingAvatar] = useState(false);
    const fileInputRef = useRef(null);

    const handleAvatarClick = () => {
        if (!isUploadingAvatar && fileInputRef.current) {
            fileInputRef.current.click();
        }
    };

    const handleAvatarChange = async (e) => {
        const file = e.target.files[0];
        if (file) {
            try {
                setIsUploadingAvatar(true);
                await compressImage(file, { maxSizeMB: 0.5, maxWidthOrHeight: 400 });
                const result = await uploadImage('avatar');
                const url = result.url || result.fileName; // adjust per backend
                updateAvatarUrl(url);
            } catch (err) {
                console.error('Avatar upload failed:', err);
            } finally {
                setIsUploadingAvatar(false);
            }
        }
        if (fileInputRef.current) {
            fileInputRef.current.value = '';
        }
    };
    
    // Collapsed state - floating toggle button
    if (!isOpen) {
        return (
            <div className={styles.collapsedSidebar}>
                <button
                    className={styles.toggleBtn}
                    onClick={onToggle}
                    title="展开侧边栏"
                >
                    <ChevronRight size={20} />
                </button>
            </div>
        );
    }

    return (
        <aside className={styles.sidebar}>
            {/* Header / Top Actions */}
            <div className={styles.header}>
                <button
                    onClick={onNewChat}
                    className={styles.newChatBtn}
                >
                    <Plus size={18} />
                    <span className={styles.newChatText}>新对话</span>
                </button>
                <button
                    className={styles.toggleBtn}
                    onClick={onToggle}
                    title="收起侧边栏"
                >
                    <ChevronLeft size={20} color="#7A5C47" />
                </button>
            </div>

            {/* Chat List */}
            <motion.div 
                className={styles.list}
                variants={listVariants}
                initial="hidden"
                animate="visible"
            >
                {chatList.map(chat => (
                    <motion.div key={chat.id} variants={itemVariants}>
                        <ChatSidebarItem
                            chat={chat}
                            isActive={currentChatId === chat.id}
                            onSelect={onSelectChat}
                            onDelete={onDeleteChat}
                            runStatus={backgroundRuns[chat.id] || null}
                        />
                    </motion.div>
                ))}
            </motion.div>

            {/* Footer */}
            {user ? (
                <div className={styles.userInfo}>
                    <div style={{ position: 'relative', cursor: 'pointer', display: 'flex' }} onClick={handleAvatarClick} title="Change Avatar">
                        <img 
                            src={user.avatarUrl || `https://ui-avatars.com/api/?name=${encodeURIComponent(user.name || user.email || 'User')}&background=F43F5E&color=fff`} 
                            alt="Avatar" 
                            className={styles.userAvatar} 
                            style={{ opacity: isUploadingAvatar ? 0.5 : 1 }}
                        />
                        {isUploadingAvatar && (
                            <div style={{ position: 'absolute', inset: 0, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                                <div style={{ width: '16px', height: '16px', borderRadius: '50%', border: '2px solid #C47B5A', borderTopColor: 'transparent', animation: 'spin 1s linear infinite' }} />
                            </div>
                        )}
                        <input 
                            type="file" 
                            accept="image/*" 
                            style={{ display: 'none' }} 
                            ref={fileInputRef} 
                            onChange={handleAvatarChange} 
                        />
                    </div>
                    <div className={styles.userDetails}>
                        <span className={styles.userName}>{user.name || 'User'}</span>
                        <span className={styles.userEmail}>{user.email}</span>
                    </div>
                    <button className={styles.logoutBtn} onClick={logout} title="退出登录">
                        <LogOut size={18} />
                    </button>
                </div>
            ) : (
                <div className={styles.footer}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', justifyContent: 'center' }}>
                        <Heart size={16} color="#C47B5A" />
                        <span style={{
                            fontSize: '14px',
                            fontWeight: 600,
                            color: '#C47B5A'
                        }}>
                            Love Master
                        </span>
                    </div>
                </div>
            )}
        </aside>
    );
};

export default ChatSidebar;
