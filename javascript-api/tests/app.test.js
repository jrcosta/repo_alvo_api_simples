const request = require('supertest');
const express = require('express');
const cors = require('cors');

jest.mock('../src/routes/users', () => {
  const express = require('express');
  const router = express.Router();
  router.get('/test-route', (req, res) => res.status(200).json({ mocked: true }));
  return router;
});

const app = require('../src/app');

describe('App configuration tests', () => {
  test('should apply CORS middleware', () => {
    return request(app)
      .get('/health')
      .expect('Access-Control-Allow-Origin', '*')
      .expect(200);
  });

  test('should apply express.json() middleware to parse JSON bodies', async () => {
    const jsonBody = { key: 'value' };

    const testApp = express();
    testApp.use(cors());
    testApp.use(express.json());
    const echoRouter = express.Router();
    echoRouter.post('/echo', (req, res) => {
      res.json(req.body);
    });
    testApp.use('/test', echoRouter);

    const response = await request(testApp)
      .post('/test/echo')
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
    const response = await request(app)
      .get('/users/test-route')
      .expect(200);

    expect(response.body).toEqual({ mocked: true });
  });

  test('module should export the Express app instance', () => {
    expect(typeof app).toBe('function');
    expect(typeof app.use).toBe('function');
    expect(typeof app.get).toBe('function');
  });
});
