import { describe, expect, it } from 'vitest';

import {
  apiRequest,
  AppError,
  getErrorTranslationKey,
  getFieldErrors,
  isErrorResponse,
  normalizeApiError,
} from '@/shared/api/errors';
import type { ErrorResponse } from '@/shared/api/generated';

const validationError: ErrorResponse = {
  timestamp: '2026-08-09T12:00:00Z',
  status: 400,
  error: 'Bad Request',
  code: 'VALIDATION_ERROR',
  message: 'Request validation failed',
  path: '/domains',
  details: [
    { field: 'name', code: 'NOT_BLANK', message: 'Name is required' },
    { field: 'name', code: 'NOT_BLANK', message: 'Name is required' },
    { field: 'name', code: 'SIZE', message: 'Name is too long' },
    { code: 'MALFORMED_REQUEST', message: 'Request is malformed' },
  ],
};

describe('API error normalization', () => {
  it('recognizes and preserves a structured backend error', () => {
    expect(isErrorResponse(validationError)).toBe(true);

    const error = normalizeApiError(validationError);

    expect(error).toBeInstanceOf(Error);
    expect(error).toMatchObject({
      kind: 'api',
      code: 'VALIDATION_ERROR',
      status: 400,
      publicMessage: 'Request validation failed',
      retryable: false,
    });
    expect(error.cause).toBe(validationError);
    expect(getErrorTranslationKey(error)).toBe('errors.codes.VALIDATION_ERROR');
  });

  it('extracts deduplicated field errors and skips object-level details', () => {
    expect(getFieldErrors(validationError)).toEqual({
      name: ['Name is required', 'Name is too long'],
    });
  });

  it('accepts a nullable field emitted for an object-level backend detail', () => {
    const objectLevelError = {
      ...validationError,
      details: [{ field: null, code: 'MALFORMED_REQUEST', message: 'Request is malformed' }],
    };

    expect(isErrorResponse(objectLevelError)).toBe(true);
    expect(normalizeApiError(objectLevelError).details).toEqual([
      { code: 'MALFORMED_REQUEST', message: 'Request is malformed' },
    ]);
    expect(getFieldErrors(objectLevelError)).toEqual({});
  });

  it('classifies fetch failures as retryable network errors', () => {
    const cause = new TypeError('fetch failed');
    const error = normalizeApiError(cause);

    expect(error).toMatchObject({ kind: 'network', retryable: true, details: [] });
    expect(error.cause).toBe(cause);
    expect(getErrorTranslationKey(error)).toBe('errors.network');
  });

  it('keeps unknown technical failures safe and non-retryable', () => {
    const error = normalizeApiError(new Error('database password leaked'));

    expect(error).toMatchObject({ kind: 'unknown', retryable: false, details: [] });
    expect(error.publicMessage).toBeUndefined();
    expect(getErrorTranslationKey(error)).toBe('errors.unknown');
    expect(isErrorResponse({ ...validationError, code: 'NEW_UNKNOWN_CODE' })).toBe(false);
  });

  it('does not wrap an already normalized error twice', () => {
    const error = new AppError({ kind: 'unknown', retryable: false, cause: null });

    expect(normalizeApiError(error)).toBe(error);
  });

  it('normalizes a rejected generated-client request', async () => {
    // Generated Fetch client rejects with the parsed ErrorResponse object rather than an Error instance.
    await expect(
      apiRequest(Promise.reject(validationError as ErrorResponse & Error)),
    ).rejects.toMatchObject({
      kind: 'api',
      code: 'VALIDATION_ERROR',
    });
    await expect(apiRequest(Promise.resolve('ok'))).resolves.toBe('ok');
  });
});
