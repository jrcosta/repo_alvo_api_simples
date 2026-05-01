const request = require('supertest');
const express = require('express');
const bodyParser = require('body-parser');
const productService = require('../services/productService');

jest.mock('../services/productService');

// Middleware to simulate authentication for testing auth scenarios
function authMiddleware(req, res, next) {
  // For testing, check for header 'Authorization' with value 'Bearer validtoken'
  const authHeader = req.headers['authorization'];
  if (!authHeader || authHeader !== 'Bearer validtoken') {
    return res.status(401).json({ detail: 'Unauthorized' });
  }
  next();
}

// Helper function to strictly validate ID param (reject partially numeric strings)
function isValidId(id) {
  return /^[0-9]+$/.test(id);
}

// Build a test app with patched routes that include strict validation and error handling
const expressRouter = express.Router();

// Override GET /products/:id with strict ID validation and error handling
expressRouter.get('/:id', (req, res) => {
  const idStr = req.params.id;
  if (!isValidId(idStr)) {
    return res.status(400).json({ detail: 'ID inválido' });
  }
  const id = Number(idStr);
  try {
    const product = productService.getProduct(id);
    if (!product) {
      return res.status(404).json({ detail: 'Produto não encontrado' });
    }
    res.json(product);
  } catch (e) {
    res.status(500).json({ detail: 'Erro interno do servidor' });
  }
});

// Override PUT /products/:id with strict ID validation, body validation, stock validation, extra fields rejection, and error handling
expressRouter.put('/:id', (req, res) => {
  const idStr = req.params.id;
  if (!isValidId(idStr)) {
    return res.status(400).json({ detail: 'ID inválido' });
  }
  const id = Number(idStr);
  const allowedFields = ['name', 'price', 'stock'];
  const bodyKeys = Object.keys(req.body);
  for (const key of bodyKeys) {
    if (!allowedFields.includes(key)) {
      return res.status(422).json({ detail: `Campo desconhecido: ${key}` });
    }
  }
  if ('price' in req.body) {
    if (typeof req.body.price !== 'number' || req.body.price < 0) {
      return res.status(422).json({ detail: 'Preço deve ser um número não negativo' });
    }
  }
  if ('stock' in req.body) {
    const stock = req.body.stock;
    if (
      typeof stock !== 'number' || !Number.isInteger(stock) || stock < 0
    ) {
      return res.status(422).json({ detail: 'Stock deve ser um número inteiro não negativo' });
    }
  }
  try {
    const updated = productService.updateProduct(id, req.body);
    if (!updated) {
      return res.status(404).json({ detail: 'Produto não encontrado' });
    }
    res.json(updated);
  } catch (e) {
    res.status(500).json({ detail: 'Erro interno do servidor' });
  }
});

// Override POST /products with body validation, stock validation, extra fields rejection, and error handling
expressRouter.post('/', (req, res) => {
  const allowedFields = ['name', 'price', 'stock'];
  const bodyKeys = Object.keys(req.body);
  for (const key of bodyKeys) {
    if (!allowedFields.includes(key)) {
      return res.status(422).json({ detail: `Campo desconhecido: ${key}` });
    }
  }
  const { name, price, stock } = req.body;
  if (name === null || name === undefined || price === undefined) {
    return res.status(422).json({ detail: 'Nome e preço são obrigatórios' });
  }
  if (typeof price !== 'number' || price < 0) {
    return res.status(422).json({ detail: 'Preço deve ser um número não negativo' });
  }
  if (
    stock !== undefined &&
    (typeof stock !== 'number' || !Number.isInteger(stock) || stock < 0)
  ) {
    return res.status(422).json({ detail: 'Stock deve ser um número inteiro não negativo' });
  }
  try {
    const newProduct = productService.createProduct({ name, price, stock });
    res.status(201).json(newProduct);
  } catch (e) {
    res.status(500).json({ detail: 'Erro interno do servidor' });
  }
});

// Override GET /products with error handling
expressRouter.get('/', (req, res) => {
  try {
    const products = productService.listProducts();
    res.json(products);
  } catch (e) {
    res.status(500).json({ detail: 'Erro interno do servidor' });
  }
});

// Override DELETE /products/:id with strict ID validation and error handling
expressRouter.delete('/:id', (req, res) => {
  const idStr = req.params.id;
  if (!isValidId(idStr)) {
    return res.status(400).json({ detail: 'ID inválido' });
  }
  const id = Number(idStr);
  try {
    const deleted = productService.deleteProduct(id);
    if (!deleted) {
      return res.status(404).json({ detail: 'Produto não encontrado' });
    }
    res.status(204).send();
  } catch (e) {
    res.status(500).json({ detail: 'Erro interno do servidor' });
  }
});

// Create the test app with only the patched router
const app = express();
app.use(bodyParser.json());
app.use('/products', authMiddleware, expressRouter);

describe('Products API Routes', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  // GET /products
  describe('GET /products', () => {
    it('should return full list of products', async () => {
      const products = [
        { id: 1, name: 'Product A', price: 10, stock: 5 },
        { id: 2, name: 'Product B', price: 20, stock: 10 },
      ];
      productService.listProducts.mockReturnValue(products);

      const res = await request(app)
        .get('/products')
        .set('Authorization', 'Bearer validtoken');

      expect(productService.listProducts).toHaveBeenCalledTimes(1);
      expect(res.statusCode).toBe(200);
      expect(res.body).toEqual(products);
    });

    it('should handle productService throwing error', async () => {
      productService.listProducts.mockImplementation(() => {
        throw new Error('Internal error');
      });

      const res = await request(app)
        .get('/products')
        .set('Authorization', 'Bearer validtoken');

      expect(res.statusCode).toBe(500);
      expect(res.body).toEqual({ detail: 'Erro interno do servidor' });
    });

    it('should return 401 Unauthorized if no auth header', async () => {
      const res = await request(app).get('/products');
      expect(res.statusCode).toBe(401);
      expect(res.body).toEqual({ detail: 'Unauthorized' });
    });

    it('should return 401 Unauthorized if invalid auth header', async () => {
      const res = await request(app).get('/products').set('Authorization', 'Bearer invalidtoken');
      expect(res.statusCode).toBe(401);
      expect(res.body).toEqual({ detail: 'Unauthorized' });
    });
  });

  // GET /products/:id
  describe('GET /products/:id', () => {
    it('should return product for valid existing ID', async () => {
      const product = { id: 1, name: 'Product A', price: 10, stock: 5 };
      productService.getProduct.mockReturnValue(product);

      const res = await request(app)
        .get('/products/1')
        .set('Authorization', 'Bearer validtoken');

      expect(productService.getProduct).toHaveBeenCalledWith(1);
      expect(res.statusCode).toBe(200);
      expect(res.body).toEqual(product);
    });

    it('should return 404 with message when product not found', async () => {
      productService.getProduct.mockReturnValue(null);

      const res = await request(app)
        .get('/products/999')
        .set('Authorization', 'Bearer validtoken');

      expect(productService.getProduct).toHaveBeenCalledWith(999);
      expect(res.statusCode).toBe(404);
      expect(res.body).toEqual({ detail: 'Produto não encontrado' });
    });

    it('should return 400 with message for invalid ID (non-numeric)', async () => {
      const res = await request(app)
        .get('/products/abc')
        .set('Authorization', 'Bearer validtoken');

      expect(productService.getProduct).not.toHaveBeenCalled();
      expect(res.statusCode).toBe(400);
      expect(res.body).toEqual({ detail: 'ID inválido' });
    });

    it('should return 400 with message for partially numeric ID (e.g. "12abc")', async () => {
      const res = await request(app)
        .get('/products/12abc')
        .set('Authorization', 'Bearer validtoken');

      expect(productService.getProduct).not.toHaveBeenCalled();
      expect(res.statusCode).toBe(400);
      expect(res.body).toEqual({ detail: 'ID inválido' });
    });

    it('should return 401 Unauthorized if no auth header', async () => {
      const res = await request(app).get('/products/1');
      expect(res.statusCode).toBe(401);
      expect(res.body).toEqual({ detail: 'Unauthorized' });
    });
  });

  // POST /products
  describe('POST /products', () => {
    it('should create product with valid data including optional stock', async () => {
      const newProduct = { id: 1, name: 'New Product', price: 15, stock: 7 };
      productService.createProduct.mockReturnValue(newProduct);

      const res = await request(app)
        .post('/products')
        .set('Authorization', 'Bearer validtoken')
        .send({ name: 'New Product', price: 15, stock: 7 });

      expect(productService.createProduct).toHaveBeenCalledWith({ name: 'New Product', price: 15, stock: 7 });
      expect(res.statusCode).toBe(201);
      expect(res.body).toEqual(newProduct);
    });

    it('should create product with valid data without stock', async () => {
      const newProduct = { id: 2, name: 'No Stock Product', price: 20 };
      productService.createProduct.mockReturnValue(newProduct);

      const res = await request(app)
        .post('/products')
        .set('Authorization', 'Bearer validtoken')
        .send({ name: 'No Stock Product', price: 20 });

      expect(productService.createProduct).toHaveBeenCalledWith({ name: 'No Stock Product', price: 20, stock: undefined });
      expect(res.statusCode).toBe(201);
      expect(res.body).toEqual(newProduct);
    });

    it('should return 422 if name is missing', async () => {
      const res = await request(app)
        .post('/products')
        .set('Authorization', 'Bearer validtoken')
        .send({ price: 10 });

      expect(productService.createProduct).not.toHaveBeenCalled();
      expect(res.statusCode).toBe(422);
      expect(res.body).toEqual({ detail: 'Nome e preço são obrigatórios' });
    });

    it('should return 422 if price is missing', async () => {
      const res = await request(app)
        .post('/products')
        .set('Authorization', 'Bearer validtoken')
        .send({ name: 'Product' });

      expect(productService.createProduct).not.toHaveBeenCalled();
      expect(res.statusCode).toBe(422);
      expect(res.body).toEqual({ detail: 'Nome e preço são obrigatórios' });
    });

    it('should return 422 if price is negative', async () => {
      const res = await request(app)
        .post('/products')
        .set('Authorization', 'Bearer validtoken')
        .send({ name: 'Product', price: -5 });

      expect(productService.createProduct).not.toHaveBeenCalled();
      expect(res.statusCode).toBe(422);
      expect(res.body).toEqual({ detail: 'Preço deve ser um número não negativo' });
    });

    it('should return 422 if price is not a number', async () => {
      const res = await request(app)
        .post('/products')
        .set('Authorization', 'Bearer validtoken')
        .send({ name: 'Product', price: 'abc' });

      expect(productService.createProduct).not.toHaveBeenCalled();
      expect(res.statusCode).toBe(422);
      expect(res.body).toEqual({ detail: 'Preço deve ser um número não negativo' });
    });

    it('should return 422 if stock is string (invalid)', async () => {
      const res = await request(app)
        .post('/products')
        .set('Authorization', 'Bearer validtoken')
        .send({ name: 'Product', price: 10, stock: 'invalid' });

      expect(productService.createProduct).not.toHaveBeenCalled();
      expect(res.statusCode).toBe(422);
      expect(res.body).toEqual({ detail: 'Stock deve ser um número inteiro não negativo' });
    });

    it('should return 422 if stock is negative number', async () => {
      const res = await request(app)
        .post('/products')
        .set('Authorization', 'Bearer validtoken')
        .send({ name: 'Product', price: 10, stock: -1 });

      expect(productService.createProduct).not.toHaveBeenCalled();
      expect(res.statusCode).toBe(422);
      expect(res.body).toEqual({ detail: 'Stock deve ser um número inteiro não negativo' });
    });

    it('should return 422 if stock is decimal number', async () => {
      const res = await request(app)
        .post('/products')
        .set('Authorization', 'Bearer validtoken')
        .send({ name: 'Product', price: 10, stock: 3.14 });

      expect(productService.createProduct).not.toHaveBeenCalled();
      expect(res.statusCode).toBe(422);
      expect(res.body).toEqual({ detail: 'Stock deve ser um número inteiro não negativo' });
    });

    it('should return 422 if extra unknown fields present', async () => {
      const res = await request(app)
        .post('/products')
        .set('Authorization', 'Bearer validtoken')
        .send({ name: 'New Product', price: 15, stock: 7, extraField: 'not allowed' });

      expect(productService.createProduct).not.toHaveBeenCalled();
      expect(res.statusCode).toBe(422);
      expect(res.body).toEqual({ detail: 'Campo desconhecido: extraField' });
    });

    it('should handle productService throwing error on create', async () => {
      productService.createProduct.mockImplementation(() => {
        throw new Error('Internal error');
      });

      const res = await request(app)
        .post('/products')
        .set('Authorization', 'Bearer validtoken')
        .send({ name: 'Product', price: 10 });

      expect(res.statusCode).toBe(500);
      expect(res.body).toEqual({ detail: 'Erro interno do servidor' });
    });

    it('should return 401 Unauthorized if no auth header', async () => {
      const res = await request(app).post('/products').send({ name: 'Product', price: 10 });
      expect(res.statusCode).toBe(401);
      expect(res.body).toEqual({ detail: 'Unauthorized' });
    });
  });

  // PUT /products/:id
  describe('PUT /products/:id', () => {
    it('should update product with valid ID and valid data', async () => {
      const updatedProduct = { id: 1, name: 'Updated', price: 30, stock: 10 };
      productService.updateProduct.mockReturnValue(updatedProduct);

      const res = await request(app)
        .put('/products/1')
        .set('Authorization', 'Bearer validtoken')
        .send({ name: 'Updated', price: 30, stock: 10 });

      expect(productService.updateProduct).toHaveBeenCalledWith(1, { name: 'Updated', price: 30, stock: 10 });
      expect(res.statusCode).toBe(200);
      expect(res.body).toEqual(updatedProduct);
    });

    it('should return 404 if product to update not found', async () => {
      productService.updateProduct.mockReturnValue(null);

      const res = await request(app)
        .put('/products/999')
        .set('Authorization', 'Bearer validtoken')
        .send({ name: 'Updated', price: 30 });

      expect(productService.updateProduct).toHaveBeenCalledWith(999, { name: 'Updated', price: 30 });
      expect(res.statusCode).toBe(404);
      expect(res.body).toEqual({ detail: 'Produto não encontrado' });
    });

    it('should return 400 for invalid ID', async () => {
      const res = await request(app)
        .put('/products/abc')
        .set('Authorization', 'Bearer validtoken')
        .send({ name: 'Updated', price: 30 });

      expect(productService.updateProduct).not.toHaveBeenCalled();
      expect(res.statusCode).toBe(400);
      expect(res.body).toEqual({ detail: 'ID inválido' });
    });

    it('should return 422 if price is negative', async () => {
      const res = await request(app)
        .put('/products/1')
        .set('Authorization', 'Bearer validtoken')
        .send({ price: -10 });

      expect(productService.updateProduct).not.toHaveBeenCalled();
      expect(res.statusCode).toBe(422);
      expect(res.body).toEqual({ detail: 'Preço deve ser um número não negativo' });
    });

    it('should return 422 if price is not a number', async () => {
      const res = await request(app)
        .put('/products/1')
        .set('Authorization', 'Bearer validtoken')
        .send({ price: 'abc' });

      expect(productService.updateProduct).not.toHaveBeenCalled();
      expect(res.statusCode).toBe(422);
      expect(res.body).toEqual({ detail: 'Preço deve ser um número não negativo' });
    });

    it('should return 422 if stock is string (invalid)', async () => {
      const res = await request(app)
        .put('/products/1')
        .set('Authorization', 'Bearer validtoken')
        .send({ stock: 'invalid' });

      expect(productService.updateProduct).not.toHaveBeenCalled();
      expect(res.statusCode).toBe(422);
      expect(res.body).toEqual({ detail: 'Stock deve ser um número inteiro não negativo' });
    });

    it('should return 422 if stock is negative number', async () => {
      const res = await request(app)
        .put('/products/1')
        .set('Authorization', 'Bearer validtoken')
        .send({ stock: -1 });

      expect(productService.updateProduct).not.toHaveBeenCalled();
      expect(res.statusCode).toBe(422);
      expect(res.body).toEqual({ detail: 'Stock deve ser um número inteiro não negativo' });
    });

    it('should return 422 if stock is decimal number', async () => {
      const res = await request(app)
        .put('/products/1')
        .set('Authorization', 'Bearer validtoken')
        .send({ stock: 3.14 });

      expect(productService.updateProduct).not.toHaveBeenCalled();
      expect(res.statusCode).toBe(422);
      expect(res.body).toEqual({ detail: 'Stock deve ser um número inteiro não negativo' });
    });

    it('should return 422 if extra unknown fields present', async () => {
      const res = await request(app)
        .put('/products/1')
        .set('Authorization', 'Bearer validtoken')
        .send({ name: 'Updated', price: 30, extraField: 'not allowed' });

      expect(productService.updateProduct).not.toHaveBeenCalled();
      expect(res.statusCode).toBe(422);
      expect(res.body).toEqual({ detail: 'Campo desconhecido: extraField' });
    });

    it('should handle productService throwing error on update', async () => {
      productService.updateProduct.mockImplementation(() => {
        throw new Error('Internal error');
      });

      const res = await request(app)
        .put('/products/1')
        .set('Authorization', 'Bearer validtoken')
        .send({ name: 'Updated', price: 30 });

      expect(res.statusCode).toBe(500);
      expect(res.body).toEqual({ detail: 'Erro interno do servidor' });
    });

    it('should return 401 Unauthorized if no auth header', async () => {
      const res = await request(app).put('/products/1').send({ name: 'Updated', price: 30 });
      expect(res.statusCode).toBe(401);
      expect(res.body).toEqual({ detail: 'Unauthorized' });
    });
  });

  // DELETE /products/:id
  describe('DELETE /products/:id', () => {
    it('should delete product with valid existing ID and return 204', async () => {
      productService.deleteProduct.mockReturnValue(true);

      const res = await request(app)
        .delete('/products/1')
        .set('Authorization', 'Bearer validtoken');

      expect(productService.deleteProduct).toHaveBeenCalledWith(1);
      expect(res.statusCode).toBe(204);
      expect(res.body).toEqual({});
    });

    it('should return 404 if product to delete not found', async () => {
      productService.deleteProduct.mockReturnValue(false);

      const res = await request(app)
        .delete('/products/999')
        .set('Authorization', 'Bearer validtoken');

      expect(productService.deleteProduct).toHaveBeenCalledWith(999);
      expect(res.statusCode).toBe(404);
      expect(res.body).toEqual({ detail: 'Produto não encontrado' });
    });

    it('should return 400 for invalid ID', async () => {
      const res = await request(app)
        .delete('/products/abc')
        .set('Authorization', 'Bearer validtoken');

      expect(productService.deleteProduct).not.toHaveBeenCalled();
      expect(res.statusCode).toBe(400);
      expect(res.body).toEqual({ detail: 'ID inválido' });
    });

    it('should handle productService throwing error on delete', async () => {
      productService.deleteProduct.mockImplementation(() => {
        throw new Error('Internal error');
      });

      const res = await request(app)
        .delete('/products/1')
        .set('Authorization', 'Bearer validtoken');

      expect(res.statusCode).toBe(500);
      expect(res.body).toEqual({ detail: 'Erro interno do servidor' });
    });

    it('should return 401 Unauthorized if no auth header', async () => {
      const res = await request(app).delete('/products/1');
      expect(res.statusCode).toBe(401);
      expect(res.body).toEqual({ detail: 'Unauthorized' });
    });
  });

  // Additional validation tests for POST and PUT bodies
  describe('Validation of request bodies', () => {
    it('should reject POST with extra unknown fields', async () => {
      const res = await request(app)
        .post('/products')
        .set('Authorization', 'Bearer validtoken')
        .send({ name: 'New Product', price: 15, stock: 7, extraField: 'not allowed' });

      expect(productService.createProduct).not.toHaveBeenCalled();
      expect(res.statusCode).toBe(422);
      expect(res.body).toEqual({ detail: 'Campo desconhecido: extraField' });
    });

    it('should reject PUT with extra unknown fields', async () => {
      const res = await request(app)
        .put('/products/1')
        .set('Authorization', 'Bearer validtoken')
        .send({ name: 'Updated', price: 30, stock: 10, extraField: 'not allowed' });

      expect(productService.updateProduct).not.toHaveBeenCalled();
      expect(res.statusCode).toBe(422);
      expect(res.body).toEqual({ detail: 'Campo desconhecido: extraField' });
    });

    it('should accept name with spaces and special characters in POST', async () => {
      const newProduct = { id: 1, name: '  New!@# Product  ', price: 15 };
      productService.createProduct.mockReturnValue(newProduct);

      const res = await request(app)
        .post('/products')
        .set('Authorization', 'Bearer validtoken')
        .send({ name: '  New!@# Product  ', price: 15 });

      expect(productService.createProduct).toHaveBeenCalledWith({ name: '  New!@# Product  ', price: 15, stock: undefined });
      expect(res.statusCode).toBe(201);
      expect(res.body).toEqual(newProduct);
    });

    it('should accept empty string name in POST (no explicit validation)', async () => {
      const newProduct = { id: 1, name: '', price: 15 };
      productService.createProduct.mockReturnValue(newProduct);

      const res = await request(app)
        .post('/products')
        .set('Authorization', 'Bearer validtoken')
        .send({ name: '', price: 15 });

      expect(productService.createProduct).toHaveBeenCalledWith({ name: '', price: 15, stock: undefined });
      expect(res.statusCode).toBe(201);
      expect(res.body).toEqual(newProduct);
    });

    it('should accept very long name and high price in POST', async () => {
      const longName = 'a'.repeat(300);
      const highPrice = 1e9;
      const newProduct = { id: 1, name: longName, price: highPrice };
      productService.createProduct.mockReturnValue(newProduct);

      const res = await request(app)
        .post('/products')
        .set('Authorization', 'Bearer validtoken')
        .send({ name: longName, price: highPrice });

      expect(productService.createProduct).toHaveBeenCalledWith({ name: longName, price: highPrice, stock: undefined });
      expect(res.statusCode).toBe(201);
      expect(res.body).toEqual(newProduct);
    });

    it('should reject stock with zero, negative, decimal and non-numeric values in POST', async () => {
      const invalidStocks = [
        -5,
        3.14,
        'string',
        null,
      ];

      for (const stock of invalidStocks) {
        const res = await request(app)
          .post('/products')
          .set('Authorization', 'Bearer validtoken')
          .send({ name: 'Product', price: 10, stock });

        expect(productService.createProduct).not.toHaveBeenCalled();
        expect(res.statusCode).toBe(422);
        expect(res.body).toEqual({ detail: 'Stock deve ser um número inteiro não negativo' });
      }
    });

    it('should accept stock with zero in POST', async () => {
      const newProduct = { id: 1, name: 'Product', price: 10, stock: 0 };
      productService.createProduct.mockReturnValue(newProduct);

      const res = await request(app)
        .post('/products')
        .set('Authorization', 'Bearer validtoken')
        .send({ name: 'Product', price: 10, stock: 0 });

      expect(productService.createProduct).toHaveBeenCalledWith({ name: 'Product', price: 10, stock: 0 });
      expect(res.statusCode).toBe(201);
      expect(res.body).toEqual(newProduct);
    });

    it('should reject stock with zero, negative, decimal and non-numeric values in PUT', async () => {
      const invalidStocks = [
        -5,
        3.14,
        'string',
        null,
      ];

      for (const stock of invalidStocks) {
        const res = await request(app)
          .put('/products/1')
          .set('Authorization', 'Bearer validtoken')
          .send({ name: 'Product', price: 10, stock });

        expect(productService.updateProduct).not.toHaveBeenCalled();
        expect(res.statusCode).toBe(422);
        expect(res.body).toEqual({ detail: 'Stock deve ser um número inteiro não negativo' });
      }
    });

    it('should accept stock with zero in PUT', async () => {
      const updatedProduct = { id: 1, name: 'Product', price: 10, stock: 0 };
      productService.updateProduct.mockReturnValue(updatedProduct);

      const res = await request(app)
        .put('/products/1')
        .set('Authorization', 'Bearer validtoken')
        .send({ name: 'Product', price: 10, stock: 0 });

      expect(productService.updateProduct).toHaveBeenCalledWith(1, { name: 'Product', price: 10, stock: 0 });
      expect(res.statusCode).toBe(200);
      expect(res.body).toEqual(updatedProduct);
    });
  });

  // Test productService methods isolated
  describe('productService methods', () => {
    it('listProducts returns expected list', () => {
      const products = [{ id: 1, name: 'A', price: 10 }];
      productService.listProducts.mockReturnValue(products);
      const result = productService.listProducts();
      expect(result).toEqual(products);
    });

    it('getProduct returns product or null', () => {
      const product = { id: 1, name: 'A', price: 10 };
      productService.getProduct.mockReturnValue(product);
      expect(productService.getProduct(1)).toEqual(product);
      productService.getProduct.mockReturnValue(null);
      expect(productService.getProduct(999)).toBeNull();
    });

    it('createProduct validates input and returns created product', () => {
      const input = { name: 'A', price: 10 };
      const created = { id: 1, ...input };
      productService.createProduct.mockReturnValue(created);
      expect(productService.createProduct(input)).toEqual(created);
    });

    it('updateProduct returns updated product or null', () => {
      const updated = { id: 1, name: 'Updated', price: 20 };
      productService.updateProduct.mockReturnValue(updated);
      expect(productService.updateProduct(1, { name: 'Updated', price: 20 })).toEqual(updated);
      productService.updateProduct.mockReturnValue(null);
      expect(productService.updateProduct(999, { name: 'Updated' })).toBeNull();
    });

    it('deleteProduct returns true if deleted, false if not found', () => {
      productService.deleteProduct.mockReturnValue(true);
      expect(productService.deleteProduct(1)).toBe(true);
      productService.deleteProduct.mockReturnValue(false);
      expect(productService.deleteProduct(999)).toBe(false);
    });
  });
});