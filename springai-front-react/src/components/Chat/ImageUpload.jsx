import React from 'react';
import { X } from 'lucide-react';
import styles from './ImageUpload.module.css';

export default function ImageUpload({
  previewUrl,
  onClear,
  isCompressing,
  isUploading,
  progress,
  fileName,
  originalSize,
  compressedSize,
}) {
  const parseSize = (size) => {
    if (!size) return '';
    return (size / 1024 / 1024).toFixed(2) + 'MB';
  };

  if (!previewUrl) return null;

  return (
    <div className={styles.imagePreview}>
      <img src={previewUrl} alt="Preview" className={styles.previewThumbnail} />
      <div className={styles.previewInfo}>
        <div className={styles.previewFileName}>
          {fileName || '聊天截图'}
          {isCompressing && `（压缩中 ${progress.toFixed(0)}%）`}
        </div>
        <div className={styles.previewSize}>
          {originalSize && `${parseSize(originalSize)} `}
          {compressedSize && (
            <>
              &rarr; <span className={styles.compressed}>{parseSize(compressedSize)}</span> &nbsp;&#x2713; 已压缩
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
        <button type="button" className={styles.removeButton} onClick={onClear} title="移除截图">
          <X size={18} />
        </button>
      )}
    </div>
  );
}
