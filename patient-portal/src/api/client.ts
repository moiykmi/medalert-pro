const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8083';
const ADMIN_BASE_URL = import.meta.env.VITE_ADMIN_BASE_URL ?? 'http://localhost:8082';
const FHIR_BASE_URL = import.meta.env.VITE_FHIR_BASE_URL ?? 'http://localhost:8081';

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

export interface Paciente {
  id: number;
  rut: string;
  nombre: string;
  telefono: string | null;
  email: string | null;
  canalPreferido: 'SMS' | 'WHATSAPP' | 'EMAIL';
  adultoMayor: boolean;
  datosActualizadosEn: string | null;
}

export interface ActualizarDatosRequest {
  telefono?: string;
  email?: string;
  canalPreferido?: 'SMS' | 'WHATSAPP' | 'EMAIL';
}

export interface Notificacion {
  id: number;
  eventoId: number | null; // null para recordatorios (ver tipo)
  pacienteId: number;
  citaId: number | null;
  tipo: 'CANCELACION' | 'RECORDATORIO_48H' | 'RECORDATORIO_24H';
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
  estado: string;
  profesionalId: number | null;
  profesionalNombre: string | null;
  profesionalEspecialidad: string | null;
  registradoPor: number | null;
  canales: string[];
  pacientesNotificados: number;
  notificacionesConfirmadas: number;
  minutosTotalesNotificacion: number | null;
  ultimoHito: string | null;
}

export interface RegistrarCancelacionRequest {
  profesionalId: number;
  fecha: string; // YYYY-MM-DD
  horaInicio?: string; // HH:mm, opcional — si se omite junto con horaFin, se cancela el día completo
  horaFin?: string; // HH:mm
  motivo?: string;
  registradoPor?: number;
}

export interface EventoCancelacion {
  id: number;
  profesionalId: number;
  fechaEvento: string;
  motivo: string | null;
  estado: string;
  registradoPor: number | null;
}

export interface Profesional {
  id: number;
  nombre: string;
  especialidad: string;
  citasAgendadas: number;
  email: string | null;
}

export interface CitaAgenda {
  id: number;
  fechaHora: string;
  estado: 'AGENDADA' | 'CANCELADA' | 'REAGENDADA' | 'ATENDIDA' | 'NO_ASISTIO';
  pacienteId: number;
  pacienteNombre: string;
  profesionalId: number;
  profesionalNombre: string;
  profesionalEspecialidad: string | null;
}

export interface ReporteMensual {
  periodo: string; // "2026-08"
  totalNotificaciones: number;
  porcentajeEntrega: number;
  reagendamientos: number;
  horasAhorradasEstimadas: number;
  horasAhorradasNotaMetodologica: string;
  tasaAusentismo: number;
  tasaAusentismoMesAnterior: number;
  notificacionesPorCanal: Array<{ canal: string; enviados: number; porcentajeEntregado: number }>;
  ausentismoEvolucion: Array<{ periodo: string; tasa: number }>;
  escalamientos: {
    smsAWhatsapp: number;
    whatsappAEmail: number;
    sinContactoDefinitivo: number;
    totalContactados: number;
  };
}

export interface BitacoraEntry {
  fecha: string;
  tipo: 'EVENTO_REGISTRADO' | 'NOTIFICACIONES_ENVIADAS' | 'ESCALAMIENTO' | 'RECORDATORIO' | 'REAGENDAMIENTO';
  titulo: string;
  detalle: string | null;
}

export interface Bitacora {
  fecha: string;
  entradas: BitacoraEntry[];
  erroresHoy: number;
}

export interface Configuracion {
  canalSmsHabilitado: boolean;
  canalWhatsappHabilitado: boolean;
  canalEmailHabilitado: boolean;
  recordatorio48hHabilitado: boolean;
  recordatorio24hHabilitado: boolean;
  escalacionMinutosEspera: number;
  escalacionMaxIntentos: number;
  actualizadoEn: string;
}

export interface AdminSesion {
  token: string;
  nombre: string;
  rol: string;
}

export interface MedicoSesion {
  token: string;
  profesionalId: number;
  nombre: string;
  especialidad: string;
}

export interface MedicoAgendaHoy {
  nombre: string;
  especialidad: string;
  citasHoy: number;
}

export interface ReportarAusenciaRequest {
  fecha: string; // YYYY-MM-DD
  horaInicio?: string; // HH:mm
  horaFin?: string; // HH:mm
  motivo?: string;
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

async function requestAdmin<T>(baseUrl: string, path: string, adminToken: string): Promise<T> {
  const response = await fetch(`${baseUrl}${path}`, {
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

async function requestAdminPost<T>(baseUrl: string, path: string, adminToken: string, body: unknown): Promise<T> {
  const response = await fetch(`${baseUrl}${path}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-Admin-Token': adminToken,
    },
    body: JSON.stringify(body),
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

async function requestFhir<T>(path: string, options: RequestInit = {}, token?: string): Promise<T> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string> | undefined),
  };
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const response = await fetch(`${FHIR_BASE_URL}${path}`, { ...options, headers });

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

async function requestAdminPut<T>(baseUrl: string, path: string, adminToken: string, body: unknown): Promise<T> {
  const response = await fetch(`${baseUrl}${path}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      'X-Admin-Token': adminToken,
    },
    body: JSON.stringify(body),
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

  if (response.status === 200 && response.headers.get('content-length') === '0') {
    return undefined as T;
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

  reagendarCita: (id: number, nuevaFechaHora: string, token: string) =>
      request<Cita>(`/citas/${id}/reagendar`, { method: 'POST', body: JSON.stringify({ nuevaFechaHora }) }, token),

  miPerfil: (token: string) => request<Paciente>('/paciente/perfil', {}, token),

  actualizarDatosContacto: (datos: ActualizarDatosRequest, token: string) =>
      request<Paciente>('/paciente/datos-contacto', { method: 'PUT', body: JSON.stringify(datos) }, token),

  adminDashboardKpis: (adminToken: string) =>
      requestAdmin<AdminDashboardKpis>(ADMIN_BASE_URL, '/admin/dashboard/kpis', adminToken),

  adminDashboardEventos: (adminToken: string, limite = 20) =>
      requestAdmin<AdminDashboardEvent[]>(ADMIN_BASE_URL, `/admin/dashboard/eventos-recientes?limite=${limite}`, adminToken),

  registrarCancelacion: (adminToken: string, datos: RegistrarCancelacionRequest) =>
      requestAdminPost<EventoCancelacion>(FHIR_BASE_URL, '/eventos/cancelacion', adminToken, datos),

  agendaDelDia: (adminToken: string, fecha: string) =>
      requestAdmin<CitaAgenda[]>(FHIR_BASE_URL, `/agenda?fecha=${fecha}`, adminToken),

  listarProfesionales: (adminToken: string, fecha: string, horaInicio?: string, horaFin?: string) => {
    const params = new URLSearchParams({ fecha });
    if (horaInicio) params.set('horaInicio', horaInicio);
    if (horaFin) params.set('horaFin', horaFin);
    return requestAdmin<Profesional[]>(FHIR_BASE_URL, `/profesionales?${params.toString()}`, adminToken);
  },

  listarPacientesAdmin: (adminToken: string) =>
      requestAdmin<Paciente[]>(BASE_URL, '/admin/pacientes', adminToken),

  historialNotificacionesPaciente: (adminToken: string, pacienteId: number) =>
      requestAdmin<Notificacion[]>(BASE_URL, `/admin/pacientes/${pacienteId}/notificaciones`, adminToken),

  actualizarDatosPacienteAdmin: (adminToken: string, pacienteId: number, datos: ActualizarDatosRequest) =>
      requestAdminPut<Paciente>(BASE_URL, `/admin/pacientes/${pacienteId}`, adminToken, datos),

  enviarNotificacionPrueba: (adminToken: string, pacienteId: number) =>
      requestAdminPost<Notificacion>(ADMIN_BASE_URL, `/admin/pacientes/${pacienteId}/notificacion-prueba`, adminToken, {}),

  reporteMensual: (adminToken: string, periodo: string) =>
      requestAdmin<ReporteMensual>(ADMIN_BASE_URL, `/admin/dashboard/reporte-mensual?periodo=${periodo}`, adminToken),

  adminLogin: (email: string, password: string) =>
      requestFhir<AdminSesion>('/admin/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) }),

  medicoLogin: (email: string, password: string) =>
      requestFhir<MedicoSesion>('/medico/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) }),

  medicoAgendaHoy: (token: string) => requestFhir<MedicoAgendaHoy>('/medico/agenda-hoy', {}, token),

  medicoReportarAusencia: (datos: ReportarAusenciaRequest, token: string) =>
      requestFhir<EventoCancelacion>('/medico/ausencia', { method: 'POST', body: JSON.stringify(datos) }, token),

  asignarCredencialesMedico: (adminToken: string, profesionalId: number, email: string, password: string) =>
      requestAdminPut<void>(FHIR_BASE_URL, `/profesionales/${profesionalId}/credenciales`, adminToken, { email, password }),

  obtenerConfiguracion: (adminToken: string) =>
      requestAdmin<Configuracion>(ADMIN_BASE_URL, '/admin/configuracion', adminToken),

  actualizarConfiguracion: (adminToken: string, config: Omit<Configuracion, 'actualizadoEn'>) =>
      requestAdminPut<Configuracion>(ADMIN_BASE_URL, '/admin/configuracion', adminToken, config),

  obtenerBitacora: (adminToken: string, fecha: string) =>
      requestAdmin<Bitacora>(ADMIN_BASE_URL, `/admin/bitacora?fecha=${fecha}`, adminToken),
};

export { ApiError };