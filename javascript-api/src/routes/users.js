const express = require('express');
const router = express.Router();
const userService = require('../services/userService');
const externalService = require('../services/externalService');

router.post('/', (req, res) => {
  const { name, email } = req.body;
  if (!name || !email) {
    return res.status(422).json({ detail: "Nome e e-mail são obrigatórios" });
  }
  const existing = userService.findByEmail(email);
  if (existing) {
    return res.status(409).json({ detail: "E-mail já cadastrado" });
  }

  const newUser = userService.createUser({ name, email });
  return res.status(201).json(newUser);
});

router.get('/count', (req, res) => {
  res.json({ count: userService.listUsers().length });
});

router.get('/first-email', (req, res) => {
  const users = userService.listUsers();
  if (users.length === 0) {
    return res.status(404).json({ detail: "Nenhum usuário encontrado" });
  }
  return res.json(users[0]);
});

router.get('/broken', (req, res) => {
  const users = userService.listUsers();
  return res.json({ total: users.length });
});

router.get('/search', (req, res) => {
  const q = req.query.q;
  if (!q) {
    return res.json([]);
  }
  const results = userService.listUsers().filter(u => 
    u.name.toLowerCase().includes(q.toLowerCase())
  );
  return res.json(results);
});

router.get('/duplicates', (req, res) => {
  const allUsers = userService.listUsers();
  const counts = {};
  for (const u of allUsers) {
    counts[u.email] = (counts[u.email] || 0) + 1;
  }
  
  const results = allUsers.filter(u => counts[u.email] > 1);
  return res.json(results);
});

router.get('/email-domains', (req, res) => {
  const domainCounts = {};
  for (const u of userService.listUsers()) {
    const domain = u.email.split('@')[1]?.toLowerCase();
    if (domain) {
      domainCounts[domain] = (domainCounts[domain] || 0) + 1;
    }
  }
  
  const results = Object.keys(domainCounts)
    .sort()
    .map(domain => ({
      domain,
      count: domainCounts[domain]
    }));
  return res.json(results);
});

router.get('/:user_id/email', (req, res) => {
  const userId = parseInt(req.params.user_id, 10);
  const user = userService.getUser(userId);
  if (!user) {
    return res.status(404).json({ detail: "Usuário não encontrado" });
  }
  return res.json({ email: user.email });
});

router.get('/by-email', (req, res) => {
  try {
    const email = req.query.email;
    if (!email) {
      return res.status(400).json({ detail: "Parâmetro email é obrigatório" });
    }
    // Optional: trim email to avoid spaces issues
    const trimmedEmail = email.trim();
    if (!trimmedEmail) {
      return res.status(400).json({ detail: "Parâmetro email é obrigatório" });
    }
    // Optional: validate email format (basic regex)
    // If invalid, return 404 to avoid exposing validation details
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(trimmedEmail)) {
      return res.status(404).json({ detail: "Usuário não encontrado" });
    }

    const user = userService.findByEmail(trimmedEmail);
    if (!user) {
      return res.status(404).json({ detail: "Usuário não encontrado" });
    }
    // Filter out sensitive fields if any
    const { password, token, ...safeUser } = user;
    return res.json(safeUser);
  } catch (error) {
    return res.status(500).json({ detail: "Erro interno do servidor" });
  }
});

router.get('/has-email', (req, res) => {
  const email = req.query.email;
  if (!email || !email.trim()) {
    return res.status(400).json({ detail: "Parâmetro email é obrigatório" });
  }

  const exists = Boolean(userService.findByEmail(email.trim()));
  return res.json({ email: email.trim(), exists });
});

router.all('/has-email', (req, res) => {
  return res.status(404).json({ detail: "Rota não encontrada" });
});

router.get('/:user_id/age-estimate', async (req, res) => {
  const userId = parseInt(req.params.user_id, 10);
  const user = userService.getUser(userId);
  if (!user) {
    return res.status(404).json({ detail: "Usuário não encontrado" });
  }

  const estimate = await externalService.estimateAge(user.name);
  return res.json(estimate);
});

router.get('/:user_id', (req, res) => {
  const userId = parseInt(req.params.user_id, 10);
  const user = userService.getUser(userId);
  if (!user) {
    return res.status(404).json({ detail: "Usuário não encontrado" });
  }
  return res.json(user);
});

router.get('/', (req, res) => {
  const limit = parseInt(req.query.limit, 10) || 100;
  const offset = parseInt(req.query.offset, 10) || 0;
  const users = userService.listUsers(limit, offset);
  return res.json(users);
});

router.put('/:user_id', (req, res) => {
  const userId = parseInt(req.params.user_id, 10);
  if (isNaN(userId)) {
    return res.status(422).json({ detail: 'ID de usuário inválido' });
  }

  const body = req.body;
  if (!body || typeof body !== 'object') {
    return res.status(422).json({ detail: "Pelo menos um dos campos 'name' ou 'email' deve ser informado" });
  }

  const { name, email } = body;

  if (!name && !email) {
    return res.status(422).json({ detail: "Pelo menos um dos campos 'name' ou 'email' deve ser informado" });
  }

  if (email) {
    const existing = userService.findByEmail(email);
    if (existing && existing.id !== userId) {
      return res.status(409).json({ detail: 'E-mail já cadastrado' });
    }
  }

  const updated = userService.updateUser(userId, { name, email });
  if (!updated) {
    return res.status(404).json({ detail: 'Usuário não encontrado' });
  }
  return res.json(updated);
});

router.delete('/:user_id', (req, res) => {
  const userId = parseInt(req.params.user_id, 10);
  if (isNaN(userId)) {
    return res.status(422).json({ detail: 'ID de usuário inválido' });
  }
  const user = userService.getUser(userId);
  if (!user) {
    return res.status(404).json({ detail: 'Usuário não encontrado' });
  }
  userService.deleteUser(userId);
  return res.status(204).send();
});

module.exports = router;