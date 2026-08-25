import { describe, expect, it } from 'vitest';
import { esRutValido, formatearRut } from './rut';

describe('esRutValido', () => {
  it('accepts a valid RUT with dash', () => {
    expect(esRutValido('12345678-5')).toBe(true);
  });

  it('accepts a valid RUT with dots and dash', () => {
    expect(esRutValido('12.345.678-5')).toBe(true);
  });

  it('accepts a valid RUT whose verifier digit is K uppercase', () => {
    expect(esRutValido('7654321-6')).toBe(true);
    expect(esRutValido('9999999-3')).toBe(true);
  });

  it('accepts a lowercase k verifier digit', () => {
    // 10000013 -> DV K (uppercased internally)
    expect(esRutValido('10000013-k')).toBe(true);
    expect(esRutValido('10000013-K')).toBe(true);
  });

  it('accepts a RUT with no separators at all', () => {
    expect(esRutValido('123456785')).toBe(true);
  });

  it('rejects a RUT with a wrong checksum digit', () => {
    expect(esRutValido('12345678-9')).toBe(false);
  });

  it('rejects malformed input with letters in the body', () => {
    expect(esRutValido('12A45678-5')).toBe(false);
  });

  it('rejects input shorter than 2 cleaned characters', () => {
    expect(esRutValido('1')).toBe(false);
    expect(esRutValido('')).toBe(false);
  });

  it('rejects input that has no digits, only noise', () => {
    expect(esRutValido('--..')).toBe(false);
  });
});

describe('formatearRut', () => {
  it('returns the cleaned value unchanged when 1 char or fewer', () => {
    expect(formatearRut('1')).toBe('1');
    expect(formatearRut('')).toBe('');
  });

  it('inserts a dash before the verifier digit while typing', () => {
    expect(formatearRut('123456785')).toBe('12345678-5');
  });

  it('strips existing dots and dashes before reformatting', () => {
    expect(formatearRut('12.345.678-5')).toBe('12345678-5');
  });

  it('uppercases a lowercase k verifier digit', () => {
    expect(formatearRut('10000013k')).toBe('10000013-K');
  });

  it('strips non-alphanumeric noise', () => {
    expect(formatearRut('12a34#5678-5')).toBe('12345678-5');
  });
});
