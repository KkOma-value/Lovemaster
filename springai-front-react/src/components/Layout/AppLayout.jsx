import React from 'react';
import StarryBackground from '../ParticleBackground/ThreeStarryBackground';

const AppLayout = ({ children }) => {
    return (
        <div style={{
            position: 'relative',
            minHeight: '100vh',
            width: '100%',
            color: 'var(--text-primary)',
            overflow: 'hidden'
        }}>
            {/* Background Layer */}
            <StarryBackground />

            {/* Content Layer - must be above background */}
            <div style={{
                position: 'relative',
                zIndex: 10,
                width: '100%',
                minHeight: '100vh'
            }}>
                {children}
            </div>
        </div>
    );
};

export default AppLayout;
