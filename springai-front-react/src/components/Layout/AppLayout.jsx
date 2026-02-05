import React from 'react';

const AppLayout = ({ children }) => {
    return (
        <div style={{
            minHeight: '100vh',
            backgroundColor: '#F3F4F6'
        }}>
            {children}
        </div>
    );
};

export default AppLayout;
