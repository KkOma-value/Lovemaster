# Proposal: Frontend UI Replacements

## Status
Approved

## Overview
Based on user feedback, the frontend homepage UI will be updated to be cleaner, dropping the excessively long texts, with improved User dropdown formatting, and staggered entrance animation for cards with smooth hover liftoffs.

## Changes
1. **Homepage layout**: Use simpler texts matching Figure 2 requirements.
2. **User Dropdown**: Compress dimensions (e.g., width to w-48, padding adjusted) while retaining all functional items.
3. **Animations**: Use `framer-motion` to add stagger entrance (delay 0.05s between children) and hover up-shift (`translateY(-8px)` with shadow depth).
