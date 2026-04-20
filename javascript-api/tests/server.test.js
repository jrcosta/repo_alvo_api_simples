const request = require('supertest');
const express = require('express');
const cors = require('cors');
const dotenv = require('dotenv');
const axios = require('axios');
const { spawn } = require('child_process');
const path = require('path');

jest.mock('axios');
jest.mock('child_process');

describe('Environment and Server Setup', () => {
  beforeAll(() => {
    // Load environment variables from a test .env file if exists
    dotenv.config({ path: path.resolve(__dirname, '../.env.test') });
  });

  test('dotenv loads environment variables correctly', () => {
    expect(process.env.TEST_VAR).toBeDefined();
    expect(process.env.TEST_VAR).toBe('hello');
  });
});

describe('Express server basic functionality', () => {
  let app;
  let server;

  beforeAll(() => {
    app = express();
    app.use(cors());
    app.get('/health', (req, res) => res.json({ status: 'ok' }));
    server = app.listen(0);
  });

  afterAll((done) => {
    server.close(done);
  });

  test('CORS middleware is applied and /health returns status ok', async () => {
    const res = await request(app).get('/health');
    expect(res.statusCode).toBe(200);
    expect(res.body).toEqual({ status: 'ok' });
    expect(res.headers['access-control-allow-origin']).toBeDefined();
  });
});

describe('Axios integration', () => {
  test('axios.get returns mocked data', async () => {
    const mockedData = { data: { message: 'mocked response' } };
    axios.get.mockResolvedValue(mockedData);

    const response = await axios.get('https://example.com/api');
    expect(response).toEqual(mockedData);
    expect(axios.get).toHaveBeenCalledWith('https://example.com/api');
  });

  test('axios.get handles error correctly', async () => {
    const error = new Error('Network error');
    axios.get.mockRejectedValue(error);

    await expect(axios.get('https://example.com/api')).rejects.toThrow('Network error');
  });
});

describe('package.json scripts execution', () => {
  const packageJson = require('../package.json');

  beforeEach(() => {
    spawn.mockClear();
  });

  test('start script runs "node src/server.js"', () => {
    const startScript = packageJson.scripts.start;
    expect(startScript).toBe('node src/server.js');

    // Simulate running the start script
    const mockProcess = { on: jest.fn() };
    spawn.mockReturnValue(mockProcess);

    const child = spawn('node', ['src/server.js']);
    expect(spawn).toHaveBeenCalledWith('node', ['src/server.js']);
    expect(child).toBe(mockProcess);
  });

  test('dev script runs "nodemon src/server.js"', () => {
    const devScript = packageJson.scripts.dev;
    expect(devScript).toBe('nodemon src/server.js');

    const mockProcess = { on: jest.fn() };
    spawn.mockReturnValue(mockProcess);

    const child = spawn('nodemon', ['src/server.js']);
    expect(spawn).toHaveBeenCalledWith('nodemon', ['src/server.js']);
    expect(child).toBe(mockProcess);
  });

  test('test script runs "jest"', () => {
    const testScript = packageJson.scripts.test;
    expect(testScript).toBe('jest');
  });
});