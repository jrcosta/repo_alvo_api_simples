class ProductService {
  constructor() {
    this.products = [
      { id: 1, name: "Teclado Mecânico", price: 299.90, stock: 10 },
      { id: 2, name: "Mouse Sem Fio", price: 149.90, stock: 25 },
      { id: 3, name: "Monitor 24\"", price: 1199.00, stock: 5 },
    ];
    this.nextId = 4;
  }

  listProducts() {
    return this.products;
  }

  getProduct(id) {
    return this.products.find(p => p.id === id);
  }

  createProduct(payload) {
    const newProduct = {
      id: this.nextId++,
      name: payload.name,
      price: payload.price,
      stock: payload.stock ?? 0,
    };
    this.products.push(newProduct);
    return newProduct;
  }

  updateProduct(id, payload) {
    const index = this.products.findIndex(p => p.id === id);
    if (index === -1) return null;
    if (payload.name !== undefined) this.products[index].name = payload.name;
    if (payload.price !== undefined) this.products[index].price = payload.price;
    if (payload.stock !== undefined) this.products[index].stock = payload.stock;
    return this.products[index];
  }

  deleteProduct(id) {
    const index = this.products.findIndex(p => p.id === id);
    if (index === -1) return false;
    this.products.splice(index, 1);
    return true;
  }
}

// Singleton instance
module.exports = new ProductService();
