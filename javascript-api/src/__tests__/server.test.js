const jestMock = require('jest-mock');

describe('server.js', () => {
  let originalEnv;
  let mockApp;
  let listenMock;
  let consoleLogMock;

  beforeEach(() => {
    // Backup original process.env
    originalEnv = { ...process.env };

    // Reset modules to allow re-require with mocks
    jest.resetModules();

    // Mock console.log
    consoleLogMock = jestMock.fn();
    global.console.log = consoleLogMock;

    // Create a mock for app.listen
    listenMock = jestMock.fn((port, cb) => {
      if (cb) cb();
      return { on: jestMock.fn() }; // simulate server object with on method
    });

    // Mock the './app' module to export an object with listen method
    mockApp = { listen: listenMock };

    // Mock require for './app' to return mockApp
    jest.mock('../app', () => mockApp, { virtual: true });
  });

  afterEach(() => {
    // Restore process.env
    process.env = originalEnv;

    // Clear all mocks
    jest.clearAllMocks();
  });

  test('should use process.env.PORT when defined and call app.listen with it', () => {
    process.env.PORT = '4000';

    // Require server.js after setting env and mocks
    require('../server');

    expect(listenMock).toHaveBeenCalledTimes(1);
    expect(listenMock).toHaveBeenCalledWith(4000, expect.any(Function));
    expect(consoleLogMock).toHaveBeenCalledWith('Server running on port 4000');
  });

  test('should fallback to port 3000 when process.env.PORT is not defined', () => {
    delete process.env.PORT;

    require('../server');

    expect(listenMock).toHaveBeenCalledTimes(1);
    expect(listenMock).toHaveBeenCalledWith(3000, expect.any(Function));
    expect(consoleLogMock).toHaveBeenCalledWith('Server running on port 3000');
  });

  test('should call app.listen with a callback function that logs the correct message', () => {
    process.env.PORT = '12345';

    require('../server');

    // Extract the callback passed to listen
    const callback = listenMock.mock.calls[0][1];
    expect(typeof callback).toBe('function');

    // Clear previous logs
    consoleLogMock.mockClear();

    // Call the callback manually
    callback();

    expect(consoleLogMock).toHaveBeenCalledWith('Server running on port 12345');
  });
});