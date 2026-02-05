import React from 'react';
import { Loader2, Search, Brain, Sparkles, CheckCircle, AlertCircle, Wrench } from 'lucide-react';

/**
 * Streaming Status Component - Bento Grid Style
 */
const StreamingStatus = ({ type, content, isVisible = true }) => {
    if (!isVisible) return null;

    const getStatusConfig = () => {
        const baseStyle = {
            display: 'flex',
            alignItems: 'center',
            gap: '10px',
            padding: '8px 14px',
            borderRadius: '12px',
            fontSize: '14px',
            fontWeight: 500,
            fontFamily: "'Inter', -apple-system, sans-serif"
        };

        switch (type) {
            case 'thinking':
                return {
                    icon: <Brain size={16} color="#8B5CF6" style={{ animation: 'pulse 1s infinite' }} />,
                    label: content || 'Thinking...',
                    style: { ...baseStyle, backgroundColor: '#F3E8FF', color: '#7C3AED' }
                };
            case 'status':
                if (content?.includes('搜索') || content?.includes('查找')) {
                    return {
                        icon: <Search size={16} color="#3B82F6" />,
                        label: content,
                        style: { ...baseStyle, backgroundColor: '#DBEAFE', color: '#2563EB' }
                    };
                }
                if (content?.includes('分析') || content?.includes('处理')) {
                    return {
                        icon: <Brain size={16} color="#EC4899" style={{ animation: 'pulse 1s infinite' }} />,
                        label: content,
                        style: { ...baseStyle, backgroundColor: '#FCE7F3', color: '#DB2777' }
                    };
                }
                if (content?.includes('工具') || content?.includes('调用')) {
                    return {
                        icon: <Wrench size={16} color="#F59E0B" />,
                        label: content,
                        style: { ...baseStyle, backgroundColor: '#FEF3C7', color: '#D97706' }
                    };
                }
                return {
                    icon: <Loader2 size={16} color="#6B7280" style={{ animation: 'spin 1s linear infinite' }} />,
                    label: content || 'Processing...',
                    style: { ...baseStyle, backgroundColor: '#F3F4F6', color: '#374151' }
                };
            case 'tool_call':
                return {
                    icon: <Wrench size={16} color="#F59E0B" style={{ animation: 'pulse 1s infinite' }} />,
                    label: content || 'Calling Tool...',
                    style: { ...baseStyle, backgroundColor: '#FEF3C7', color: '#D97706' }
                };
            case 'done':
                return {
                    icon: <CheckCircle size={16} color="#10B981" />,
                    label: 'Done',
                    style: { ...baseStyle, backgroundColor: '#D1FAE5', color: '#059669' }
                };
            case 'error':
                return {
                    icon: <AlertCircle size={16} color="#EF4444" />,
                    label: content || 'Error',
                    style: { ...baseStyle, backgroundColor: '#FEE2E2', color: '#DC2626' }
                };
            default:
                return {
                    icon: <Sparkles size={16} color="#FDA4AF" style={{ animation: 'pulse 1s infinite' }} />,
                    label: content || 'Processing...',
                    style: { ...baseStyle, backgroundColor: '#FFF1F2', color: '#BE123C' }
                };
        }
    };

    const config = getStatusConfig();

    return (
        <div style={config.style}>
            {config.icon}
            <span>{config.label}</span>
        </div>
    );
};

export default StreamingStatus;
