import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { errorMessage } from '@/api/errors';
import { exportContacts, type ExportSelection } from '@/api/exports';
import { Button } from '@/components/ui/button';
import { isLanguage } from '@/i18n';

interface ExportButtonProps {
  groupId: string;
  /** The number of selected Contacts; with 0 the button is disabled (R-011-01). */
  count: number;
  /** What to send: the ids or the filter of the Selection. */
  getSelection: () => Promise<ExportSelection>;
}

/** Saves the file in the browser under the name the server gave it (R-011-04). */
function save(blob: Blob, fileName: string) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  link.click();
  URL.revokeObjectURL(url);
}

/** WF-06: "Export to Excel" of the Selection bar with its running and refused states (US-011). */
export function ExportButton({ groupId, count, getSelection }: ExportButtonProps) {
  const { t, i18n } = useTranslation();
  const [running, setRunning] = useState(false);
  const [failure, setFailure] = useState<unknown>(null);

  const run = async () => {
    setRunning(true);
    setFailure(null);
    try {
      const lang = isLanguage(i18n.language) ? i18n.language : 'sl';
      const file = await exportContacts(groupId, await getSelection(), lang);
      save(file.blob, file.fileName);
    } catch (error) {
      setFailure(error);
    } finally {
      setRunning(false);
    }
  };

  return (
    <>
      <Button
        variant="outline"
        className="ml-auto"
        disabled={count === 0 || running}
        aria-busy={running || undefined}
        onClick={() => void run()}
      >
        {running ? t('export.running') : t('export.button')}
      </Button>
      {failure ? (
        <p role="alert" className="basis-full font-semibold text-danger">
          {errorMessage(t, failure)}
        </p>
      ) : null}
    </>
  );
}
