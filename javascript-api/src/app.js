const express = require('express');
const cors = require('cors');
const userRoutes = require('./routes/users');
const pingRoutes = require('./routes/ping');

const app = express();

app.use(cors());
app.use(express.json());

app.get('/health', (req, res) => {
  res.json({ status: 'ok' });
});

app.use('/users', userRoutes);
app.use('/ping', pingRoutes);

module.exports = app;
