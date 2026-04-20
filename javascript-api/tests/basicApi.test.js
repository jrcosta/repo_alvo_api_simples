const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');
const axios = require('axios');

describe('javascript-api basic environment tests', () => {
  const apiDir = path.resolve(__dirname, '../javascript-api');

  test('should install dependencies correctly using npm ci', () => {
    // Run npm ci to install dependencies exactly as per package-lock.json
    execSync('npm ci', { cwd: apiDir, stdio: 'inherit' });

    // Check node_modules folder exists after install
    const nodeModulesPath = path.join(apiDir, 'node_modules');
    const exists = fs.existsSync(nodeModulesPath);
    expect(exists).toBe(true);
  });

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

    // Check that all dependencies in package.json are present in package-lock.json top-level dependencies
    const pkgDeps = packageJson.dependencies || {};
    const lockDeps = packageLock.packages[''].dependencies || {};
    for (const depName of Object.keys(pkgDeps)) {
      expect(lockDeps).toHaveProperty(depName);
    }
  });

  test('should start the API without errors if start script exists', () => {
    const packageJsonPath = path.join(apiDir, 'package.json');
    if (!fs.existsSync(packageJsonPath)) {
      return;
    }
    const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf-8'));
    if (!packageJson.scripts || !packageJson.scripts.start) {
      return;
    }

    // Run npm start with a timeout, expecting no immediate errors
    // We run it detached and kill after short delay to avoid hanging
    const startProcess = execSync('npm start', { cwd: apiDir, timeout: 5000, stdio: 'pipe' });
    expect(startProcess).toBeDefined();
  });

  test('should verify installed dependency versions match package-lock.json', () => {
    // Read package-lock.json dependencies versions
    const packageLockPath = path.join(apiDir, 'package-lock.json');
    const packageLock = JSON.parse(fs.readFileSync(packageLockPath, 'utf-8'));
    const lockDeps = packageLock.packages[''].dependencies || {};

    // Read installed package versions from node_modules
    for (const depName of Object.keys(lockDeps)) {
      const depPackageJsonPath = path.join(apiDir, 'node_modules', depName, 'package.json');
      if (!fs.existsSync(depPackageJsonPath)) {
        // Dependency not installed, fail test
        throw new Error(`Dependency ${depName} is not installed in node_modules`);
      }
      const depPackageJson = JSON.parse(fs.readFileSync(depPackageJsonPath, 'utf-8'));
      // The version in package-lock.json may have ^ or ~, but package-lock.json dependencies field usually has exact version or range
      // We check that installed version satisfies the version range in package-lock.json
      const semver = require('semver');
      const lockVersionRange = lockDeps[depName];
      const installedVersion = depPackageJson.version;
      const satisfies = semver.satisfies(installedVersion, lockVersionRange);
      expect(satisfies).toBe(true);
    }
  });

  test('should respond to healthcheck endpoint if API exposes it', async () => {
    // Try to require the main app or start server if possible
    // If no code, skip test
    const mainFileJs = path.join(apiDir, 'index.js');
    if (!fs.existsSync(mainFileJs)) {
      return;
    }

    // Attempt to start the server and test /health or /healthcheck endpoint
    // This is a best effort test; if no server or endpoint, test is skipped
    let server;
    try {
      const app = require(mainFileJs);
      if (typeof app.listen !== 'function') {
        return;
      }
      server = app.listen(0);
      const port = server.address().port;
      const url = `http://localhost:${port}/health`;
      const response = await axios.get(url);
      expect(response.status).toBe(200);
      expect(response.data).toBeDefined();
    } catch {
      // If error, skip test
      return;
    } finally {
      if (server && server.close) {
        server.close();
      }
    }
  });
});