import React from 'react';

const AppLayout = ({ children }) => {
    return (
        <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
            {children}
        </div>
    );
};

export default AppLayout;
