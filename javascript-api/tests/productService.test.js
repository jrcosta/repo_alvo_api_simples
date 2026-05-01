const productService = require('../src/services/productService');

describe('ProductService Unit Tests', () => {
  beforeEach(() => {
    // Reset productService state before each test
    productService.products = [
      { id: 1, name: "Teclado Mecânico", price: 299.90, stock: 10, category: "perifericos" },
      { id: 2, name: "Mouse Sem Fio", price: 149.90, stock: 25, category: "perifericos" },
      { id: 3, name: "Monitor 24\"", price: 1199.00, stock: 5, category: "monitores" },
      { id: 4, name: "Headset USB", price: 199.90, stock: 15, category: "audio" },
      { id: 5, name: "Webcam HD", price: 249.90, stock: 8, category: "perifericos" },
    ];
    productService.nextId = 6;
    productService.discounts = {};
  });

  describe('listProducts', () => {
    test('should return products filtered by combined filters: category (case insensitive), search (case insensitive), price range and inStock', () => {
      // Add discount to test effectivePrice filtering
      productService.applyDiscount(1, 10); // 299.90 * 0.9 = 269.91
      productService.applyDiscount(2, 50); // 149.90 * 0.5 = 74.95

      const filters = {
        category: 'Perifericos', // mixed case
        search: 'mouse',         // lowercase search
        minPrice: 70,
        maxPrice: 150,
        inStock: true,
      };
      const results = productService.listProducts(filters);

      expect(results).toHaveLength(1);
      expect(results[0].id).toBe(2);
      expect(results[0].category).toBe('perifericos');
      expect(results[0].name.toLowerCase()).toContain('mouse');
      expect(results[0].effectivePrice).toBeCloseTo(74.95, 2);
      expect(results[0].stock).toBeGreaterThan(0);
    });

    test('should return all products if no filters applied', () => {
      const results = productService.listProducts();
      expect(results.length).toBe(productService.products.length);
    });
  });

  describe('createProduct', () => {
    test('should create product with category normalized to lowercase', () => {
      const payload = {
        name: 'Teclado Gamer',
        price: 399.99,
        stock: 20,
        category: 'Perifericos',
      };
      const created = productService.createProduct(payload);
      expect(created.id).toBe(productService.nextId - 1);
      expect(created.name).toBe(payload.name);
      expect(created.price).toBe(payload.price);
      expect(created.stock).toBe(payload.stock);
      expect(created.category).toBe('perifericos');
    });

    test('should create product with default category "geral" if category not provided', () => {
      const payload = {
        name: 'Cabo USB',
        price: 19.99,
      };
      const created = productService.createProduct(payload);
      expect(created.category).toBe('geral');
      expect(created.stock).toBe(0);
    });

    test('should throw error if name is missing', () => {
      expect(() => productService.createProduct({ price: 10 })).toThrow("name e price são obrigatórios");
    });

    test('should throw error if price is missing', () => {
      expect(() => productService.createProduct({ name: 'Produto' })).toThrow("name e price são obrigatórios");
    });

    test('should throw error if price is negative', () => {
      expect(() => productService.createProduct({ name: 'Produto', price: -1 })).toThrow("price deve ser número não negativo");
    });

    test('should throw error if price is not a number', () => {
      expect(() => productService.createProduct({ name: 'Produto', price: 'abc' })).toThrow("price deve ser número não negativo");
    });
  });

  describe('updateProduct', () => {
    test('should update product category and price with valid values', () => {
      const updated = productService.updateProduct(1, { category: 'Audio', price: 350 });
      expect(updated).not.toBeNull();
      expect(updated.category).toBe('audio');
      expect(updated.price).toBe(350);
    });

    test('should throw error when updating product with invalid price (negative)', () => {
      expect(() => productService.updateProduct(1, { price: -10 })).toThrow("price deve ser número não negativo");
    });

    test('should throw error when updating product with invalid price (non-number)', () => {
      expect(() => productService.updateProduct(1, { price: 'abc' })).toThrow("price deve ser número não negativo");
    });

    test('should return null when updating non-existent product', () => {
      const result = productService.updateProduct(999, { price: 100 });
      expect(result).toBeNull();
    });

    test('should update product name and stock', () => {
      const updated = productService.updateProduct(2, { name: 'Mouse Gamer', stock: 30 });
      expect(updated.name).toBe('Mouse Gamer');
      expect(updated.stock).toBe(30);
    });

    test('should update product category to lowercase', () => {
      const updated = productService.updateProduct(3, { category: 'MONITORES' });
      expect(updated.category).toBe('monitores');
    });
  });

  describe('applyDiscount and removeDiscount', () => {
    test('should apply valid discount and calculate effective price correctly', () => {
      const productBefore = productService.getProduct(1);
      expect(productBefore.discount).toBe(0);

      const updated = productService.applyDiscount(1, 20);
      expect(updated.discount).toBe(20);
      expect(updated.effectivePrice).toBeCloseTo(299.90 * 0.8, 2);
    });

    test('should throw error when applying discount less than 0', () => {
      expect(() => productService.applyDiscount(1, -5)).toThrow("Desconto deve estar entre 0 e 100");
    });

    test('should throw error when applying discount greater than 100', () => {
      expect(() => productService.applyDiscount(1, 150)).toThrow("Desconto deve estar entre 0 e 100");
    });

    test('should return null when applying discount to non-existent product', () => {
      const result = productService.applyDiscount(999, 10);
      expect(result).toBeNull();
    });

    test('should remove existing discount and return true', () => {
      productService.applyDiscount(1, 30);
      const removed = productService.removeDiscount(1);
      expect(removed).toBe(true);
      const product = productService.getProduct(1);
      expect(product.discount).toBe(0);
    });

    test('should return false when removing discount that does not exist', () => {
      const removed = productService.removeDiscount(2);
      expect(removed).toBe(false);
    });
  });

  describe('reserveStock', () => {
    test('should reserve stock with valid quantity and reduce stock accordingly', () => {
      const productBefore = productService.getProduct(1);
      const reserved = productService.reserveStock(1, 5);
      expect(reserved.stock).toBe(productBefore.stock - 5);
    });

    test('should throw error when reserving zero quantity', () => {
      expect(() => productService.reserveStock(1, 0)).toThrow("Quantidade deve ser positiva");
    });

    test('should throw error when reserving negative quantity', () => {
      expect(() => productService.reserveStock(1, -3)).toThrow("Quantidade deve ser positiva");
    });

    test('should throw error when reserving quantity greater than stock', () => {
      const product = productService.getProduct(1);
      expect(() => productService.reserveStock(1, product.stock + 1)).toThrow(/Estoque insuficiente/);
    });

    test('should return null when reserving stock for non-existent product', () => {
      const result = productService.reserveStock(999, 1);
      expect(result).toBeNull();
    });
  });

  describe('deleteProduct', () => {
    test('should delete product and remove associated discounts', () => {
      productService.applyDiscount(1, 10);
      const deleted = productService.deleteProduct(1);
      expect(deleted).toBe(true);
      expect(productService.getProduct(1)).toBeNull();
      expect(productService.discounts[1]).toBeUndefined();
    });

    test('should return false when deleting non-existent product', () => {
      const deleted = productService.deleteProduct(999);
      expect(deleted).toBe(false);
    });
  });

  describe('listCategories', () => {
    test('should return unique sorted categories', () => {
      const categories = productService.listCategories();
      expect(Array.isArray(categories)).toBe(true);
      expect(categories).toEqual([...new Set(productService.products.map(p => p.category))].sort());
    });

    test('should not contain duplicates and be sorted alphabetically', () => {
      productService.products.push({ id: 6, name: 'Produto X', price: 10, stock: 1, category: 'Audio' });
      const categories = productService.listCategories();
      expect(categories).toEqual(['audio', 'monitores', 'perifericos']);
    });
  });

  describe('listProducts filters regression', () => {
    test('should filter by category case insensitive', () => {
      const results = productService.listProducts({ category: 'PERIFERICOS' });
      expect(results.every(p => p.category === 'perifericos')).toBe(true);
    });

    test('should filter by search case insensitive', () => {
      const results = productService.listProducts({ search: 'monitor' });
      expect(results.length).toBe(1);
      expect(results[0].name.toLowerCase()).toContain('monitor');
    });

    test('should filter by minPrice and maxPrice using effectivePrice', () => {
      productService.applyDiscount(3, 50); // 1199 * 0.5 = 599.5
      const results = productService.listProducts({ minPrice: 500, maxPrice: 600 });
      expect(results.some(p => p.id === 3)).toBe(true);
    });

    test('should filter by inStock true', () => {
      productService.products[0].stock = 0;
      const results = productService.listProducts({ inStock: true });
      expect(results.every(p => p.stock > 0)).toBe(true);
      expect(results.some(p => p.id === 1)).toBe(false);
    });
  });

  describe('discounts cumulative and partial removal', () => {
    test('should apply multiple discounts cumulatively and calculate correct effective price', () => {
      // Apply 20% discount to product 1
      productService.applyDiscount(1, 20);
      // Apply 10% discount to product 2
      productService.applyDiscount(2, 10);

      const p1 = productService.getProduct(1);
      const p2 = productService.getProduct(2);

      expect(p1.discount).toBe(20);
      expect(p1.effectivePrice).toBeCloseTo(299.90 * 0.8, 2);

      expect(p2.discount).toBe(10);
      expect(p2.effectivePrice).toBeCloseTo(149.90 * 0.9, 2);
    });

    test('should remove discount partially and update price and internal state correctly', () => {
      productService.applyDiscount(1, 30);
      productService.applyDiscount(2, 15);

      let p1 = productService.getProduct(1);
      let p2 = productService.getProduct(2);
      expect(p1.discount).toBe(30);
      expect(p2.discount).toBe(15);

      const removed = productService.removeDiscount(1);
      expect(removed).toBe(true);

      p1 = productService.getProduct(1);
      p2 = productService.getProduct(2);

      expect(p1.discount).toBe(0);
      expect(p2.discount).toBe(15);
    });
  });

  describe('create and update product with invalid or nonexistent categories', () => {
    test('should create product with invalid category and normalize to lowercase', () => {
      const payload = { name: 'Produto X', price: 100, category: 'CategoriaInvalida' };
      const created = productService.createProduct(payload);
      expect(created.category).toBe('categoriainvalida');
    });

    test('should update product with invalid category and normalize to lowercase', () => {
      const updated = productService.updateProduct(1, { category: 'CategoriaInvalida' });
      expect(updated.category).toBe('categoriainvalida');
    });
  });

  describe('reserveStock concurrency simulation', () => {
    test('should handle multiple sequential reservations correctly without stock inconsistency', () => {
      const initialStock = productService.getProduct(1).stock;

      // Reserve 3 units
      const res1 = productService.reserveStock(1, 3);
      expect(res1.stock).toBe(initialStock - 3);

      // Reserve 2 units
      const res2 = productService.reserveStock(1, 2);
      expect(res2.stock).toBe(initialStock - 5);

      // Reserve remaining stock
      const remaining = productService.getProduct(1).stock;
      const res3 = productService.reserveStock(1, remaining);
      expect(res3.stock).toBe(0);

      // Attempt to reserve more should throw
      expect(() => productService.reserveStock(1, 1)).toThrow(/Estoque insuficiente/);
    });

    test('should throw error and not partially decrement stock on invalid reservation', () => {
      const initialStock = productService.getProduct(2).stock;
      try {
        productService.reserveStock(2, 5);
        productService.reserveStock(2, -1);
      } catch (e) {
        // ignore error
      }
      const afterStock = productService.getProduct(2).stock;
      expect(afterStock).toBe(initialStock - 5);
    });
  });

  describe('exclusion of product does not leave orphan discounts', () => {
    test('should not leave discounts in memory after product deletion', () => {
      productService.applyDiscount(3, 25);
      expect(productService.discounts[3]).toBe(25);
      const deleted = productService.deleteProduct(3);
      expect(deleted).toBe(true);
      expect(productService.discounts[3]).toBeUndefined();
    });
  });
});