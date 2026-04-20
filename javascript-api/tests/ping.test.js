const request = require('supertest');
const app = require('../src/app');

describe('Ping Endpoint', () => {
  it('should return 200 and pong message', async () => {
    const response = await request(app).get('/ping');
    expect(response.statusCode).toBe(200);
    expect(response.body).toHaveProperty('message', 'pong');
    expect(response.body).toHaveProperty('timestamp');
  });
});
