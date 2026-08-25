/** Limpia el RUT dejando solo dígitos y K/k */
function limpiar(rut: string): string {
  return rut.replace(/[^0-9kK]/g, '').toUpperCase();
}

/** Formatea mientras el usuario escribe: 111111111 -> 11111111-1 */
export function formatearRut(valor: string): string {
  const limpio = limpiar(valor);
  if (limpio.length <= 1) return limpio;
  const cuerpo = limpio.slice(0, -1);
  const dv = limpio.slice(-1);
  return `${cuerpo}-${dv}`;
}

/** Calcula el dígito verificador y valida el RUT completo (algoritmo módulo 11) */
export function esRutValido(rut: string): boolean {
  const limpio = limpiar(rut);
  if (limpio.length < 2) return false;

  const cuerpo = limpio.slice(0, -1);
  const dvIngresado = limpio.slice(-1);

  if (!/^\d+$/.test(cuerpo)) return false;

  let suma = 0;
  let multiplicador = 2;
  for (let i = cuerpo.length - 1; i >= 0; i--) {
    suma += Number(cuerpo[i]) * multiplicador;
    multiplicador = multiplicador === 7 ? 2 : multiplicador + 1;
  }

  const resto = 11 - (suma % 11);
  const dvCalculado = resto === 11 ? '0' : resto === 10 ? 'K' : String(resto);

  return dvCalculado === dvIngresado;
}
