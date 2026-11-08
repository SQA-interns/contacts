import { Menu } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Link, NavLink } from 'react-router';
import { brand } from '@/brand';
import { LanguageSwitch } from '@/components/LanguageSwitch';
import { Button } from '@/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { DEFAULT_LANGUAGE, isLanguage, type Language } from '@/i18n';

/** Header with logo placeholder, product name, navigation and language switch (UI-10, UI-23). */
export function Header() {
  const { t, i18n } = useTranslation();
  const language: Language = isLanguage(i18n.language) ? i18n.language : DEFAULT_LANGUAGE;
  const name = brand.name[language];
  const links = [{ to: '/groups', label: t('nav.groups') }];
  return (
    <header className="border-b bg-surface">
      <div className="mx-auto flex max-w-[1200px] items-center justify-between gap-3 px-4 py-3 md:px-6">
        <Link to="/" className="flex items-center gap-3 font-semibold">
          <span
            aria-hidden="true"
            className="inline-flex size-10 items-center justify-center rounded-[8px] border bg-surface-raised"
          >
            {brand.logoText}
          </span>
          <span>{name}</span>
        </Link>
        <nav aria-label={t('nav.main')} className="hidden items-center gap-4 md:flex">
          {links.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              className="min-h-11 content-center px-2 aria-[current=page]:font-semibold"
            >
              {link.label}
            </NavLink>
          ))}
        </nav>
        <div className="flex items-center gap-2">
          <LanguageSwitch />
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="icon" className="md:hidden" aria-label={t('nav.menu')}>
                <Menu aria-hidden="true" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              {links.map((link) => (
                <DropdownMenuItem key={link.to} asChild>
                  <Link to={link.to}>{link.label}</Link>
                </DropdownMenuItem>
              ))}
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </div>
    </header>
  );
}
