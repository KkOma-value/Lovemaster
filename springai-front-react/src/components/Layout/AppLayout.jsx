import React from 'react';

const AppLayout = ({ children }) => {
    return (
        <div style={{ minHeight: '100vh' }}>
            {children}
        </div>
    );
};

export default AppLayout;
