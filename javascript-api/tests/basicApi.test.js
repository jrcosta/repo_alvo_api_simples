const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

describe('javascript-api basic environment tests', () => {
  // __dirname is javascript-api/tests, so '..' resolves to javascript-api
  const apiDir = path.resolve(__dirname, '..');

  test('should have package-lock.json and package.json in sync', () => {
    const packageJsonPath = path.join(apiDir, 'package.json');
    const packageLockPath = path.join(apiDir, 'package-lock.json');

    expect(fs.existsSync(packageJsonPath)).toBe(true);
    expect(fs.existsSync(packageLockPath)).toBe(true);

    const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf-8'));
    const packageLock = JSON.parse(fs.readFileSync(packageLockPath, 'utf-8'));

    // Basic check: name and version should match
    expect(packageLock.name).toBe(packageJson.name);
    expect(packageLock.version).toBe(packageJson.version);

    // Check that all dependencies in package.json are present in package-lock.json
    const pkgDeps = packageJson.dependencies || {};
    const lockDeps = packageLock.packages[''].dependencies || {};
    for (const depName of Object.keys(pkgDeps)) {
      expect(lockDeps).toHaveProperty(depName);
    }
  });

  test('should have a valid start script defined in package.json', () => {
    const packageJsonPath = path.join(apiDir, 'package.json');
    const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf-8'));

    expect(packageJson.scripts).toBeDefined();
    expect(packageJson.scripts.start).toBeDefined();
    expect(packageJson.scripts.start).toMatch(/node.*server/i);
  });

  test('should verify all declared dependencies are installed in node_modules', () => {
    const packageLockPath = path.join(apiDir, 'package-lock.json');
    const packageLock = JSON.parse(fs.readFileSync(packageLockPath, 'utf-8'));
    const lockDeps = packageLock.packages[''].dependencies || {};

    for (const depName of Object.keys(lockDeps)) {
      const depPackageJsonPath = path.join(apiDir, 'node_modules', depName, 'package.json');
      expect(fs.existsSync(depPackageJsonPath)).toBe(true);
    }
  });

  test('should respond to healthcheck endpoint', async () => {
    const app = require('../src/app');
    const request = require('supertest');

    const res = await request(app).get('/health');
    expect(res.status).toBe(200);
    expect(res.body).toEqual({ status: 'ok' });
  });
});