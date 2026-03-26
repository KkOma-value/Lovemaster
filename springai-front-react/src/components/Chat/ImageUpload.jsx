import React, { useRef } from 'react';
import { Paperclip, X } from 'lucide-react';
import styles from './ImageUpload.module.css';

export default function ImageUpload({ 
  onImageSelect, 
  previewUrl, 
  onClear, 
  isCompressing, 
  isUploading, 
  progress,
  fileName,
  originalSize,
  compressedSize
}) {
  const fileInputRef = useRef(null);

  const handleIconClick = () => {
    if (fileInputRef.current) {
      fileInputRef.current.click();
    }
  };

  const parseSize = (size) => {
    if (!size) return '';
    return (size / 1024 / 1024).toFixed(2) + 'MB';
  };

  return (
    <div className={styles.container}>
      {previewUrl && (
        <div className={styles.imagePreview}>
          <img src={previewUrl} alt="Preview" className={styles.previewThumbnail} />
          <div className={styles.previewInfo}>
            <div className={styles.previewFileName}>
              {fileName || 'Image'}
              {isCompressing && ` (Compressing... ${progress.toFixed(0)}%)`}
            </div>
            <div className={styles.previewSize}>
              {originalSize && `${parseSize(originalSize)} `}
              {compressedSize && (
                <>
                  &rarr; <span className={styles.compressed}>{parseSize(compressedSize)}</span> &nbsp;&#x2713; Compressed
                </>
              )}
            </div>
            {(isCompressing || isUploading) && (
              <div className={styles.progressBar}>
                <div 
                  className={styles.progressFill} 
                  style={{ width: `${progress}%` }} 
                />
              </div>
            )}
          </div>
          {!isCompressing && !isUploading && (
            <button type="button" className={styles.removeButton} onClick={onClear} title="Remove image">
              <X size={18} />
            </button>
          )}
        </div>
      )}
      
      <button 
        type="button" 
        className={styles.attachButton} 
        onClick={handleIconClick}
        title="Upload Image"
        disabled={isCompressing || isUploading}
      >
        <Paperclip size={20} />
      </button>

      <input
        type="file"
        accept="image/*"
        style={{ display: 'none' }}
        ref={fileInputRef}
        onChange={(e) => {
          const file = e.target.files[0];
          if (file) {
            onImageSelect(file);
          }
          // Reset input so the same file can be selected again if removed
          if (fileInputRef.current) {
            fileInputRef.current.value = '';
          }
        }}
      />
    </div>
  );
}
