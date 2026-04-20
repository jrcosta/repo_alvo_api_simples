const axios = require('axios');
const externalService = require('../externalService');

jest.mock('axios');

describe('ExternalService.estimateAge', () => {
  const originalConsoleError = console.error;

  beforeEach(() => {
    jest.clearAllMocks();
    console.error = jest.fn();
  });

  afterAll(() => {
    console.error = originalConsoleError;
  });

  test('estimateAge returns data with name, age and count when API returns valid age', async () => {
    const name = 'Ana';
    const apiResponse = {
      data: {
        name: 'Ana',
        age: 30,
        count: 100,
      },
    };
    axios.get.mockResolvedValue(apiResponse);

    const result = await externalService.estimateAge(name);

    expect(axios.get).toHaveBeenCalledTimes(1);
    expect(axios.get).toHaveBeenCalledWith(`https://api.agify.io?name=${encodeURIComponent(name)}`);
    expect(result).toEqual({
      name: 'Ana',
      age: 30,
      count: 100,
    });
  });

  test('estimateAge returns default object with age null and count 0 when API returns age null', async () => {
    const name = 'UnknownName';
    const apiResponse = {
      data: {
        name: 'UnknownName',
        age: null,
        count: 0,
      },
    };
    axios.get.mockResolvedValue(apiResponse);

    const result = await externalService.estimateAge(name);

    expect(axios.get).toHaveBeenCalledWith(`https://api.agify.io?name=${encodeURIComponent(name)}`);
    expect(result).toEqual({
      name: 'UnknownName',
      age: null,
      count: 0,
    });
  });

  test('estimateAge returns default object with age null and count 0 when API returns no data', async () => {
    const name = 'NoDataName';
    const apiResponse = {};
    axios.get.mockResolvedValue(apiResponse);

    const result = await externalService.estimateAge(name);

    expect(axios.get).toHaveBeenCalledWith(`https://api.agify.io?name=${encodeURIComponent(name)}`);
    expect(result).toEqual({
      name: 'NoDataName',
      age: null,
      count: 0,
    });
  });

  test('estimateAge returns default object and logs error when axios.get throws', async () => {
    const name = 'ErrorName';
    const errorMessage = 'Network error';
    axios.get.mockRejectedValue(new Error(errorMessage));

    const result = await externalService.estimateAge(name);

    expect(axios.get).toHaveBeenCalledWith(`https://api.agify.io?name=${encodeURIComponent(name)}`);
    expect(console.error).toHaveBeenCalledTimes(1);
    expect(console.error).toHaveBeenCalledWith(expect.stringContaining(`Error fetching age for ${name}: ${errorMessage}`));
    expect(result).toEqual({
      name: 'ErrorName',
      age: null,
      count: 0,
    });
  });

  test('estimateAge calls axios.get with correctly encoded name containing special characters and spaces', async () => {
    const name = 'José da Silva & Co.';
    const apiResponse = {
      data: {
        name: 'José da Silva & Co.',
        age: 45,
        count: 50,
      },
    };
    axios.get.mockResolvedValue(apiResponse);

    const result = await externalService.estimateAge(name);

    expect(axios.get).toHaveBeenCalledWith(`https://api.agify.io?name=${encodeURIComponent(name)}`);
    expect(result).toEqual({
      name: 'José da Silva & Co.',
      age: 45,
      count: 50,
    });
  });

  test('estimateAge handles empty string name gracefully', async () => {
    const name = '';
    const apiResponse = {
      data: {
        name: '',
        age: null,
        count: 0,
      },
    };
    axios.get.mockResolvedValue(apiResponse);

    const result = await externalService.estimateAge(name);

    expect(axios.get).toHaveBeenCalledWith(`https://api.agify.io?name=${encodeURIComponent(name)}`);
    expect(result).toEqual({
      name: '',
      age: null,
      count: 0,
    });
  });

  test('estimateAge handles null name gracefully', async () => {
    // encodeURIComponent(null) returns 'null' (no throw); the axios mock is
    // cleared and will return undefined, causing the catch block to run,
    // which returns the fallback object.
    const result = await externalService.estimateAge(null);
    expect(result).toEqual({ name: null, age: null, count: 0 });
  });
});