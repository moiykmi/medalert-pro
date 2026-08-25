import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { api, ApiError } from './client';

function jsonResponse(body: unknown, init: { status?: number; headers?: Record<string, string> } = {}) {
  const status = init.status ?? 200;
  const text = JSON.stringify(body);
  return {
    ok: status >= 200 && status < 300,
    status,
    headers: {
      get: (name: string) => {
        if (name === 'content-length') return init.headers?.['content-length'] ?? String(text.length);
        return init.headers?.[name] ?? null;
      },
    },
    json: async () => body,
    text: async () => text,
  } as unknown as Response;
}

function emptyResponse(status = 200) {
  return {
    ok: status >= 200 && status < 300,
    status,
    headers: { get: (name: string) => (name === 'content-length' ? '0' : null) },
    json: async () => {
      throw new Error('no body');
    },
    text: async () => '',
  } as unknown as Response;
}

describe('api client', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn());
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('solicitarOtp POSTs the RUT to the auth endpoint', async () => {
    (fetch as ReturnType<typeof vi.fn>).mockResolvedValue(emptyResponse(200));

    await api.solicitarOtp('12345678-5');

    expect(fetch).toHaveBeenCalledWith('http://localhost:8083/auth/solicitar-otp', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ rut: '12345678-5' }),
    });
  });

  it('verificarOtp POSTs rut and codigo and returns the parsed session', async () => {
    const sesion = { token: 'abc', pacienteId: 1, nombre: 'Juana' };
    (fetch as ReturnType<typeof vi.fn>).mockResolvedValue(jsonResponse(sesion));

    const result = await api.verificarOtp('12345678-5', '123456');

    expect(fetch).toHaveBeenCalledWith('http://localhost:8083/auth/verificar-otp', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ rut: '12345678-5', codigo: '123456' }),
    });
    expect(result).toEqual(sesion);
  });

  it('misCitas sends the bearer token and GETs /citas', async () => {
    const citas = [{ id: 1, pacienteId: 1, profesionalId: 2, fechaHora: '2026-01-01', estado: 'AGENDADA' }];
    (fetch as ReturnType<typeof vi.fn>).mockResolvedValue(jsonResponse(citas));

    const result = await api.misCitas('tok-1');

    expect(fetch).toHaveBeenCalledWith('http://localhost:8083/citas', {
      headers: { 'Content-Type': 'application/json', Authorization: 'Bearer tok-1' },
    });
    expect(result).toEqual(citas);
  });

  it('misNotificaciones GETs /notificaciones with the token', async () => {
    (fetch as ReturnType<typeof vi.fn>).mockResolvedValue(jsonResponse([]));

    await api.misNotificaciones('tok-2');

    expect(fetch).toHaveBeenCalledWith('http://localhost:8083/notificaciones', {
      headers: { 'Content-Type': 'application/json', Authorization: 'Bearer tok-2' },
    });
  });

  it('confirmarNotificacion POSTs to the id-scoped confirm endpoint', async () => {
    const notif = { id: 5, eventoId: 1, pacienteId: 1, citaId: null, canal: 'SMS', intentoNumero: 1, estadoEnvio: 'CONFIRMADO', enviadoEn: null, confirmadoEn: null };
    (fetch as ReturnType<typeof vi.fn>).mockResolvedValue(jsonResponse(notif));

    const result = await api.confirmarNotificacion(5, 'tok-3');

    expect(fetch).toHaveBeenCalledWith('http://localhost:8083/notificaciones/5/confirmar', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: 'Bearer tok-3' },
    });
    expect(result).toEqual(notif);
  });

  it('adminDashboardKpis GETs the admin base URL with X-Admin-Token', async () => {
    const kpis = { generadoEn: '2026-01-01' };
    (fetch as ReturnType<typeof vi.fn>).mockResolvedValue(jsonResponse(kpis));

    const result = await api.adminDashboardKpis('admin-tok');

    expect(fetch).toHaveBeenCalledWith('http://localhost:8082/admin/dashboard/kpis', {
      method: 'GET',
      headers: { 'Content-Type': 'application/json', 'X-Admin-Token': 'admin-tok' },
    });
    expect(result).toEqual(kpis);
  });

  it('adminDashboardEventos defaults limite to 20 and forwards a custom limite', async () => {
    (fetch as ReturnType<typeof vi.fn>).mockResolvedValue(jsonResponse([]));

    await api.adminDashboardEventos('admin-tok');
    expect(fetch).toHaveBeenCalledWith(
      'http://localhost:8082/admin/dashboard/eventos-recientes?limite=20',
      expect.objectContaining({ method: 'GET' })
    );

    await api.adminDashboardEventos('admin-tok', 5);
    expect(fetch).toHaveBeenCalledWith(
      'http://localhost:8082/admin/dashboard/eventos-recientes?limite=5',
      expect.objectContaining({ method: 'GET' })
    );
  });

  it('throws an ApiError carrying the status and server message on non-OK responses', async () => {
    (fetch as ReturnType<typeof vi.fn>).mockResolvedValue(jsonResponse({ message: 'RUT no encontrado' }, { status: 404 }));

    await expect(api.solicitarOtp('12345678-5')).rejects.toMatchObject({
      status: 404,
      message: 'RUT no encontrado',
    });
    await expect(api.solicitarOtp('12345678-5')).rejects.toBeInstanceOf(ApiError);
  });

  it('falls back to a generic message when the error body is not JSON', async () => {
    const response = {
      ok: false,
      status: 500,
      headers: { get: () => null },
      json: async () => {
        throw new Error('not json');
      },
      text: async () => '',
    } as unknown as Response;
    (fetch as ReturnType<typeof vi.fn>).mockResolvedValue(response);

    await expect(api.solicitarOtp('12345678-5')).rejects.toMatchObject({
      status: 500,
      message: 'Error 500',
    });
  });

  it('returns undefined for a 200 response with content-length 0 without parsing the body', async () => {
    (fetch as ReturnType<typeof vi.fn>).mockResolvedValue(emptyResponse(200));

    const result = await api.solicitarOtp('12345678-5');

    expect(result).toBeUndefined();
  });
});
