const request = require('supertest');
const express = require('express');
const router = require('../routes/users');
const userService = require('../services/userService');

const app = express();
app.use(express.json());
app.use('/users', router);

describe('GET /users/has-email endpoint', () => {
  beforeEach(() => {
    userService.users = [
      { id: 1, name: "Alice", email: "alice@example.com" },
      { id: 2, name: "Bob", email: "bob@example.com" },
      { id: 3, name: "Charlie", email: "charlie@example.com" }
    ];
  });

  test('should return 400 when email parameter is missing', async () => {
    const res = await request(app).get('/users/has-email');
    expect(res.status).toBe(400);
    expect(res.body).toEqual({ detail: "Parâmetro email é obrigatório" });
  });

  test('should return 400 when email parameter is empty', async () => {
    const res = await request(app).get('/users/has-email?email=');
    expect(res.status).toBe(400);
    expect(res.body).toEqual({ detail: "Parâmetro email é obrigatório" });
  });

  test('should return exists=true when email is found', async () => {
    const res = await request(app).get('/users/has-email?email=alice@example.com');
    expect(res.status).toBe(200);
    expect(res.body).toEqual({ email: "alice@example.com", exists: true });
  });

  test('should trim email and return exists=true when found', async () => {
    const res = await request(app).get('/users/has-email?email=%20alice@example.com%20');
    expect(res.status).toBe(200);
    expect(res.body).toEqual({ email: "alice@example.com", exists: true });
  });

  test('should return exists=false when email is not found', async () => {
    const res = await request(app).get('/users/has-email?email=unknown@example.com');
    expect(res.status).toBe(200);
    expect(res.body).toEqual({ email: "unknown@example.com", exists: false });
  });
});
