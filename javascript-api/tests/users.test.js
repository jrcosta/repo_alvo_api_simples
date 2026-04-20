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
    expect(Array.isArray(res.body)).toBe(true);
    expect(res.body.length).toBeGreaterThan(0);
    expect(res.body[0]).toHaveProperty('name', 'Alice');
  });

  it('should create user with valid data and return 201 with id', async () => {
    const newUser = { name: 'Dan', email: 'dan@example.com' };
    const res = await request(app).post('/users').send(newUser);
    expect(res.status).toBe(201);
    expect(res.body).toHaveProperty('id');
    expect(res.body.name).toBe(newUser.name);
    expect(res.body.email).toBe(newUser.email);
  });

  it('should return 409 if email already exists', async () => {
    const email = 'duplicate409@example.com';
    await request(app).post('/users').send({ name: 'Dup1', email });
    const res = await request(app).post('/users').send({ name: 'Dup2', email });
    expect(res.status).toBe(409);
    expect(res.body).toHaveProperty('detail');
  });

  it('should return 422 when creating user with empty name', async () => {
    const invalidUser = { name: '', email: 'valid@example.com' };
    const res = await request(app).post('/users').send(invalidUser);
    expect(res.status).toBe(422);
    expect(res.body).toHaveProperty('detail');
  });

  it('should return 422 when creating user without email', async () => {
    const invalidUser = { name: 'NoEmail' };
    const res = await request(app).post('/users').send(invalidUser);
    expect(res.status).toBe(422);
    expect(res.body).toHaveProperty('detail');
  });

  it('should list users with pagination respecting limit parameter', async () => {
    const res = await request(app).get('/users?limit=2&offset=0');
    expect(res.status).toBe(200);
    expect(Array.isArray(res.body)).toBe(true);
    expect(res.body.length).toBeLessThanOrEqual(2);
  });

  it('should list users with offset skipping the first user', async () => {
    const allUsers = await request(app).get('/users');
    // Guard: need at least 2 users to meaningfully test offset
    expect(allUsers.body.length).toBeGreaterThan(1);

    const pagedUsers = await request(app).get('/users?limit=10&offset=1');
    expect(pagedUsers.status).toBe(200);
    expect(Array.isArray(pagedUsers.body)).toBe(true);
    expect(pagedUsers.body.length).toBe(allUsers.body.length - 1);
    expect(pagedUsers.body[0].id).toBe(allUsers.body[1].id);
  });

  it('should handle concurrent creation attempts with same email and return 409 for one', async () => {
    const email = 'concurrent_test@example.com';
    const user1 = { name: 'Concurrent1', email };
    const user2 = { name: 'Concurrent2', email };

    const [res1, res2] = await Promise.all([
      request(app).post('/users').send(user1),
      request(app).post('/users').send(user2),
    ]);

    const statuses = [res1.status, res2.status];
    expect(statuses).toContain(201);
    expect(statuses).toContain(409);
  });
});