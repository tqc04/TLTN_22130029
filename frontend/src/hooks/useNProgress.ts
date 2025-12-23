import { useEffect } from 'react';
import NProgress from 'nprogress';

export function useNProgress(loading: boolean) {
  useEffect(() => {
    if (loading) {
      NProgress.start();
    } else {
      NProgress.done();
    }

    return () => {
      NProgress.done();
    };
  }, [loading]);
}
