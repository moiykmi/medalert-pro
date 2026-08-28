import { FormEvent, useEffect, useMemo, useState } from 'react';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { AdminDashboardEvent, AdminDashboardKpis, api, ApiError, Bitacora, CitaAgenda, Configuracion, Notificacion, Paciente, Profesional, ReporteMensual } from '../api/client';
import './AdminDashboard.css';

const STORAGE_KEY = 'medalert_admin_token';
const STORAGE_KEY_ROL = 'medalert_admin_rol';
const ROL_SUPERUSER = 'SUPERUSER';
const ROLES_CON_REPORTES = [ROL_SUPERUSER, 'ADMIN'];
const POLLING_MS = 20_000;

type Tab = 'dashboard' | 'agenda' | 'pacientes' | 'notificaciones' | 'reportes' | 'configuracion' | 'bitacora';

function hoyISO(): string {
  return new Date().toISOString().slice(0, 10);
}

function mesActualISO(): string {
  return hoyISO().slice(0, 7);
}

function ultimosMeses(cantidad: number): string[] {
  const meses: string[] = [];
  const base = new Date();
  for (let i = 0; i < cantidad; i++) {
    const d = new Date(base.getFullYear(), base.getMonth() - i, 1);
    meses.push(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`);
  }
  return meses;
}

function formatPeriodo(periodo: string): string {
  const [anio, mes] = periodo.split('-').map(Number);
  return new Date(anio, mes - 1, 1).toLocaleDateString('es-CL', { month: 'long', year: 'numeric' });
}

function formatMesCorto(periodo: string): string {
  const [anio, mes] = periodo.split('-').map(Number);
  return new Date(anio, mes - 1, 1).toLocaleDateString('es-CL', { month: 'short' });
}

type EstadoCanal = 'confirmado' | 'pendiente' | 'reintentando' | 'sin_respuesta';
const ESTADOS: EstadoCanal[] = ['confirmado', 'pendiente', 'reintentando', 'sin_respuesta'];
const ESTADO_LABEL: Record<EstadoCanal, string> = {
  confirmado: 'Confirmado',
  pendiente: 'Pendiente',
  reintentando: 'Reintentando',
  sin_respuesta: 'Sin respuesta',
};

function formatDate(iso: string | null): string {
  if (!iso) return 'Sin datos';
  const date = new Date(iso);
  return date.toLocaleString('es-CL', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function formatWeek(isoDate: string): string {
  const date = new Date(`${isoDate}T00:00:00`);
  return date.toLocaleDateString('es-CL', { day: '2-digit', month: '2-digit' });
}

function pctColorClass(value: number, goal: number, inverse = false): string {
  const ok = inverse ? value <= goal : value >= goal;
  return ok ? 'kv ok' : 'kv warn';
}

function iniciales(nombre: string): string {
  const partes = nombre.trim().split(/\s+/);
  return ((partes[0]?.[0] ?? '') + (partes[1]?.[0] ?? '')).toUpperCase();
}

const ESTILO_TIPO_BITACORA: Record<string, { bg: string; color: string; icono: string }> = {
  EVENTO_REGISTRADO: { bg: '#FFE4E6', color: '#BE123C', icono: 'ti-calendar-x' },
  NOTIFICACIONES_ENVIADAS: { bg: '#DBEAFE', color: '#1D4ED8', icono: 'ti-send' },
  ESCALAMIENTO: { bg: '#FEF3C7', color: '#92400E', icono: 'ti-arrows-right' },
  RECORDATORIO: { bg: '#D1FAE5', color: '#065F46', icono: 'ti-bell' },
  REAGENDAMIENTO: { bg: '#EDE9FE', color: '#5B21B6', icono: 'ti-calendar-plus' },
};

const CANAL_ICONO: Record<string, string> = {
  SMS: 'ti-device-mobile',
  WHATSAPP: 'ti-brand-whatsapp',
  EMAIL: 'ti-mail',
};

const TIPO_NOTIF_LABEL: Record<string, string> = {
  CANCELACION: 'Aviso de cancelación',
  RECORDATORIO_48H: 'Recordatorio 48h',
  RECORDATORIO_24H: 'Recordatorio 24h',
  PRUEBA: 'Mensaje de prueba',
};

const ESTADO_ENVIO_CHIP: Record<string, string> = {
  CONFIRMADO: 'ok',
  LEIDO: 'ok',
  ENVIADO: 'off',
  PENDIENTE: 'off',
  FALLIDO: 'warn',
  SIN_RESPUESTA: 'warn',
};

const ESTADO_ENVIO_LABEL: Record<string, string> = {
  CONFIRMADO: 'Confirmado',
  LEIDO: 'Leído',
  ENVIADO: 'Enviado',
  PENDIENTE: 'Pendiente',
  FALLIDO: 'Fallido',
  SIN_RESPUESTA: 'Sin respuesta',
};

// Estado real de entrega reportado por Twilio (webhook) — solo SMS/WhatsApp.
const ESTADO_ENTREGA_LABEL: Record<string, string> = {
  queued: 'En cola',
  sent: 'Enviado al operador',
  delivered: 'Entregado',
  undelivered: 'No entregado',
  failed: 'Falló la entrega',
  read: 'Leído',
};

const ESTADO_ENTREGA_COLOR: Record<string, string> = {
  queued: '#94A3B8',
  sent: '#94A3B8',
  delivered: '#059669',
  undelivered: '#BE123C',
  failed: '#BE123C',
  read: '#059669',
};

const ESTADO_CITA_DOT: Record<string, string> = {
  AGENDADA: '#10B981',
  ATENDIDA: '#10B981',
  CANCELADA: '#EF4444',
  NO_ASISTIO: '#EF4444',
  REAGENDADA: '#7C3AED',
};

const ESTADO_CITA_CHIP: Record<string, string> = {
  AGENDADA: 'ok',
  ATENDIDA: 'ok',
  CANCELADA: 'warn',
  NO_ASISTIO: 'warn',
  REAGENDADA: 'off',
};

const ESTADO_CITA_LABEL: Record<string, string> = {
  AGENDADA: 'Agendada',
  ATENDIDA: 'Atendida',
  CANCELADA: 'Cancelada',
  NO_ASISTIO: 'No asistió',
  REAGENDADA: 'Reagendada',
};

function formatHora(iso: string): string {
  return new Date(iso).toLocaleTimeString('es-CL', { hour: '2-digit', minute: '2-digit' });
}

export function AdminDashboard() {
  const [adminToken, setAdminToken] = useState(() => localStorage.getItem(STORAGE_KEY) ?? '');
  const [rol, setRol] = useState(() => localStorage.getItem(STORAGE_KEY_ROL) ?? ROL_SUPERUSER);
  const [draftToken, setDraftToken] = useState('');
  const [modoLogin, setModoLogin] = useState<'token' | 'individual'>('token');
  const [emailLogin, setEmailLogin] = useState('');
  const [passwordLogin, setPasswordLogin] = useState('');
  const [ingresandoLogin, setIngresandoLogin] = useState(false);
  const [tab, setTab] = useState<Tab>('dashboard');
  const [kpis, setKpis] = useState<AdminDashboardKpis | null>(null);
  const [eventos, setEventos] = useState<AdminDashboardEvent[]>([]);
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [profesionalId, setProfesionalId] = useState('');
  const [fechaCancelacion, setFechaCancelacion] = useState(hoyISO);
  const [horaInicioCancelacion, setHoraInicioCancelacion] = useState('');
  const [horaFinCancelacion, setHoraFinCancelacion] = useState('');
  const [motivo, setMotivo] = useState('');
  const [registradoPor, setRegistradoPor] = useState('');
  const [enviandoCancelacion, setEnviandoCancelacion] = useState(false);
  const [mensajeCancelacion, setMensajeCancelacion] = useState<string | null>(null);
  const [errorCancelacion, setErrorCancelacion] = useState<string | null>(null);
  const [profesionales, setProfesionales] = useState<Profesional[]>([]);
  const [cargandoProfesionales, setCargandoProfesionales] = useState(false);
  const [agendaCitas, setAgendaCitas] = useState<CitaAgenda[]>([]);
  const [cargandoAgenda, setCargandoAgenda] = useState(false);

  const [configurandoAccesoId, setConfigurandoAccesoId] = useState<number | null>(null);
  const [emailAcceso, setEmailAcceso] = useState('');
  const [passwordAcceso, setPasswordAcceso] = useState('');
  const [guardandoAcceso, setGuardandoAcceso] = useState(false);
  const [mensajeAcceso, setMensajeAcceso] = useState<string | null>(null);
  const [errorAcceso, setErrorAcceso] = useState<string | null>(null);

  const [pacientes, setPacientes] = useState<Paciente[]>([]);
  const [cargandoPacientes, setCargandoPacientes] = useState(false);
  const [busquedaPaciente, setBusquedaPaciente] = useState('');
  const [pacienteSeleccionado, setPacienteSeleccionado] = useState<Paciente | null>(null);
  const [notifsPaciente, setNotifsPaciente] = useState<Notificacion[]>([]);
  const [cargandoNotifsPaciente, setCargandoNotifsPaciente] = useState(false);
  const [editandoPaciente, setEditandoPaciente] = useState(false);
  const [editTelefono, setEditTelefono] = useState('');
  const [editEmail, setEditEmail] = useState('');
  const [editCanalPreferido, setEditCanalPreferido] = useState<'SMS' | 'WHATSAPP' | 'EMAIL'>('SMS');
  const [guardandoEdicionPaciente, setGuardandoEdicionPaciente] = useState(false);
  const [errorEdicionPaciente, setErrorEdicionPaciente] = useState<string | null>(null);
  const [enviandoPrueba, setEnviandoPrueba] = useState(false);
  const [mensajePrueba, setMensajePrueba] = useState<string | null>(null);
  const [errorPrueba, setErrorPrueba] = useState<string | null>(null);
  const [creandoCita, setCreandoCita] = useState(false);
  const [citaProfesionalId, setCitaProfesionalId] = useState('');
  const [citaFechaHora, setCitaFechaHora] = useState('');
  const [guardandoCita, setGuardandoCita] = useState(false);
  const [mensajeCita, setMensajeCita] = useState<string | null>(null);
  const [errorCita, setErrorCita] = useState<string | null>(null);
  const [creandoCitaAgenda, setCreandoCitaAgenda] = useState(false);
  const [citaAgendaPacienteId, setCitaAgendaPacienteId] = useState('');
  const [citaAgendaProfesionalId, setCitaAgendaProfesionalId] = useState('');
  const [citaAgendaFechaHora, setCitaAgendaFechaHora] = useState('');
  const [guardandoCitaAgenda, setGuardandoCitaAgenda] = useState(false);
  const [mensajeCitaAgenda, setMensajeCitaAgenda] = useState<string | null>(null);
  const [errorCitaAgenda, setErrorCitaAgenda] = useState<string | null>(null);

  const [filtroProfesionalHistorial, setFiltroProfesionalHistorial] = useState('');

  const [periodoReporte, setPeriodoReporte] = useState(mesActualISO);
  const [reporte, setReporte] = useState<ReporteMensual | null>(null);
  const [cargandoReporte, setCargandoReporte] = useState(false);

  const [configuracion, setConfiguracion] = useState<Configuracion | null>(null);
  const [cargandoConfiguracion, setCargandoConfiguracion] = useState(false);
  const [guardandoConfiguracion, setGuardandoConfiguracion] = useState(false);
  const [mensajeConfiguracion, setMensajeConfiguracion] = useState<string | null>(null);
  const [errorConfiguracion, setErrorConfiguracion] = useState<string | null>(null);

  const [fechaBitacora, setFechaBitacora] = useState(hoyISO);
  const [bitacora, setBitacora] = useState<Bitacora | null>(null);
  const [cargandoBitacora, setCargandoBitacora] = useState(false);

  async function cargarDashboard(tokenActual: string) {
    setCargando(true);
    setError(null);
    try {
      const [kpisResp, eventosResp] = await Promise.all([
        api.adminDashboardKpis(tokenActual),
        api.adminDashboardEventos(tokenActual, 100),
      ]);
      setKpis(kpisResp);
      setEventos(eventosResp);
    } catch (err) {
      if (err instanceof ApiError && (err.status === 401 || err.status === 403)) {
        setAdminToken('');
        localStorage.removeItem(STORAGE_KEY);
        localStorage.removeItem(STORAGE_KEY_ROL);
        setError('Token administrativo inválido o ausente.');
        return;
      }
      setError('No pudimos cargar el dashboard administrativo.');
    } finally {
      setCargando(false);
    }
  }

  async function cargarProfesionales(tokenActual: string, fecha: string, horaInicio?: string, horaFin?: string) {
    setCargandoProfesionales(true);
    try {
      const resp = await api.listarProfesionales(tokenActual, fecha, horaInicio, horaFin);
      setProfesionales(resp);
    } catch {
      setProfesionales([]);
    } finally {
      setCargandoProfesionales(false);
    }
  }

  async function cargarAgenda(tokenActual: string, fecha: string) {
    setCargandoAgenda(true);
    try {
      const resp = await api.agendaDelDia(tokenActual, fecha);
      setAgendaCitas(resp);
    } catch {
      setAgendaCitas([]);
    } finally {
      setCargandoAgenda(false);
    }
  }

  function abrirFormularioAcceso(p: Profesional) {
    setConfigurandoAccesoId(p.id);
    setEmailAcceso(p.email ?? '');
    setPasswordAcceso('');
    setMensajeAcceso(null);
    setErrorAcceso(null);
  }

  async function guardarAccesoMedico(e: FormEvent, profesionalId: number) {
    e.preventDefault();
    setGuardandoAcceso(true);
    setMensajeAcceso(null);
    setErrorAcceso(null);
    try {
      await api.asignarCredencialesMedico(adminToken, profesionalId, emailAcceso.trim(), passwordAcceso);
      setMensajeAcceso('Acceso configurado. El profesional ya puede ingresar en /medico.');
      setConfigurandoAccesoId(null);
      cargarProfesionales(adminToken, fechaCancelacion, horaInicioCancelacion || undefined, horaFinCancelacion || undefined);
    } catch (err) {
      setErrorAcceso(err instanceof ApiError ? err.message : 'No pudimos guardar el acceso.');
    } finally {
      setGuardandoAcceso(false);
    }
  }

  async function cargarPacientes(tokenActual: string) {
    setCargandoPacientes(true);
    try {
      const resp = await api.listarPacientesAdmin(tokenActual);
      setPacientes(resp);
    } catch {
      setPacientes([]);
    } finally {
      setCargandoPacientes(false);
    }
  }

  async function seleccionarPaciente(p: Paciente) {
    setPacienteSeleccionado(p);
    setEditandoPaciente(false);
    setErrorEdicionPaciente(null);
    setMensajePrueba(null);
    setErrorPrueba(null);
    setCreandoCita(false);
    setErrorCita(null);
    setMensajeCita(null);
    setCargandoNotifsPaciente(true);
    try {
      const resp = await api.historialNotificacionesPaciente(adminToken, p.id);
      setNotifsPaciente(resp);
    } catch {
      setNotifsPaciente([]);
    } finally {
      setCargandoNotifsPaciente(false);
    }
  }

  function abrirEdicionPaciente() {
    if (!pacienteSeleccionado) return;
    setEditTelefono(pacienteSeleccionado.telefono ?? '');
    setEditEmail(pacienteSeleccionado.email ?? '');
    setEditCanalPreferido(pacienteSeleccionado.canalPreferido);
    setErrorEdicionPaciente(null);
    setEditandoPaciente(true);
  }

  async function guardarEdicionPaciente(e: FormEvent) {
    e.preventDefault();
    if (!pacienteSeleccionado) return;
    setGuardandoEdicionPaciente(true);
    setErrorEdicionPaciente(null);
    try {
      const actualizado = await api.actualizarDatosPacienteAdmin(adminToken, pacienteSeleccionado.id, {
        telefono: editTelefono.trim() || undefined,
        email: editEmail.trim() || undefined,
        canalPreferido: editCanalPreferido,
      });
      setPacienteSeleccionado(actualizado);
      setPacientes((prev) => prev.map((p) => (p.id === actualizado.id ? actualizado : p)));
      setEditandoPaciente(false);
    } catch (err) {
      setErrorEdicionPaciente(err instanceof ApiError ? err.message : 'No pudimos guardar los cambios.');
    } finally {
      setGuardandoEdicionPaciente(false);
    }
  }

  async function enviarPrueba() {
    if (!pacienteSeleccionado) return;
    setEnviandoPrueba(true);
    setMensajePrueba(null);
    setErrorPrueba(null);
    try {
      const notif = await api.enviarNotificacionPrueba(adminToken, pacienteSeleccionado.id);
      setMensajePrueba(
          notif.estadoEnvio === 'ENVIADO'
              ? `Prueba enviada por ${notif.canal}.`
              : `El envío por ${notif.canal} falló — revisa la configuración del canal.`,
      );
      setNotifsPaciente((prev) => [notif, ...prev]);
    } catch (err) {
      setErrorPrueba(err instanceof ApiError ? err.message : 'No pudimos enviar la prueba.');
    } finally {
      setEnviandoPrueba(false);
    }
  }

  function abrirCrearCita() {
    setCitaProfesionalId('');
    setCitaFechaHora('');
    setErrorCita(null);
    setMensajeCita(null);
    setCreandoCita(true);
  }

  async function crearCitaPaciente(e: FormEvent) {
    e.preventDefault();
    if (!pacienteSeleccionado || !citaProfesionalId || !citaFechaHora) return;
    setGuardandoCita(true);
    setErrorCita(null);
    setMensajeCita(null);
    try {
      await api.crearCita(adminToken, {
        pacienteId: pacienteSeleccionado.id,
        profesionalId: Number(citaProfesionalId),
        fechaHora: citaFechaHora,
      });
      setMensajeCita('Cita creada.');
      setCitaFechaHora('');
    } catch (err) {
      setErrorCita(err instanceof ApiError ? err.message : 'No pudimos crear la cita.');
    } finally {
      setGuardandoCita(false);
    }
  }

  function abrirCrearCitaAgenda() {
    setCitaAgendaPacienteId('');
    setCitaAgendaProfesionalId('');
    setCitaAgendaFechaHora(fechaCancelacion ? `${fechaCancelacion}T09:00` : '');
    setErrorCitaAgenda(null);
    setMensajeCitaAgenda(null);
    setCreandoCitaAgenda(true);
  }

  async function crearCitaDesdeAgenda(e: FormEvent) {
    e.preventDefault();
    if (!citaAgendaPacienteId || !citaAgendaProfesionalId || !citaAgendaFechaHora) return;
    setGuardandoCitaAgenda(true);
    setErrorCitaAgenda(null);
    setMensajeCitaAgenda(null);
    try {
      await api.crearCita(adminToken, {
        pacienteId: Number(citaAgendaPacienteId),
        profesionalId: Number(citaAgendaProfesionalId),
        fechaHora: citaAgendaFechaHora,
      });
      setMensajeCitaAgenda('Cita creada.');
      cargarAgenda(adminToken, fechaCancelacion);
    } catch (err) {
      setErrorCitaAgenda(err instanceof ApiError ? err.message : 'No pudimos crear la cita.');
    } finally {
      setGuardandoCitaAgenda(false);
    }
  }

  async function cargarReporte(tokenActual: string, periodo: string) {
    setCargandoReporte(true);
    try {
      const resp = await api.reporteMensual(tokenActual, periodo);
      setReporte(resp);
    } catch {
      setReporte(null);
    } finally {
      setCargandoReporte(false);
    }
  }

  async function cargarConfiguracion(tokenActual: string) {
    setCargandoConfiguracion(true);
    try {
      const resp = await api.obtenerConfiguracion(tokenActual);
      setConfiguracion(resp);
    } catch {
      setConfiguracion(null);
    } finally {
      setCargandoConfiguracion(false);
    }
  }

  async function cargarBitacora(tokenActual: string, fecha: string) {
    setCargandoBitacora(true);
    try {
      const resp = await api.obtenerBitacora(tokenActual, fecha);
      setBitacora(resp);
    } catch {
      setBitacora(null);
    } finally {
      setCargandoBitacora(false);
    }
  }

  async function persistirConfiguracion(nuevaConfig: Configuracion) {
    const previa = configuracion;
    setConfiguracion(nuevaConfig);
    setGuardandoConfiguracion(true);
    setMensajeConfiguracion(null);
    setErrorConfiguracion(null);
    try {
      const { actualizadoEn: _actualizadoEn, ...sinFecha } = nuevaConfig;
      const guardada = await api.actualizarConfiguracion(adminToken, sinFecha);
      setConfiguracion(guardada);
      setMensajeConfiguracion('Cambios guardados.');
    } catch (err) {
      setConfiguracion(previa); // revertir si falló
      setErrorConfiguracion(err instanceof ApiError ? err.message : 'No pudimos guardar el cambio.');
    } finally {
      setGuardandoConfiguracion(false);
    }
  }

  function cambiarToggle(campo: keyof Omit<Configuracion, 'actualizadoEn' | 'escalacionMinutosEspera' | 'escalacionMaxIntentos'>) {
    if (!configuracion) return;
    persistirConfiguracion({ ...configuracion, [campo]: !configuracion[campo] });
  }

  function cambiarEscalamiento(campo: 'escalacionMinutosEspera' | 'escalacionMaxIntentos', valor: number) {
    if (!configuracion) return;
    persistirConfiguracion({ ...configuracion, [campo]: valor });
  }

  const rangoHorarioInvalido =
      horaInicioCancelacion !== '' && horaFinCancelacion !== '' && horaFinCancelacion <= horaInicioCancelacion;

  async function registrarCancelacion(e: FormEvent) {
    e.preventDefault();
    if (!profesionalId.trim() || !fechaCancelacion || rangoHorarioInvalido) return;

    setEnviandoCancelacion(true);
    setMensajeCancelacion(null);
    setErrorCancelacion(null);
    try {
      const evento = await api.registrarCancelacion(adminToken, {
        profesionalId: Number(profesionalId),
        fecha: fechaCancelacion,
        horaInicio: horaInicioCancelacion || undefined,
        horaFin: horaFinCancelacion || undefined,
        motivo: motivo.trim() || undefined,
        registradoPor: registradoPor.trim() ? Number(registradoPor) : undefined,
      });
      setMensajeCancelacion(`Evento #${evento.id} registrado (estado: ${evento.estado}).`);
      setProfesionalId('');
      setMotivo('');
      setRegistradoPor('');
      cargarDashboard(adminToken);
      cargarProfesionales(adminToken, fechaCancelacion, horaInicioCancelacion || undefined, horaFinCancelacion || undefined);
    } catch (err) {
      setErrorCancelacion(err instanceof ApiError ? err.message : 'No pudimos registrar la cancelación.');
    } finally {
      setEnviandoCancelacion(false);
    }
  }

  useEffect(() => {
    if (!adminToken) return;
    cargarDashboard(adminToken);
    cargarPacientes(adminToken);
    const id = window.setInterval(() => cargarDashboard(adminToken), POLLING_MS);
    return () => window.clearInterval(id);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [adminToken]);

  useEffect(() => {
    if (!adminToken || !fechaCancelacion || rangoHorarioInvalido) return;
    cargarProfesionales(adminToken, fechaCancelacion, horaInicioCancelacion || undefined, horaFinCancelacion || undefined);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [adminToken, fechaCancelacion, horaInicioCancelacion, horaFinCancelacion]);

  useEffect(() => {
    if (!adminToken || tab !== 'reportes') return;
    cargarReporte(adminToken, periodoReporte);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [adminToken, tab, periodoReporte]);

  useEffect(() => {
    if (!adminToken || tab !== 'configuracion' || configuracion) return;
    cargarConfiguracion(adminToken);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [adminToken, tab]);

  useEffect(() => {
    if (!adminToken || tab !== 'bitacora') return;
    cargarBitacora(adminToken, fechaBitacora);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [adminToken, tab, fechaBitacora]);

  useEffect(() => {
    if (!adminToken || tab !== 'agenda' || !fechaCancelacion) return;
    cargarAgenda(adminToken, fechaCancelacion);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [adminToken, tab, fechaCancelacion]);

  const profesionalSeleccionado = useMemo(
      () => profesionales.find((p) => String(p.id) === profesionalId) ?? null,
      [profesionales, profesionalId],
  );

  const pacientesFiltrados = useMemo(() => {
    const q = busquedaPaciente.trim().toLowerCase();
    if (!q) return pacientes;
    return pacientes.filter(
        (p) => p.nombre.toLowerCase().includes(q) || p.rut.toLowerCase().includes(q),
    );
  }, [pacientes, busquedaPaciente]);

  const profesionalesEnHistorial = useMemo(() => {
    const mapa = new Map<number, string>();
    for (const e of eventos) {
      if (e.profesionalId != null && e.profesionalNombre) mapa.set(e.profesionalId, e.profesionalNombre);
    }
    return Array.from(mapa.entries()).map(([id, nombre]) => ({ id, nombre }));
  }, [eventos]);

  const eventosFiltrados = useMemo(() => {
    if (!filtroProfesionalHistorial) return eventos;
    return eventos.filter((e) => String(e.profesionalId) === filtroProfesionalHistorial);
  }, [eventos, filtroProfesionalHistorial]);

  const canalPie = useMemo(() => {
    if (!kpis) return [];
    const totals = new Map<string, number>();
    for (const point of kpis.channelStatusDistribution) {
      totals.set(point.canal, (totals.get(point.canal) ?? 0) + point.total);
    }
    return Array.from(totals.entries()).map(([canal, total]) => ({ canal, total }));
  }, [kpis]);

  const canalStacked = useMemo(() => {
    if (!kpis) return [];
    const byCanal = new Map<string, Record<EstadoCanal, number>>();
    for (const point of kpis.channelStatusDistribution) {
      if (!ESTADOS.includes(point.estado as EstadoCanal)) continue;
      const current =
        byCanal.get(point.canal) ??
        { confirmado: 0, pendiente: 0, reintentando: 0, sin_respuesta: 0 };
      current[point.estado as EstadoCanal] = point.total;
      byCanal.set(point.canal, current);
    }
    return Array.from(byCanal.entries()).map(([canal, values]) => ({ canal, ...values }));
  }, [kpis]);

  function ingresarAdminToken(e: FormEvent) {
    e.preventDefault();
    if (!draftToken.trim()) return;
    localStorage.setItem(STORAGE_KEY, draftToken.trim());
    localStorage.setItem(STORAGE_KEY_ROL, ROL_SUPERUSER);
    setRol(ROL_SUPERUSER);
    setAdminToken(draftToken.trim());
    setDraftToken('');
  }

  async function ingresarLoginIndividual(e: FormEvent) {
    e.preventDefault();
    if (!emailLogin.trim() || !passwordLogin) return;
    setIngresandoLogin(true);
    setError(null);
    try {
      const sesion = await api.adminLogin(emailLogin.trim(), passwordLogin);
      localStorage.setItem(STORAGE_KEY, sesion.token);
      localStorage.setItem(STORAGE_KEY_ROL, sesion.rol);
      setRol(sesion.rol);
      setAdminToken(sesion.token);
      setEmailLogin('');
      setPasswordLogin('');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No pudimos iniciar sesión.');
    } finally {
      setIngresandoLogin(false);
    }
  }

  useEffect(() => {
    if (tab === 'reportes' && !ROLES_CON_REPORTES.includes(rol)) {
      setTab('dashboard');
    }
  }, [tab, rol]);

  if (!adminToken) {
    return (
      <div className="ma-login-wrap">
        <div className="ma-login-card">
          <div className="logo" style={{ marginBottom: 18, color: '#0F172A' }}>
            <i className="ti ti-bell-ringing" style={{ color: '#0C447C' }} />
            MedAlert Pro
            <span className="logo-sub" style={{ color: '#64748B' }}>Panel Administrativo</span>
          </div>
          <div className="ma-login-tabs" style={{ display: 'flex', gap: 8, marginBottom: 14 }}>
            <button
                type="button"
                className={`btn-sec${modoLogin === 'token' ? ' on' : ''}`}
                onClick={() => setModoLogin('token')}
            >
              Token administrativo
            </button>
            <button
                type="button"
                className={`btn-sec${modoLogin === 'individual' ? ' on' : ''}`}
                onClick={() => setModoLogin('individual')}
            >
              Usuario y contraseña
            </button>
          </div>

          {modoLogin === 'token' ? (
            <form onSubmit={ingresarAdminToken} className="ma-login-form">
              <label htmlFor="admin-token" className="form-label">Token administrativo (X-Admin-Token)</label>
              <input
                id="admin-token"
                type="password"
                className="form-input"
                value={draftToken}
                onChange={(e) => setDraftToken(e.target.value)}
                placeholder="Ingresa el token de administración"
                autoComplete="off"
                required
              />
              <button type="submit" className="btn-primary full">
                <i className="ti ti-login" /> Ingresar al dashboard
              </button>
            </form>
          ) : (
            <form onSubmit={ingresarLoginIndividual} className="ma-login-form">
              <label htmlFor="admin-email" className="form-label">Correo</label>
              <input
                id="admin-email"
                type="email"
                className="form-input"
                value={emailLogin}
                onChange={(e) => setEmailLogin(e.target.value)}
                placeholder="nombre@clinica.cl"
                autoComplete="username"
                required
              />
              <label htmlFor="admin-password" className="form-label">Contraseña</label>
              <input
                id="admin-password"
                type="password"
                className="form-input"
                value={passwordLogin}
                onChange={(e) => setPasswordLogin(e.target.value)}
                placeholder="Contraseña"
                autoComplete="current-password"
                required
              />
              <button type="submit" className="btn-primary full" disabled={ingresandoLogin}>
                <i className="ti ti-login" /> {ingresandoLogin ? 'Ingresando…' : 'Ingresar al dashboard'}
              </button>
            </form>
          )}
          {error && <p className="ma-error-text">{error}</p>}
        </div>
      </div>
    );
  }

  return (
    <div className="ma-app">
      <div className="topbar">
        <div className="logo">
          <i className="ti ti-bell-ringing" />
          MedAlert Pro
          <span className="logo-sub">Consultorio Tomás Rojas Vergara · Los Lagos</span>
        </div>
        <div className="nav">
          <button className={`nb ${tab === 'dashboard' ? 'on' : ''}`} onClick={() => setTab('dashboard')}>
            <i className="ti ti-layout-dashboard" />Dashboard
          </button>
          <button className={`nb ${tab === 'agenda' ? 'on' : ''}`} onClick={() => setTab('agenda')}>
            <i className="ti ti-calendar-event" />Agenda
          </button>
          <button className={`nb ${tab === 'pacientes' ? 'on' : ''}`} onClick={() => setTab('pacientes')}>
            <i className="ti ti-users" />Pacientes
          </button>
          <button className={`nb ${tab === 'notificaciones' ? 'on' : ''}`} onClick={() => setTab('notificaciones')}>
            <i className="ti ti-history" />Historial
          </button>
          {ROLES_CON_REPORTES.includes(rol) && (
            <button className={`nb ${tab === 'reportes' ? 'on' : ''}`} onClick={() => setTab('reportes')}>
              <i className="ti ti-chart-bar" />Reportes
            </button>
          )}
          <button className={`nb ${tab === 'bitacora' ? 'on' : ''}`} onClick={() => setTab('bitacora')}>
            <i className="ti ti-list-check" />Bitácora
          </button>
          <button className={`nb ${tab === 'configuracion' ? 'on' : ''}`} onClick={() => setTab('configuracion')}>
            <i className="ti ti-settings" />Configuración
          </button>
        </div>
        <button
            className="nb"
            title="Cerrar acceso admin"
            onClick={() => {
              localStorage.removeItem(STORAGE_KEY);
              localStorage.removeItem(STORAGE_KEY_ROL);
              setAdminToken('');
              setRol(ROL_SUPERUSER);
            }}
        >
          <i className="ti ti-logout" />
        </button>
        <div className="avatar">AD</div>
      </div>

      <div className="ma-content">
        {error && (
          <div className="card ma-error-card">
            <p>{error}</p>
            <button className="btn-sec" onClick={() => cargarDashboard(adminToken)}>
              <i className="ti ti-refresh" /> Reintentar
            </button>
          </div>
        )}

        {tab === 'dashboard' && (
          <>
            <div className="stitle"><i className="ti ti-layout-dashboard" style={{ color: '#1D4ED8' }} /> Dashboard de KPIs</div>
            {kpis && <p className="ma-meta">Actualizado: {formatDate(kpis.generadoEn)}</p>}

            {cargando && !kpis && <p className="ma-vacio">Cargando dashboard…</p>}
            {!cargando && !kpis && !error && (
              <div className="card ma-vacio-card"><p>No hay datos aún para mostrar.</p></div>
            )}

            {kpis && (
              <>
                <div className="kpis">
                  <div className="kpi">
                    <div className="kpi-icon" style={{ background: '#DBEAFE', color: '#1D4ED8' }}><i className="ti ti-send" /></div>
                    <div className="kl">Tasa de entrega exitosa</div>
                    <div className={pctColorClass(kpis.deliveryRate.porcentajeExito, 95)}>{kpis.deliveryRate.porcentajeExito.toFixed(2)}%</div>
                    <div className="ks">{kpis.deliveryRate.entregasExitosas} de {kpis.deliveryRate.totalNotificaciones} notificaciones</div>
                  </div>
                  <div className="kpi">
                    <div className="kpi-icon" style={{ background: '#D1FAE5', color: '#065F46' }}><i className="ti ti-target-arrow" /></div>
                    <div className="kl">Contacto efectivo (1er intento)</div>
                    <div className={pctColorClass(kpis.contactEffectiveness.porcentajePrimerIntento, 50)}>{kpis.contactEffectiveness.porcentajePrimerIntento.toFixed(2)}%</div>
                    <div className="ks">Escalamiento: {kpis.contactEffectiveness.porcentajeTrasEscalamiento.toFixed(2)}%</div>
                  </div>
                  <div className="kpi">
                    <div className="kpi-icon" style={{ background: '#FEF3C7', color: '#92400E' }}><i className="ti ti-clock" /></div>
                    <div className="kl">Tiempo total por evento</div>
                    <div className={pctColorClass(kpis.notificationTime.promedioMinutos, 5, true)}>{kpis.notificationTime.promedioMinutos.toFixed(2)} min</div>
                    <div className="ks">Máximo observado: {kpis.notificationTime.maximoMinutos.toFixed(2)} min</div>
                  </div>
                  <div className="kpi">
                    <div className="kpi-icon" style={{ background: '#EDE9FE', color: '#5B21B6' }}><i className="ti ti-calendar-plus" /></div>
                    <div className="kl">Reagendamientos por portal</div>
                    <div className={pctColorClass(kpis.portalReschedule.porcentajeReagendamiento, 30)}>{kpis.portalReschedule.porcentajeReagendamiento.toFixed(2)}%</div>
                    <div className="ks">{kpis.portalReschedule.citasReagendadas} de {kpis.portalReschedule.totalCitasCanceladas} citas</div>
                  </div>
                  <div className="kpi">
                    <div className="kpi-icon" style={{ background: '#FFE4E6', color: '#BE123C' }}><i className="ti ti-address-book" /></div>
                    <div className="kl">Pacientes con contacto actualizado</div>
                    <div className={pctColorClass(kpis.contactUpdate.porcentajeActualizados, 95)}>{kpis.contactUpdate.porcentajeActualizados.toFixed(2)}%</div>
                    <div className="ks">{kpis.contactUpdate.pacientesActualizados} de {kpis.contactUpdate.totalPacientes} pacientes</div>
                  </div>
                </div>

                <div className="two">
                  <div>
                    <div className="stitle" style={{ fontSize: 13 }}>Distribución por canal</div>
                    <div className="card">
                      {canalPie.length === 0 ? (
                        <p className="ma-vacio">Sin notificaciones registradas.</p>
                      ) : (
                        <div className="chart-wrap">
                          <ResponsiveContainer width="100%" height={240}>
                            <PieChart>
                              <Pie data={canalPie} dataKey="total" nameKey="canal" cx="50%" cy="50%" outerRadius={80} fill="#1D4ED8" />
                              <Tooltip />
                            </PieChart>
                          </ResponsiveContainer>
                        </div>
                      )}
                    </div>

                    <div className="stitle" style={{ fontSize: 13 }}>Estados por canal</div>
                    <div className="card">
                      {canalStacked.length === 0 ? (
                        <p className="ma-vacio">Sin datos de estado por canal.</p>
                      ) : (
                        <div className="chart-wrap">
                          <ResponsiveContainer width="100%" height={240}>
                            <BarChart data={canalStacked}>
                              <CartesianGrid strokeDasharray="3 3" />
                              <XAxis dataKey="canal" />
                              <YAxis />
                              <Tooltip />
                              <Legend formatter={(v) => ESTADO_LABEL[v as EstadoCanal] ?? v} />
                              <Bar stackId="a" dataKey="confirmado" fill="#10B981" />
                              <Bar stackId="a" dataKey="pendiente" fill="#F59E0B" />
                              <Bar stackId="a" dataKey="reintentando" fill="#EF4444" />
                              <Bar stackId="a" dataKey="sin_respuesta" fill="#94A3B8" />
                            </BarChart>
                          </ResponsiveContainer>
                        </div>
                      )}
                    </div>
                  </div>

                  <div>
                    <div className="stitle" style={{ fontSize: 13 }}>Frecuencia semanal de cancelaciones</div>
                    <div className="card">
                      {kpis.weeklyCancellationHistory.length === 0 ? (
                        <p className="ma-vacio">Sin eventos de cancelación aún.</p>
                      ) : (
                        <div className="chart-wrap">
                          <ResponsiveContainer width="100%" height={240}>
                            <LineChart
                              data={kpis.weeklyCancellationHistory.map((p) => ({
                                semana: formatWeek(p.semanaInicio),
                                eventos: p.totalEventos,
                              }))}
                            >
                              <CartesianGrid strokeDasharray="3 3" />
                              <XAxis dataKey="semana" />
                              <YAxis allowDecimals={false} />
                              <Tooltip />
                              <Line type="monotone" dataKey="eventos" stroke="#1D4ED8" strokeWidth={2} />
                            </LineChart>
                          </ResponsiveContainer>
                        </div>
                      )}
                    </div>

                    <div className="stitle" style={{ fontSize: 13 }}>Profesionales — hoy</div>
                    <div className="card">
                      {profesionales.length === 0 ? (
                        <p className="ma-vacio">Sin profesionales registrados.</p>
                      ) : (
                        profesionales.map((p) => (
                          <div key={p.id} className="stat-row">
                            <div className="stat-icon" style={{ background: p.citasAgendadas > 0 ? '#FFE4E6' : '#D1FAE5', color: p.citasAgendadas > 0 ? '#BE123C' : '#065F46' }}>
                              {iniciales(p.nombre)}
                            </div>
                            <div className="stat-name">{p.nombre}<br /><span style={{ color: '#94A3B8', fontSize: 11 }}>{p.especialidad}</span></div>
                            <div className="stat-val">{p.citasAgendadas} citas</div>
                          </div>
                        ))
                      )}
                    </div>
                  </div>
                </div>
              </>
            )}
          </>
        )}

        {tab === 'agenda' && (
          <>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 10, marginBottom: creandoCitaAgenda ? 12 : 0 }}>
              <div className="stitle" style={{ marginBottom: 0 }}>
                <i className="ti ti-calendar-event" style={{ color: '#059669' }} /> Agenda del día — {fechaCancelacion ? formatDate(`${fechaCancelacion}T00:00:00`).split(',')[0] : ''}
              </div>
              <button type="button" className="btn-sec" style={{ fontSize: 12 }} onClick={() => (creandoCitaAgenda ? setCreandoCitaAgenda(false) : abrirCrearCitaAgenda())}>
                <i className="ti ti-calendar-plus" style={{ fontSize: 14 }} />{creandoCitaAgenda ? 'Cancelar' : 'Crear cita'}
              </button>
            </div>

            {creandoCitaAgenda && (
              <form
                  onSubmit={crearCitaDesdeAgenda}
                  className="card"
                  style={{ marginBottom: 14, display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: 10, alignItems: 'end' }}
              >
                <div>
                  <label className="form-label">Paciente</label>
                  <select
                      className="form-select"
                      style={{ width: '100%' }}
                      value={citaAgendaPacienteId}
                      onChange={(e) => setCitaAgendaPacienteId(e.target.value)}
                      required
                  >
                    <option value="" disabled>Seleccionar...</option>
                    {pacientes.map((p) => (
                        <option key={p.id} value={p.id}>{p.nombre}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="form-label">Profesional</label>
                  <select
                      className="form-select"
                      style={{ width: '100%' }}
                      value={citaAgendaProfesionalId}
                      onChange={(e) => setCitaAgendaProfesionalId(e.target.value)}
                      required
                  >
                    <option value="" disabled>Seleccionar...</option>
                    {profesionales.map((p) => (
                        <option key={p.id} value={p.id}>{p.nombre} — {p.especialidad}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="form-label">Fecha y hora</label>
                  <input
                      type="datetime-local"
                      className="form-input"
                      value={citaAgendaFechaHora}
                      onChange={(e) => setCitaAgendaFechaHora(e.target.value)}
                      required
                  />
                </div>
                <button type="submit" className="btn-primary" style={{ justifyContent: 'center', fontSize: 12 }} disabled={guardandoCitaAgenda}>
                  {guardandoCitaAgenda ? 'Creando…' : 'Crear cita'}
                </button>
                {errorCitaAgenda && <p className="ma-error-text" style={{ gridColumn: '1 / -1', margin: 0 }}>{errorCitaAgenda}</p>}
                {mensajeCitaAgenda && <p className="ma-exito-text" style={{ gridColumn: '1 / -1', margin: 0 }}>{mensajeCitaAgenda}</p>}
              </form>
            )}

            <div className="two">
              <div>
                <div className="card" style={{ marginBottom: 14 }}>
                  <div style={{ display: 'flex', gap: 16, marginBottom: 14, paddingBottom: 12, borderBottom: '1px solid #F1F5F9' }}>
                    {(['AGENDADA', 'CANCELADA', 'REAGENDADA'] as const).map((estado) => (
                        <div key={estado} style={{ textAlign: 'center' }}>
                          <div style={{ fontSize: 20, fontWeight: 700, color: ESTADO_CITA_DOT[estado] }}>
                            {agendaCitas.filter((c) => c.estado === estado).length}
                          </div>
                          <div style={{ fontSize: 10, color: '#64748B' }}>{ESTADO_CITA_LABEL[estado]}s</div>
                        </div>
                    ))}
                  </div>

                  {cargandoAgenda && <p className="ma-vacio">Cargando agenda…</p>}
                  {!cargandoAgenda && agendaCitas.length === 0 && (
                      <p className="ma-vacio">No hay citas registradas para este día.</p>
                  )}
                  {!cargandoAgenda && agendaCitas.map((c) => (
                      <div
                          key={c.id}
                          style={{
                            display: 'flex', alignItems: 'center', gap: 10, padding: '8px 0',
                            borderBottom: '1px solid #F1F5F9',
                            background: c.estado === 'CANCELADA' || c.estado === 'NO_ASISTIO' ? '#FFF7F7' : undefined,
                          }}
                      >
                        <div style={{ width: 48, fontSize: 12, fontWeight: 600, color: ESTADO_CITA_DOT[c.estado] ?? '#0F172A' }}>
                          {formatHora(c.fechaHora)}
                        </div>
                        <div style={{ width: 8, height: 8, borderRadius: '50%', background: ESTADO_CITA_DOT[c.estado] ?? '#94A3B8', flexShrink: 0 }} />
                        <div style={{ flex: 1, minWidth: 0 }}>
                          <div style={{ fontSize: 13, fontWeight: 500, color: '#0F172A', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                            {c.pacienteNombre}
                          </div>
                          <div style={{ fontSize: 11, color: '#64748B' }}>
                            {c.profesionalEspecialidad ? `${c.profesionalEspecialidad} · ` : ''}{c.profesionalNombre}
                          </div>
                        </div>
                        <span className={`chip ${ESTADO_CITA_CHIP[c.estado] ?? 'off'}`} style={{ marginLeft: 'auto', flexShrink: 0 }}>
                          {ESTADO_CITA_LABEL[c.estado] ?? c.estado}
                        </span>
                      </div>
                  ))}
                </div>
              </div>

              <div>
                <div className="stitle" style={{ fontSize: 13 }}>Registrar ausencia médica</div>
                <div className="card">
                  <div style={{ marginBottom: 14 }}>
                    <label className="form-label">Fecha de la agenda cancelada</label>
                    <input
                      type="date"
                      className="form-input"
                      value={fechaCancelacion}
                      onChange={(e) => setFechaCancelacion(e.target.value)}
                      required
                    />
                  </div>
                  <div style={{ marginBottom: 14 }}>
                    <label className="form-label">Rango de horas (opcional — vacío = día completo)</label>
                    <div style={{ display: 'flex', gap: 10 }}>
                      <input
                        type="time"
                        className="form-input"
                        value={horaInicioCancelacion}
                        onChange={(e) => setHoraInicioCancelacion(e.target.value)}
                        aria-label="Hora de inicio"
                      />
                      <input
                        type="time"
                        className="form-input"
                        value={horaFinCancelacion}
                        onChange={(e) => setHoraFinCancelacion(e.target.value)}
                        aria-label="Hora de término"
                      />
                    </div>
                    {rangoHorarioInvalido && (
                      <p className="ma-error-text" style={{ marginTop: 6 }}>
                        La hora de término debe ser posterior a la hora de inicio.
                      </p>
                    )}
                  </div>
                  <div style={{ marginBottom: 14 }}>
                    <label className="form-label">Profesional ausente</label>
                    <select
                        className="form-select"
                        value={profesionalId}
                        onChange={(e) => setProfesionalId(e.target.value)}
                        required
                    >
                      <option value="" disabled>
                        {cargandoProfesionales ? 'Cargando…' : 'Seleccionar profesional...'}
                      </option>
                      {profesionales.map((p) => (
                          <option key={p.id} value={p.id}>
                            {p.nombre} — {p.especialidad} ({p.citasAgendadas} citas)
                          </option>
                      ))}
                    </select>
                  </div>
                  <div style={{ marginBottom: 14 }}>
                    <label className="form-label">Motivo</label>
                    <input
                      type="text"
                      className="form-input"
                      value={motivo}
                      onChange={(e) => setMotivo(e.target.value)}
                      placeholder="Ej: Licencia médica"
                    />
                  </div>
                  <div style={{ marginBottom: 16 }}>
                    <label className="form-label">ID del usuario admin que registra (opcional)</label>
                    <input
                      type="number"
                      min={1}
                      className="form-input"
                      value={registradoPor}
                      onChange={(e) => setRegistradoPor(e.target.value)}
                    />
                  </div>

                  {profesionalSeleccionado && (
                    <div className="warn-box" style={{ marginBottom: 16 }}>
                      <i className="ti ti-alert-triangle" />
                      <span>
                        Se notificará a <strong>{profesionalSeleccionado.citasAgendadas}</strong> paciente
                        {profesionalSeleccionado.citasAgendadas === 1 ? '' : 's'} agendado
                        {profesionalSeleccionado.citasAgendadas === 1 ? '' : 's'}
                        {horaInicioCancelacion && horaFinCancelacion
                            ? ` entre las ${horaInicioCancelacion} y las ${horaFinCancelacion}`
                            : ' ese día'}. El sistema enviará las notificaciones de forma inmediata y automática.
                      </span>
                    </div>
                  )}

                  <form onSubmit={registrarCancelacion}>
                    <button
                        type="submit"
                        className="btn-primary full"
                        disabled={enviandoCancelacion || !profesionalId || rangoHorarioInvalido}
                    >
                      <i className="ti ti-send" />
                      {enviandoCancelacion
                          ? 'Registrando…'
                          : `Activar notificaciones${profesionalSeleccionado ? ` — ${profesionalSeleccionado.citasAgendadas} pacientes` : ''}`}
                    </button>
                  </form>
                </div>
                {mensajeCancelacion && <p className="ma-exito-text">{mensajeCancelacion}</p>}
                {errorCancelacion && <p className="ma-error-text">{errorCancelacion}</p>}

                <div className="stitle" style={{ fontSize: 13, marginTop: 20 }}>Profesionales del día</div>
                {mensajeAcceso && <p className="ma-exito-text">{mensajeAcceso}</p>}
                {profesionales.length === 0 && !cargandoProfesionales && (
                  <div className="card-sm"><p className="ma-vacio">Sin profesionales registrados.</p></div>
                )}
                {profesionales.map((p) => (
                  <div key={p.id} className="card-sm" style={{ marginBottom: 8 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                      <div style={{
                        width: 34, height: 34, borderRadius: '50%',
                        background: p.citasAgendadas > 0 ? '#FFE4E6' : '#D1FAE5',
                        color: p.citasAgendadas > 0 ? '#BE123C' : '#065F46',
                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                        fontSize: 12, fontWeight: 700, flexShrink: 0,
                      }}>
                        {iniciales(p.nombre)}
                      </div>
                      <div style={{ flex: 1 }}>
                        <div style={{ fontSize: 13, fontWeight: 500 }}>{p.nombre}</div>
                        <div style={{ fontSize: 11, color: '#64748B' }}>{p.especialidad} · {p.citasAgendadas} citas</div>
                      </div>
                      <span className={`badge ${p.citasAgendadas > 0 ? 'danger' : 'done'}`}>
                        <i className={`ti ${p.citasAgendadas > 0 ? 'ti-circle-x' : 'ti-check'}`} style={{ fontSize: 10 }} />
                        {p.citasAgendadas > 0 ? 'Con citas hoy' : 'Sin citas'}
                      </span>
                    </div>

                    <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 8, paddingTop: 8, borderTop: '1px solid #F1F5F9' }}>
                      <span
                          className={`chip ${p.email ? 'ok' : 'off'}`}
                          style={{ flex: 1, minWidth: 0, overflow: 'hidden', textOverflow: 'ellipsis' }}
                          title={p.email ?? undefined}
                      >
                        <i className={`ti ${p.email ? 'ti-lock' : 'ti-lock-open'}`} style={{ fontSize: 10, flexShrink: 0 }} />
                        <span style={{ overflow: 'hidden', textOverflow: 'ellipsis' }}>
                          {p.email ? `Portal médico: ${p.email}` : 'Sin acceso al portal médico'}
                        </span>
                      </span>
                      <button
                          type="button"
                          className="btn-sec"
                          style={{ fontSize: 11, padding: '5px 10px' }}
                          onClick={() => configurandoAccesoId === p.id ? setConfigurandoAccesoId(null) : abrirFormularioAcceso(p)}
                      >
                        {p.email ? 'Cambiar' : 'Configurar acceso'}
                      </button>
                    </div>

                    {configurandoAccesoId === p.id && (
                      <form onSubmit={(e) => guardarAccesoMedico(e, p.id)} style={{ marginTop: 10 }}>
                        <label className="form-label">Email de acceso</label>
                        <input
                            type="email"
                            className="form-input"
                            style={{ marginBottom: 8 }}
                            value={emailAcceso}
                            onChange={(e) => setEmailAcceso(e.target.value)}
                            required
                        />
                        <label className="form-label">Contraseña nueva (mín. 8 caracteres)</label>
                        <input
                            type="password"
                            className="form-input"
                            style={{ marginBottom: 10 }}
                            value={passwordAcceso}
                            onChange={(e) => setPasswordAcceso(e.target.value)}
                            minLength={8}
                            required
                        />
                        <div style={{ display: 'flex', gap: 8 }}>
                          <button type="submit" className="btn-primary" style={{ fontSize: 12, flex: 1 }} disabled={guardandoAcceso}>
                            {guardandoAcceso ? 'Guardando…' : 'Guardar acceso'}
                          </button>
                          <button type="button" className="btn-sec" style={{ fontSize: 12 }} onClick={() => setConfigurandoAccesoId(null)}>
                            Cancelar
                          </button>
                        </div>
                        {errorAcceso && <p className="ma-error-text" style={{ marginTop: 6 }}>{errorAcceso}</p>}
                      </form>
                    )}
                  </div>
                ))}
              </div>
            </div>
          </>
        )}

        {tab === 'pacientes' && (
          <>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 16, flexWrap: 'wrap' }}>
              <div className="stitle" style={{ marginBottom: 0 }}><i className="ti ti-users" style={{ color: '#1D4ED8' }} /> Gestión de pacientes</div>
              <input
                  type="text"
                  className="form-input"
                  placeholder="Buscar por nombre o RUT..."
                  style={{ width: 240, marginLeft: 'auto' }}
                  value={busquedaPaciente}
                  onChange={(e) => setBusquedaPaciente(e.target.value)}
              />
            </div>

            <div className="two">
              <div>
                {cargandoPacientes && <p className="ma-vacio">Cargando pacientes…</p>}

                {!cargandoPacientes && pacientesFiltrados.length === 0 && (
                  <div className="card ma-vacio-card">
                    <p>{pacientes.length === 0 ? 'No hay pacientes registrados.' : 'Sin resultados para tu búsqueda.'}</p>
                  </div>
                )}

                {!cargandoPacientes && pacientesFiltrados.map((p) => (
                  <div
                      key={p.id}
                      className="pat-card"
                      onClick={() => seleccionarPaciente(p)}
                      style={{
                        cursor: 'pointer',
                        borderColor: pacienteSeleccionado?.id === p.id ? '#0C447C' : undefined,
                        boxShadow: pacienteSeleccionado?.id === p.id ? '0 0 0 1px #0C447C' : undefined,
                      }}
                  >
                    <div className="pat-avatar" style={{ background: '#DBEAFE', color: '#1D4ED8' }}>{iniciales(p.nombre)}</div>
                    <div className="pat-info">
                      <div className="pat-name">{p.nombre}</div>
                      <div className="pat-rut">RUT: {p.rut} · Tel: {p.telefono ?? 'sin registrar'}</div>
                      <div className="pat-tags">
                        <span className="chip off">{p.canalPreferido}</span>
                        {p.adultoMayor && <span className="chip off">Adulto mayor</span>}
                      </div>
                    </div>
                    <span className={`chip ${p.telefono && p.email ? 'ok' : 'warn'}`}>
                      {p.telefono && p.email ? 'Datos OK' : 'Actualizar'}
                    </span>
                  </div>
                ))}
              </div>

              <div>
                {pacienteSeleccionado ? (
                  <>
                    <div className="stitle" style={{ fontSize: 13 }}>Detalle del paciente</div>
                    <div className="card">
                      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 14, paddingBottom: 14, borderBottom: '1px solid #F1F5F9' }}>
                        <div style={{ width: 48, height: 48, borderRadius: '50%', background: '#DBEAFE', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 18, fontWeight: 700, color: '#1D4ED8' }}>
                          {iniciales(pacienteSeleccionado.nombre)}
                        </div>
                        <div>
                          <div style={{ fontSize: 15, fontWeight: 600, color: '#0F172A' }}>{pacienteSeleccionado.nombre}</div>
                          <div style={{ fontSize: 12, color: '#64748B' }}>RUT: {pacienteSeleccionado.rut}</div>
                        </div>
                      </div>
                      <table className="tbl" style={{ marginBottom: 12 }}>
                        <tbody>
                          <tr>
                            <td style={{ color: '#64748B', width: '40%' }}><i className="ti ti-device-mobile" style={{ fontSize: 14, verticalAlign: -2, marginRight: 5 }} />Teléfono</td>
                            <td>{pacienteSeleccionado.telefono ?? 'Sin registrar'}</td>
                          </tr>
                          <tr>
                            <td style={{ color: '#64748B' }}><i className="ti ti-mail" style={{ fontSize: 14, verticalAlign: -2, marginRight: 5 }} />Email</td>
                            <td>{pacienteSeleccionado.email ?? 'Sin registrar'}</td>
                          </tr>
                          <tr>
                            <td style={{ color: '#64748B' }}><i className="ti ti-send" style={{ fontSize: 14, verticalAlign: -2, marginRight: 5 }} />Canal preferido</td>
                            <td>{pacienteSeleccionado.canalPreferido}</td>
                          </tr>
                          <tr>
                            <td style={{ color: '#64748B' }}><i className="ti ti-user" style={{ fontSize: 14, verticalAlign: -2, marginRight: 5 }} />Adulto mayor</td>
                            <td>{pacienteSeleccionado.adultoMayor ? 'Sí' : 'No'}</td>
                          </tr>
                          <tr>
                            <td style={{ color: '#64748B' }}><i className="ti ti-refresh" style={{ fontSize: 14, verticalAlign: -2, marginRight: 5 }} />Datos actualizados</td>
                            <td>{formatDate(pacienteSeleccionado.datosActualizadosEn)}</td>
                          </tr>
                        </tbody>
                      </table>

                      {editandoPaciente && (
                        <form onSubmit={guardarEdicionPaciente} style={{ marginBottom: 12, background: '#F8FAFC', border: '1px solid #E2E8F0', borderRadius: 9, padding: 10 }}>
                          <label className="form-label">Teléfono</label>
                          <input
                              type="text"
                              className="form-input"
                              style={{ marginBottom: 8 }}
                              value={editTelefono}
                              onChange={(e) => setEditTelefono(e.target.value)}
                              placeholder="+56912345678"
                          />
                          <label className="form-label">Email</label>
                          <input
                              type="email"
                              className="form-input"
                              style={{ marginBottom: 8 }}
                              value={editEmail}
                              onChange={(e) => setEditEmail(e.target.value)}
                          />
                          <label className="form-label">Canal preferido</label>
                          <select
                              className="form-select"
                              style={{ marginBottom: 10, width: '100%' }}
                              value={editCanalPreferido}
                              onChange={(e) => setEditCanalPreferido(e.target.value as 'SMS' | 'WHATSAPP' | 'EMAIL')}
                          >
                            <option value="SMS">SMS</option>
                            <option value="WHATSAPP">WhatsApp</option>
                            <option value="EMAIL">Email</option>
                          </select>
                          {errorEdicionPaciente && <p className="ma-error-text" style={{ marginBottom: 8 }}>{errorEdicionPaciente}</p>}
                          <div style={{ display: 'flex', gap: 8 }}>
                            <button type="submit" className="btn-primary" style={{ flex: 1, justifyContent: 'center', fontSize: 12 }} disabled={guardandoEdicionPaciente}>
                              {guardandoEdicionPaciente ? 'Guardando…' : 'Guardar'}
                            </button>
                            <button type="button" className="btn-sec" style={{ fontSize: 12 }} onClick={() => setEditandoPaciente(false)}>
                              Cancelar
                            </button>
                          </div>
                        </form>
                      )}

                      <div className="divider" />
                      <div className="stitle" style={{ fontSize: 13, marginBottom: 8 }}>Historial de notificaciones</div>

                      {cargandoNotifsPaciente && <p className="ma-vacio">Cargando historial…</p>}

                      {!cargandoNotifsPaciente && notifsPaciente.length === 0 && (
                        <p style={{ fontSize: 12, color: '#94A3B8' }}>Sin notificaciones registradas para este paciente.</p>
                      )}

                      {!cargandoNotifsPaciente && notifsPaciente.map((n) => (
                        <div key={n.id} style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '8px 0', borderBottom: '1px solid #F1F5F9' }}>
                          <div style={{ width: 28, height: 28, borderRadius: 8, background: '#F1F5F9', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
                            <i className={`ti ${CANAL_ICONO[n.canal] ?? 'ti-send'}`} style={{ fontSize: 14, color: '#1D4ED8' }} />
                          </div>
                          <div style={{ flex: 1, minWidth: 0 }}>
                            <div style={{ fontSize: 12, fontWeight: 500, color: '#0F172A' }}>{TIPO_NOTIF_LABEL[n.tipo] ?? n.tipo}</div>
                            <div style={{ fontSize: 11, color: '#94A3B8' }}>{n.enviadoEn ? formatDate(n.enviadoEn) : 'Sin enviar'}</div>
                            {n.estadoEntrega && (
                                <div style={{ fontSize: 10, color: ESTADO_ENTREGA_COLOR[n.estadoEntrega] ?? '#94A3B8', marginTop: 2 }}>
                                  Entrega real: {ESTADO_ENTREGA_LABEL[n.estadoEntrega] ?? n.estadoEntrega}
                                </div>
                            )}
                          </div>
                          <span className={`chip ${ESTADO_ENVIO_CHIP[n.estadoEnvio] ?? 'off'}`} style={{ fontSize: 10 }}>
                            {ESTADO_ENVIO_LABEL[n.estadoEnvio] ?? n.estadoEnvio}
                          </span>
                        </div>
                      ))}

                      <div className="divider" style={{ margin: '10px 0' }} />
                      {mensajePrueba && <p className="ma-exito-text" style={{ fontSize: 12 }}>{mensajePrueba}</p>}
                      {errorPrueba && <p className="ma-error-text" style={{ fontSize: 12 }}>{errorPrueba}</p>}
                      <div style={{ display: 'flex', gap: 8 }}>
                        <button
                            type="button"
                            className="btn-primary"
                            style={{ flex: 1, justifyContent: 'center', fontSize: 12 }}
                            onClick={abrirEdicionPaciente}
                        >
                          <i className="ti ti-edit" style={{ fontSize: 14 }} />Editar datos
                        </button>
                        <button
                            type="button"
                            className="btn-sec"
                            style={{ flex: 1, justifyContent: 'center', fontSize: 12 }}
                            onClick={enviarPrueba}
                            disabled={enviandoPrueba}
                        >
                          <i className="ti ti-send" style={{ fontSize: 14 }} />{enviandoPrueba ? 'Enviando…' : 'Enviar prueba'}
                        </button>
                        <button
                            type="button"
                            className="btn-sec"
                            style={{ flex: 1, justifyContent: 'center', fontSize: 12 }}
                            onClick={abrirCrearCita}
                        >
                          <i className="ti ti-calendar-plus" style={{ fontSize: 14 }} />Crear cita
                        </button>
                      </div>

                      {creandoCita && (
                        <form
                            onSubmit={crearCitaPaciente}
                            style={{ marginTop: 12, background: '#F8FAFC', border: '1px solid #E2E8F0', borderRadius: 9, padding: 10 }}
                        >
                          <label className="form-label">Profesional</label>
                          <select
                              className="form-select"
                              style={{ marginBottom: 8, width: '100%' }}
                              value={citaProfesionalId}
                              onChange={(e) => setCitaProfesionalId(e.target.value)}
                              required
                          >
                            <option value="" disabled>Seleccionar profesional...</option>
                            {profesionales.map((p) => (
                                <option key={p.id} value={p.id}>{p.nombre} — {p.especialidad}</option>
                            ))}
                          </select>
                          <label className="form-label">Fecha y hora</label>
                          <input
                              type="datetime-local"
                              className="form-input"
                              style={{ marginBottom: 10 }}
                              value={citaFechaHora}
                              onChange={(e) => setCitaFechaHora(e.target.value)}
                              required
                          />
                          {errorCita && <p className="ma-error-text" style={{ marginBottom: 8 }}>{errorCita}</p>}
                          {mensajeCita && <p className="ma-exito-text" style={{ marginBottom: 8 }}>{mensajeCita}</p>}
                          <div style={{ display: 'flex', gap: 8 }}>
                            <button type="submit" className="btn-primary" style={{ flex: 1, justifyContent: 'center', fontSize: 12 }} disabled={guardandoCita}>
                              {guardandoCita ? 'Creando…' : 'Crear cita'}
                            </button>
                            <button type="button" className="btn-sec" style={{ fontSize: 12 }} onClick={() => setCreandoCita(false)}>
                              Cerrar
                            </button>
                          </div>
                        </form>
                      )}
                    </div>
                  </>
                ) : (
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: 300, color: '#94A3B8', flexDirection: 'column', gap: 10 }}>
                    <i className="ti ti-user-search" style={{ fontSize: 40 }} />
                    <div style={{ fontSize: 13 }}>Selecciona un paciente para ver su detalle</div>
                  </div>
                )}
              </div>
            </div>
          </>
        )}

        {tab === 'notificaciones' && (
          <>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16, flexWrap: 'wrap', gap: 10 }}>
              <div className="stitle" style={{ marginBottom: 0 }}>
                <i className="ti ti-history" style={{ color: '#7C3AED' }} /> Historial de avisos de ausencia
              </div>
              <div style={{ display: 'flex', gap: 8 }}>
                <select
                    className="form-select"
                    style={{ width: 200, fontSize: 12 }}
                    value={filtroProfesionalHistorial}
                    onChange={(e) => setFiltroProfesionalHistorial(e.target.value)}
                >
                  <option value="">Todos los profesionales</option>
                  {profesionalesEnHistorial.map((p) => (
                      <option key={p.id} value={p.id}>{p.nombre}</option>
                  ))}
                </select>
                <button className="btn-sec" style={{ fontSize: 12 }} onClick={() => window.print()}>
                  <i className="ti ti-download" style={{ fontSize: 14 }} /> Exportar
                </button>
              </div>
            </div>

            {eventosFiltrados.length === 0 ? (
              <div className="card ma-vacio-card"><p>No hay avisos registrados.</p></div>
            ) : (
              <div className="card" style={{ overflowX: 'auto' }}>
                <table className="tbl">
                  <thead>
                    <tr>
                      <th>Fecha</th>
                      <th>Profesional</th>
                      <th>Motivo</th>
                      <th>Registrado por</th>
                      <th>Canal aviso</th>
                      <th>Pacientes</th>
                      <th>Contactados</th>
                      <th>Estado</th>
                    </tr>
                  </thead>
                  <tbody>
                    {eventosFiltrados.map((evento) => {
                      const pct = evento.pacientesNotificados > 0
                          ? ((evento.notificacionesConfirmadas / evento.pacientesNotificados) * 100).toFixed(1)
                          : null;
                      return (
                        <tr key={evento.eventoId}>
                          <td>
                            <strong>{formatDate(evento.fechaEvento).split(',')[0]}</strong>
                            <div style={{ fontSize: 11, color: '#94A3B8' }}>{formatDate(evento.fechaEvento)}</div>
                          </td>
                          <td>
                            {evento.profesionalNombre ?? '—'}
                            {evento.profesionalEspecialidad && (
                                <div style={{ fontSize: 11, color: '#94A3B8' }}>{evento.profesionalEspecialidad}</div>
                            )}
                          </td>
                          <td><span className="chip off">{evento.motivo ?? 'Sin motivo'}</span></td>
                          <td style={{ fontSize: 12 }}>{evento.registradoPor != null ? `Usuario #${evento.registradoPor}` : '—'}</td>
                          <td>
                            <div style={{ display: 'flex', gap: 4, flexWrap: 'wrap' }}>
                              {evento.canales.length === 0 && <span style={{ color: '#94A3B8', fontSize: 11 }}>—</span>}
                              {evento.canales.map((c) => (
                                  <span key={c} className="chip ok" style={{ fontSize: 10 }}>
                                    {c === 'WHATSAPP' ? 'WA' : c === 'EMAIL' ? 'Email' : 'SMS'}
                                  </span>
                              ))}
                            </div>
                          </td>
                          <td><strong>{evento.pacientesNotificados}</strong></td>
                          <td>
                            <strong style={{ color: '#059669' }}>{evento.notificacionesConfirmadas}</strong>
                            {pct && <div style={{ fontSize: 11, color: '#94A3B8' }}>{pct}%</div>}
                          </td>
                          <td>
                            <span className={`badge ${evento.estado === 'COMPLETADO' ? 'done' : 'act'}`}>
                              {evento.estado === 'COMPLETADO' ? 'Completado' : 'En curso'}
                            </span>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </>
        )}

        {tab === 'reportes' && (
          <>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16, flexWrap: 'wrap', gap: 10 }}>
              <div className="stitle" style={{ marginBottom: 0 }}>
                <i className="ti ti-chart-bar" style={{ color: '#1D4ED8' }} /> Reportes y KPIs — {formatPeriodo(periodoReporte)}
              </div>
              <div style={{ display: 'flex', gap: 8 }}>
                <select
                    className="form-select"
                    style={{ width: 160, fontSize: 12 }}
                    value={periodoReporte}
                    onChange={(e) => setPeriodoReporte(e.target.value)}
                >
                  {ultimosMeses(12).map((m) => (
                      <option key={m} value={m}>{formatPeriodo(m)}</option>
                  ))}
                </select>
                <button className="btn-sec" style={{ fontSize: 12 }} onClick={() => window.print()}>
                  <i className="ti ti-download" style={{ fontSize: 14 }} /> Exportar PDF
                </button>
              </div>
            </div>

            {cargandoReporte && !reporte && <p className="ma-vacio">Cargando reporte…</p>}
            {!cargandoReporte && !reporte && (
              <div className="card ma-vacio-card"><p>No pudimos cargar el reporte de este período.</p></div>
            )}

            {reporte && (
              <>
                <div className="kpis">
                  <div className="kpi">
                    <div className="kpi-icon" style={{ background: '#DBEAFE', color: '#1D4ED8' }}><i className="ti ti-send" /></div>
                    <div className="kl">Total notificaciones</div>
                    <div className="kv">{reporte.totalNotificaciones}</div>
                    <div className="ks">{reporte.porcentajeEntrega.toFixed(2)}% tasa entrega</div>
                  </div>
                  <div className="kpi">
                    <div className="kpi-icon" style={{ background: '#EDE9FE', color: '#5B21B6' }}><i className="ti ti-calendar-plus" /></div>
                    <div className="kl">Reagendamientos</div>
                    <div className="kv">{reporte.reagendamientos}</div>
                    <div className="ks">vía portal (autoservicio)</div>
                  </div>
                  <div className="kpi" title={reporte.horasAhorradasNotaMetodologica}>
                    <div className="kpi-icon" style={{ background: '#FEF3C7', color: '#92400E' }}><i className="ti ti-clock-off" /></div>
                    <div className="kl">Horas admin. ahorradas <i className="ti ti-info-circle" style={{ fontSize: 11 }} /></div>
                    <div className="kv">{reporte.horasAhorradasEstimadas.toFixed(1)} h</div>
                    <div className="ks">estimado — este mes</div>
                  </div>
                  <div className="kpi">
                    <div className="kpi-icon" style={{ background: '#D1FAE5', color: '#065F46' }}><i className="ti ti-trending-down" /></div>
                    <div className="kl">Tasa de ausentismo</div>
                    <div className={pctColorClass(reporte.tasaAusentismo, reporte.tasaAusentismoMesAnterior, true)}>
                      {reporte.tasaAusentismo.toFixed(2)}%
                    </div>
                    <div className="ks">Mes anterior: {reporte.tasaAusentismoMesAnterior.toFixed(2)}%</div>
                  </div>
                </div>

                <div className="two">
                  <div>
                    <div className="stitle" style={{ fontSize: 13 }}>Notificaciones por canal</div>
                    <div className="card">
                      {reporte.notificacionesPorCanal.length === 0 ? (
                        <p className="ma-vacio">Sin notificaciones en este período.</p>
                      ) : (
                        <>
                          <div className="chart-wrap" style={{ marginBottom: 8 }}>
                            <ResponsiveContainer width="100%" height={200}>
                              <BarChart data={reporte.notificacionesPorCanal}>
                                <CartesianGrid strokeDasharray="3 3" />
                                <XAxis dataKey="canal" />
                                <YAxis allowDecimals={false} />
                                <Tooltip />
                                <Bar dataKey="enviados" fill="#1D4ED8" />
                              </BarChart>
                            </ResponsiveContainer>
                          </div>
                          {reporte.notificacionesPorCanal.map((c) => (
                            <div key={c.canal} className="stat-row">
                              <div className="stat-name">{c.canal}</div>
                              <div className="stat-val">{c.enviados} enviados · {c.porcentajeEntregado.toFixed(1)}% entregado</div>
                            </div>
                          ))}
                        </>
                      )}
                    </div>
                  </div>

                  <div>
                    <div className="stitle" style={{ fontSize: 13 }}>Ausentismo mensual — evolución</div>
                    <div className="card">
                      <div className="chart-wrap" style={{ marginBottom: 8 }}>
                        <ResponsiveContainer width="100%" height={180}>
                          <LineChart data={reporte.ausentismoEvolucion.map((p) => ({ mes: formatMesCorto(p.periodo), tasa: p.tasa }))}>
                            <CartesianGrid strokeDasharray="3 3" />
                            <XAxis dataKey="mes" />
                            <YAxis allowDecimals={false} />
                            <Tooltip />
                            <Line type="monotone" dataKey="tasa" stroke="#EF4444" strokeWidth={2} />
                          </LineChart>
                        </ResponsiveContainer>
                      </div>
                      <div className="divider" />
                      <div className="stitle" style={{ fontSize: 13, marginBottom: 8 }}>Escalamientos por canal</div>
                      <div className="stat-row">
                        <div className="stat-name">SMS → WhatsApp (escalados)</div>
                        <div className="stat-val">{reporte.escalamientos.smsAWhatsapp} de {reporte.escalamientos.totalContactados}</div>
                      </div>
                      <div className="stat-row">
                        <div className="stat-name">WhatsApp → Email (escalados)</div>
                        <div className="stat-val">{reporte.escalamientos.whatsappAEmail} de {reporte.escalamientos.totalContactados}</div>
                      </div>
                      <div className="stat-row">
                        <div className="stat-name">Sin contacto definitivo</div>
                        <div className="stat-val" style={{ color: reporte.escalamientos.sinContactoDefinitivo > 0 ? '#BE123C' : undefined }}>
                          {reporte.escalamientos.sinContactoDefinitivo}
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </>
            )}
          </>
        )}

        {tab === 'configuracion' && (
          <>
            <div className="stitle"><i className="ti ti-settings" style={{ color: '#0C447C' }} /> Configuración del sistema</div>

            {cargandoConfiguracion && !configuracion && <p className="ma-vacio">Cargando configuración…</p>}
            {!cargandoConfiguracion && !configuracion && (
              <div className="card ma-vacio-card"><p>No pudimos cargar la configuración.</p></div>
            )}

            {mensajeConfiguracion && <p className="ma-exito-text">{mensajeConfiguracion}</p>}
            {errorConfiguracion && <p className="ma-error-text">{errorConfiguracion}</p>}

            {configuracion && (
              <div className="two">
                <div>
                  <div className="stitle" style={{ fontSize: 13 }}>Canales de notificación</div>
                  <div className="card" style={{ marginBottom: 14 }}>
                    <div className="sw-row">
                      <div className="sw-info">
                        <div className="sw-label"><i className="ti ti-device-mobile" style={{ fontSize: 14, verticalAlign: -2, color: '#1D4ED8' }} /> SMS (Twilio)</div>
                        <div className="sw-sub">Canal primario universal — sin internet</div>
                      </div>
                      <div
                          className={`switch ${configuracion.canalSmsHabilitado ? 'on' : ''}`}
                          onClick={() => !guardandoConfiguracion && cambiarToggle('canalSmsHabilitado')}
                      />
                    </div>
                    <div className="sw-row">
                      <div className="sw-info">
                        <div className="sw-label"><i className="ti ti-brand-whatsapp" style={{ fontSize: 14, verticalAlign: -2, color: '#059669' }} /> WhatsApp Business</div>
                        <div className="sw-sub">Canal secundario — requiere datos móviles</div>
                      </div>
                      <div
                          className={`switch ${configuracion.canalWhatsappHabilitado ? 'on' : ''}`}
                          onClick={() => !guardandoConfiguracion && cambiarToggle('canalWhatsappHabilitado')}
                      />
                    </div>
                    <div className="sw-row">
                      <div className="sw-info">
                        <div className="sw-label"><i className="ti ti-mail" style={{ fontSize: 14, verticalAlign: -2, color: '#7C3AED' }} /> Email</div>
                        <div className="sw-sub">Canal de respaldo — para pacientes con correo</div>
                      </div>
                      <div
                          className={`switch ${configuracion.canalEmailHabilitado ? 'on' : ''}`}
                          onClick={() => !guardandoConfiguracion && cambiarToggle('canalEmailHabilitado')}
                      />
                    </div>
                  </div>

                  {!configuracion.canalSmsHabilitado && !configuracion.canalWhatsappHabilitado && !configuracion.canalEmailHabilitado && (
                    <div className="warn-box">
                      <i className="ti ti-alert-triangle" />
                      <span>Sin ningún canal habilitado, el sistema no podrá enviar ninguna notificación ni recordatorio.</span>
                    </div>
                  )}
                </div>

                <div>
                  <div className="stitle" style={{ fontSize: 13 }}>Recordatorios automáticos</div>
                  <div className="card" style={{ marginBottom: 14 }}>
                    <div className="sw-row">
                      <div className="sw-info">
                        <div className="sw-label">Recordatorio 48 horas antes</div>
                        <div className="sw-sub">Envío diario a las 09:00 hrs (hora Chile)</div>
                      </div>
                      <div
                          className={`switch ${configuracion.recordatorio48hHabilitado ? 'on' : ''}`}
                          onClick={() => !guardandoConfiguracion && cambiarToggle('recordatorio48hHabilitado')}
                      />
                    </div>
                    <div className="sw-row">
                      <div className="sw-info">
                        <div className="sw-label">Recordatorio 24 horas antes</div>
                        <div className="sw-sub">Envío diario a las 09:00 hrs (hora Chile)</div>
                      </div>
                      <div
                          className={`switch ${configuracion.recordatorio24hHabilitado ? 'on' : ''}`}
                          onClick={() => !guardandoConfiguracion && cambiarToggle('recordatorio24hHabilitado')}
                      />
                    </div>
                  </div>

                  <div className="stitle" style={{ fontSize: 13 }}>Escalamiento automático</div>
                  <div className="card" style={{ marginBottom: 14 }}>
                    <div style={{ marginBottom: 12 }}>
                      <label className="form-label">Tiempo de espera antes de escalar</label>
                      <select
                          className="form-select"
                          value={configuracion.escalacionMinutosEspera}
                          disabled={guardandoConfiguracion}
                          onChange={(e) => cambiarEscalamiento('escalacionMinutosEspera', Number(e.target.value))}
                      >
                        <option value={1}>1 minuto (pruebas)</option>
                        <option value={5}>5 minutos (pruebas)</option>
                        <option value={30}>30 minutos</option>
                        <option value={60}>60 minutos</option>
                        <option value={90}>90 minutos</option>
                        <option value={120}>120 minutos</option>
                      </select>
                    </div>
                    <div>
                      <label className="form-label">Máximo de intentos por paciente</label>
                      <select
                          className="form-select"
                          value={configuracion.escalacionMaxIntentos}
                          disabled={guardandoConfiguracion}
                          onChange={(e) => cambiarEscalamiento('escalacionMaxIntentos', Number(e.target.value))}
                      >
                        <option value={2}>2 intentos</option>
                        <option value={3}>3 intentos</option>
                        <option value={4}>4 intentos</option>
                      </select>
                    </div>
                  </div>

                  <div className="info-box">
                    <i className="ti ti-info-circle" />
                    <span>Última actualización: {formatDate(configuracion.actualizadoEn)}. Los cambios se aplican de inmediato — no requieren reiniciar el sistema.</span>
                  </div>
                </div>
              </div>
            )}
          </>
        )}

        {tab === 'bitacora' && (
          <>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16, flexWrap: 'wrap', gap: 10 }}>
              <div className="stitle" style={{ marginBottom: 0 }}>
                <i className="ti ti-list-check" style={{ color: '#059669' }} /> Bitácora de eventos del sistema
              </div>
              <input
                  type="date"
                  className="form-input"
                  style={{ width: 160, fontSize: 12 }}
                  value={fechaBitacora}
                  onChange={(e) => setFechaBitacora(e.target.value)}
              />
            </div>

            {cargandoBitacora && !bitacora && <p className="ma-vacio">Cargando bitácora…</p>}
            {!cargandoBitacora && !bitacora && (
              <div className="card ma-vacio-card"><p>No pudimos cargar la bitácora.</p></div>
            )}

            {bitacora && (
              <div className="two">
                <div>
                  <div className="stitle" style={{ fontSize: 13 }}>Timeline del día</div>
                  <div className="card">
                    {bitacora.entradas.length === 0 ? (
                      <p className="ma-vacio">Sin eventos registrados este día.</p>
                    ) : (
                      <div className="timeline">
                        {bitacora.entradas.map((entrada, i) => {
                          const estilo = ESTILO_TIPO_BITACORA[entrada.tipo] ?? { bg: '#F1F5F9', color: '#64748B', icono: 'ti-point' };
                          return (
                            <div key={i} className="tl-item">
                              <div className="tl-dot" style={{ background: estilo.bg, color: estilo.color }}>
                                <i className={`ti ${estilo.icono}`} style={{ fontSize: 11 }} />
                              </div>
                              <div className="tl-time">{formatHora(entrada.fecha)}</div>
                              <div className="tl-title">{entrada.titulo}</div>
                              {entrada.detalle && <div className="tl-sub">{entrada.detalle}</div>}
                            </div>
                          );
                        })}
                      </div>
                    )}
                  </div>
                </div>

                <div>
                  <div className="stitle" style={{ fontSize: 13 }}>Errores del sistema</div>
                  <div className="card">
                    {bitacora.erroresHoy === 0 ? (
                      <div style={{ textAlign: 'center', padding: 20, color: '#94A3B8' }}>
                        <i className="ti ti-circle-check" style={{ fontSize: 32, color: '#10B981', display: 'block', marginBottom: 8 }} />
                        <div style={{ fontSize: 13, fontWeight: 500, color: '#059669' }}>Sin errores críticos este día</div>
                        <div style={{ fontSize: 12, marginTop: 4 }}>Todos los envíos se procesaron correctamente</div>
                      </div>
                    ) : (
                      <div className="warn-box">
                        <i className="ti ti-alert-triangle" />
                        <span>{bitacora.erroresHoy} notificación(es) fallida(s) este día — revisa el Historial de avisos para más detalle.</span>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
