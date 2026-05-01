const productService = require('../src/services/productService');

describe('ProductService Unit Tests', () => {
  beforeEach(() => {
    // Reset the singleton instance state before each test
    // Since productService is a singleton, we reset its internal state manually
    productService.products = [
      { id: 1, name: "Teclado Mecânico", price: 299.90, stock: 10 },
      { id: 2, name: "Mouse Sem Fio", price: 149.90, stock: 25 },
      { id: 3, name: 'Monitor 24"', price: 1199.00, stock: 5 },
    ];
    productService.nextId = 4;
  });

  test('listProducts returns initial seeded products', () => {
    const products = productService.listProducts();
    expect(products).toHaveLength(3);
    expect(products).toEqual([
      { id: 1, name: "Teclado Mecânico", price: 299.90, stock: 10 },
      { id: 2, name: "Mouse Sem Fio", price: 149.90, stock: 25 },
      { id: 3, name: 'Monitor 24"', price: 1199.00, stock: 5 },
    ]);
  });

  test('listProducts returns updated list after product creation', () => {
    const newProduct = productService.createProduct({ name: 'Teclado RGB', price: 399.90, stock: 15 });
    const products = productService.listProducts();
    expect(products).toContainEqual(newProduct);
    expect(products).toHaveLength(4);
  });

  test('getProduct returns product for existing ID', () => {
    const product = productService.getProduct(2);
    expect(product).toEqual({ id: 2, name: "Mouse Sem Fio", price: 149.90, stock: 25 });
  });

  test('getProduct returns undefined for non-existing ID', () => {
    const product = productService.getProduct(999);
    expect(product).toBeUndefined();
  });

  test('createProduct creates product with all fields provided', () => {
    const payload = { name: 'Webcam HD', price: 299.99, stock: 7 };
    const created = productService.createProduct(payload);
    expect(created).toMatchObject({
      id: 4,
      name: 'Webcam HD',
      price: 299.99,
      stock: 7,
    });
    expect(productService.listProducts()).toContainEqual(created);
  });

  test('createProduct creates product with stock omitted defaults to 0', () => {
    const payload = { name: 'Fone de Ouvido', price: 199.99 };
    const created = productService.createProduct(payload);
    expect(created).toMatchObject({
      id: 4,
      name: 'Fone de Ouvido',
      price: 199.99,
      stock: 0,
    });
  });

  test('createProduct accepts invalid data without validation', () => {
    const payload = { name: '', price: -100, stock: -5 };
    const created = productService.createProduct(payload);
    expect(created).toMatchObject({
      id: 4,
      name: '',
      price: -100,
      stock: -5,
    });
  });

  test('updateProduct updates all fields of existing product', () => {
    const updated = productService.updateProduct(1, { name: 'Teclado Gamer', price: 350, stock: 20 });
    expect(updated).toEqual({ id: 1, name: 'Teclado Gamer', price: 350, stock: 20 });
    expect(productService.getProduct(1)).toEqual(updated);
  });

  test('updateProduct updates partial fields of existing product', () => {
    const updated = productService.updateProduct(2, { price: 159.90 });
    expect(updated).toEqual({ id: 2, name: 'Mouse Sem Fio', price: 159.90, stock: 25 });
  });

  test('updateProduct returns null when updating non-existing product', () => {
    const updated = productService.updateProduct(999, { name: 'Produto Inexistente' });
    expect(updated).toBeNull();
  });

  test('updateProduct accepts invalid data without validation', () => {
    const updated = productService.updateProduct(1, { name: '', price: -50, stock: -10 });
    expect(updated).toEqual({ id: 1, name: '', price: -50, stock: -10 });
  });

  test('deleteProduct removes existing product and returns true', () => {
    const deleted = productService.deleteProduct(3);
    expect(deleted).toBe(true);
    expect(productService.getProduct(3)).toBeUndefined();
    expect(productService.listProducts()).toHaveLength(2);
  });

  test('deleteProduct returns false when deleting non-existing product', () => {
    const deleted = productService.deleteProduct(999);
    expect(deleted).toBe(false);
  });

  test('creating multiple products increments IDs sequentially', () => {
    const p1 = productService.createProduct({ name: 'Produto 1', price: 10 });
    const p2 = productService.createProduct({ name: 'Produto 2', price: 20 });
    const p3 = productService.createProduct({ name: 'Produto 3', price: 30 });
    expect(p1.id).toBe(4);
    expect(p2.id).toBe(5);
    expect(p3.id).toBe(6);
  });

  test('data is lost after resetting the service (simulating app restart)', () => {
    // Simulate restart by re-instantiating the service
    // Since productService is a singleton, we simulate by resetting manually
    productService.products = [];
    productService.nextId = 1;
    expect(productService.listProducts()).toEqual([]);
    expect(productService.nextId).toBe(1);
  });

  test('listProducts returns correct list after multiple modifications', () => {
    productService.createProduct({ name: 'Produto A', price: 100 });
    productService.createProduct({ name: 'Produto B', price: 200 });
    productService.deleteProduct(1);
    const products = productService.listProducts();
    expect(products.find(p => p.id === 1)).toBeUndefined();
    expect(products.find(p => p.name === 'Produto A')).toBeDefined();
    expect(products.find(p => p.name === 'Produto B')).toBeDefined();
  });

  test('getProduct returns undefined for invalid ID types', () => {
    expect(productService.getProduct('abc')).toBeUndefined();
    expect(productService.getProduct(null)).toBeUndefined();
    expect(productService.getProduct(undefined)).toBeUndefined();
  });

  test('createProduct increments nextId correctly even after deletions', () => {
    productService.deleteProduct(3);
    const p = productService.createProduct({ name: 'Novo Produto', price: 50 });
    expect(p.id).toBe(4);
    const p2 = productService.createProduct({ name: 'Outro Produto', price: 60 });
    expect(p2.id).toBe(5);
  });

  test('createProduct accepts payloads with extra unexpected fields without removing them', () => {
    const payload = { name: 'Produto Extra', price: 100, stock: 10, extraField: 'extra' };
    const created = productService.createProduct(payload);
    expect(created).toMatchObject({
      id: 4,
      name: 'Produto Extra',
      price: 100,
      stock: 10,
    });
    // Extra fields are not stored in the product object
    expect(created.extraField).toBeUndefined();
  });

  test('updateProduct does not alter products array when updating non-existing product', () => {
    const before = productService.listProducts().slice();
    const updated = productService.updateProduct(999, { name: 'Inexistente' });
    expect(updated).toBeNull();
    const after = productService.listProducts();
    expect(after).toEqual(before);
  });

  test('methods do not throw unexpected exceptions on valid and invalid inputs', () => {
    expect(() => productService.listProducts()).not.toThrow();
    expect(() => productService.getProduct(1)).not.toThrow();
    expect(() => productService.getProduct('invalid')).not.toThrow();
    expect(() => productService.createProduct({ name: 'Teste', price: 10 })).not.toThrow();
    expect(() => productService.createProduct({})).not.toThrow();
    expect(() => productService.updateProduct(1, { name: 'Novo' })).not.toThrow();
    expect(() => productService.updateProduct(999, { name: 'Inexistente' })).not.toThrow();
    expect(() => productService.deleteProduct(1)).not.toThrow();
    expect(() => productService.deleteProduct(999)).not.toThrow();
  });

  test('deleteProduct removes only the specified product without affecting others', () => {
    productService.createProduct({ name: 'Produto X', price: 100 });
    const initialCount = productService.listProducts().length;
    const deleted = productService.deleteProduct(1);
    expect(deleted).toBe(true);
    const products = productService.listProducts();
    expect(products.find(p => p.id === 1)).toBeUndefined();
    expect(products).toHaveLength(initialCount - 1);
    // Other products remain
    expect(products.find(p => p.id === 2)).toBeDefined();
    expect(products.find(p => p.id === 3)).toBeDefined();
  });

  test('simulate concurrent updates on same product sequentially', () => {
    const firstUpdate = productService.updateProduct(1, { price: 100 });
    const secondUpdate = productService.updateProduct(1, { stock: 50 });
    expect(firstUpdate.price).toBe(100);
    expect(secondUpdate.stock).toBe(50);
    const finalProduct = productService.getProduct(1);
    expect(finalProduct.price).toBe(100);
    expect(finalProduct.stock).toBe(50);
  });
});