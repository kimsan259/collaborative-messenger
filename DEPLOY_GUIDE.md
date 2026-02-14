# Deploy Guide (GitHub + Auto Update)

## 1. Create GitHub repository
Create this repo on your account:
- `https://github.com/kimsan259/collaborative-messenger`

## 2. Initialize and push local source
Run in project root:
```bash
git init
git branch -M main
git add .
git commit -m "feat: initial collaborative messenger release"
git remote add origin https://github.com/kimsan259/collaborative-messenger.git
git push -u origin main
```

## 3. Prepare VPS once
Install:
- Docker
- Docker Compose plugin
- Git

Then clone once on VPS:
```bash
git clone https://github.com/kimsan259/collaborative-messenger.git ~/collaborative-messenger
cd ~/collaborative-messenger
docker compose up -d --build
```

## 4. Configure GitHub Actions secrets
Repository -> Settings -> Secrets and variables -> Actions

Create:
- `VPS_HOST`: server ip/domain
- `VPS_USER`: ssh username
- `VPS_SSH_KEY`: private key content

## 5. Auto update flow
After setup:
1. Local code change
2. `git push origin main`
3. GitHub Actions deploy job runs
4. VPS pulls latest code and restarts containers

## 6. Notes
- Current deploy workflow assumes repository name is `collaborative-messenger`.
- For production, change default passwords in `application.yml` and `docker-compose.yml`.
- Add HTTPS reverse proxy (Nginx + Certbot) before public release.
