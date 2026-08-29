import { FormEvent, useEffect, useState } from 'react';
import { api, ApiError, MedicoAgendaHoy, MedicoSesion } from '../api/client';
import './AdminDashboard.css';

const STORAGE_KEY = 'medalert_medico_sesion';

function hoyISO(): string {
  return new Date().toISOString().slice(0, 10);
}

function cargarSesionGuardada(): MedicoSesion | null {
  try {
    const crudo = localStorage.getItem(STORAGE_KEY);
    return crudo ? (JSON.parse(crudo) as MedicoSesion) : null;
  } catch {
    return null;
  }
}

function iniciales(nombre: string): string {
  const partes = nombre.trim().split(/\s+/);
  return ((partes[0]?.[0] ?? '') + (partes[1]?.[0] ?? '')).toUpperCase();
}

export function MedicoPortal() {
  const [sesion, setSesion] = useState<MedicoSesion | null>(cargarSesionGuardada);

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [ingresando, setIngresando] = useState(false);
  const [errorLogin, setErrorLogin] = useState<string | null>(null);

  const [agenda, setAgenda] = useState<MedicoAgendaHoy | null>(null);
  const [cargandoAgenda, setCargandoAgenda] = useState(false);

  const [mostrarForm, setMostrarForm] = useState(false);
  const [fecha, setFecha] = useState(hoyISO);
  const [horaInicio, setHoraInicio] = useState('');
  const [horaFin, setHoraFin] = useState('');
  const [motivo, setMotivo] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [mensaje, setMensaje] = useState<string | null>(null);
  const [errorAusencia, setErrorAusencia] = useState<string | null>(null);

  const rangoHorarioInvalido = horaInicio !== '' && horaFin !== '' && horaFin <= horaInicio;

  async function cargarAgenda(tokenActual: string) {
    setCargandoAgenda(true);
    try {
      const resp = await api.medicoAgendaHoy(tokenActual);
      setAgenda(resp);
    } catch (err) {
      if (err instanceof ApiError && (err.status === 401 || err.status === 403)) {
        salir();
        return;
      }
      setAgenda(null);
    } finally {
      setCargandoAgenda(false);
    }
  }

  useEffect(() => {
    if (!sesion) return;
    cargarAgenda(sesion.token);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sesion]);

  async function ingresar(e: FormEvent) {
    e.preventDefault();
    setIngresando(true);
    setErrorLogin(null);
    try {
      const nuevaSesion = await api.medicoLogin(email.trim(), password);
      localStorage.setItem(STORAGE_KEY, JSON.stringify(nuevaSesion));
      setSesion(nuevaSesion);
      setPassword('');
    } catch (err) {
      setErrorLogin(err instanceof ApiError ? err.message : 'No pudimos iniciar sesión.');
    } finally {
      setIngresando(false);
    }
  }

  function salir() {
    localStorage.removeItem(STORAGE_KEY);
    setSesion(null);
    setAgenda(null);
  }

  async function reportarAusencia(e: FormEvent) {
    e.preventDefault();
    if (!sesion || rangoHorarioInvalido) return;

    setEnviando(true);
    setMensaje(null);
    setErrorAusencia(null);
    try {
      await api.medicoReportarAusencia(
          {
            fecha,
            horaInicio: horaInicio || undefined,
            horaFin: horaFin || undefined,
            motivo: motivo.trim() || undefined,
          },
          sesion.token,
      );
      setMensaje(`Listo. Se está notificando a tus ${agenda?.citasHoy ?? 0} pacientes agendados.`);
      setMostrarForm(false);
      setMotivo('');
      setHoraInicio('');
      setHoraFin('');
      cargarAgenda(sesion.token);
    } catch (err) {
      setErrorAusencia(err instanceof ApiError ? err.message : 'No pudimos registrar tu ausencia.');
    } finally {
      setEnviando(false);
    }
  }

  if (!sesion) {
    return (
      <div className="ma-login-wrap">
        <div className="ma-login-card">
          <div className="logo" style={{ marginBottom: 18, color: '#0F172A' }}>
            <i className="ti ti-stethoscope" style={{ color: '#0C447C' }} />
            MedAlert Pro
            <span className="logo-sub" style={{ color: '#64748B' }}>Portal del Profesional</span>
          </div>
          <form onSubmit={ingresar} className="ma-login-form">
            <label htmlFor="medico-email" className="form-label">Email</label>
            <input
              id="medico-email"
              type="email"
              className="form-input"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              autoComplete="username"
              required
            />
            <label htmlFor="medico-password" className="form-label" style={{ marginTop: 12 }}>Contraseña</label>
            <input
              id="medico-password"
              type="password"
              className="form-input"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="current-password"
              required
            />
            <button type="submit" className="btn-primary full" style={{ marginTop: 16 }} disabled={ingresando}>
              <i className="ti ti-login" /> {ingresando ? 'Ingresando…' : 'Ingresar'}
            </button>
          </form>
          {errorLogin && <p className="ma-error-text">{errorLogin}</p>}
        </div>
      </div>
    );
  }

  return (
    <div className="ma-app">
      <div className="topbar">
        <div className="logo">
          <i className="ti ti-stethoscope" />
          MedAlert Pro
          <span className="logo-sub">Portal del Profesional</span>
        </div>
        <button className="nb" style={{ marginLeft: 'auto' }} title="Cerrar sesión" onClick={salir}>
          <i className="ti ti-logout" />Cerrar sesión
        </button>
        <div className="avatar">{iniciales(agenda?.nombre ?? sesion.nombre)}</div>
      </div>

      <div className="ma-content" style={{ maxWidth: 640 }}>
        {cargandoAgenda && !agenda && <p className="ma-vacio">Cargando tu agenda…</p>}

        {agenda && (
          <div className="card" style={{ marginBottom: 16 }}>
            <div className="stitle"><i className="ti ti-calendar" style={{ color: '#0C447C' }} /> {agenda.nombre}</div>
            <p className="ma-meta" style={{ marginBottom: 14 }}>{agenda.especialidad}</p>

            <div className="kpis" style={{ marginBottom: 16 }}>
              <div className="kpi">
                <div className="kpi-icon" style={{ background: '#DBEAFE', color: '#1D4ED8' }}><i className="ti ti-users" /></div>
                <div className="kl">Pacientes agendados hoy</div>
                <div className="kv">{agenda.citasHoy}</div>
              </div>
            </div>

            {!mostrarForm && (
              <button className="btn-primary red full" onClick={() => setMostrarForm(true)}>
                <i className="ti ti-calendar-x" /> Reportar mi ausencia de hoy
              </button>
            )}
          </div>
        )}

        {mensaje && <div className="card ok-box" style={{ marginBottom: 16 }}><i className="ti ti-circle-check" /><span>{mensaje}</span></div>}

        {mostrarForm && agenda && (
          <div className="card">
            <div className="stitle" style={{ fontSize: 13, color: '#DC2626' }}><i className="ti ti-alert-triangle" /> Reportar ausencia</div>

            <form onSubmit={reportarAusencia}>
              <div style={{ marginBottom: 14 }}>
                <label className="form-label">Fecha</label>
                <input type="date" className="form-input" value={fecha} onChange={(e) => setFecha(e.target.value)} required />
              </div>

              <div style={{ marginBottom: 14 }}>
                <label className="form-label">Rango de horas (opcional — vacío = todo el día)</label>
                <div style={{ display: 'flex', gap: 10 }}>
                  <input type="time" className="form-input" value={horaInicio} onChange={(e) => setHoraInicio(e.target.value)} aria-label="Hora de inicio" />
                  <input type="time" className="form-input" value={horaFin} onChange={(e) => setHoraFin(e.target.value)} aria-label="Hora de término" />
                </div>
                {rangoHorarioInvalido && (
                  <p className="ma-error-text" style={{ marginTop: 6 }}>La hora de término debe ser posterior a la hora de inicio.</p>
                )}
              </div>

              <div style={{ marginBottom: 16 }}>
                <label className="form-label">Mensaje para tus pacientes (opcional)</label>
                <input
                  type="text"
                  className="form-input"
                  value={motivo}
                  onChange={(e) => setMotivo(e.target.value)}
                  placeholder="Ej: Me encuentro con licencia médica"
                />
              </div>

              <div className="warn-box" style={{ marginBottom: 16 }}>
                <i className="ti ti-alert-triangle" />
                <span>Esta acción notificará a <strong>{agenda.citasHoy}</strong> pacientes de manera inmediata.</span>
              </div>

              <div style={{ display: 'flex', gap: 10 }}>
                <button type="submit" className="btn-primary red full" disabled={enviando || rangoHorarioInvalido}>
                  <i className="ti ti-send" /> {enviando ? 'Enviando…' : 'Confirmar y enviar notificaciones'}
                </button>
                <button type="button" className="btn-sec" onClick={() => setMostrarForm(false)}>Cancelar</button>
              </div>
            </form>

            {errorAusencia && <p className="ma-error-text">{errorAusencia}</p>}
          </div>
        )}
      </div>
    </div>
  );
}
