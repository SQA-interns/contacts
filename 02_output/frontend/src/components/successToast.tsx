import { toast, type ExternalToast } from 'sonner';

/**
 * A success toast whose message has the role `status` (UI-13, UI-15): Sonner's toast itself has none, so
 * a screen reader and the end-to-end tests (`getByRole('status')`) find the result of an action only
 * this way.
 */
export function successToast(message: string, options?: ExternalToast) {
  return toast.success(<span role="status">{message}</span>, options);
}
