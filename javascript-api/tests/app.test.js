const request = require('supertest');
const express = require('express');
const cors = require('cors');
const jestMock = require('jest-mock');

jest.mock('../src/routes/users', () => {
  const express = require('express');
  const router = express.Router();
  router.get('/test-route', (req, res) => res.status(200).json({ mocked: true }));
  return router;
});

describe('App configuration tests including /ping route', () => {
  let app;
  let pingRoutesMock;

  beforeAll(() => {
    // Clear module cache to allow re-mocking
    jest.resetModules();
  });

  beforeEach(() => {
    // Mock the pingRoutes module
    pingRoutesMock = express.Router();
    pingRoutesMock.get('/', (req, res) => res.status(200).json({ pong: true }));

    jest.doMock('../src/routes/ping', () => pingRoutesMock);

    // Require app after mocking pingRoutes
    app = require('../src/app');
  });

  afterEach(() => {
    jest.resetModules();
    jest.clearAllMocks();
  });

  test('should mount pingRoutes router on /ping path', async () => {
    const response = await request(app)
      .get('/ping')
      .expect(200)
      .expect('Content-Type', /json/);

    expect(response.body).toEqual({ pong: true });
  });

  test('should respond with 404 for unsupported HTTP methods on /ping', async () => {
    await request(app).post('/ping').expect(404);
    await request(app).put('/ping').expect(404);
    await request(app).delete('/ping').expect(404);
    await request(app).patch('/ping').expect(404);
  });

  test('should apply CORS middleware to /ping route', async () => {
    const response = await request(app)
      .get('/ping')
      .expect(200);

    expect(response.headers['access-control-allow-origin']).toBe('*');
  });

  test('should apply express.json() middleware to /ping route', async () => {
    // Create a test router that echoes JSON body to verify json middleware
    const testRouter = express.Router();
    testRouter.post('/', (req, res) => {
      res.json(req.body);
    });

    jest.doMock('../src/routes/ping', () => testRouter);
    jest.resetModules();
    app = require('../src/app');

    const testPayload = { test: 'data' };
    const response = await request(app)
      .post('/ping')
      .send(testPayload)
      .set('Content-Type', 'application/json')
      .expect(200);

    expect(response.body).toEqual(testPayload);
  });

  test('should not affect existing /health route after adding /ping', async () => {
    const response = await request(app)
      .get('/health')
      .expect(200)
      .expect('Content-Type', /json/);

    expect(response.body).toEqual({ status: 'ok' });
  });

  test('should not affect existing /users route after adding /ping', async () => {
    const response = await request(app)
      .get('/users/test-route')
      .expect(200)
      .expect('Content-Type', /json/);

    expect(response.body).toEqual({ mocked: true });
  });
});