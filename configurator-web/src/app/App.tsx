import { RouterProvider, type createBrowserRouter } from 'react-router-dom';

import { AppProviders } from '@/app/providers/AppProviders';
import { appRouter } from '@/app/router/router';

type AppRouter = ReturnType<typeof createBrowserRouter>;

interface AppProps {
  router?: AppRouter;
}

export function App({ router = appRouter }: AppProps) {
  return (
    <AppProviders>
      <RouterProvider router={router} />
    </AppProviders>
  );
}
