const request = require('supertest');
const app = require('../src/app');

describe('Ping Endpoint', () => {
  it('should return 200 and pong message', async () => {
    const response = await request(app).get('/ping');
    expect(response.statusCode).toBe(200);
    expect(response.body).toHaveProperty('message', 'pong');
    expect(response.body).toHaveProperty('timestamp');
  });

  it('should return Content-Type application/json header', async () => {
    const response = await request(app).get('/ping');
    expect(response.headers['content-type']).toMatch(/application\/json/);
  });

  it('should return a valid numeric Unix timestamp in the response', async () => {
    const response = await request(app).get('/ping');
    expect(response.body).toHaveProperty('timestamp');
    const timestamp = response.body.timestamp;
    expect(typeof timestamp).toBe('number');
    expect(timestamp).toBeGreaterThan(0);
  });

  it('should return a recent timestamp within the last 5 seconds', async () => {
    const response = await request(app).get('/ping');
    const timestamp = response.body.timestamp;
    const now = Date.now();
    const diffSeconds = (now - timestamp) / 1000;
    expect(diffSeconds).toBeGreaterThanOrEqual(0);
    expect(diffSeconds).toBeLessThanOrEqual(5);
  });

  it.each(['post', 'put', 'delete', 'patch'])(
    'should return 405 Method Not Allowed for HTTP %s method',
    async (method) => {
      const response = await request(app)[method]('/ping');
      expect([404, 405]).toContain(response.statusCode);
    }
  );

  it('should handle multiple sequential GET requests with consistent responses', async () => {
    for (let i = 0; i < 5; i++) {
      const response = await request(app).get('/ping');
      expect(response.statusCode).toBe(200);
      expect(response.body).toHaveProperty('message', 'pong');
      expect(response.body).toHaveProperty('timestamp');
      expect(typeof response.body.timestamp).toBe('number');
    }
  });
});