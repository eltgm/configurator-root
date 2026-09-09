import {
  Button,
  Group,
  Modal,
  Stack,
  Text,
  type ButtonProps,
  type ModalProps,
} from '@mantine/core';
import { createContext, useContext, useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { UNSAFE_DataRouterContext, useBlocker } from 'react-router-dom';

import { DomainChangeGuardContext } from '@/features/domains/model/domain-change-guard-context';

const FormCloseContext = createContext<(() => void) | null>(null);

export function FormModalCancelButton(props: ButtonProps) {
  const close = useContext(FormCloseContext);
  return <Button {...props} onClick={() => close?.()} />;
}

function NavigationGuard({
  onDiscard,
  canLeave,
  pending,
}: {
  onDiscard: () => void;
  canLeave: (() => boolean) | undefined;
  pending: boolean;
}) {
  const { t } = useTranslation();
  const blocker = useBlocker(() => !canLeave?.());
  return (
    <Modal
      closeButtonProps={{ 'aria-label': t('common.close') }}
      opened={blocker.state === 'blocked'}
      onClose={() => blocker.reset?.()}
      title={t('ux.unsavedTitle')}
      centered
    >
      <Stack>
        <Text>{t('ux.unsavedDescription')}</Text>
        <Group justify="flex-end">
          <Button variant="default" data-autofocus onClick={() => blocker.reset?.()}>
            {t('ux.stay')}
          </Button>
          <Button
            color="red"
            disabled={pending}
            onClick={() => {
              onDiscard();
              blocker.proceed?.();
            }}
          >
            {t('ux.discard')}
          </Button>
        </Group>
      </Stack>
    </Modal>
  );
}

export function GuardedFormModal({
  dirty,
  pending,
  canLeave,
  children,
  onClose,
  opened,
  ...props
}: ModalProps & { dirty: boolean; pending: boolean; canLeave?: () => boolean }) {
  const { t } = useTranslation();
  const domainGuard = useContext(DomainChangeGuardContext);
  const router = useContext(UNSAFE_DataRouterContext);
  const [confirming, setConfirming] = useState(false);
  if (!opened && confirming) setConfirming(false);
  const stayRef = useRef<HTMLButtonElement>(null);
  const returnFocus = useRef<HTMLElement | null>(null);
  const closeRef = useRef(onClose);
  useEffect(() => {
    closeRef.current = onClose;
  }, [onClose]);
  const register = domainGuard?.register;
  useEffect(() => {
    if (!opened || !dirty) return;
    const key = Symbol('modal-guard');
    register?.(key, { dirty: true, discard: () => closeRef.current() });
    const preventClose = (event: BeforeUnloadEvent) => {
      if (!canLeave?.()) event.preventDefault();
    };
    window.addEventListener('beforeunload', preventClose);
    return () => {
      register?.(key, null);
      window.removeEventListener('beforeunload', preventClose);
    };
  }, [dirty, opened, register, canLeave]);
  const requestClose = () => {
    if (pending) return;
    if (dirty) {
      returnFocus.current =
        document.activeElement instanceof HTMLElement ? document.activeElement : null;
      setConfirming(true);
    } else onClose();
  };
  useEffect(() => {
    if (confirming) stayRef.current?.focus();
  }, [confirming]);
  const stay = () => {
    setConfirming(false);
    requestAnimationFrame(() => returnFocus.current?.focus());
  };
  return (
    <>
      <Modal
        closeButtonProps={{ 'aria-label': t('common.close') }}
        {...props}
        opened={opened}
        onClose={confirming ? stay : requestClose}
        title={confirming ? t('ux.unsavedTitle') : props.title}
      >
        <FormCloseContext.Provider value={requestClose}>
          <div hidden={confirming}>{children}</div>
        </FormCloseContext.Provider>
        {confirming ? (
          <Stack>
            <Text>{t('ux.unsavedDescription')}</Text>
            <Group justify="flex-end">
              <Button ref={stayRef} variant="default" onClick={stay}>
                {t('ux.stay')}
              </Button>
              <Button
                color="red"
                onClick={() => {
                  setConfirming(false);
                  onClose();
                }}
              >
                {t('ux.discard')}
              </Button>
            </Group>
          </Stack>
        ) : null}
      </Modal>
      {opened && dirty && router ? (
        <NavigationGuard onDiscard={onClose} canLeave={canLeave} pending={pending} />
      ) : null}
    </>
  );
}
