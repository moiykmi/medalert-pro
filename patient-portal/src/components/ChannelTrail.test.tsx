import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { ChannelTrail } from './ChannelTrail';

describe('ChannelTrail', () => {
  it('renders the three channels in escalation order with their Spanish labels', () => {
    render(<ChannelTrail canalesIntentados={[]} />);
    const items = screen.getAllByRole('listitem');
    expect(items).toHaveLength(3);
    expect(items[0]).toHaveTextContent('SMS');
    expect(items[1]).toHaveTextContent('WhatsApp');
    expect(items[2]).toHaveTextContent('Correo');
  });

  it('marks only the attempted channels as intentado', () => {
    const { container } = render(<ChannelTrail canalesIntentados={['SMS', 'WHATSAPP']} />);
    const dots = container.querySelectorAll('.trail__dot');
    expect(dots[0]).toHaveClass('trail__dot--intentado');
    expect(dots[1]).toHaveClass('trail__dot--intentado');
    expect(dots[2]).not.toHaveClass('trail__dot--intentado');
  });

  it('marks the active channel distinctly from merely attempted ones', () => {
    const { container } = render(
      <ChannelTrail canalesIntentados={['SMS', 'WHATSAPP']} canalActivo="WHATSAPP" />
    );
    const dots = container.querySelectorAll('.trail__dot');
    expect(dots[0]).not.toHaveClass('trail__dot--activo');
    expect(dots[1]).toHaveClass('trail__dot--activo');
    expect(dots[1]).toHaveClass('trail__dot--intentado');
    expect(dots[2]).not.toHaveClass('trail__dot--activo');
  });
});
