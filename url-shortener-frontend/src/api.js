import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080';

const api = axios.create({
  baseURL: API_BASE_URL,
});

// Automatically attach the JWT token to every request, if one exists
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export const register = (username, email, password) =>
  api.post('/api/auth/register', { username, email, password });

export const login = (username, password) =>
  api.post('/api/auth/login', { username, password });

export const createShortUrl = (originalUrl) =>
  api.post('/api/urls', { originalUrl });

export const getMyUrls = () => api.get('/api/urls');

export default api;