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
import { AdminDashboardEvent, AdminDashboardKpis, api, ApiError, Paciente, Profesional } from '../api/client';
import { Button } from '../components/Button';
import { Card } from '../components/Card';
import { TextField } from '../components/TextField';
import './AdminDashboard.css';

const STORAGE_KEY = 'medalert_admin_token';
const POLLING_MS = 20_000;

function hoyISO(): string {
  return new Date().toISOString().slice(0, 10);
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

function pctColor(value: number, goal: number, inverse = false): string {
  const ok = inverse ? value <= goal : value >= goal;
  return ok ? 'kpi__value--ok' : 'kpi__value--warn';
}

export function AdminDashboard() {
  const [adminToken, setAdminToken] = useState(() => localStorage.getItem(STORAGE_KEY) ?? '');
  const [draftToken, setDraftToken] = useState('');
  const [kpis, setKpis] = useState<AdminDashboardKpis | null>(null);
  const [eventos, setEventos] = useState<AdminDashboardEvent[]>([]);
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [profesionalId, setProfesionalId] = useState('');
  const [fechaCancelacion, setFechaCancelacion] = useState(hoyISO);
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

  async function cargarProfesionales(tokenActual: string, fecha: string) {
    setCargandoProfesionales(true);
    try {
      const resp = await api.listarProfesionales(tokenActual, fecha);
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

  async function registrarCancelacion(e: FormEvent) {
    e.preventDefault();
    if (!profesionalId.trim() || !fechaCancelacion) return;

    setEnviandoCancelacion(true);
    setMensajeCancelacion(null);
    setErrorCancelacion(null);
    try {
      const evento = await api.registrarCancelacion(adminToken, {
        profesionalId: Number(profesionalId),
        fecha: fechaCancelacion,
        motivo: motivo.trim() || undefined,
        registradoPor: registradoPor.trim() ? Number(registradoPor) : undefined,
      });
      setMensajeCancelacion(`Evento #${evento.id} registrado (estado: ${evento.estado}).`);
      setProfesionalId('');
      setMotivo('');
      setRegistradoPor('');
      cargarDashboard(adminToken);
      cargarProfesionales(adminToken, fechaCancelacion);
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
    if (!adminToken || !fechaCancelacion) return;
    cargarProfesionales(adminToken, fechaCancelacion);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [adminToken, fechaCancelacion]);

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
      <div className="admin-dashboard">
        <header className="admin-dashboard__header">
          <div>
            <p className="admin-dashboard__eyebrow">MedAlert Pro · Panel Administrativo</p>
            <h1>Dashboard de KPIs</h1>
          </div>
        </header>

        <Card className="admin-dashboard__login-card">
          <form onSubmit={ingresarAdminToken} className="admin-dashboard__login-form">
            <label htmlFor="admin-token">Token administrativo (X-Admin-Token)</label>
            <input
              id="admin-token"
              type="password"
              value={draftToken}
              onChange={(e) => setDraftToken(e.target.value)}
              placeholder="Ingresa el token de administración"
              autoComplete="off"
              required
            />
            <Button type="submit">Ingresar al dashboard</Button>
          </form>
          {error && (
            <p className="admin-dashboard__error" role="alert">
              {error}
            </p>
          )}
        </Card>
      </div>
    );
  }

  return (
    <div className="admin-dashboard">
      <header className="admin-dashboard__header">
        <div>
          <p className="admin-dashboard__eyebrow">MedAlert Pro · Panel Administrativo</p>
          <h1>Dashboard de KPIs</h1>
          {kpis && (
            <p className="admin-dashboard__meta">Actualizado: {formatDate(kpis.generadoEn)}</p>
          )}
        </div>
        <Button
          variant="ghost"
          onClick={() => {
            localStorage.removeItem(STORAGE_KEY);
            setAdminToken('');
          }}
        >
          Cerrar acceso admin
        </Button>
      </header>

      <Card className="admin-dashboard__cancelacion-card">
        <h2 className="admin-dashboard__section-title">Registrar cancelación de cita</h2>
        <p className="admin-dashboard__vacio">
          Registra la ausencia de un profesional para disparar el aviso multicanal a los pacientes agendados ese día.
        </p>
        <form onSubmit={registrarCancelacion} className="admin-dashboard__cancelacion-form">
          <TextField
            label="Fecha de la agenda cancelada"
            type="date"
            value={fechaCancelacion}
            onChange={(e) => setFechaCancelacion(e.target.value)}
            required
          />
          <div className="field">
            <label htmlFor="profesional-ausente" className="field__label">
              Profesional ausente
            </label>
            <select
                id="profesional-ausente"
                className="field__input"
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
          <TextField
            label="Motivo"
            value={motivo}
            onChange={(e) => setMotivo(e.target.value)}
            placeholder="Ej: Licencia médica"
          />
          <TextField
            label="ID del usuario admin que registra (opcional)"
            type="number"
            min={1}
            value={registradoPor}
            onChange={(e) => setRegistradoPor(e.target.value)}
          />
          {profesionalSeleccionado && (
              <p className="admin-dashboard__preview-aviso">
                Se notificará a <strong>{profesionalSeleccionado.citasAgendadas}</strong> paciente
                {profesionalSeleccionado.citasAgendadas === 1 ? '' : 's'} agendado
                {profesionalSeleccionado.citasAgendadas === 1 ? '' : 's'} ese día.
              </p>
          )}
          <Button type="submit" disabled={enviandoCancelacion || !profesionalId}>
            {enviandoCancelacion ? 'Registrando…' : 'Registrar cancelación'}
          </Button>
        </form>
        {mensajeCancelacion && <p className="admin-dashboard__exito" role="status">{mensajeCancelacion}</p>}
        {errorCancelacion && <p className="admin-dashboard__error" role="alert">{errorCancelacion}</p>}
      </Card>

      {error && (
        <Card className="admin-dashboard__error-card" role="alert">
          <p>{error}</p>
          <Button variant="secondary" onClick={() => cargarDashboard(adminToken)}>
            Reintentar
          </Button>
        </Card>
      )}

      {cargando && !kpis && <p className="admin-dashboard__vacio">Cargando dashboard…</p>}

      {!cargando && !kpis && !error && (
        <Card className="admin-dashboard__vacio-card">
          <p>No hay datos aún para mostrar.</p>
        </Card>
      )}

      {kpis && (
        <>
          <section className="admin-dashboard__kpis" aria-label="Indicadores clave">
            <Card className="kpi">
              <p>Tasa de entrega exitosa</p>
              <h2 className={`kpi__value ${pctColor(kpis.deliveryRate.porcentajeExito, 95)}`}>
                {kpis.deliveryRate.porcentajeExito.toFixed(2)}%
              </h2>
              <p>{kpis.deliveryRate.entregasExitosas} de {kpis.deliveryRate.totalNotificaciones} notificaciones</p>
            </Card>

            <Card className="kpi">
              <p>Contacto efectivo (1er intento)</p>
              <h2 className={`kpi__value ${pctColor(kpis.contactEffectiveness.porcentajePrimerIntento, 50)}`}>
                {kpis.contactEffectiveness.porcentajePrimerIntento.toFixed(2)}%
              </h2>
              <p>Escalamiento: {kpis.contactEffectiveness.porcentajeTrasEscalamiento.toFixed(2)}%</p>
            </Card>

            <Card className="kpi">
              <p>Tiempo total por evento</p>
              <h2 className={`kpi__value ${pctColor(kpis.notificationTime.promedioMinutos, 5, true)}`}>
                {kpis.notificationTime.promedioMinutos.toFixed(2)} min
              </h2>
              <p>Máximo observado: {kpis.notificationTime.maximoMinutos.toFixed(2)} min</p>
            </Card>

            <Card className="kpi">
              <p>Reagendamientos por portal</p>
              <h2 className={`kpi__value ${pctColor(kpis.portalReschedule.porcentajeReagendamiento, 30)}`}>
                {kpis.portalReschedule.porcentajeReagendamiento.toFixed(2)}%
              </h2>
              <p>{kpis.portalReschedule.citasReagendadas} de {kpis.portalReschedule.totalCitasCanceladas} citas</p>
            </Card>

            <Card className="kpi">
              <p>Pacientes con contacto actualizado</p>
              <h2 className={`kpi__value ${pctColor(kpis.contactUpdate.porcentajeActualizados, 95)}`}>
                {kpis.contactUpdate.porcentajeActualizados.toFixed(2)}%
              </h2>
              <p>{kpis.contactUpdate.pacientesActualizados} de {kpis.contactUpdate.totalPacientes} pacientes</p>
            </Card>
          </section>

          <section className="admin-dashboard__charts" aria-label="Visualizaciones">
            <Card className="chart-card">
              <h3>Distribución por canal</h3>
              {canalPie.length === 0 ? (
                <p className="admin-dashboard__vacio">Sin notificaciones registradas.</p>
              ) : (
                <div className="chart-wrap">
                  <ResponsiveContainer width="100%" height={260}>
                    <PieChart>
                      <Pie data={canalPie} dataKey="total" nameKey="canal" cx="50%" cy="50%" outerRadius={90} />
                      <Tooltip />
                    </PieChart>
                  </ResponsiveContainer>
                </div>
              )}
            </Card>

            <Card className="chart-card">
              <h3>Estados por canal</h3>
              {canalStacked.length === 0 ? (
                <p className="admin-dashboard__vacio">Sin datos de estado por canal.</p>
              ) : (
                <div className="chart-wrap">
                  <ResponsiveContainer width="100%" height={260}>
                    <BarChart data={canalStacked}>
                      <CartesianGrid strokeDasharray="3 3" />
                      <XAxis dataKey="canal" />
                      <YAxis />
                      <Tooltip />
                      <Legend formatter={(v) => ESTADO_LABEL[v as EstadoCanal] ?? v} />
                      <Bar stackId="a" dataKey="confirmado" fill="#2F7A4D" />
                      <Bar stackId="a" dataKey="pendiente" fill="#D98E2B" />
                      <Bar stackId="a" dataKey="reintentando" fill="#AE3B34" />
                      <Bar stackId="a" dataKey="sin_respuesta" fill="#7B7E83" />
                    </BarChart>
                  </ResponsiveContainer>
                </div>
              )}
            </Card>

            <Card className="chart-card">
              <h3>Frecuencia semanal de cancelaciones</h3>
              {kpis.weeklyCancellationHistory.length === 0 ? (
                <p className="admin-dashboard__vacio">Sin eventos de cancelación aún.</p>
              ) : (
                <div className="chart-wrap">
                  <ResponsiveContainer width="100%" height={260}>
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
                      <Line type="monotone" dataKey="eventos" stroke="#0B5E64" strokeWidth={2} />
                    </LineChart>
                  </ResponsiveContainer>
                </div>
              )}
            </Card>
          </section>

          <section aria-labelledby="eventos-recientes">
            <h2 id="eventos-recientes" className="admin-dashboard__section-title">Eventos recientes</h2>
            {eventos.length === 0 ? (
              <Card className="admin-dashboard__vacio-card">
                <p>No hay eventos recientes.</p>
              </Card>
            ) : (
              <div className="admin-dashboard__event-list">
                {eventos.map((evento) => (
                  <Card key={evento.eventoId} className="event-card">
                    <div className="event-card__row">
                      <p className="event-card__title">Evento #{evento.eventoId}</p>
                      <p className="event-card__date">{formatDate(evento.fechaEvento)}</p>
                    </div>
                    <p className="event-card__meta">Motivo: {evento.motivo ?? 'Sin motivo'}</p>
                    <p className="event-card__meta">
                      Pacientes notificados: {evento.pacientesNotificados} · Confirmados: {evento.notificacionesConfirmadas}
                    </p>
                    <p className="event-card__meta">
                      Tiempo total: {evento.minutosTotalesNotificacion == null ? 'Sin datos' : `${evento.minutosTotalesNotificacion.toFixed(2)} min`}
                    </p>
                  </Card>
                ))}
              </div>
            )}
          </section>

          <section aria-labelledby="pacientes-titulo">
            <div className="admin-dashboard__pacientes-header">
              <h2 id="pacientes-titulo" className="admin-dashboard__section-title">Pacientes</h2>
              <input
                  type="text"
                  className="admin-dashboard__pacientes-buscar"
                  placeholder="Buscar por nombre o RUT..."
                  value={busquedaPaciente}
                  onChange={(e) => setBusquedaPaciente(e.target.value)}
              />
            </div>

            {cargandoPacientes && <p className="admin-dashboard__vacio">Cargando pacientes…</p>}

            {!cargandoPacientes && pacientesFiltrados.length === 0 && (
                <Card className="admin-dashboard__vacio-card">
                  <p>{pacientes.length === 0 ? 'No hay pacientes registrados.' : 'Sin resultados para tu búsqueda.'}</p>
                </Card>
            )}

            {!cargandoPacientes && pacientesFiltrados.length > 0 && (
                <div className="admin-dashboard__pacientes-lista">
                  {pacientesFiltrados.map((p) => (
                      <Card key={p.id} className="paciente-card">
                        <div className="paciente-card__info">
                          <p className="paciente-card__nombre">{p.nombre}</p>
                          <p className="paciente-card__meta">
                            RUT: {p.rut} · Tel: {p.telefono ?? 'sin registrar'}
                          </p>
                          <p className="paciente-card__meta">
                            Canal preferido: {p.canalPreferido}
                            {p.adultoMayor ? ' · Adulto mayor' : ''}
                          </p>
                        </div>
                        <span className={`chip ${p.telefono && p.email ? 'chip--ok' : 'chip--warn'}`}>
                          {p.telefono && p.email ? 'Datos OK' : 'Actualizar datos'}
                        </span>
                      </Card>
                  ))}
                </div>
            )}
          </section>
        </>
      )}
    </div>
  );
}
