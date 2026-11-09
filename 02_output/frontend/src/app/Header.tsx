import { ChevronDown, CircleUserRound, Menu } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Link, NavLink, useNavigate } from 'react-router';
import { useAuth } from '@/auth/AuthProvider';
import { brand } from '@/brand';
import { LanguageSwitch } from '@/components/LanguageSwitch';
import { Button } from '@/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { DEFAULT_LANGUAGE, isLanguage, type Language } from '@/i18n';

/**
 * Header with logo placeholder, product name, navigation, language switch and user menu (UI-10, UI-23). Group
 * navigation needs a verified e-mail (R-001-04); signing out ends the session (R-001-10).
 */
export function Header() {
  const { t, i18n } = useTranslation();
  const { account, signOut } = useAuth();
  const navigate = useNavigate();
  const language: Language = isLanguage(i18n.language) ? i18n.language : DEFAULT_LANGUAGE;
  const links = account?.emailVerified ? [{ to: '/groups', label: t('nav.groups') }] : [];

  async function handleSignOut() {
    await signOut();
    await navigate('/sign-in', { replace: true });
  }

  return (
    <header className="border-b bg-surface">
      <div className="mx-auto flex max-w-[1200px] items-center justify-between gap-3 px-4 py-3 md:px-6">
        <Link to="/" className="flex items-center gap-3 font-semibold">
          <span
            aria-hidden="true"
            className="inline-flex size-10 items-center justify-center rounded-[8px] border border-primary bg-primary text-primary-foreground"
          >
            {brand.logoText}
          </span>
          <span>{brand.name[language]}</span>
        </Link>
        {links.length > 0 ? (
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
        ) : null}
        <div className="flex items-center gap-2">
          <LanguageSwitch />
          {account ? (
            <>
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button
                    variant="ghost"
                    className="hidden h-11 md:inline-flex"
                    aria-label={t('nav.userMenu')}
                  >
                    <CircleUserRound aria-hidden="true" />
                    <ChevronDown aria-hidden="true" />
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end">
                  <DropdownMenuLabel className="font-normal text-text-muted">
                    {account.email}
                  </DropdownMenuLabel>
                  <DropdownMenuSeparator />
                  {account.emailVerified ? (
                    <DropdownMenuItem asChild>
                      <Link to="/account">{t('nav.account')}</Link>
                    </DropdownMenuItem>
                  ) : null}
                  <DropdownMenuItem onSelect={() => void handleSignOut()}>
                    {t('nav.signOut')}
                  </DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button
                    variant="ghost"
                    size="icon"
                    className="size-11 md:hidden"
                    aria-label={t('nav.menu')}
                  >
                    <Menu aria-hidden="true" />
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end">
                  {links.map((link) => (
                    <DropdownMenuItem key={link.to} asChild>
                      <Link to={link.to}>{link.label}</Link>
                    </DropdownMenuItem>
                  ))}
                  {links.length > 0 ? <DropdownMenuSeparator /> : null}
                  {account.emailVerified ? (
                    <DropdownMenuItem asChild>
                      <Link to="/account">{t('nav.account')}</Link>
                    </DropdownMenuItem>
                  ) : null}
                  <DropdownMenuItem onSelect={() => void handleSignOut()}>
                    {t('nav.signOut')}
                  </DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
            </>
          ) : null}
        </div>
      </div>
    </header>
  );
}
