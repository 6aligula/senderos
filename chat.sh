cat > bridge.sh << 'EOF'
#!/bin/bash
set -e

echo "🚀 Instalando PicoClaw Bridge..."

sudo apt update
sudo apt install -y nodejs npm

sudo mkdir -p /opt/picoclaw-bridge
sudo chown $USER:$USER /opt/picoclaw-bridge
cd /opt/picoclaw-bridge

npm init -y
npm install express

cat > index.js << 'JS'
import express from "express";
import { execFile } from "child_process";

const app = express();
app.use(express.json());

app.post("/chat", (req, res) => {
  const message = (req.body?.message || "").toString().trim();
  if (!message) return res.status(400).json({ error: "message required" });

  execFile("picoclaw", ["agent", "-m", message], { timeout: 120000 }, (err, stdout, stderr) => {
    if (err) return res.status(500).json({ error: stderr || err.message });
    res.json({ reply: stdout.trim() });
  });
});

app.listen(3005, "127.0.0.1", () => console.log("PicoClaw bridge on 3005"));
JS

sudo tee /etc/systemd/system/picoclaw-bridge.service > /dev/null << 'SERVICE'
[Unit]
Description=PicoClaw HTTP Bridge
After=network.target

[Service]
ExecStart=/usr/bin/node /opt/picoclaw-bridge/index.js
WorkingDirectory=/opt/picoclaw-bridge
Restart=always
RestartSec=5
User=pi
Environment=NODE_ENV=production

[Install]
WantedBy=multi-user.target
SERVICE

sudo systemctl daemon-reload
sudo systemctl enable picoclaw-bridge
sudo systemctl restart picoclaw-bridge

echo "✅ Instalado. Prueba:"
echo "curl -X POST http://localhost:3005/chat -H 'Content-Type: application/json' -d '{\"message\":\"Hola\"}'"
EOF
chmod +x bridge.sh
