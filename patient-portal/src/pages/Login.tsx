import { FormEvent, useState } from 'react';
import { api, ApiError, Sesion } from '../api/client';
import { Button } from '../components/Button';
import { Card } from '../components/Card';
import { TextField } from '../components/TextField';
import { formatearRut } from '../utils/rut';
import './Login.css';

interface LoginProps {
  onIngreso: (sesion: Sesion) => void;
}

type Paso = 'rut' | 'codigo';

export function Login({ onIngreso }: LoginProps) {
  const [paso, setPaso] = useState<Paso>('rut');
  const [rut, setRut] = useState('');
  const [codigo, setCodigo] = useState('');
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [avisoEnviado, setAvisoEnviado] = useState(false);

  async function solicitarCodigo(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setCargando(true);
    try {
      await api.solicitarOtp(rut);
      setPaso('codigo');
      setAvisoEnviado(true);
    } catch (err) {
      setError(
        err instanceof ApiError && err.status === 404
          ? 'No encontramos ese RUT en el sistema. Verifica que esté bien escrito.'
          : 'No pudimos enviar el código. Inténtalo de nuevo en unos segundos.'
      );
    } finally {
      setCargando(false);
    }
  }

  async function verificarCodigo(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setCargando(true);
    try {
      const sesion = await api.verificarOtp(rut, codigo);
      onIngreso(sesion);
    } catch (err) {
      setError(
        err instanceof ApiError && err.status === 401
          ? 'El código no es correcto o ya expiró. Solicita uno nuevo si es necesario.'
          : 'No pudimos verificar el código. Inténtalo de nuevo.'
      );
    } finally {
      setCargando(false);
    }
  }

  return (
    <div className="login">
      <div className="login__intro">
        <p className="login__eyebrow">Consultorio Tomás Rojas Vergara · Los Lagos</p>
        <h1>Portal de Pacientes</h1>
        <p className="login__subtitle">
          Revisa el estado de tus citas y confirma los avisos que te hemos enviado.
        </p>
      </div>

      <Card className="login__card">
        {paso === 'rut' && (
          <form onSubmit={solicitarCodigo} className="login__form" noValidate>
            <TextField
              label="Tu RUT"
              hint="Lo usamos para enviarte un código de verificación por SMS o correo."
              placeholder="11.111.111-1"
              value={rut}
              onChange={(e) => setRut(formatearRut(e.target.value))}
              inputMode="text"
              autoComplete="off"
              required
              error={error ?? undefined}
            />
            <Button type="submit" fullWidth disabled={cargando || rut.length < 3}>
              {cargando ? 'Enviando código…' : 'Enviar código de verificación'}
            </Button>
          </form>
        )}

        {paso === 'codigo' && (
          <form onSubmit={verificarCodigo} className="login__form" noValidate>
            {avisoEnviado && (
              <p className="login__aviso" role="status">
                Te enviamos un código de 6 dígitos. Puede tardar unos segundos en llegar.
              </p>
            )}
            <TextField
              label="Código de verificación"
              hint="Ingresa los 6 dígitos que te enviamos."
              placeholder="000000"
              value={codigo}
              onChange={(e) => setCodigo(e.target.value.replace(/\D/g, '').slice(0, 6))}
              inputMode="numeric"
              autoComplete="one-time-code"
              maxLength={6}
              required
              error={error ?? undefined}
            />
            <Button type="submit" fullWidth disabled={cargando || codigo.length !== 6}>
              {cargando ? 'Verificando…' : 'Ingresar'}
            </Button>
            <Button
              type="button"
              variant="ghost"
              onClick={() => {
                setPaso('rut');
                setCodigo('');
                setError(null);
              }}
            >
              Usar otro RUT
            </Button>
          </form>
        )}
      </Card>
    </div>
  );
}
