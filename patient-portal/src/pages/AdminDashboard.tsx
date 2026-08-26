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
import { AdminDashboardEvent, AdminDashboardKpis, api, ApiError, Paciente, Profesional, ReporteMensual } from '../api/client';
import './AdminDashboard.css';

const STORAGE_KEY = 'medalert_admin_token';
const POLLING_MS = 20_000;

type Tab = 'dashboard' | 'agenda' | 'pacientes' | 'notificaciones' | 'reportes';

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

export function AdminDashboard() {
  const [adminToken, setAdminToken] = useState(() => localStorage.getItem(STORAGE_KEY) ?? '');
  const [draftToken, setDraftToken] = useState('');
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

  const [pacientes, setPacientes] = useState<Paciente[]>([]);
  const [cargandoPacientes, setCargandoPacientes] = useState(false);
  const [busquedaPaciente, setBusquedaPaciente] = useState('');

  const [periodoReporte, setPeriodoReporte] = useState(mesActualISO);
  const [reporte, setReporte] = useState<ReporteMensual | null>(null);
  const [cargandoReporte, setCargandoReporte] = useState(false);

  async function cargarDashboard(tokenActual: string) {
    setCargando(true);
    setError(null);
    try {
      const [kpisResp, eventosResp] = await Promise.all([
        api.adminDashboardKpis(tokenActual),
        api.adminDashboardEventos(tokenActual, 20),
      ]);
      setKpis(kpisResp);
      setEventos(eventosResp);
    } catch (err) {
      if (err instanceof ApiError && (err.status === 401 || err.status === 403)) {
        setAdminToken('');
        localStorage.removeItem(STORAGE_KEY);
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
    setAdminToken(draftToken.trim());
    setDraftToken('');
  }

  if (!adminToken) {
    return (
      <div className="ma-login-wrap">
        <div className="ma-login-card">
          <div className="logo" style={{ marginBottom: 18, color: '#0F172A' }}>
            <i className="ti ti-bell-ringing" style={{ color: '#0C447C' }} />
            MedAlert Pro
            <span className="logo-sub" style={{ color: '#64748B' }}>Panel Administrativo</span>
          </div>
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
            <i className="ti ti-send" />Notificaciones
          </button>
          <button className={`nb ${tab === 'reportes' ? 'on' : ''}`} onClick={() => setTab('reportes')}>
            <i className="ti ti-chart-bar" />Reportes
          </button>
        </div>
        <button
            className="nb"
            title="Cerrar acceso admin"
            onClick={() => {
              localStorage.removeItem(STORAGE_KEY);
              setAdminToken('');
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
            <div className="stitle"><i className="ti ti-calendar-event" style={{ color: '#059669' }} /> Registrar ausencia médica</div>
            <div className="two">
              <div>
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
              </div>

              <div>
                <div className="stitle" style={{ fontSize: 13 }}>Profesionales del día</div>
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

            {cargandoPacientes && <p className="ma-vacio">Cargando pacientes…</p>}

            {!cargandoPacientes && pacientesFiltrados.length === 0 && (
              <div className="card ma-vacio-card">
                <p>{pacientes.length === 0 ? 'No hay pacientes registrados.' : 'Sin resultados para tu búsqueda.'}</p>
              </div>
            )}

            {!cargandoPacientes && pacientesFiltrados.map((p) => (
              <div key={p.id} className="pat-card">
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
          </>
        )}

        {tab === 'notificaciones' && (
          <>
            <div className="stitle"><i className="ti ti-send" style={{ color: '#1D4ED8' }} /> Centro de notificaciones — eventos recientes</div>
            {eventos.length === 0 ? (
              <div className="card ma-vacio-card"><p>No hay eventos recientes.</p></div>
            ) : (
              eventos.map((evento) => (
                <div key={evento.eventoId} className="card" style={{ marginBottom: 10 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12, marginBottom: 6 }}>
                    <span style={{ fontFamily: 'inherit', fontWeight: 700, fontSize: 13 }}>Evento #{evento.eventoId}</span>
                    <span style={{ color: '#94A3B8', fontSize: 12 }}>{formatDate(evento.fechaEvento)}</span>
                  </div>
                  <p style={{ fontSize: 12, color: '#64748B' }}>Motivo: {evento.motivo ?? 'Sin motivo'}</p>
                  <p style={{ fontSize: 12, color: '#64748B' }}>
                    Pacientes notificados: {evento.pacientesNotificados} · Confirmados: {evento.notificacionesConfirmadas}
                  </p>
                  <p style={{ fontSize: 12, color: '#64748B' }}>
                    Tiempo total: {evento.minutosTotalesNotificacion == null ? 'Sin datos' : `${evento.minutosTotalesNotificacion.toFixed(2)} min`}
                  </p>
                </div>
              ))
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
      </div>
    </div>
  );
}
