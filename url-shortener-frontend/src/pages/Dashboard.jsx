import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { createShortUrl, getMyUrls } from '../api';

function Dashboard() {
  const [originalUrl, setOriginalUrl] = useState('');
  const [urls, setUrls] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  const handleLogout = () => {
    localStorage.removeItem('token');
    navigate('/login');
  };

  // Runs once when the page opens: load this user's saved links from the server
  useEffect(() => {
    const loadUrls = async () => {
      try {
        const response = await getMyUrls();
        setUrls(response.data);
      } catch (err) {
        if (err.response && (err.response.status === 401 || err.response.status === 403)) {
          // token is missing, expired, or invalid: send the user back to login
          localStorage.removeItem('token');
          navigate('/login');
        } else {
          setError('Could not load your URLs. Is the server running?');
        }
      } finally {
        setLoading(false);
      }
    };
    loadUrls();
  }, [navigate]);

  const handleCreateUrl = async (e) => {
    e.preventDefault();
    setError('');
    try {
      const response = await createShortUrl(originalUrl);
      setUrls((current) => [response.data, ...current]);
      setOriginalUrl('');
    } catch (err) {
      setError('Failed to create short URL. Please check the URL and try again.');
    }
  };

  return (
    <div style={{ maxWidth: '700px', margin: '50px auto', fontFamily: 'sans-serif' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h2>My Shortened URLs</h2>
        <button onClick={handleLogout}>Logout</button>
      </div>

      <form onSubmit={handleCreateUrl} style={{ display: 'flex', gap: '10px', marginBottom: '20px' }}>
        <input
          type="url"
          placeholder="Paste a URL to shorten..."
          value={originalUrl}
          onChange={(e) => setOriginalUrl(e.target.value)}
          style={{ flex: 1, padding: '8px' }}
          required
        />
        <button type="submit" style={{ padding: '8px 16px' }}>
          Shorten
        </button>
      </form>

      {error && <p style={{ color: 'red' }}>{error}</p>}

      <table style={{ width: '100%', borderCollapse: 'collapse' }}>
        <thead>
          <tr style={{ borderBottom: '2px solid #333', textAlign: 'left' }}>
            <th style={{ padding: '8px' }}>Short URL</th>
            <th style={{ padding: '8px' }}>Original URL</th>
            <th style={{ padding: '8px' }}>Clicks</th>
            <th style={{ padding: '8px' }}>Category</th>
          </tr>
        </thead>
        <tbody>
          {urls.map((url) => (
            <tr key={url.shortCode} style={{ borderBottom: '1px solid #ddd' }}>
              <td style={{ padding: '8px' }}>
                <a href={url.shortUrl} target="_blank" rel="noopener noreferrer">
                  {url.shortUrl}
                </a>
              </td>
              <td style={{ padding: '8px', maxWidth: '250px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                {url.originalUrl}
              </td>
              <td style={{ padding: '8px' }}>{url.clickCount}</td>
              <td style={{ padding: '8px' }}>{url.category || '—'}</td>
            </tr>
          ))}
        </tbody>
      </table>

      {loading && <p style={{ color: '#888' }}>Loading your URLs...</p>}
      {!loading && urls.length === 0 && !error && (
        <p style={{ color: '#888' }}>No URLs yet — create one above.</p>
      )}
    </div>
  );
}

export default Dashboard;