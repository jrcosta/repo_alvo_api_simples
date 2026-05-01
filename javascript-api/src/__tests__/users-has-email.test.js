const request = require('supertest');
const express = require('express');
const usersRouter = require('../routes/users');
const userService = require('../services/userService');

jest.mock('../services/userService');

const app = express();
app.use(express.json());
app.use('/users', usersRouter);

describe('Rota /users/has-email', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('GET /has-email com email existente retorna exists true', async () => {
    userService.findByEmail.mockReturnValue({ id: 1, email: 'test@example.com' });

    const res = await request(app).get('/users/has-email').query({ email: 'test@example.com' });

    expect(res.statusCode).toBe(200);
    expect(res.body).toEqual({ email: 'test@example.com', exists: true });
    expect(userService.findByEmail).toHaveBeenCalledWith('test@example.com');
  });

  test('GET /has-email com email inexistente retorna exists false', async () => {
    userService.findByEmail.mockReturnValue(null);

    const res = await request(app).get('/users/has-email').query({ email: 'notfound@example.com' });

    expect(res.statusCode).toBe(200);
    expect(res.body).toEqual({ email: 'notfound@example.com', exists: false });
    expect(userService.findByEmail).toHaveBeenCalledWith('notfound@example.com');
  });

  test('GET /has-email sem parâmetro email retorna 400 com mensagem apropriada', async () => {
    const res = await request(app).get('/users/has-email');

    expect(res.statusCode).toBe(400);
    expect(res.body).toEqual({ detail: 'Parâmetro email é obrigatório' });
  });

  test('GET /has-email com email vazio retorna 400 com mensagem apropriada', async () => {
    const res = await request(app).get('/users/has-email').query({ email: '   ' });

    expect(res.statusCode).toBe(400);
    expect(res.body).toEqual({ detail: 'Parâmetro email é obrigatório' });
  });

  const methods = ['post', 'put', 'delete', 'patch', 'options', 'head'];

  methods.forEach(method => {
    test(`${method.toUpperCase()} /has-email retorna 404 com { detail: "Rota não encontrada" }`, async () => {
      const res = await request(app)[method]('/users/has-email');

      expect(res.statusCode).toBe(404);
      expect(res.body).toEqual({ detail: 'Rota não encontrada' });
    });

    test(`${method.toUpperCase()} /has-email com query params retorna 404 com { detail: "Rota não encontrada" }`, async () => {
      const res = await request(app)[method]('/users/has-email').query({ email: 'test@example.com' });

      expect(res.statusCode).toBe(404);
      expect(res.body).toEqual({ detail: 'Rota não encontrada' });
    });

    test(`${method.toUpperCase()} /has-email com corpo retorna 404 com { detail: "Rota não encontrada" }`, async () => {
      const res = await request(app)[method]('/users/has-email').send({ email: 'test@example.com' });

      expect(res.statusCode).toBe(404);
      expect(res.body).toEqual({ detail: 'Rota não encontrada' });
    });
  });
});