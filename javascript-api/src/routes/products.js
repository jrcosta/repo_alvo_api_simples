const express = require('express');
const router = express.Router();
const productService = require('../services/productService');

// GET /products — lista todos os produtos
router.get('/', (req, res) => {
  res.json(productService.listProducts());
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
  const { name, price, stock } = req.body;
  if (!name || price === undefined) {
    return res.status(422).json({ detail: "Nome e preço são obrigatórios" });
  }
  if (typeof price !== 'number' || price < 0) {
    return res.status(422).json({ detail: "Preço deve ser um número não negativo" });
  }
  const newProduct = productService.createProduct({ name, price, stock });
  res.status(201).json(newProduct);
});

// PUT /products/:id — atualiza um produto existente
router.put('/:id', (req, res) => {
  const id = parseInt(req.params.id, 10);
  if (isNaN(id)) {
    return res.status(400).json({ detail: "ID inválido" });
  }
  const updated = productService.updateProduct(id, req.body);
  if (!updated) {
    return res.status(404).json({ detail: "Produto não encontrado" });
  }
  res.json(updated);
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

module.exports = router;
