import type { ApiErrorCode, ApiErrorDetail, ErrorResponse } from '@/shared/api/generated';

const apiErrorCodes = new Set<ApiErrorCode>([
  'BUSINESS_ERROR',
  'INTERNAL_ERROR',
  'NOT_FOUND',
  'ENTITY_ALREADY_EXISTS',
  'ENTITY_HAS_RELATED_ENTITIES',
  'DOMAIN_HAS_CONFIGURATIONS',
  'COMPONENT_ARCHIVED',
  'CONFIGURATION_CONFLICT',
  'VALIDATION_ERROR',
  'IMAGE_TOO_LARGE',
  'UNSUPPORTED_IMAGE_FORMAT',
  'EXTERNAL_STORAGE_UNAVAILABLE',
]);

type ApiErrorDetailPayload = Omit<ApiErrorDetail, 'field'> & {
  field?: string | null | undefined;
};

type ErrorResponsePayload = Omit<ErrorResponse, 'details'> & {
  details: Array<ApiErrorDetailPayload>;
};

interface AppErrorOptions {
  kind: 'api' | 'network' | 'unknown';
  code?: ApiErrorCode | undefined;
  status?: number | undefined;
  publicMessage?: string | undefined;
  details?: ReadonlyArray<ApiErrorDetail> | undefined;
  retryable: boolean;
  cause: unknown;
}

export class AppError extends Error {
  readonly kind: AppErrorOptions['kind'];
  readonly code?: ApiErrorCode | undefined;
  readonly status?: number | undefined;
  readonly publicMessage?: string | undefined;
  readonly details: ReadonlyArray<ApiErrorDetail>;
  readonly retryable: boolean;

  constructor(options: AppErrorOptions) {
    super(options.publicMessage ?? 'Application request failed', { cause: options.cause });
    this.name = 'AppError';
    this.kind = options.kind;
    this.code = options.code;
    this.status = options.status;
    this.publicMessage = options.publicMessage;
    this.details = options.details ?? [];
    this.retryable = options.retryable;
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}

function isApiErrorCode(value: unknown): value is ApiErrorCode {
  return typeof value === 'string' && apiErrorCodes.has(value as ApiErrorCode);
}

function isApiErrorDetail(value: unknown): value is ApiErrorDetail {
  return (
    isRecord(value) &&
    typeof value['code'] === 'string' &&
    typeof value['message'] === 'string' &&
    (value['field'] === undefined || value['field'] === null || typeof value['field'] === 'string')
  );
}

export function isErrorResponse(value: unknown): value is ErrorResponsePayload {
  return (
    isRecord(value) &&
    typeof value['timestamp'] === 'string' &&
    typeof value['status'] === 'number' &&
    typeof value['error'] === 'string' &&
    isApiErrorCode(value['code']) &&
    typeof value['message'] === 'string' &&
    typeof value['path'] === 'string' &&
    Array.isArray(value['details']) &&
    value['details'].every(isApiErrorDetail)
  );
}

export function isAppError(value: unknown): value is AppError {
  return value instanceof AppError;
}

export function normalizeApiError(error: unknown): AppError {
  if (isAppError(error)) {
    return error;
  }

  if (isErrorResponse(error)) {
    return new AppError({
      kind: 'api',
      code: error.code,
      status: error.status,
      publicMessage: error.message,
      details: error.details.map((detail) =>
        detail.field
          ? { field: detail.field, code: detail.code, message: detail.message }
          : { code: detail.code, message: detail.message },
      ),
      retryable: error.status >= 500,
      cause: error,
    });
  }

  if (error instanceof TypeError) {
    return new AppError({
      kind: 'network',
      retryable: true,
      cause: error,
    });
  }

  return new AppError({
    kind: 'unknown',
    retryable: false,
    cause: error,
  });
}

export async function apiRequest<T>(request: Promise<T>): Promise<T> {
  try {
    return await request;
  } catch (error) {
    throw normalizeApiError(error);
  }
}

export async function apiData<T>(request: Promise<{ data: T }>): Promise<T> {
  const response = await apiRequest(request);
  return response.data;
}

export function getFieldErrors(error: unknown): Readonly<Record<string, ReadonlyArray<string>>> {
  const normalizedError = normalizeApiError(error);
  const errorsByField: Record<string, Array<string>> = {};

  for (const detail of normalizedError.details) {
    if (!detail.field) {
      continue;
    }

    const fieldErrors = errorsByField[detail.field] ?? [];
    if (!fieldErrors.includes(detail.message)) {
      fieldErrors.push(detail.message);
    }
    errorsByField[detail.field] = fieldErrors;
  }

  return errorsByField;
}

export function getErrorTranslationKey(error: unknown): string {
  const normalizedError = normalizeApiError(error);

  if (normalizedError.kind === 'network') {
    return 'errors.network';
  }
  if (normalizedError.kind === 'unknown' || !normalizedError.code) {
    return 'errors.unknown';
  }

  return `errors.codes.${normalizedError.code}`;
}
