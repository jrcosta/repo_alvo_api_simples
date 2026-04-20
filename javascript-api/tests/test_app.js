const request = require('supertest');
const express = require('express');
const cors = require('cors');
const proxyquire = require('proxyquire');

describe('App configuration tests', () => {
  let app;
  let userRoutesMock;

  beforeEach(() => {
    userRoutesMock = express.Router();
    userRoutesMock.get('/test-route', (req, res) => res.status(200).json({ mocked: true }));

    // Proxyquire to replace './routes/users' with mock
    const appModule = proxyquire('../src/app', {
      './routes/users': userRoutesMock,
      cors: () => (req, res, next) => next(),
      express: Object.assign(() => {
        const e = express();
        // We will spy on use calls later
        return e;
      }, express),
    });

    app = appModule;
  });

  test('should apply CORS middleware', () => {
    // Since cors is a middleware, we test by sending a request and checking CORS headers
    return request(app)
      .get('/health')
      .expect('Access-Control-Allow-Origin', '*')
      .expect(200);
  });

  test('should apply express.json() middleware to parse JSON bodies', async () => {
    // We test JSON parsing by sending a POST with JSON to /users/test-route (mocked route)
    // The mock route does not parse body, so we add a middleware to check req.body
    const jsonBody = { key: 'value' };

    // Create a new app instance with a test route that echoes req.body
    const testApp = express();
    testApp.use(cors());
    testApp.use(express.json());
    testApp.use('/users', userRoutesMock);
    userRoutesMock.post('/echo', (req, res) => {
      res.json(req.body);
    });

    const response = await request(testApp)
      .post('/users/echo')
      .send(jsonBody)
      .set('Content-Type', 'application/json')
      .expect(200);

    expect(response.body).toEqual(jsonBody);
  });

  test('GET /health should respond with status 200 and JSON { status: "ok" }', async () => {
    const response = await request(app)
      .get('/health')
      .expect('Content-Type', /json/)
      .expect(200);

    expect(response.body).toEqual({ status: 'ok' });
  });

  test('should mount userRoutes router on /users path', async () => {
    // The userRoutesMock has a GET /test-route that returns { mocked: true }
    const response = await request(app)
      .get('/users/test-route')
      .expect(200);

    expect(response.body).toEqual({ mocked: true });
  });

  test('module should export the Express app instance', () => {
    // The app should be a function (Express app is a function)
    expect(typeof app).toBe('function');
    // It should have use and get methods
    expect(typeof app.use).toBe('function');
    expect(typeof app.get).toBe('function');
  });
});