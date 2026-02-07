import React from 'react';
import { Circle, CheckCircle2, Loader2 } from 'lucide-react';
import styles from './ManusPanel.module.css';

const ManusTasks = ({ tasks }) => {
    if (tasks.length === 0) {
        return null;
    }

    const getTaskIcon = (status) => {
        switch (status) {
            case 'completed':
                return <CheckCircle2 size={16} className={`${styles.taskIcon} ${styles.taskIcon.completed}`} />;
            case 'active':
                return <Loader2 size={16} className={`${styles.taskIcon} ${styles.taskIcon.active}`} />;
            case 'pending':
            default:
                return <Circle size={16} className={styles.taskIcon} style={{ opacity: 0.4 }} />;
        }
    };

    const completedCount = tasks.filter(t => t.status === 'completed').length;
    const activeCount = tasks.filter(t => t.status === 'active').length;

    return (
        <div className={styles.taskProgress}>
            <div className={styles.progressHeader}>
                <span>任务进度</span>
                <span className={styles.progressCount}>
                    {completedCount}/{tasks.length} 已完成
                    {activeCount > 0 && ` · ${activeCount} 进行中`}
                </span>
            </div>
            <div className={styles.taskList}>
                {tasks.map((task, index) => (
                    <div key={index} className={`${styles.taskItem} ${task.status}`}>
                        {getTaskIcon(task.status)}
                        <span>{task.name}</span>
                    </div>
                ))}
            </div>
        </div>
    );
};

export default ManusTasks;
