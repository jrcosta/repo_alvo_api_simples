const express = require('express');
const router = express.Router();
const productService = require('../services/productService');

// GET /products — lista produtos com filtros opcionais
// Query: category, search, minPrice, maxPrice, inStock
router.get('/', (req, res) => {
  const { category, search, inStock } = req.query;
  const minPrice = req.query.minPrice !== undefined ? parseFloat(req.query.minPrice) : undefined;
  const maxPrice = req.query.maxPrice !== undefined ? parseFloat(req.query.maxPrice) : undefined;
  res.json(productService.listProducts({ category, search, minPrice, maxPrice, inStock: inStock === 'true' }));
});

// GET /products/categories — lista categorias únicas
router.get('/categories', (req, res) => {
  res.json(productService.listCategories());
});

// GET /products/low-stock — produtos com estoque baixo
// Query: threshold (default 5)
router.get('/low-stock', (req, res) => {
  const threshold = req.query.threshold !== undefined ? parseInt(req.query.threshold, 10) : 5;
  res.json(productService.listLowStock(threshold));
});

// GET /products/:id — busca produto por ID
router.get('/:id', (req, res) => {
  const id = parseInt(req.params.id, 10);
  if (isNaN(id)) {
    return res.status(400).json({ detail: "ID inválido" });
  }
  const product = productService.getProduct(id);
  if (!product) {
    return res.status(404).json({ detail: "Produto não encontrado" });
  }
  res.json(product);
});

// POST /products — cria um novo produto
router.post('/', (req, res) => {
  try {
    const newProduct = productService.createProduct(req.body);
    res.status(201).json(newProduct);
  } catch (err) {
    res.status(422).json({ detail: err.message });
  }
});

// PUT /products/:id — atualiza um produto existente
router.put('/:id', (req, res) => {
  const id = parseInt(req.params.id, 10);
  if (isNaN(id)) {
    return res.status(400).json({ detail: "ID inválido" });
  }
  try {
    const updated = productService.updateProduct(id, req.body);
    if (!updated) {
      return res.status(404).json({ detail: "Produto não encontrado" });
    }
    res.json(updated);
  } catch (err) {
    res.status(422).json({ detail: err.message });
  }
});

// DELETE /products/:id — remove um produto
router.delete('/:id', (req, res) => {
  const id = parseInt(req.params.id, 10);
  if (isNaN(id)) {
    return res.status(400).json({ detail: "ID inválido" });
  }
  const deleted = productService.deleteProduct(id);
  if (!deleted) {
    return res.status(404).json({ detail: "Produto não encontrado" });
  }
  res.status(204).send();
});

// POST /products/:id/discount — aplica desconto percentual
router.post('/:id/discount', (req, res) => {
  const id = parseInt(req.params.id, 10);
  if (isNaN(id)) {
    return res.status(400).json({ detail: "ID inválido" });
  }
  const { percent } = req.body;
  if (percent === undefined) {
    return res.status(422).json({ detail: "Campo 'percent' é obrigatório" });
  }
  try {
    const product = productService.applyDiscount(id, percent);
    if (!product) {
      return res.status(404).json({ detail: "Produto não encontrado" });
    }
    res.json(product);
  } catch (err) {
    res.status(422).json({ detail: err.message });
  }
});

// DELETE /products/:id/discount — remove desconto
router.delete('/:id/discount', (req, res) => {
  const id = parseInt(req.params.id, 10);
  if (isNaN(id)) {
    return res.status(400).json({ detail: "ID inválido" });
  }
  const removed = productService.removeDiscount(id);
  if (!removed) {
    return res.status(404).json({ detail: "Produto não encontrado ou sem desconto ativo" });
  }
  res.status(204).send();
});

// POST /products/:id/reserve — reserva unidades do estoque
router.post('/:id/reserve', (req, res) => {
  const id = parseInt(req.params.id, 10);
  if (isNaN(id)) {
    return res.status(400).json({ detail: "ID inválido" });
  }
  const { quantity } = req.body;
  if (!quantity) {
    return res.status(422).json({ detail: "Campo 'quantity' é obrigatório" });
  }
  try {
    const product = productService.reserveStock(id, quantity);
    if (!product) {
      return res.status(404).json({ detail: "Produto não encontrado" });
    }
    res.json(product);
  } catch (err) {
    res.status(422).json({ detail: err.message });
  }
});

module.exports = router;
