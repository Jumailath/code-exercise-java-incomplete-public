interface PaginationProps {
  page: number; // 0-based, as Spring sends it
  totalPages: number;
  onPageChange: (page: number) => void;
  disabled?: boolean;
}

export function Pagination({ page, totalPages, onPageChange, disabled }: PaginationProps) {
  if (totalPages <= 1) return null;

  const isFirst = page <= 0;
  const isLast = page >= totalPages - 1;

  return (
    <nav className="pagination" aria-label="URL list pages">
      <button
        className="btn-secondary"
        onClick={() => onPageChange(page - 1)}
        disabled={disabled || isFirst}
      >
        ← Previous
      </button>
      <span className="pagination__status" aria-live="polite">
        Page {page + 1} of {totalPages}
      </span>
      <button
        className="btn-secondary"
        onClick={() => onPageChange(page + 1)}
        disabled={disabled || isLast}
      >
        Next →
      </button>
    </nav>
  );
}
