import { useTranslation } from 'react-i18next';
import { Outlet } from 'react-router';
import { Header } from './Header';

/** App shell: skip link, header, one `main` landmark, footer (UI-09, UI-19). */
export function Layout() {
  const { t } = useTranslation();
  return (
    <div className="flex min-h-screen flex-col">
      <a href="#content" className="sr-only focus:not-sr-only focus:p-3">
        {t('app.skipToContent')}
      </a>
      <Header />
      <main id="content" className="mx-auto w-full max-w-[1200px] flex-1 px-4 py-6 md:px-6">
        <Outlet />
      </main>
      <footer className="border-t bg-surface">
        <div className="mx-auto max-w-[1200px] px-4 py-4 text-sm text-text-muted md:px-6">
          {t('footer.text')}
        </div>
      </footer>
    </div>
  );
}
