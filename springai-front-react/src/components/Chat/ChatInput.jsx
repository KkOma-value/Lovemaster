import React, { useEffect, useRef } from 'react';
import { Send } from 'lucide-react';
import ImageUpload from './ImageUpload';
import { useImageUpload } from '../../hooks/useImageUpload';
import styles from './ChatArea.module.css';

const ChatInput = ({ inputValue, setInputValue, onSend, isLoading }) => {
    const textareaRef = useRef(null);
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
            const newHeight = Math.min(scrollHeight, 200);
            textareaRef.current.style.height = `${newHeight}px`;
        }
    }, [inputValue]);

    const handleSend = async () => {
        if (!inputValue.trim() && !compressedFile) return;

        let uploadedImageUrl = null;
        if (compressedFile) {
            try {
                const result = await uploadImage();
                uploadedImageUrl = result.url || result.fileName; // Assuming API returns URL or filename
            } catch (err) {
                console.error('Failed to upload', err);
                return; // halt send if upload fails
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
                <ImageUpload 
                    onImageSelect={compressImage}
                />
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
                <button
                    onClick={handleSend}
                    disabled={(!inputValue.trim() && !compressedFile) || isLoading || isUploading}
                    className={styles.sendBtn}
                >
                    <Send size={18} />
                </button>
            </div>

        </div>
    );
};

export default ChatInput;
