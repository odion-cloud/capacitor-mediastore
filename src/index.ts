import { registerPlugin } from '@capacitor/core';

import type { CapacitorMediaStorePlugin } from './definitions';

const CapacitorMediaStore = registerPlugin<CapacitorMediaStorePlugin>('CapacitorMediaStore', {
  web: () => import('./web').then(m => new m.CapacitorMediaStoreWeb()),
});

export * from './definitions';
export { CapacitorMediaStore };
