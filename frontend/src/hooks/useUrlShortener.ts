import { useState, useCallback, useEffect } from 'react';
import { api } from '../services/api';
import type { ShortenUrlResponse, UrlListItem } from '../types/api';

interface UseUrlShortenerReturn {
  urls: UrlListItem[];
  totalCount: number;
  page: number;
  totalPages: number;
  loading: boolean;
  error: string | null;
  lastCreated: ShortenUrlResponse | null;
  shorten: (fullUrl: string, customAlias?: string) => Promise<void>;
  deleteUrl: (alias: string) => Promise<void>;
  goToPage: (page: number) => Promise<void>;
  clearLastCreated: () => void;
}

export function useUrlShortener(): UseUrlShortenerReturn {
  const [urls, setUrls] = useState<UrlListItem[]>([]);
  const [totalCount, setTotalCount] = useState(0);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [lastCreated, setLastCreated] = useState<ShortenUrlResponse | null>(null);

  const goToPage = useCallback(async (requested: number) => {
    setLoading(true);
    setError(null);
    try {
      let result = await api.listAll(requested);
      // The requested page can be past the end (e.g. after deleting the last row
      // on the last page). Step back to the real last page instead of showing nothing.
      if (result.urls.length === 0 && result.totalPages > 0 && requested >= result.totalPages) {
        result = await api.listAll(result.totalPages - 1);
      }
      setUrls(result.urls);
      setTotalCount(result.totalElements);
      setTotalPages(result.totalPages);
      setPage(result.number);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load URLs.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    goToPage(0);
  }, [goToPage]);

  const shorten = useCallback(async (fullUrl: string, customAlias?: string) => {
    setError(null);
    setLastCreated(null);
    try {
      const result = await api.shorten({ fullUrl, customAlias: customAlias || undefined });
      setLastCreated(result);
      // The list is sorted newest first, so the new URL is at the top of page 0.
      await goToPage(0);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to shorten URL.');
    }
  }, [goToPage]);

  const deleteUrl = useCallback(async (alias: string) => {
    setError(null);
    try {
      await api.delete(alias);
      await goToPage(page);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to delete URL.');
    }
  }, [goToPage, page]);

  const clearLastCreated = useCallback(() => setLastCreated(null), []);

  return {
    urls, totalCount, page, totalPages, loading, error, lastCreated,
    shorten, deleteUrl, goToPage, clearLastCreated,
  };
}
