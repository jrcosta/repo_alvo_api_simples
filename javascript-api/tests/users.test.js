const request = require('supertest');
const app = require('../src/app');

describe('API Health Check', () => {
  it('should return status ok on /health', async () => {
    const res = await request(app).get('/health');
    expect(res.status).toBe(200);
    expect(res.body).toEqual({ status: 'ok' });
  });
});

describe('User Endpoints', () => {
  it('should list users', async () => {
    const res = await request(app).get('/users');
    expect(res.status).toBe(200);
    expect(res.body.length).toBeGreaterThan(0);
    expect(res.body[0]).toHaveProperty('name', 'Alice');
  });

  it('should create user', async () => {
    const res = await request(app)
      .post('/users')
      .send({ name: 'Dan', email: 'dan@example.com' });
    expect(res.status).toBe(201);
    expect(res.body).toHaveProperty('id');
    expect(res.body.name).toBe('Dan');
  });

  it('should return 409 if email exists', async () => {
    await request(app).post('/users').send({ name: 'Dan2', email: 'dan@example.com' });
    const res = await request(app)
      .post('/users')
      .send({ name: 'Dan3', email: 'dan@example.com' });
    expect(res.status).toBe(409);
  });
});