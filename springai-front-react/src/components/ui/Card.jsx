import React, { useRef } from 'react';
import styles from './Card.module.css';

export const Card = ({ children, className = '', hoverable = false, onClick }) => {
    const cardRef = useRef(null);

    const handleMouseMove = (e) => {
        if (!hoverable || !cardRef.current) return;

        const rect = cardRef.current.getBoundingClientRect();
        const x = e.clientX - rect.left;
        const y = e.clientY - rect.top;

        cardRef.current.style.setProperty('--mouse-x', `${x}px`);
        cardRef.current.style.setProperty('--mouse-y', `${y}px`);
    };

    return (
        <div
            ref={cardRef}
            className={`${styles.card} ${hoverable ? styles.hoverable : ''} ${className}`}
            onMouseMove={handleMouseMove}
            onClick={onClick}
        >
            <div className={styles.content}>
                {children}
            </div>
        </div>
    );
};

export default Card;
