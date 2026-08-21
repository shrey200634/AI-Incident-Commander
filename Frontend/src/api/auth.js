import axios from 'axios';

const TOKEN_KEY = 'aic_token';

export const authStorage = {
  getToken: () => localStorage.getItem(TOKEN_KEY),
  setToken: (token) => localStorage.setItem(TOKEN_KEY, token),
  clearToken: () => localStorage.removeItem(TOKEN_KEY),
  isLoggedIn: () => !!localStorage.getItem(TOKEN_KEY),
};

export const login = (userName, password) =>
  axios.post('/api/auth/login', { userName, password }).then((r) => {
    authStorage.setToken(r.data.token);
    return r.data;
  });