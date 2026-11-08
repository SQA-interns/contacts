import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it } from 'vitest';
import { AuthFailure } from '@/auth/client';
import { setLanguage } from '@/i18n';
import { fakeAuthClient } from '@/test/fakeAuth';
import { renderApp } from '@/test/renderApp';

async function askReset(email: string) {
  await userEvent.type(await screen.findByLabelText(/E-pošta/), email);
  await userEvent.click(screen.getByRole('button', { name: 'Pošlji povezavo' }));
}

describe('ResetPasswordPage WF-03 (US-001)', () => {
  beforeEach(async () => {
    await setLanguage('sl');
  });

  it.each(['nobody@acme.com', 'ana@acme.com'])(
    'R-001-08 a reset for %s starts Firebase reset and shows the same confirmation (EX-001-08)',
    async (email) => {
      const { client } = fakeAuthClient(null);
      renderApp('/reset-password', client);

      await askReset(email);

      expect(client.resetPassword).toHaveBeenCalledWith(email);
      const status = await screen.findByRole('status');
      expect(status).toHaveTextContent(
        'Preverite svojo e-pošto.Če je naslov registriran, smo nanj poslali povezavo za ponastavitev gesla.',
      );
      expect(screen.queryByRole('alert')).not.toBeInTheDocument();
      expect(screen.getByRole('link', { name: 'Nazaj na prijavo' })).toHaveAttribute('href', '/sign-in');
    },
  );

  it('an address that is not an e-mail is refused at the field', async () => {
    const { client } = fakeAuthClient(null);
    renderApp('/reset-password', client);

    await askReset('ana@');

    expect(screen.getByLabelText(/E-pošta/)).toHaveAccessibleDescription('E-poštni naslov ni veljaven.');
    expect(client.resetPassword).not.toHaveBeenCalled();
  });

  it('an address Firebase finds invalid is shown at the field', async () => {
    const { client } = fakeAuthClient(null);
    client.resetPassword.mockRejectedValue(new AuthFailure('invalid_email'));
    renderApp('/reset-password', client);

    await askReset('ana@acme.com');

    expect(await screen.findByText('E-poštni naslov ni veljaven.')).toBeInTheDocument();
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });

  it('shows the generic error when Firebase cannot be reached', async () => {
    const { client } = fakeAuthClient(null);
    client.resetPassword.mockRejectedValue(new AuthFailure('unavailable'));
    renderApp('/reset-password', client);

    await askReset('ana@acme.com');

    expect(await screen.findByRole('alert')).toHaveTextContent('Nekaj je šlo narobe. Poskusite znova.');
  });
});
