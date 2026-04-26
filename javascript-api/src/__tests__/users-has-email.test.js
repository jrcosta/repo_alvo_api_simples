const request = require('supertest');
const express = require('express');
const router = require('../routes/users');
const userService = require('../services/userService');

jest.mock('../services/userService');

const app = express();
app.use(express.json());
app.use('/', router);

describe('GET /has-email endpoint', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('should return 400 if email parameter is missing', async () => {
    const res = await request(app).get('/has-email');
    expect(res.status).toBe(400);
    expect(res.body).toEqual({ detail: "Parâmetro email é obrigatório" });
    expect(userService.findByEmail).not.toHaveBeenCalled();
  });

  test('should return 400 if email parameter is empty or only spaces', async () => {
    const res = await request(app).get('/has-email?email=   ');
    expect(res.status).toBe(400);
    expect(res.body).toEqual({ detail: "Parâmetro email é obrigatório" });
    expect(userService.findByEmail).not.toHaveBeenCalled();
  });

  test('should return 200 and exists true for valid and registered email', async () => {
    const email = 'usuario@exemplo.com';
    userService.findByEmail.mockReturnValue({ id: 1, email });

    const res = await request(app).get(`/has-email?email=${email}`);
    expect(res.status).toBe(200);
    expect(res.body).toEqual({ email, exists: true });
    expect(userService.findByEmail).toHaveBeenCalledWith(email);
  });

  test('should return 200 and exists false for valid but not registered email', async () => {
    const email = 'naoexiste@exemplo.com';
    userService.findByEmail.mockReturnValue(null);

    const res = await request(app).get(`/has-email?email=${email}`);
    expect(res.status).toBe(200);
    expect(res.body).toEqual({ email, exists: false });
    expect(userService.findByEmail).toHaveBeenCalledWith(email);
  });

  test('should trim email parameter before querying', async () => {
    const emailRaw = '  usuario@exemplo.com  ';
    const emailTrimmed = 'usuario@exemplo.com';
    userService.findByEmail.mockReturnValue({ id: 1, email: emailTrimmed });

    const res = await request(app).get(`/has-email?email=${encodeURIComponent(emailRaw)}`);
    expect(res.status).toBe(200);
    expect(res.body).toEqual({ email: emailTrimmed, exists: true });
    expect(userService.findByEmail).toHaveBeenCalledWith(emailTrimmed);
  });

  test('should return 200 and exists false for malformed email (no format validation)', async () => {
    const email = 'abc@@';
    userService.findByEmail.mockReturnValue(null);

    const res = await request(app).get(`/has-email?email=${email}`);
    expect(res.status).toBe(200);
    expect(res.body).toEqual({ email, exists: false });
    expect(userService.findByEmail).toHaveBeenCalledWith(email);
  });

  test('should return 500 if userService.findByEmail throws an error', async () => {
    const email = 'usuario@exemplo.com';
    userService.findByEmail.mockImplementation(() => {
      throw new Error('DB failure');
    });

    const res = await request(app).get(`/has-email?email=${email}`);
    // According to current implementation, error is not caught, so express returns 500
    expect(res.status).toBe(500);
  });

  test('should return JSON with only email and exists keys', async () => {
    const email = 'usuario@exemplo.com';
    userService.findByEmail.mockReturnValue({ id: 1, email, password: 'secret', token: 'token123' });

    const res = await request(app).get(`/has-email?email=${email}`);
    expect(res.status).toBe(200);
    expect(Object.keys(res.body).sort()).toEqual(['email', 'exists']);
  });

  test('should reject requests with multiple email parameters with 400', async () => {
    const res = await request(app).get('/has-email?email=one@example.com&email=two@example.com');
    // The current implementation does not explicitly handle multiple params,
    // but express query parser returns array in this case.
    // So we expect 400 because email.trim() would fail (email is array).
    expect(res.status).toBe(400);
    expect(res.body).toEqual({ detail: "Parâmetro email é obrigatório" });
    expect(userService.findByEmail).not.toHaveBeenCalled();
  });

  test('should handle emails with valid special characters correctly', async () => {
    const email = 'user+tag@example.com';
    userService.findByEmail.mockReturnValue({ id: 1, email });

    const res = await request(app).get(`/has-email?email=${encodeURIComponent(email)}`);
    expect(res.status).toBe(200);
    expect(res.body).toEqual({ email, exists: true });
    expect(userService.findByEmail).toHaveBeenCalledWith(email);
  });

  test('should respond with 405 Method Not Allowed for non-GET methods', async () => {
    const methods = ['post', 'put', 'delete', 'patch'];
    for (const method of methods) {
      const res = await request(app)[method]('/has-email?email=test@example.com');
      expect(res.status).toBe(405);
    }
  });

  test('should not return any sensitive user data even if userService returns corrupted data', async () => {
    const email = 'corrupt@example.com';
    userService.findByEmail.mockReturnValue({ id: 1, email, password: 'secret', token: 'token123', extra: 'data' });

    const res = await request(app).get(`/has-email?email=${email}`);
    expect(res.status).toBe(200);
    expect(res.body).toEqual({ email, exists: true });
    expect(Object.keys(res.body)).toEqual(['email', 'exists']);
  });

  test('should coexist with /by-email endpoint without interference', async () => {
    // Mock userService.findByEmail for /has-email
    userService.findByEmail.mockImplementation(email => {
      if (email === 'exists@example.com') return { id: 1, email };
      return null;
    });

    // Test /has-email
    const resHasEmail = await request(app).get('/has-email?email=exists@example.com');
    expect(resHasEmail.status).toBe(200);
    expect(resHasEmail.body).toEqual({ email: 'exists@example.com', exists: true });

    // Test /by-email with valid email
    const resByEmail = await request(app).get('/by-email?email=exists@example.com');
    expect(resByEmail.status).toBe(200);
    expect(resByEmail.body.email).toBe('exists@example.com');
    expect(resByEmail.body).not.toHaveProperty('password');
    expect(resByEmail.body).not.toHaveProperty('token');

    // Test /by-email with invalid email format returns 404
    const resByEmailInvalid = await request(app).get('/by-email?email=invalid@@');
    expect(resByEmailInvalid.status).toBe(404);
  });

  test('should handle concurrent requests consistently', async () => {
    const emailExists = 'concurrent@example.com';
    const emailNotExists = 'notfound@example.com';

    userService.findByEmail.mockImplementation(email => {
      if (email === emailExists) return { id: 1, email };
      return null;
    });

    const requests = [
      request(app).get(`/has-email?email=${emailExists}`),
      request(app).get(`/has-email?email=${emailNotExists}`),
      request(app).get(`/has-email?email=   ${emailExists}   `),
      request(app).get(`/has-email?email=abc@@`),
    ];

    const responses = await Promise.all(requests);

    expect(responses[0].status).toBe(200);
    expect(responses[0].body).toEqual({ email: emailExists, exists: true });

    expect(responses[1].status).toBe(200);
    expect(responses[1].body).toEqual({ email: emailNotExists, exists: false });

    expect(responses[2].status).toBe(200);
    expect(responses[2].body).toEqual({ email: emailExists, exists: true });

    expect(responses[3].status).toBe(200);
    expect(responses[3].body).toEqual({ email: 'abc@@', exists: false });
  });
});