import { FormEvent, useEffect, useMemo, useState } from 'react';
import { api, ApiError, Cita, Notificacion, Paciente, Sesion } from '../api/client';
import { Button } from '../components/Button';
import { Card } from '../components/Card';
import { ChannelTrail } from '../components/ChannelTrail';
import { TextField } from '../components/TextField';
import './Portal.css';

interface PortalProps {
  sesion: Sesion;
  onSalir: () => void;
}

const ESTADO_CITA_LABEL: Record<string, string> = {
  AGENDADA: 'Agendada',
  CANCELADA: 'Cancelada',
  REAGENDADA: 'Reagendada',
  ATENDIDA: 'Atendida',
  NO_ASISTIO: 'No asistió',
};

interface AvisoAgrupado {
  eventoId: number;
  citaId: number | null;
  canalesIntentados: string[];
  ultimaNotificacion: Notificacion;
  estado: 'confirmado' | 'pendiente' | 'reintentando' | 'sin_respuesta';
}

function agruparPorEvento(notificaciones: Notificacion[]): AvisoAgrupado[] {
  const grupos = new Map<number, Notificacion[]>();
  for (const n of notificaciones) {
    const lista = grupos.get(n.eventoId) ?? [];
    lista.push(n);
    grupos.set(n.eventoId, lista);
  }

  return Array.from(grupos.entries())
      .map(([eventoId, items]) => {
        const ordenados = [...items].sort((a, b) => a.intentoNumero - b.intentoNumero);
        const ultima = ordenados[ordenados.length - 1];
        const confirmado = items.some((n) => n.estadoEnvio === 'CONFIRMADO');

        // Si el último intento falló al enviarse, el sistema todavía está
        // reintentando por otro canal (escalamiento) — no tiene sentido pedirle
        // al paciente que confirme un mensaje que nunca le llegó.
        // Si agotó los 3 canales sin confirmación (SIN_RESPUESTA), igual se le
        // permite confirmar por si el mensaje sí llegó pero nunca hizo click —
        // solo cambia la etiqueta para que quede claro que el sistema ya dejó
        // de reintentar automáticamente.
        const estado: AvisoAgrupado['estado'] = confirmado
            ? 'confirmado'
            : ultima.estadoEnvio === 'FALLIDO'
                ? 'reintentando'
                : ultima.estadoEnvio === 'SIN_RESPUESTA'
                    ? 'sin_respuesta'
                    : 'pendiente';

        return {
          eventoId,
          citaId: ultima.citaId,
          canalesIntentados: ordenados.map((n) => n.canal),
          ultimaNotificacion: ultima,
          estado,
        };
      })
      .sort((a, b) => b.eventoId - a.eventoId);
}

function formatearFecha(iso: string | null): string {
  if (!iso) return 'Sin fecha';
  const fecha = new Date(iso);
  return fecha.toLocaleString('es-CL', {
    day: '2-digit',
    month: 'long',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export function Portal({ sesion, onSalir }: PortalProps) {
  const [citas, setCitas] = useState<Cita[]>([]);
  const [notificaciones, setNotificaciones] = useState<Notificacion[]>([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [confirmandoId, setConfirmandoId] = useState<number | null>(null);
  const [mensajeEstado, setMensajeEstado] = useState<string | null>(null);
  const [reagendandoId, setReagendandoId] = useState<number | null>(null);
  const [nuevaFecha, setNuevaFecha] = useState<Record<number, string>>({});
  const [enviandoReagendo, setEnviandoReagendo] = useState<number | null>(null);

  const [perfil, setPerfil] = useState<Paciente | null>(null);
  const [editandoPerfil, setEditandoPerfil] = useState(false);
  const [telefonoDraft, setTelefonoDraft] = useState('');
  const [emailDraft, setEmailDraft] = useState('');
  const [canalDraft, setCanalDraft] = useState<'SMS' | 'WHATSAPP' | 'EMAIL'>('SMS');
  const [guardandoPerfil, setGuardandoPerfil] = useState(false);

  async function cargarDatos() {
    setCargando(true);
    setError(null);
    try {
      const [citasResp, notifResp, perfilResp] = await Promise.all([
        api.misCitas(sesion.token),
        api.misNotificaciones(sesion.token),
        api.miPerfil(sesion.token),
      ]);
      setCitas(citasResp);
      setNotificaciones(notifResp);
      setPerfil(perfilResp);
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onSalir();
        return;
      }
      setError('No pudimos cargar tu información. Intenta recargar la página.');
    } finally {
      setCargando(false);
    }
  }

  useEffect(() => {
    cargarDatos();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const avisos = useMemo(() => agruparPorEvento(notificaciones), [notificaciones]);

  function abrirEdicionPerfil() {
    setTelefonoDraft(perfil?.telefono ?? '');
    setEmailDraft(perfil?.email ?? '');
    setCanalDraft(perfil?.canalPreferido ?? 'SMS');
    setEditandoPerfil(true);
  }

  async function guardarPerfil(e: FormEvent) {
    e.preventDefault();
    setGuardandoPerfil(true);
    setMensajeEstado(null);
    try {
      const actualizado = await api.actualizarDatosContacto(
          { telefono: telefonoDraft.trim(), email: emailDraft.trim(), canalPreferido: canalDraft },
          sesion.token,
      );
      setPerfil(actualizado);
      setEditandoPerfil(false);
      setMensajeEstado('Tus datos de contacto fueron actualizados.');
    } catch (err) {
      setMensajeEstado(
          err instanceof ApiError ? err.message : 'No pudimos actualizar tus datos. Intenta de nuevo.',
      );
    } finally {
      setGuardandoPerfil(false);
    }
  }

  async function reagendar(citaId: number) {
    const valor = nuevaFecha[citaId];
    if (!valor) return;

    setEnviandoReagendo(citaId);
    setMensajeEstado(null);
    try {
      await api.reagendarCita(citaId, new Date(valor).toISOString(), sesion.token);
      setMensajeEstado('Tu cita fue reagendada.');
      setReagendandoId(null);
      await cargarDatos();
    } catch (err) {
      setMensajeEstado(
          err instanceof ApiError ? err.message : 'No pudimos reagendar tu cita. Intenta de nuevo.',
      );
    } finally {
      setEnviandoReagendo(null);
    }
  }

  async function confirmar(id: number) {
    setConfirmandoId(id);
    setMensajeEstado(null);
    try {
      await api.confirmarNotificacion(id, sesion.token);
      setMensajeEstado('Confirmación registrada. Gracias por avisarnos.');
      await cargarDatos();
    } catch {
      setMensajeEstado('No pudimos registrar tu confirmación. Intenta de nuevo.');
    } finally {
      setConfirmandoId(null);
    }
  }

  return (
      <div className="portal">
        <header className="portal__header">
          <div>
            <p className="portal__eyebrow">Hola,</p>
            <h1>{sesion.nombre}</h1>
          </div>
          <Button variant="ghost" onClick={onSalir}>
            Cerrar sesión
          </Button>
        </header>

        {error && (
            <Card className="portal__error" role="alert">
              <p>{error}</p>
              <Button variant="secondary" onClick={cargarDatos}>
                Reintentar
              </Button>
            </Card>
        )}

        {mensajeEstado && (
            <p className="portal__aviso-global" role="status">
              {mensajeEstado}
            </p>
        )}

        <section aria-labelledby="avisos-titulo">
          <h2 id="avisos-titulo" className="portal__seccion-titulo">
            Avisos de cancelación
          </h2>

          {cargando && <p className="portal__vacio">Cargando tus avisos…</p>}

          {!cargando && avisos.length === 0 && (
              <Card className="portal__vacio-card">
                <p>No tienes avisos de cancelación por ahora.</p>
              </Card>
          )}

          <div className="portal__lista">
            {avisos.map((aviso) => (
                <Card key={aviso.eventoId} className="aviso">
                  <div className="aviso__encabezado">
                    <div>
                      <p className="aviso__fecha">
                        {aviso.estado === 'reintentando'
                            ? 'Estamos intentando contactarte por otro medio'
                            : `Enviado el ${formatearFecha(aviso.ultimaNotificacion.enviadoEn)}`}
                      </p>
                      <p className="aviso__texto">
                        Tu cita fue cancelada. Te contactamos por los siguientes canales:
                      </p>
                    </div>
                    <span className={`badge badge--estado-${aviso.estado}`}>
                  {aviso.estado === 'confirmado' && 'Confirmado'}
                      {aviso.estado === 'pendiente' && 'Pendiente'}
                      {aviso.estado === 'reintentando' && 'Reintentando'}
                      {aviso.estado === 'sin_respuesta' && 'Sin confirmar'}
                </span>
                  </div>

                  <ChannelTrail
                      canalesIntentados={aviso.canalesIntentados}
                      canalActivo={aviso.ultimaNotificacion.canal}
                  />

                  {(aviso.estado === 'pendiente' || aviso.estado === 'sin_respuesta') && (
                      <Button
                          onClick={() => confirmar(aviso.ultimaNotificacion.id)}
                          disabled={confirmandoId === aviso.ultimaNotificacion.id}
                      >
                        {confirmandoId === aviso.ultimaNotificacion.id
                            ? 'Confirmando…'
                            : 'Confirmar que recibí este aviso'}
                      </Button>
                  )}

                  {aviso.estado === 'reintentando' && (
                      <p className="aviso__nota">
                        No pudimos contactarte por {aviso.ultimaNotificacion.canal === 'SMS' ? 'SMS' : aviso.ultimaNotificacion.canal === 'WHATSAPP' ? 'WhatsApp' : 'correo'} — el sistema intentará por otro canal en los próximos minutos.
                      </p>
                  )}
                </Card>
            ))}
          </div>
        </section>

        <section aria-labelledby="citas-titulo">
          <h2 id="citas-titulo" className="portal__seccion-titulo">
            Mis citas
          </h2>

          {!cargando && citas.length === 0 && (
              <Card className="portal__vacio-card">
                <p>No tienes citas registradas.</p>
              </Card>
          )}

          <div className="portal__lista">
            {citas.map((cita) => (
                <Card key={cita.id} className="cita-card">
                  <div className="cita">
                    <div>
                      <p className="cita__fecha">{formatearFecha(cita.fechaHora)}</p>
                      <p className="cita__meta">Cita N.º {cita.id}</p>
                    </div>
                    <span className={`badge badge--${cita.estado.toLowerCase()}`}>
                  {ESTADO_CITA_LABEL[cita.estado] ?? cita.estado}
                </span>
                  </div>

                  {cita.estado === 'CANCELADA' && reagendandoId !== cita.id && (
                      <Button variant="secondary" onClick={() => setReagendandoId(cita.id)}>
                        Reagendar
                      </Button>
                  )}

                  {cita.estado === 'CANCELADA' && reagendandoId === cita.id && (
                      <div className="cita__reagendar-form">
                        <input
                            type="datetime-local"
                            className="cita__reagendar-input"
                            value={nuevaFecha[cita.id] ?? ''}
                            onChange={(e) =>
                                setNuevaFecha((prev) => ({ ...prev, [cita.id]: e.target.value }))
                            }
                        />
                        <div className="cita__reagendar-acciones">
                          <Button
                              onClick={() => reagendar(cita.id)}
                              disabled={!nuevaFecha[cita.id] || enviandoReagendo === cita.id}
                          >
                            {enviandoReagendo === cita.id ? 'Reagendando…' : 'Confirmar nueva fecha'}
                          </Button>
                          <Button variant="ghost" onClick={() => setReagendandoId(null)}>
                            Cancelar
                          </Button>
                        </div>
                      </div>
                  )}
                </Card>
            ))}
          </div>
        </section>

        <section aria-labelledby="datos-titulo">
          <h2 id="datos-titulo" className="portal__seccion-titulo">
            Mis datos de contacto
          </h2>

          {perfil && !editandoPerfil && (
              <Card className="datos-contacto">
                <div className="datos-contacto__fila">
                  <span className="datos-contacto__label">Teléfono</span>
                  <span>{perfil.telefono ?? 'Sin registrar'}</span>
                </div>
                <div className="datos-contacto__fila">
                  <span className="datos-contacto__label">Correo</span>
                  <span>{perfil.email ?? 'Sin registrar'}</span>
                </div>
                <div className="datos-contacto__fila">
                  <span className="datos-contacto__label">Canal preferido</span>
                  <span>{perfil.canalPreferido}</span>
                </div>
                <Button variant="secondary" onClick={abrirEdicionPerfil}>
                  Actualizar datos
                </Button>
              </Card>
          )}

          {perfil && editandoPerfil && (
              <Card className="datos-contacto">
                <form onSubmit={guardarPerfil} className="datos-contacto__form">
                  <TextField
                      label="Teléfono"
                      value={telefonoDraft}
                      onChange={(e) => setTelefonoDraft(e.target.value)}
                      placeholder="+56912345678"
                      hint="Incluye el código de país, ej: +56912345678"
                  />
                  <TextField
                      label="Correo"
                      type="email"
                      value={emailDraft}
                      onChange={(e) => setEmailDraft(e.target.value)}
                  />
                  <div className="field">
                    <label htmlFor="canal-preferido" className="field__label">
                      Canal preferido
                    </label>
                    <select
                        id="canal-preferido"
                        className="field__input"
                        value={canalDraft}
                        onChange={(e) => setCanalDraft(e.target.value as 'SMS' | 'WHATSAPP' | 'EMAIL')}
                    >
                      <option value="SMS">SMS</option>
                      <option value="WHATSAPP">WhatsApp</option>
                      <option value="EMAIL">Correo</option>
                    </select>
                  </div>
                  <div className="cita__reagendar-acciones">
                    <Button type="submit" disabled={guardandoPerfil}>
                      {guardandoPerfil ? 'Guardando…' : 'Guardar cambios'}
                    </Button>
                    <Button variant="ghost" onClick={() => setEditandoPerfil(false)}>
                      Cancelar
                    </Button>
                  </div>
                </form>
              </Card>
          )}
        </section>
      </div>
  );
}