import React, { useEffect, useRef } from 'react';
import { Send, Paperclip } from 'lucide-react';
import ImageUpload from './ImageUpload';
import { useImageUpload } from '../../hooks/useImageUpload';
import styles from './ChatArea.module.css';

const ChatInput = ({ inputValue, setInputValue, onSend, isLoading }) => {
    const textareaRef = useRef(null);
    const fileInputRef = useRef(null);
    const {
        compressImage,
        uploadImage,
        reset,
        isCompressing,
        isUploading,
        progress,
        preview,
        compressedFile,
        originalFile
    } = useImageUpload();

    useEffect(() => {
        if (textareaRef.current) {
            textareaRef.current.style.height = 'auto';
            const scrollHeight = textareaRef.current.scrollHeight;
            textareaRef.current.style.height = `${Math.min(scrollHeight, 200)}px`;
        }
    }, [inputValue]);

    const handleSend = async () => {
        if (!inputValue.trim() && !compressedFile) return;

        let uploadedImageUrl = null;
        if (compressedFile) {
            try {
                const result = await uploadImage();
                uploadedImageUrl = result.url || result.fileName;
            } catch (err) {
                console.error('Failed to upload', err);
                return;
            }
        }

        onSend(inputValue, uploadedImageUrl);
        reset();
    };

    const handleKeyDown = (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleSend();
        }
    };

    return (
        <div className={styles.inputArea} style={{ position: 'relative' }}>
            {preview && (
                <ImageUpload
                    previewUrl={preview}
                    isCompressing={isCompressing}
                    isUploading={isUploading}
                    progress={progress}
                    fileName={originalFile?.name}
                    originalSize={originalFile?.size}
                    compressedSize={compressedFile?.size}
                    onClear={reset}
                />
            )}
            <div className={styles.inputWrapper}>
                <textarea
                    ref={textareaRef}
                    value={inputValue}
                    onChange={(e) => setInputValue(e.target.value)}
                    onKeyDown={handleKeyDown}
                    placeholder="输入消息..."
                    className={styles.textarea}
                    rows={1}
                    disabled={isLoading || isUploading}
                />
                <div className={styles.inputToolbar}>
                    <button
                        type="button"
                        className={styles.attachBtn}
                        onClick={() => fileInputRef.current?.click()}
                        title="上传截图"
                        disabled={isCompressing || isUploading}
                    >
                        <Paperclip size={18} />
                    </button>
                    <input
                        type="file"
                        accept="image/*"
                        ref={fileInputRef}
                        style={{ display: 'none' }}
                        onChange={(e) => {
                            const file = e.target.files[0];
                            if (file) compressImage(file);
                            if (fileInputRef.current) fileInputRef.current.value = '';
                        }}
                    />
                    <button
                        onClick={handleSend}
                        disabled={(!inputValue.trim() && !compressedFile) || isLoading || isUploading}
                        className={styles.sendBtn}
                    >
                        <Send size={18} />
                    </button>
                </div>
            </div>
        </div>
    );
};

export default ChatInput;
