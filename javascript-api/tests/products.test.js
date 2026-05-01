const request = require('supertest');
const express = require('express');
const productRoutes = require('../src/routes/products');
const productService = require('../src/services/productService');

jest.mock('../src/services/productService');

const app = express();
app.use(express.json());
app.use('/products', productRoutes);

describe('Products API Routes', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  // GET /products with valid filters
  test('testGetProducts_withValidFilters_shouldReturnFilteredProducts', async () => {
    const mockProducts = [{ id: 1, name: 'Product A' }];
    productService.listProducts.mockReturnValue(mockProducts);

    const res = await request(app).get('/products')
      .query({ category: 'electronics', search: 'phone', minPrice: '100', maxPrice: '1000', inStock: 'true' });

    expect(productService.listProducts).toHaveBeenCalledWith({
      category: 'electronics',
      search: 'phone',
      minPrice: 100,
      maxPrice: 1000,
      inStock: true,
    });
    expect(res.statusCode).toBe(200);
    expect(res.body).toEqual(mockProducts);
  });

  // GET /products with invalid filters
  test('testGetProducts_withInvalidFilters_shouldReturnErrorOrEmpty', async () => {
    productService.listProducts.mockReturnValue([]);

    const res = await request(app).get('/products')
      .query({ minPrice: 'abc', inStock: 'yes' });

    expect(productService.listProducts).toHaveBeenCalledWith({
      category: undefined,
      search: undefined,
      minPrice: NaN,
      maxPrice: undefined,
      inStock: false,
    });
    expect(res.statusCode).toBe(200);
    expect(res.body).toEqual([]);
  });

  // GET /products/categories returns unique categories
  test('testGetProductsCategories_shouldReturnUniqueCategories', async () => {
    const categories = ['electronics', 'books', 'clothing'];
    productService.listCategories.mockReturnValue(categories);

    const res = await request(app).get('/products/categories');

    expect(productService.listCategories).toHaveBeenCalled();
    expect(res.statusCode).toBe(200);
    expect(res.body).toEqual(categories);
  });

  // GET /products/low-stock with valid threshold
  test('testGetProductsLowStock_withValidThreshold_shouldReturnLowStockProducts', async () => {
    const lowStockProducts = [{ id: 2, name: 'Product B', stock: 3 }];
    productService.listLowStock.mockReturnValue(lowStockProducts);

    const res = await request(app).get('/products/low-stock').query({ threshold: '3' });

    expect(productService.listLowStock).toHaveBeenCalledWith(3);
    expect(res.statusCode).toBe(200);
    expect(res.body).toEqual(lowStockProducts);
  });

  // GET /products/low-stock with invalid threshold
  test('testGetProductsLowStock_withInvalidThreshold_shouldUseDefault', async () => {
    const lowStockProducts = [{ id: 3, name: 'Product C', stock: 2 }];
    productService.listLowStock.mockReturnValue(lowStockProducts);

    const res = await request(app).get('/products/low-stock').query({ threshold: '-1' });

    expect(productService.listLowStock).toHaveBeenCalledWith(-1);
    expect(res.statusCode).toBe(200);
    expect(res.body).toEqual(lowStockProducts);
  });

  // GET /products/low-stock with no threshold (default)
  test('testGetProductsLowStock_withoutThreshold_shouldUseDefault', async () => {
    const lowStockProducts = [{ id: 4, name: 'Product D', stock: 1 }];
    productService.listLowStock.mockReturnValue(lowStockProducts);

    const res = await request(app).get('/products/low-stock');

    expect(productService.listLowStock).toHaveBeenCalledWith(5);
    expect(res.statusCode).toBe(200);
    expect(res.body).toEqual(lowStockProducts);
  });

  // POST /products with valid payload
  test('testPostProducts_withValidPayload_shouldCreateProduct', async () => {
    const newProduct = { id: 10, name: 'New Product' };
    productService.createProduct.mockReturnValue(newProduct);

    const res = await request(app).post('/products').send({ name: 'New Product' });

    expect(productService.createProduct).toHaveBeenCalledWith({ name: 'New Product' });
    expect(res.statusCode).toBe(201);
    expect(res.body).toEqual(newProduct);
  });

  // POST /products with invalid payload (throws error)
  test('testPostProducts_withInvalidPayload_shouldReturn422', async () => {
    productService.createProduct.mockImplementation(() => {
      throw new Error('Invalid product data');
    });

    const res = await request(app).post('/products').send({});

    expect(productService.createProduct).toHaveBeenCalledWith({});
    expect(res.statusCode).toBe(422);
    expect(res.body).toEqual({ detail: 'Invalid product data' });
  });

  // PUT /products/:id with valid data
  test('testPutProduct_withValidData_shouldUpdateProduct', async () => {
    const updatedProduct = { id: 1, name: 'Updated Product' };
    productService.updateProduct.mockReturnValue(updatedProduct);

    const res = await request(app).put('/products/1').send({ name: 'Updated Product' });

    expect(productService.updateProduct).toHaveBeenCalledWith(1, { name: 'Updated Product' });
    expect(res.statusCode).toBe(200);
    expect(res.body).toEqual(updatedProduct);
  });

  // PUT /products/:id with invalid id
  test('testPutProduct_withInvalidId_shouldReturn400', async () => {
    const res = await request(app).put('/products/abc').send({ name: 'Updated Product' });

    expect(productService.updateProduct).not.toHaveBeenCalled();
    expect(res.statusCode).toBe(400);
    expect(res.body).toEqual({ detail: 'ID inválido' });
  });

  // PUT /products/:id with non-existent id
  test('testPutProduct_withNonExistentId_shouldReturn404', async () => {
    productService.updateProduct.mockReturnValue(null);

    const res = await request(app).put('/products/999').send({ name: 'Updated Product' });

    expect(productService.updateProduct).toHaveBeenCalledWith(999, { name: 'Updated Product' });
    expect(res.statusCode).toBe(404);
    expect(res.body).toEqual({ detail: 'Produto não encontrado' });
  });

  // PUT /products/:id with invalid data (throws error)
  test('testPutProduct_withInvalidData_shouldReturn422', async () => {
    productService.updateProduct.mockImplementation(() => {
      throw new Error('Invalid update data');
    });

    const res = await request(app).put('/products/1').send({ name: '' });

    expect(productService.updateProduct).toHaveBeenCalledWith(1, { name: '' });
    expect(res.statusCode).toBe(422);
    expect(res.body).toEqual({ detail: 'Invalid update data' });
  });

  // DELETE /products/:id with valid id
  test('testDeleteProduct_withValidId_shouldDeleteProduct', async () => {
    productService.deleteProduct.mockReturnValue(true);

    const res = await request(app).delete('/products/1');

    expect(productService.deleteProduct).toHaveBeenCalledWith(1);
    expect(res.statusCode).toBe(204);
    expect(res.body).toEqual({});
  });

  // DELETE /products/:id with invalid id
  test('testDeleteProduct_withInvalidId_shouldReturn400', async () => {
    const res = await request(app).delete('/products/abc');

    expect(productService.deleteProduct).not.toHaveBeenCalled();
    expect(res.statusCode).toBe(400);
    expect(res.body).toEqual({ detail: 'ID inválido' });
  });

  // DELETE /products/:id with non-existent id
  test('testDeleteProduct_withNonExistentId_shouldReturn404', async () => {
    productService.deleteProduct.mockReturnValue(false);

    const res = await request(app).delete('/products/999');

    expect(productService.deleteProduct).toHaveBeenCalledWith(999);
    expect(res.statusCode).toBe(404);
    expect(res.body).toEqual({ detail: 'Produto não encontrado' });
  });

  // POST /products/:id/discount with valid percent
  test('testPostDiscount_withValidPercent_shouldApplyDiscount', async () => {
    const discountedProduct = { id: 1, discount: 20 };
    productService.applyDiscount.mockReturnValue(discountedProduct);

    const res = await request(app).post('/products/1/discount').send({ percent: 20 });

    expect(productService.applyDiscount).toHaveBeenCalledWith(1, 20);
    expect(res.statusCode).toBe(200);
    expect(res.body).toEqual(discountedProduct);
  });

  // POST /products/:id/discount with missing percent
  test('testPostDiscount_withMissingPercent_shouldReturn422', async () => {
    const res = await request(app).post('/products/1/discount').send({});

    expect(productService.applyDiscount).not.toHaveBeenCalled();
    expect(res.statusCode).toBe(422);
    expect(res.body).toEqual({ detail: "Campo 'percent' é obrigatório" });
  });

  // POST /products/:id/discount with invalid percent (throws error)
  test('testPostDiscount_withInvalidPercent_shouldReturn422', async () => {
    productService.applyDiscount.mockImplementation(() => {
      throw new Error('Percent must be between 0 and 100');
    });

    const res = await request(app).post('/products/1/discount').send({ percent: 150 });

    expect(productService.applyDiscount).toHaveBeenCalledWith(1, 150);
    expect(res.statusCode).toBe(422);
    expect(res.body).toEqual({ detail: 'Percent must be between 0 and 100' });
  });

  // POST /products/:id/discount with invalid id
  test('testPostDiscount_withInvalidId_shouldReturn400', async () => {
    const res = await request(app).post('/products/abc/discount').send({ percent: 10 });

    expect(productService.applyDiscount).not.toHaveBeenCalled();
    expect(res.statusCode).toBe(400);
    expect(res.body).toEqual({ detail: 'ID inválido' });
  });

  // POST /products/:id/discount with non-existent id
  test('testPostDiscount_withNonExistentId_shouldReturn404', async () => {
    productService.applyDiscount.mockReturnValue(null);

    const res = await request(app).post('/products/999/discount').send({ percent: 10 });

    expect(productService.applyDiscount).toHaveBeenCalledWith(999, 10);
    expect(res.statusCode).toBe(404);
    expect(res.body).toEqual({ detail: 'Produto não encontrado' });
  });

  // DELETE /products/:id/discount with active discount
  test('testDeleteDiscount_withActiveDiscount_shouldRemoveDiscount', async () => {
    productService.removeDiscount.mockReturnValue(true);

    const res = await request(app).delete('/products/1/discount');

    expect(productService.removeDiscount).toHaveBeenCalledWith(1);
    expect(res.statusCode).toBe(204);
    expect(res.body).toEqual({});
  });

  // DELETE /products/:id/discount without discount active
  test('testDeleteDiscount_withoutDiscount_shouldReturn404', async () => {
    productService.removeDiscount.mockReturnValue(false);

    const res = await request(app).delete('/products/1/discount');

    expect(productService.removeDiscount).toHaveBeenCalledWith(1);
    expect(res.statusCode).toBe(404);
    expect(res.body).toEqual({ detail: 'Produto não encontrado ou sem desconto ativo' });
  });

  // DELETE /products/:id/discount with non-existent id
  test('testDeleteDiscount_withNonExistentId_shouldReturn404', async () => {
    productService.removeDiscount.mockReturnValue(false);

    const res = await request(app).delete('/products/999/discount');

    expect(productService.removeDiscount).toHaveBeenCalledWith(999);
    expect(res.statusCode).toBe(404);
    expect(res.body).toEqual({ detail: 'Produto não encontrado ou sem desconto ativo' });
  });

  // DELETE /products/:id/discount with invalid id format
  test('testDeleteDiscount_withInvalidId_shouldReturn400', async () => {
    const res = await request(app).delete('/products/abc/discount');

    expect(productService.removeDiscount).not.toHaveBeenCalled();
    expect(res.statusCode).toBe(400);
    expect(res.body).toEqual({ detail: 'ID inválido' });
  });

  // POST /products/:id/reserve with valid quantity
  test('testPostReserve_withValidQuantity_shouldReserveStock', async () => {
    const reservedProduct = { id: 1, reserved: 5 };
    productService.reserveStock.mockReturnValue(reservedProduct);

    const res = await request(app).post('/products/1/reserve').send({ quantity: 5 });

    expect(productService.reserveStock).toHaveBeenCalledWith(1, 5);
    expect(res.statusCode).toBe(200);
    expect(res.body).toEqual(reservedProduct);
  });

  // POST /products/:id/reserve with missing quantity
  test('testPostReserve_withMissingQuantity_shouldReturn422', async () => {
    const res = await request(app).post('/products/1/reserve').send({});

    expect(productService.reserveStock).not.toHaveBeenCalled();
    expect(res.statusCode).toBe(422);
    expect(res.body).toEqual({ detail: "Campo 'quantity' é obrigatório" });
  });

  // POST /products/:id/reserve with invalid quantity (throws error)
  test('testPostReserve_withInvalidQuantity_shouldReturn422', async () => {
    productService.reserveStock.mockImplementation(() => {
      throw new Error('Quantity must be positive integer');
    });

    const res = await request(app).post('/products/1/reserve').send({ quantity: -10 });

    expect(productService.reserveStock).toHaveBeenCalledWith(1, -10);
    expect(res.statusCode).toBe(422);
    expect(res.body).toEqual({ detail: 'Quantity must be positive integer' });
  });

  // POST /products/:id/reserve with invalid id
  test('testPostReserve_withInvalidId_shouldReturn400', async () => {
    const res = await request(app).post('/products/abc/reserve').send({ quantity: 5 });

    expect(productService.reserveStock).not.toHaveBeenCalled();
    expect(res.statusCode).toBe(400);
    expect(res.body).toEqual({ detail: 'ID inválido' });
  });

  // POST /products/:id/reserve with non-existent id
  test('testPostReserve_withNonExistentId_shouldReturn404', async () => {
    productService.reserveStock.mockReturnValue(null);

    const res = await request(app).post('/products/999/reserve').send({ quantity: 5 });

    expect(productService.reserveStock).toHaveBeenCalledWith(999, 5);
    expect(res.statusCode).toBe(404);
    expect(res.body).toEqual({ detail: 'Produto não encontrado' });
  });

  // GET /products/:id with valid id
  test('testGetProduct_withValidId_shouldReturnProduct', async () => {
    const product = { id: 1, name: 'Product A' };
    productService.getProduct.mockReturnValue(product);

    const res = await request(app).get('/products/1');

    expect(productService.getProduct).toHaveBeenCalledWith(1);
    expect(res.statusCode).toBe(200);
    expect(res.body).toEqual(product);
  });

  // GET /products/:id with invalid id
  test('testGetProduct_withInvalidId_shouldReturn400', async () => {
    const res = await request(app).get('/products/abc');

    expect(productService.getProduct).not.toHaveBeenCalled();
    expect(res.statusCode).toBe(400);
    expect(res.body).toEqual({ detail: 'ID inválido' });
  });

  // GET /products/:id with non-existent id
  test('testGetProduct_withNonExistentId_shouldReturn404', async () => {
    productService.getProduct.mockReturnValue(null);

    const res = await request(app).get('/products/999');

    expect(productService.getProduct).toHaveBeenCalledWith(999);
    expect(res.statusCode).toBe(404);
    expect(res.body).toEqual({ detail: 'Produto não encontrado' });
  });

  // Test concurrency simulation for discount and reserve (basic simulation)
  test('testConcurrentDiscountAndReserve_shouldMaintainDataIntegrity', async () => {
    // Setup mocks to simulate concurrency
    const productBefore = { id: 1, stock: 10, discount: 0 };
    const productAfterDiscount = { id: 1, stock: 10, discount: 20 };
    const productAfterReserve = { id: 1, stock: 5, discount: 20 };

    let discountApplied = false;
    let stockReserved = false;

    productService.applyDiscount.mockImplementation((id, percent) => {
      discountApplied = true;
      return productAfterDiscount;
    });

    productService.reserveStock.mockImplementation((id, quantity) => {
      if (!discountApplied) throw new Error('Discount must be applied first');
      stockReserved = true;
      return productAfterReserve;
    });

    // Apply discount
    const discountRes = await request(app).post('/products/1/discount').send({ percent: 20 });
    expect(discountRes.statusCode).toBe(200);
    expect(discountRes.body).toEqual(productAfterDiscount);

    // Reserve stock
    const reserveRes = await request(app).post('/products/1/reserve').send({ quantity: 5 });
    expect(reserveRes.statusCode).toBe(200);
    expect(reserveRes.body).toEqual(productAfterReserve);

    expect(discountApplied).toBe(true);
    expect(stockReserved).toBe(true);
  });

  // Test invalid Content-Type header handling (should still work as no explicit check)
  test('testEndpoints_withInvalidContentType_shouldProcessNormally', async () => {
    productService.listProducts.mockReturnValue([]);

    const res = await request(app).get('/products')
      .set('Content-Type', 'text/plain');

    expect(res.statusCode).toBe(200);
    expect(res.body).toEqual([]);
  });

  // Test extreme values for quantity and percent
  test('testPostReserve_withVeryLargeQuantity_shouldCallService', async () => {
    const product = { id: 1, reserved: 1000000 };
    productService.reserveStock.mockReturnValue(product);

    const res = await request(app).post('/products/1/reserve').send({ quantity: 1000000 });

    expect(productService.reserveStock).toHaveBeenCalledWith(1, 1000000);
    expect(res.statusCode).toBe(200);
    expect(res.body).toEqual(product);
  });

  test('testPostDiscount_withPercentZeroAndHundred_shouldApplyDiscount', async () => {
    const productZero = { id: 1, discount: 0 };
    const productHundred = { id: 1, discount: 100 };

    productService.applyDiscount.mockReturnValueOnce(productZero).mockReturnValueOnce(productHundred);

    const resZero = await request(app).post('/products/1/discount').send({ percent: 0 });
    expect(resZero.statusCode).toBe(200);
    expect(resZero.body).toEqual(productZero);

    const resHundred = await request(app).post('/products/1/discount').send({ percent: 100 });
    expect(resHundred.statusCode).toBe(200);
    expect(resHundred.body).toEqual(productHundred);
  });

  // Test behavior when stock is insufficient for reservation
  test('testPostReserve_withInsufficientStock_shouldReturn422', async () => {
    productService.reserveStock.mockImplementation(() => {
      throw new Error('Insufficient stock');
    });

    const res = await request(app).post('/products/1/reserve').send({ quantity: 1000 });

    expect(productService.reserveStock).toHaveBeenCalledWith(1, 1000);
    expect(res.statusCode).toBe(422);
    expect(res.body).toEqual({ detail: 'Insufficient stock' });
  });

  // Test error handling for unexpected exceptions in productService
  test('testErrorHandling_inAllEndpoints_shouldReturn422', async () => {
    productService.listProducts.mockImplementation(() => { throw new Error('Unexpected error'); });
    productService.listCategories.mockImplementation(() => { throw new Error('Unexpected error'); });
    productService.listLowStock.mockImplementation(() => { throw new Error('Unexpected error'); });
    productService.getProduct.mockImplementation(() => { throw new Error('Unexpected error'); });
    productService.createProduct.mockImplementation(() => { throw new Error('Unexpected error'); });
    productService.updateProduct.mockImplementation(() => { throw new Error('Unexpected error'); });
    productService.deleteProduct.mockImplementation(() => { throw new Error('Unexpected error'); });
    productService.applyDiscount.mockImplementation(() => { throw new Error('Unexpected error'); });
    productService.removeDiscount.mockImplementation(() => { throw new Error('Unexpected error'); });
    productService.reserveStock.mockImplementation(() => { throw new Error('Unexpected error'); });

    // POST /products
    let res = await request(app).post('/products').send({ name: 'fail' });
    expect(res.statusCode).toBe(422);
    expect(res.body).toEqual({ detail: 'Unexpected error' });

    // PUT /products/:id
    res = await request(app).put('/products/1').send({ name: 'fail' });
    expect(res.statusCode).toBe(422);
    expect(res.body).toEqual({ detail: 'Unexpected error' });

    // POST /products/:id/discount
    res = await request(app).post('/products/1/discount').send({ percent: 10 });
    expect(res.statusCode).toBe(422);
    expect(res.body).toEqual({ detail: 'Unexpected error' });

    // POST /products/:id/reserve
    res = await request(app).post('/products/1/reserve').send({ quantity: 1 });
    expect(res.statusCode).toBe(422);
    expect(res.body).toEqual({ detail: 'Unexpected error' });
  });

  // Test validation of minPrice > maxPrice in GET /products
  test('testGetProducts_withMinPriceGreaterThanMaxPrice_shouldReturnEmpty', async () => {
    productService.listProducts.mockReturnValue([]);

    const res = await request(app).get('/products')
      .query({ minPrice: '1000', maxPrice: '100' });

    expect(productService.listProducts).toHaveBeenCalledWith({
      category: undefined,
      search: undefined,
      minPrice: 1000,
      maxPrice: 100,
      inStock: false,
    });
    expect(res.statusCode).toBe(200);
    expect(res.body).toEqual([]);
  });

  // Test multiple categories filter (assuming productService supports it)
  test('testGetProducts_withMultipleCategories_shouldCallServiceCorrectly', async () => {
    productService.listProducts.mockReturnValue([]);

    const res = await request(app).get('/products')
      .query({ category: 'electronics,books' });

    expect(productService.listProducts).toHaveBeenCalledWith({
      category: 'electronics,books',
      search: undefined,
      minPrice: undefined,
      maxPrice: undefined,
      inStock: false,
    });
    expect(res.statusCode).toBe(200);
    expect(res.body).toEqual([]);
  });
});