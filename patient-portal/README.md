# patient-portal (HU09-11 — Frontend)

Portal web del paciente: React 18 + TypeScript + Vite. Consume la API de
`patient-portal-service` (puerto 8083).

## Decisiones de diseño (para tu informe/defensa)

- **Accesibilidad primero (WCAG 2.1 AA):** tipografía **Atkinson Hyperlegible**
  para todo el texto de lectura — es una fuente diseñada específicamente para
  legibilidad y baja visión, coherente con tu población objetivo (adultos
  mayores, zona rural). Foco de teclado siempre visible, etiquetas asociadas a
  cada campo, mensajes de error anunciados con `role="alert"`, y se respeta
  `prefers-reduced-motion`.
- **Mobile-first:** botones grandes (mínimo 52px de alto), un solo flujo lineal
  sin menús ni navegación compleja — pensado para el paciente que abre el link
  desde un SMS en su celular.
- **Elemento visual distintivo:** la "línea de canal" (`ChannelTrail`) muestra
  el recorrido real de escalamiento de tu sistema (SMS → WhatsApp → Correo),
  en vez de un ícono genérico de notificación — refleja literalmente el
  comportamiento de HU06-08 que ya construiste en el backend.
- **Paleta:** verde azulado (`#0B5E64`) como color principal — transmite
  confianza clínica sin ser el azul genérico de SaaS — con un acento cálido en
  ámbar (`#D98E2B`) reservado solo para elementos activos/de atención.

## Requisitos previos
- Node.js 18+ instalado.
- `patient-portal-service` corriendo en `http://localhost:8083` (puerto
  configurado en `src/api/client.ts` — si lo cambias ahí, cámbialo también).
- Al menos un paciente de prueba con OTP funcionando (ver README de
  `patient-portal-service`).

## Cómo correrlo

```bash
cd patient-portal
npm install
npm run dev
```

Abre `http://localhost:5173` en el navegador.

## Flujo de prueba manual

1. Ingresa el RUT de un paciente de prueba (ej. `11111111-1`).
2. Revisa el log de `patient-portal-service` para ver el código OTP generado
   (mientras no tengas SMS real funcionando, esta es tu forma de probar).
3. Ingresa el código de 6 dígitos.
4. Deberías ver la pantalla principal con "Avisos de cancelación" y "Mis citas".
5. Si hay un aviso pendiente, haz click en "Confirmar que recibí este aviso" —
   debería desaparecer el botón y cambiar el estado a "Confirmado".
6. Recarga la página (F5) — la sesión debería mantenerse (queda guardada en
   `localStorage` hasta que cierres sesión o expire en el backend, 30 min).

## Compilar para producción

```bash
npm run build
```
Genera la carpeta `dist/` lista para servir estáticamente (ej. detrás de Nginx,
o en el mismo servidor que el resto de la infraestructura Docker).

## Estructura

```
src/
├── api/client.ts        # Cliente HTTP tipado hacia patient-portal-service
├── components/           # Button, Card, TextField, ChannelTrail (reutilizables)
├── pages/
│   ├── Login.tsx          # Flujo RUT -> OTP
│   └── Portal.tsx         # Citas + avisos + confirmación
├── styles/global.css     # Tokens de diseño (color, tipografía, foco)
└── utils/rut.ts          # Formateo y validación de RUT chileno
```

## Troubleshooting
- **Pantalla en blanco / error de red al cargar** → confirma que
  `patient-portal-service` esté corriendo. Si ves un error de CORS en la
  consola del navegador, verifica que tengas la clase `CorsConfig.java` en el
  backend (agregada junto con este frontend) y que el servicio se haya
  reiniciado después de agregarla.
- **El código OTP nunca es válido** → revisa que no haya expirado (5 min) o
  que no lo estés reutilizando (es de un solo uso).
