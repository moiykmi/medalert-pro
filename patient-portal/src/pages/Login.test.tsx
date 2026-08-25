import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { Login } from './Login';
import { api, ApiError } from '../api/client';

vi.mock('../api/client', async () => {
  const actual = await vi.importActual<typeof import('../api/client')>('../api/client');
  return {
    ...actual,
    api: {
      solicitarOtp: vi.fn(),
      verificarOtp: vi.fn(),
    },
  };
});

describe('Login', () => {
  beforeEach(() => {
    vi.mocked(api.solicitarOtp).mockReset();
    vi.mocked(api.verificarOtp).mockReset();
  });

  it('requests an OTP for the entered RUT and advances to the code step', async () => {
    const user = userEvent.setup();
    vi.mocked(api.solicitarOtp).mockResolvedValue(undefined);

    render(<Login onIngreso={vi.fn()} />);

    await user.type(screen.getByLabelText('Tu RUT'), '123456785');
    await user.click(screen.getByRole('button', { name: /enviar código/i }));

    await waitFor(() => expect(api.solicitarOtp).toHaveBeenCalledWith('12345678-5'));
    expect(await screen.findByLabelText('Código de verificación')).toBeInTheDocument();
  });

  it('shows a not-found message when solicitarOtp rejects with a 404 ApiError', async () => {
    const user = userEvent.setup();
    vi.mocked(api.solicitarOtp).mockRejectedValue(new ApiError(404, 'no encontrado'));

    render(<Login onIngreso={vi.fn()} />);

    await user.type(screen.getByLabelText('Tu RUT'), '123456785');
    await user.click(screen.getByRole('button', { name: /enviar código/i }));

    expect(await screen.findByRole('alert')).toHaveTextContent(/no encontramos ese rut/i);
  });

  it('verifies the code and calls onIngreso with the returned session', async () => {
    const user = userEvent.setup();
    const onIngreso = vi.fn();
    const sesion = { token: 'tok', pacienteId: 1, nombre: 'Juana' };
    vi.mocked(api.solicitarOtp).mockResolvedValue(undefined);
    vi.mocked(api.verificarOtp).mockResolvedValue(sesion);

    render(<Login onIngreso={onIngreso} />);

    await user.type(screen.getByLabelText('Tu RUT'), '123456785');
    await user.click(screen.getByRole('button', { name: /enviar código/i }));
    await screen.findByLabelText('Código de verificación');

    await user.type(screen.getByLabelText('Código de verificación'), '654321');
    await user.click(screen.getByRole('button', { name: /^ingresar$/i }));

    await waitFor(() => expect(api.verificarOtp).toHaveBeenCalledWith('12345678-5', '654321'));
    expect(onIngreso).toHaveBeenCalledWith(sesion);
  });
});
