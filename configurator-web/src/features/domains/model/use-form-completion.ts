import { useCallback, useEffect, useRef } from 'react';

/** A successful save can navigate before React has rendered the closed dialog. */
export function useFormCompletion(opened: boolean) {
  const completed = useRef(false);
  useEffect(() => {
    if (opened) completed.current = false;
  }, [opened]);
  const markComplete = useCallback(() => {
    completed.current = true;
  }, []);
  const canLeave = useCallback(() => completed.current, []);
  return { markComplete, canLeave };
}
