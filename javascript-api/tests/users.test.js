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
  beforeEach(async () => {
    // Assuming there is an endpoint or mechanism to reset the DB for tests isolation
    // If not, this is a placeholder to highlight the need for DB cleanup
    if (app.resetTestDB) {
      await app.resetTestDB();
    }
  });

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
    const email = 'dan@example.com';
    await request(app).post('/users').send({ name: 'Dan2', email });
    const res = await request(app).post('/users').send({ name: 'Dan3', email });
    expect(res.status).toBe(409);
  });

  it('should return 400 when creating user with invalid email format', async () => {
    const invalidUser = { name: 'Invalid Email', email: 'invalid-email' };
    const res = await request(app).post('/users').send(invalidUser);
    expect(res.status).toBe(400);
    expect(res.body).toHaveProperty('error');
  });

  it('should return 400 when creating user with empty name', async () => {
    const invalidUser = { name: '', email: 'valid@example.com' };
    const res = await request(app).post('/users').send(invalidUser);
    expect(res.status).toBe(400);
    expect(res.body).toHaveProperty('error');
  });

  it('should return empty list when no users exist', async () => {
    if (app.resetTestDB) {
      await app.resetTestDB();
    }
    // Assuming resetTestDB clears all users
    const res = await request(app).get('/users');
    expect(res.status).toBe(200);
    expect(Array.isArray(res.body)).toBe(true);
    expect(res.body.length).toBe(0);
  });

  it('should list users with pagination parameters limit and offset', async () => {
    // Seed multiple users for pagination test
    const usersToCreate = [
      { name: 'User1', email: 'user1@example.com' },
      { name: 'User2', email: 'user2@example.com' },
      { name: 'User3', email: 'user3@example.com' },
    ];
    for (const user of usersToCreate) {
      await request(app).post('/users').send(user);
    }

    const res = await request(app).get('/users?limit=2&offset=1');
    expect(res.status).toBe(200);
    expect(Array.isArray(res.body)).toBe(true);
    expect(res.body.length).toBeLessThanOrEqual(2);
    // Check that the users returned are from the expected offset
    expect(res.body[0]).toHaveProperty('email', 'user2@example.com');
  });

  it('should return 404 when searching user by email that does not exist', async () => {
    const res = await request(app).get('/users/by-email').query({ email: 'notfound@example.com' });
    expect(res.status).toBe(404);
    expect(res.body).toHaveProperty('detail');
  });

  it('should return user when searching by existing email', async () => {
    const user = { name: 'SearchUser', email: 'searchuser@example.com' };
    await request(app).post('/users').send(user);
    const res = await request(app).get('/users/by-email').query({ email: user.email });
    expect(res.status).toBe(200);
    expect(res.body).toHaveProperty('email', user.email);
    expect(res.body).toHaveProperty('name', user.name);
    expect(res.body).not.toHaveProperty('password');
  });

  it('should handle concurrent creation attempts with same email and return 409 for one', async () => {
    const email = 'concurrent@example.com';
    const user1 = { name: 'Concurrent1', email };
    const user2 = { name: 'Concurrent2', email };

    // Fire two requests almost simultaneously
    const [res1, res2] = await Promise.all([
      request(app).post('/users').send(user1),
      request(app).post('/users').send(user2),
    ]);

    const statuses = [res1.status, res2.status];
    expect(statuses).toContain(201);
    expect(statuses).toContain(409);
  });
});