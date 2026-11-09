import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { App } from './app/App';
import './i18n';
import './styles/index.css';
import { applyTheme, FINAL_THEME } from './styles/theme';

applyTheme(FINAL_THEME);
const root = document.getElementById('root');
if (!root) throw new Error('root element missing');
createRoot(root).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
