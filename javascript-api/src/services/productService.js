class ProductService {
  constructor() {
    this.products = [
      { id: 1, name: "Teclado Mecânico", price: 299.90, stock: 10, category: "perifericos" },
      { id: 2, name: "Mouse Sem Fio", price: 149.90, stock: 25, category: "perifericos" },
      { id: 3, name: "Monitor 24\"", price: 1199.00, stock: 5, category: "monitores" },
      { id: 4, name: "Headset USB", price: 199.90, stock: 15, category: "audio" },
      { id: 5, name: "Webcam HD", price: 249.90, stock: 8, category: "perifericos" },
    ];
    this.nextId = 6;
    this.discounts = {};
  }

  listProducts({ category, search, minPrice, maxPrice, inStock } = {}) {
    let results = [...this.products];

    if (category) {
      results = results.filter(p => p.category === category.toLowerCase());
    }
    if (search) {
      const term = search.toLowerCase();
      results = results.filter(p => p.name.toLowerCase().includes(term));
    }
    if (minPrice !== undefined) {
      results = results.filter(p => this._effectivePrice(p) >= minPrice);
    }
    if (maxPrice !== undefined) {
      results = results.filter(p => this._effectivePrice(p) <= maxPrice);
    }
    if (inStock) {
      results = results.filter(p => p.stock > 0);
    }

    return results.map(p => this._serialize(p));
  }

  getProduct(id) {
    const product = this.products.find(p => p.id === id);
    return product ? this._serialize(product) : null;
  }

  createProduct(payload) {
    if (!payload.name || payload.price === undefined) {
      throw new Error("name e price são obrigatórios");
    }
    if (typeof payload.price !== "number" || payload.price < 0) {
      throw new Error("price deve ser número não negativo");
    }
    const newProduct = {
      id: this.nextId++,
      name: payload.name,
      price: payload.price,
      stock: payload.stock ?? 0,
      category: payload.category ? payload.category.toLowerCase() : "geral",
    };
    this.products.push(newProduct);
    return this._serialize(newProduct);
  }

  updateProduct(id, payload) {
    const product = this.products.find(p => p.id === id);
    if (!product) return null;
    if (payload.name !== undefined) product.name = payload.name;
    if (payload.price !== undefined) {
      if (typeof payload.price !== "number" || payload.price < 0) {
        throw new Error("price deve ser número não negativo");
      }
      product.price = payload.price;
    }
    if (payload.stock !== undefined) product.stock = payload.stock;
    if (payload.category !== undefined) product.category = payload.category.toLowerCase();
    return this._serialize(product);
  }

  deleteProduct(id) {
    const index = this.products.findIndex(p => p.id === id);
    if (index === -1) return false;
    this.products.splice(index, 1);
    delete this.discounts[id];
    return true;
  }

  applyDiscount(id, percent) {
    if (percent < 0 || percent > 100) {
      throw new Error("Desconto deve estar entre 0 e 100");
    }
    const product = this.products.find(p => p.id === id);
    if (!product) return null;
    this.discounts[id] = percent;
    return this._serialize(product);
  }

  removeDiscount(id) {
    if (!this.discounts[id]) return false;
    delete this.discounts[id];
    return true;
  }

  reserveStock(id, quantity) {
    if (quantity <= 0) throw new Error("Quantidade deve ser positiva");
    const product = this.products.find(p => p.id === id);
    if (!product) return null;
    if (product.stock < quantity) {
      throw new Error(`Estoque insuficiente: disponível ${product.stock}, solicitado ${quantity}`);
    }
    product.stock -= quantity;
    return this._serialize(product);
  }

  listLowStock(threshold = 5) {
    return this.products
      .filter(p => p.stock <= threshold)
      .map(p => this._serialize(p));
  }

  listCategories() {
    return [...new Set(this.products.map(p => p.category.toLowerCase()))].sort();
  }

  _effectivePrice(product) {
    const discount = this.discounts[product.id] || 0;
    return discount > 0
      ? parseFloat((product.price * (1 - discount / 100)).toFixed(2))
      : product.price;
  }

  _serialize(product) {
    const effectivePrice = this._effectivePrice(product);
    const discount = this.discounts[product.id] || 0;
    return {
      id: product.id,
      name: product.name,
      price: product.price,
      effectivePrice,
      discount,
      stock: product.stock,
      category: product.category,
    };
  }
}

module.exports = new ProductService();
