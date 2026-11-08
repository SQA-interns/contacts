import { useTranslation } from 'react-i18next';
import { Button } from '@/components/ui/button';
import { LANGUAGES, isLanguage, setLanguage } from '@/i18n';

/** Language switch of the header (US-019): every text changes at once, form state is kept by React. */
export function LanguageSwitch() {
  const { t, i18n } = useTranslation();
  return (
    <div role="group" aria-label={t('language.label')} className="flex gap-1">
      {LANGUAGES.map((language) => (
        <Button
          key={language}
          size="sm"
          variant={i18n.language === language ? 'default' : 'ghost'}
          aria-pressed={i18n.language === language}
          lang={language}
          onClick={() => isLanguage(language) && void setLanguage(language)}
        >
          {language.toUpperCase()}
        </Button>
      ))}
    </div>
  );
}
