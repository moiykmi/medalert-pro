const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8083';
const ADMIN_BASE_URL = import.meta.env.VITE_ADMIN_BASE_URL ?? 'http://localhost:8082';

export interface Sesion {
  token: string;
  pacienteId: number;
  nombre: string;
}

export interface Cita {
  id: number;
  pacienteId: number;
  profesionalId: number;
  fechaHora: string;
  estado: 'AGENDADA' | 'CANCELADA' | 'REAGENDADA' | 'ATENDIDA' | 'NO_ASISTIO';
}

export interface Notificacion {
  id: number;
  eventoId: number;
  pacienteId: number;
  citaId: number | null;
  canal: 'SMS' | 'WHATSAPP' | 'EMAIL';
  intentoNumero: number;
  estadoEnvio: 'PENDIENTE' | 'ENVIADO' | 'FALLIDO' | 'LEIDO' | 'CONFIRMADO' | 'SIN_RESPUESTA';
  enviadoEn: string | null;
  confirmadoEn: string | null;
}

export interface AdminDashboardKpis {
  generadoEn: string;
  deliveryRate: {
    totalNotificaciones: number;
    entregasExitosas: number;
    porcentajeExito: number;
  };
  contactEffectiveness: {
    totalContactosEfectivos: number;
    primerIntento: number;
    trasEscalamiento: number;
    porcentajePrimerIntento: number;
    porcentajeTrasEscalamiento: number;
  };
  notificationTime: {
    totalEventos: number;
    promedioMinutos: number;
    maximoMinutos: number;
  };
  portalReschedule: {
    totalCitasCanceladas: number;
    citasReagendadas: number;
    porcentajeReagendamiento: number;
  };
  contactUpdate: {
    totalPacientes: number;
    pacientesActualizados: number;
    porcentajeActualizados: number;
  };
  channelStatusDistribution: Array<{
    canal: 'SMS' | 'WHATSAPP' | 'EMAIL' | string;
    estado: 'confirmado' | 'pendiente' | 'reintentando' | 'sin_respuesta' | string;
    total: number;
  }>;
  weeklyCancellationHistory: Array<{
    semanaInicio: string;
    totalEventos: number;
  }>;
}

export interface AdminDashboardEvent {
  eventoId: number;
  fechaEvento: string;
  motivo: string | null;
  pacientesNotificados: number;
  notificacionesConfirmadas: number;
  minutosTotalesNotificacion: number | null;
  ultimoHito: string | null;
}

class ApiError extends Error {
  status: number;
  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

async function request<T>(path: string, options: RequestInit = {}, token?: string): Promise<T> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string> | undefined),
  };
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const response = await fetch(`${BASE_URL}${path}`, { ...options, headers });

  if (!response.ok) {
    let mensaje = `Error ${response.status}`;
    try {
      const cuerpo = await response.json();
      mensaje = cuerpo?.message || cuerpo?.error || mensaje;
    } catch {
      // el cuerpo no era JSON, se deja el mensaje genérico
    }
    throw new ApiError(response.status, mensaje);
  }

  if (response.status === 200 && response.headers.get('content-length') === '0') {
    return undefined as T;
  }

  const text = await response.text();
  return text ? (JSON.parse(text) as T) : (undefined as T);
}

async function requestAdmin<T>(path: string, adminToken: string): Promise<T> {
  const response = await fetch(`${ADMIN_BASE_URL}${path}`, {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
      'X-Admin-Token': adminToken,
    },
  });

  if (!response.ok) {
    let mensaje = `Error ${response.status}`;
    try {
      const cuerpo = await response.json();
      mensaje = cuerpo?.message || cuerpo?.error || mensaje;
    } catch {
      // el cuerpo no era JSON, se deja el mensaje genérico
    }
    throw new ApiError(response.status, mensaje);
  }

  const text = await response.text();
  return text ? (JSON.parse(text) as T) : (undefined as T);
}

export const api = {
  solicitarOtp: (rut: string) =>
      request<void>('/auth/solicitar-otp', { method: 'POST', body: JSON.stringify({ rut }) }),

  verificarOtp: (rut: string, codigo: string) =>
      request<Sesion>('/auth/verificar-otp', { method: 'POST', body: JSON.stringify({ rut, codigo }) }),

  misCitas: (token: string) => request<Cita[]>('/citas', {}, token),

  misNotificaciones: (token: string) => request<Notificacion[]>('/notificaciones', {}, token),

  confirmarNotificacion: (id: number, token: string) =>
      request<Notificacion>(`/notificaciones/${id}/confirmar`, { method: 'POST' }, token),

  adminDashboardKpis: (adminToken: string) =>
      requestAdmin<AdminDashboardKpis>('/admin/dashboard/kpis', adminToken),

  adminDashboardEventos: (adminToken: string, limite = 20) =>
      requestAdmin<AdminDashboardEvent[]>(`/admin/dashboard/eventos-recientes?limite=${limite}`, adminToken),
};

export { ApiError };