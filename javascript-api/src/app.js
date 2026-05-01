const express = require('express');
const cors = require('cors');
const userRoutes = require('./routes/users');
const pingRoutes = require('./routes/ping');
const productRoutes = require('./routes/products');

const app = express();

app.use(cors());
app.use(express.json());

app.get('/health', (req, res) => {
  res.json({ status: 'ok' });
});

app.use('/users', userRoutes);
app.use('/ping', pingRoutes);
app.use('/products', productRoutes);

module.exports = app;
