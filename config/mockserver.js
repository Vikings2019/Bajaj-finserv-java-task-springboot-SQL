// mock-server.js
const express = require('express');
const bodyParser = require('body-parser');
const app = express();
app.use(bodyParser.json());

app.post('/hiring/generateWebhook/JAVA', (req, res) => {
  console.log("generateWebhook received:", req.body);
  res.json({
    webhook: "http://localhost:8081/hiring/testWebhook/JAVA",
    accessToken: "mocked-jwt-token-123"
  });
});

app.post('/hiring/testWebhook/JAVA', (req, res) => {
  console.log("testWebhook received: headers:", req.headers);
  console.log("testWebhook body:", req.body);
  res.json({ status: "ok", received: req.body });
});

app.listen(8081, () => console.log("Mock server running on 8081"));
