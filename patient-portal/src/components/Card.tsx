import { HTMLAttributes } from 'react';
import './Card.css';

export function Card({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return <div className={['card', className].filter(Boolean).join(' ')} {...props} />;
}
