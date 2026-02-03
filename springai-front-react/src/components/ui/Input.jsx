import React from 'react';
import styles from './Input.module.css';

export const Input = ({ icon: Icon, className = '', ...props }) => {
    return (
        <div className={`${styles.wrapper} ${className}`}>
            {Icon && <Icon className={styles.icon} size={20} />}
            <input
                className={`${styles.input} ${Icon ? styles.withIcon : ''}`}
                {...props}
            />
        </div>
    );
};

export default Input;
