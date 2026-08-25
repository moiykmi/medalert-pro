import { useState } from 'react';
import { Sesion } from './api/client';
import { AdminDashboard } from './pages/AdminDashboard';
import { Login } from './pages/Login';
import { Portal } from './pages/Portal';

const STORAGE_KEY = 'medalert_sesion';

function cargarSesionGuardada(): Sesion | null {
  try {
    const crudo = localStorage.getItem(STORAGE_KEY);
    return crudo ? (JSON.parse(crudo) as Sesion) : null;
  } catch {
    return null;
  }
}

export default function App() {
  const adminRoute =
    window.location.pathname.startsWith('/admin') || window.location.pathname.startsWith('/dashboard');
  const [sesion, setSesion] = useState<Sesion | null>(cargarSesionGuardada);

  function ingresar(nuevaSesion: Sesion) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(nuevaSesion));
    setSesion(nuevaSesion);
  }

  function salir() {
    localStorage.removeItem(STORAGE_KEY);
    setSesion(null);
  }

  if (adminRoute) {
    return <AdminDashboard />;
  }

  return sesion ? <Portal sesion={sesion} onSalir={salir} /> : <Login onIngreso={ingresar} />;
}
