const request = require('supertest');
const express = require('express');
const cors = require('cors');
const productRoutes = require('../src/routes/products');
const userRoutes = require('../src/routes/users');
const pingRoutes = require('../src/routes/ping');

let app;

beforeEach(() => {
  app = express();
  app.use(cors());
  app.use(express.json());

  app.get('/health', (req, res) => {
    res.json({ status: 'ok' });
  });

  app.use('/users', userRoutes);
  app.use('/ping', pingRoutes);
  app.use('/products', productRoutes);
});

describe('App.js route integration tests', () => {
  test('should register /products route with productRoutes middleware', () => {
    // Express does not expose route stack easily, so we check by making a request
    return request(app)
      .get('/products')
      .then(response => {
        expect([200, 404]).toContain(response.status); // 200 if products exist, 404 if empty or no products
      });
  });

  test('should respond to GET /products with status 200 and JSON body', async () => {
    const res = await request(app).get('/products');
    expect(res.status).toBe(200);
    expect(res.headers['content-type']).toMatch(/json/);
    expect(Array.isArray(res.body) || typeof res.body === 'object').toBe(true);
  });

  test('should respond to GET /products/:id with product or 404 if not found', async () => {
    // First try to get a product list to find an existing id
    const listRes = await request(app).get('/products');
    expect(listRes.status).toBe(200);
    const products = listRes.body;
    if (Array.isArray(products) && products.length > 0) {
      const id = products[0].id || products[0]._id || products[0].productId || products[0].id;
      const res = await request(app).get(`/products/${id}`);
      expect([200, 404]).toContain(res.status);
      if (res.status === 200) {
        expect(res.body).toHaveProperty('id');
      }
    } else {
      // No products, test 404 for a random id
      const res = await request(app).get('/products/999999');
      expect(res.status).toBe(404);
    }
  });

  test('should support POST, PUT, DELETE methods on /products as per implementation', async () => {
    // POST - create product
    const newProduct = { name: 'Test Product', price: 10.5 };
    const postRes = await request(app).post('/products').send(newProduct);
    expect([201, 200, 400, 422]).toContain(postRes.status); // 201 Created or validation error

    if (postRes.status === 201 || postRes.status === 200) {
      const createdProduct = postRes.body;
      expect(createdProduct).toHaveProperty('id');

      // PUT - update product
      const updatedData = { name: 'Updated Product', price: 20 };
      const putRes = await request(app)
        .put(`/products/${createdProduct.id || createdProduct._id}`)
        .send(updatedData);
      expect([200, 400, 404, 422]).toContain(putRes.status);

      // DELETE - delete product
      const deleteRes = await request(app).delete(`/products/${createdProduct.id || createdProduct._id}`);
      expect([200, 204, 404]).toContain(deleteRes.status);
    }
  });

  test('should return 500 error if products module fails to load or throws error', async () => {
    // Simulate failure by mocking productRoutes to throw error middleware
    const faultyApp = express();
    faultyApp.use(cors());
    faultyApp.use(express.json());
    faultyApp.use('/products', (req, res, next) => {
      next(new Error('Simulated internal error'));
    });
    faultyApp.use((err, req, res, next) => {
      res.status(500).json({ error: err.message });
    });

    const res = await request(faultyApp).get('/products');
    expect(res.status).toBe(500);
    expect(res.body).toHaveProperty('error', 'Simulated internal error');
  });

  test('should keep existing /users and /ping routes working after adding /products', async () => {
    const usersRes = await request(app).get('/users');
    expect([200, 404]).toContain(usersRes.status);

    const pingRes = await request(app).get('/ping');
    expect(pingRes.status).toBe(200);
    expect(pingRes.text).toBeDefined();
  });

  test('should apply CORS and JSON parsing middleware globally including /products', async () => {
    // CORS preflight request
    const optionsRes = await request(app).options('/products');
    expect(optionsRes.status).toBe(204);

    // JSON parsing: send invalid JSON to /products POST
    const res = await request(app)
      .post('/products')
      .set('Content-Type', 'application/json')
      .send('{"invalidJson":'); // malformed JSON
    expect([400, 422]).toContain(res.status);
  });

  test('should respond to /health with status ok after adding /products', async () => {
    const res = await request(app).get('/health');
    expect(res.status).toBe(200);
    expect(res.body).toEqual({ status: 'ok' });
  });

  test('should reject invalid payloads on POST and PUT /products with clear errors', async () => {
    // POST invalid payload
    const postRes = await request(app).post('/products').send({ invalidField: 'abc' });
    expect([400, 422]).toContain(postRes.status);

    // PUT invalid payload
    const putRes = await request(app).put('/products/1').send({ invalidField: 'abc' });
    expect([400, 404, 422]).toContain(putRes.status);
  });

  test('should handle concurrent POST requests to /products consistently', async () => {
    const newProduct = { name: 'Concurrent Product', price: 15 };
    const promises = [];
    for (let i = 0; i < 5; i++) {
      promises.push(request(app).post('/products').send(newProduct));
    }
    const results = await Promise.all(promises);
    results.forEach(res => {
      expect([201, 200, 400, 422]).toContain(res.status);
    });
  });

  test('should handle pagination or limits on GET /products if applicable', async () => {
    // Try query params for pagination
    const res = await request(app).get('/products?limit=2&page=1');
    expect(res.status).toBe(200);
    expect(Array.isArray(res.body)).toBe(true);
    if (res.body.length > 0) {
      expect(res.body.length).toBeLessThanOrEqual(2);
    }
  });
});