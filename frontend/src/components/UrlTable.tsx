import type { UrlListItem } from '../types/api';

interface UrlTableProps {
  urls: UrlListItem[];
  onDelete: (alias: string) => void;
}

export function UrlTable({ urls, onDelete }: UrlTableProps) {
  if (urls.length === 0) {
    return <div className="empty-state"><p>No shortened URLs yet.</p></div>;
  }

  return (
    <div className="url-table-wrapper">
      <table className="url-table">
        <colgroup>
          <col className="url-table__col-short" />
          <col />
          <col className="url-table__col-actions" />
        </colgroup>
        <thead>
          <tr>
            <th>Short URL</th>
            <th>Original URL</th>
            <th className="actions-cell"><span className="sr-only">Actions</span></th>
          </tr>
        </thead>
        <tbody>
          {urls.map((u) => (
            <tr key={u.alias}>
              <td>
                <a
                  href={u.shortUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="short-url-link"
                  title={u.shortUrl}
                >
                  {u.shortUrl}
                </a>
              </td>
              <td>
                <span className="full-url" title={u.fullUrl}>{u.fullUrl}</span>
              </td>
              <td className="actions-cell">
                <button
                  className="btn-danger"
                  onClick={() => onDelete(u.alias)}
                  aria-label={`Delete ${u.alias}`}
                >
                  Delete
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
