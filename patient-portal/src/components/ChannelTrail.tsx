import './ChannelTrail.css';

const ORDEN: Array<'SMS' | 'WHATSAPP' | 'EMAIL'> = ['SMS', 'WHATSAPP', 'EMAIL'];

const ETIQUETAS: Record<string, string> = {
  SMS: 'SMS',
  WHATSAPP: 'WhatsApp',
  EMAIL: 'Correo',
};

interface ChannelTrailProps {
  /** Canales por los que ya se intentó contactar a este paciente, en orden de intento */
  canalesIntentados: string[];
  /** Canal donde efectivamente se logró el envío o la confirmación (si lo hay) */
  canalActivo?: string;
}

/**
 * Elemento visual distintivo del portal: en vez de un ícono genérico de
 * "notificación", muestra el recorrido real de escalamiento del sistema
 * (SMS -> WhatsApp -> Correo), que es el comportamiento central de MedAlert Pro.
 */
export function ChannelTrail({ canalesIntentados, canalActivo }: ChannelTrailProps) {
  return (
    <div className="trail" role="list" aria-label="Canales de notificación intentados">
      {ORDEN.map((canal, index) => {
        const intentado = canalesIntentados.includes(canal);
        const activo = canal === canalActivo;
        return (
          <div className="trail__step" key={canal} role="listitem">
            <span
              className={[
                'trail__dot',
                intentado ? 'trail__dot--intentado' : '',
                activo ? 'trail__dot--activo' : '',
              ].join(' ')}
              aria-hidden="true"
            />
            <span className="trail__label">{ETIQUETAS[canal]}</span>
            {index < ORDEN.length - 1 && <span className="trail__connector" aria-hidden="true" />}
          </div>
        );
      })}
    </div>
  );
}
