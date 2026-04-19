import React, { useEffect, useRef } from 'react';
import { Send, Image as ImageIcon, Sparkles } from 'lucide-react';
import ImageUpload from './ImageUpload';
import { useImageUpload } from '../../hooks/useImageUpload';

const MAX_CHARS = 2000;

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
    originalFile,
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

  const canSend = (!!inputValue.trim() || !!compressedFile) && !isLoading && !isUploading;
  const charCount = inputValue.length;

  return (
    <div
      className="soft-card relative"
      style={{
        padding: '14px 16px 12px',
        boxShadow: '0 10px 32px rgba(196,123,90,0.1), inset 0 1px 0 rgba(255,255,255,0.8)',
      }}
    >
      {preview && (
        <div style={{ marginBottom: 10 }}>
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
        </div>
      )}

      <textarea
        ref={textareaRef}
        value={inputValue}
        onChange={(e) => setInputValue(e.target.value.slice(0, MAX_CHARS))}
        onKeyDown={handleKeyDown}
        placeholder="把今天的心情，慢慢说给我听…"
        className="w-full resize-none body-font text-[15px] leading-relaxed bg-transparent"
        style={{
          color: 'var(--text-ink)',
          minHeight: 60,
          border: 'none',
          outline: 'none',
          fontFamily: 'var(--font-body)',
        }}
        rows={2}
        disabled={isLoading || isUploading}
      />

      <div className="flex items-center justify-between">
        <div className="flex items-center gap-1.5">
          <button
            type="button"
            title="上传截图"
            onClick={() => fileInputRef.current?.click()}
            disabled={isCompressing || isUploading}
            className="grid place-items-center"
            style={{
              width: 32,
              height: 32,
              borderRadius: 10,
              color: 'var(--text-body)',
              background: 'transparent',
              border: 'none',
              cursor: 'pointer',
              transition: 'all .2s',
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.background = 'var(--bg-peach)';
              e.currentTarget.style.color = 'var(--primary-dark)';
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.background = 'transparent';
              e.currentTarget.style.color = 'var(--text-body)';
            }}
          >
            <ImageIcon size={16} />
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
          <span
            className="grid place-items-center"
            title="温柔提示"
            style={{
              width: 32,
              height: 32,
              borderRadius: 10,
              color: 'var(--text-faint)',
            }}
          >
            <Sparkles size={14} />
          </span>
        </div>

        <div className="flex items-center gap-3">
          <span className="text-[11px]" style={{ color: 'var(--text-muted)' }}>
            {charCount}/{MAX_CHARS}
          </span>
          <button
            onClick={handleSend}
            disabled={!canSend}
            className="grid place-items-center"
            style={{
              width: 40,
              height: 40,
              borderRadius: 14,
              background: canSend ? 'linear-gradient(135deg, #F2A987, #E89B7A)' : '#EDE4D8',
              color: canSend ? '#FFFAF5' : '#B09080',
              boxShadow: canSend ? '0 8px 20px rgba(232,155,122,0.4)' : 'none',
              transition: 'all .2s',
              border: 'none',
              cursor: canSend ? 'pointer' : 'not-allowed',
            }}
          >
            <Send size={16} />
          </button>
        </div>
      </div>
    </div>
  );
};

export default ChatInput;
