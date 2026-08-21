import axios from 'axios';

const TOKEN_KEY = 'aic_token';

export const authStorage = {
  getToken: () => sessionStorage.getItem(TOKEN_KEY),
  setToken: (token) => sessionStorage.setItem(TOKEN_KEY, token),
  clearToken: () => sessionStorage.removeItem(TOKEN_KEY),
  isLoggedIn: () => !!sessionStorage.getItem(TOKEN_KEY),
};

export const login = (userName, password) =>
  axios.post('/api/auth/login', { userName, password }).then((r) => {
    authStorage.setToken(r.data.token);
    return r.data;
  });