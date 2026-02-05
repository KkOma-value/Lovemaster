import React from 'react';

export const Button = ({
    children,
    variant = 'primary',
    size = 'medium',
    loading = false,
    className = '',
    disabled,
    onClick,
    type = 'button',
    style = {},
    ...props
}) => {
    const [isHovered, setIsHovered] = React.useState(false);

    const getVariantStyles = () => {
        switch (variant) {
            case 'secondary':
                return {
                    backgroundColor: isHovered ? '#F3F4F6' : '#fff',
                    color: '#111827',
                    border: '1px solid #E5E7EB'
                };
            case 'ghost':
                return {
                    backgroundColor: isHovered ? '#F3F4F6' : 'transparent',
                    color: '#111827',
                    border: 'none'
                };
            case 'danger':
                return {
                    backgroundColor: isHovered ? '#EF4444' : '#FEE2E2',
                    color: isHovered ? '#fff' : '#DC2626',
                    border: 'none'
                };
            default: // primary - pink gradient style
                return {
                    background: isHovered
                        ? 'linear-gradient(135deg, #F472B6 0%, #FDA4AF 100%)'
                        : 'linear-gradient(135deg, #FDA4AF 0%, #FECDD3 100%)',
                    color: '#fff',
                    border: 'none',
                    boxShadow: isHovered
                        ? '0 8px 24px rgba(253, 164, 175, 0.4)'
                        : '0 4px 16px rgba(253, 164, 175, 0.3)'
                };
        }
    };

    const getSizeStyles = () => {
        switch (size) {
            case 'small':
                return { padding: '8px 16px', fontSize: '13px', borderRadius: '10px' };
            case 'large':
                return { padding: '16px 32px', fontSize: '16px', borderRadius: '16px' };
            default:
                return { padding: '12px 24px', fontSize: '14px', borderRadius: '12px' };
        }
    };

    const baseStyle = {
        fontFamily: "'Inter', -apple-system, sans-serif",
        fontWeight: 600,
        cursor: disabled || loading ? 'not-allowed' : 'pointer',
        opacity: disabled || loading ? 0.5 : 1,
        transition: 'all 0.2s ease',
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        gap: '8px',
        ...getVariantStyles(),
        ...getSizeStyles(),
        ...style
    };

    return (
        <button
            className={className}
            style={baseStyle}
            disabled={disabled || loading}
            onClick={onClick}
            type={type}
            onMouseEnter={() => setIsHovered(true)}
            onMouseLeave={() => setIsHovered(false)}
            {...props}
        >
            {loading && (
                <span style={{
                    width: '16px',
                    height: '16px',
                    border: '2px solid currentColor',
                    borderTopColor: 'transparent',
                    borderRadius: '50%',
                    animation: 'spin 0.8s linear infinite'
                }} />
            )}
            {children}
        </button>
    );
};
