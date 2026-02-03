import React from 'react';
import styles from './Button.module.css';

export const Button = ({
    children,
    variant = 'primary',
    size = 'medium',
    loading = false,
    className = '',
    disabled,
    onClick,
    type = 'button',
    ...props
}) => {
    const buttonClasses = [
        styles.button,
        styles[variant],
        styles[size],
        loading ? styles.loading : '',
        className
    ].join(' ');

    return (
        <button
            className={buttonClasses}
            disabled={disabled || loading}
            onClick={onClick}
            type={type}
            {...props}
        >
            {loading && <div className={styles.spinner} />}
            {children}
        </button>
    );
};

export default Button;
