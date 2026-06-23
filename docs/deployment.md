# 健康预约项目部署步骤

本文按生产环境部署编写，覆盖后台管理前端、小程序前端、后台管理后端服务。当前项目由三部分组成：

- `backend`：Spring Boot 后端服务，Docker 方式部署，连接外部 MySQL。
- `admin-web`：Vue 3 + Vite 后台管理前端，构建后作为静态站点部署。
- `miniprogram`：原生微信小程序，使用微信开发者工具上传并在微信公众平台发布。

以下示例域名请替换为实际域名：

- 后端 API 域名：`https://api.example.com`
- 后台管理域名：`https://admin.example.com`
- 小程序 AppID：`wx_xxxxxxxxxxxxxxxx`

## 1. 部署前准备

### 1.1 服务器与基础软件

后端服务器建议准备：

- Linux 服务器 1 台。
- Docker Engine 24+。
- Docker Compose v2。
- 可访问的 MySQL 8.0 数据库。
- 已备案并配置 HTTPS 证书的 API 域名。
- 防火墙开放 `80`、`443`，如需直连后端调试再开放映射端口。

后台管理前端构建机器需要：

- Node.js 20 LTS。
- npm，项目当前包含 `package-lock.json`，生产构建优先使用 `npm ci`。

后端构建在 Dockerfile 内完成，镜像使用 Maven 3.9.9 和 JDK 17。

### 1.2 数据库准备

在 MySQL 中创建生产库和账号：

```sql
CREATE DATABASE health DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'health'@'%' IDENTIFIED BY '<强密码>';
GRANT ALL PRIVILEGES ON health.* TO 'health'@'%';
FLUSH PRIVILEGES;
```

确认数据库连接串包含时区参数：

```text
jdbc:mysql://<mysql-host>:3306/health?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
```

后端启用 Flyway，首次启动会执行 `backend/src/main/resources/db/migration` 下的初始化脚本。不要手工改动已经上线执行过的 Flyway 版本脚本。

### 1.3 域名规划

推荐生产环境：

- `api.example.com` 反向代理到后端容器 `8080`。
- `admin.example.com` 托管 `admin-web/dist` 静态文件。
- 小程序请求域名配置为 `https://api.example.com`。

微信小程序线上环境必须使用 HTTPS，不能使用 IP、`localhost` 或非 `443` 标准证书域名作为 request 合法域名。

## 2. 后端服务 Docker 部署

### 2.1 准备代码与环境文件

在服务器拉取代码后进入项目根目录：

```bash
cd /opt/health
cp .env.example .env
```

编辑 `.env`，不要把真实密码提交到 Git：

```bash
HEALTH_BACKEND_PORT=8080

SPRING_DATASOURCE_URL=jdbc:mysql://<mysql-host>:3306/health?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
SPRING_DATASOURCE_USERNAME=health
SPRING_DATASOURCE_PASSWORD=<mysql-password>

HEALTH_WECHAT_APP_ID=<小程序 AppID>
HEALTH_WECHAT_APP_SECRET=<小程序 AppSecret>
HEALTH_WECHAT_TOKEN_SECRET=<至少 32 位随机字符串>
```

如果后台管理前端和后端不是同源部署，需要在 `docker-compose.yml` 的 `health-backend.environment` 中增加 CORS 允许来源：

```yaml
APP_CORS_ALLOWED_ORIGINS: ${APP_CORS_ALLOWED_ORIGINS:-https://admin.example.com}
```

同时在 `.env` 中配置：

```bash
APP_CORS_ALLOWED_ORIGINS=https://admin.example.com
```

### 2.2 构建并启动容器

项目根目录已有 `docker-compose.yml` 和 `backend/Dockerfile`：

```bash
docker compose up -d --build health-backend
```

查看容器状态：

```bash
docker compose ps
docker compose logs -f health-backend
```

正常启动后，后端监听容器内 `8080`，主机端口由 `.env` 的 `HEALTH_BACKEND_PORT` 决定。上传文件目录挂载为：

```text
./data/uploads -> /app/data/uploads
```

### 2.3 配置 Nginx 反向代理

建议用 Nginx 对外提供 HTTPS，并把请求转发给本机 Docker 映射端口：

```nginx
server {
    listen 80;
    server_name api.example.com;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name api.example.com;

    ssl_certificate     /etc/nginx/certs/api.example.com.pem;
    ssl_certificate_key /etc/nginx/certs/api.example.com.key;

    client_max_body_size 6m;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
    }
}
```

重载 Nginx：

```bash
nginx -t
systemctl reload nginx
```

### 2.4 后端验收

执行：

```bash
curl -i https://api.example.com/api/store
```

预期：

- HTTP 状态为 `200`。
- 响应体为项目统一格式，`success` 为 `true`。
- 后端日志没有 Flyway、数据库连接、微信配置读取错误。

常用运维命令：

```bash
docker compose logs -f health-backend
docker compose restart health-backend
docker compose pull
docker compose up -d --build health-backend
docker compose down
```

## 3. 后台管理前端部署

### 3.1 配置 API 地址

后台管理端请求基址来自 `admin-web/src/api/http.ts` 中的 `VITE_API_BASE_URL`。生产构建前在 `admin-web` 下创建 `.env.production`：

```bash
cd admin-web
cat > .env.production <<'EOF'
VITE_API_BASE_URL=https://api.example.com
EOF
```

如果管理端和 API 同域部署，也可以把 `VITE_API_BASE_URL` 配成同源地址或反向代理地址，但要确保浏览器实际请求能到后端。

### 3.2 构建

```bash
cd admin-web
npm ci
npm run build
```

构建产物位于：

```text
admin-web/dist
```

### 3.3 部署静态文件

把 `admin-web/dist` 发布到 Nginx 静态目录，例如：

```bash
mkdir -p /var/www/health-admin
rsync -av --delete dist/ /var/www/health-admin/
```

Nginx 示例：

```nginx
server {
    listen 80;
    server_name admin.example.com;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name admin.example.com;

    ssl_certificate     /etc/nginx/certs/admin.example.com.pem;
    ssl_certificate_key /etc/nginx/certs/admin.example.com.key;

    root /var/www/health-admin;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

重载：

```bash
nginx -t
systemctl reload nginx
```

### 3.4 管理端验收

浏览器打开：

```text
https://admin.example.com
```

检查：

- 登录页可正常打开。
- 登录接口请求到 `https://api.example.com/api/admin/auth/login`。
- 登录成功后可进入服务项目、技师、排班、预约列表页面。
- 图片上传后，后端返回的 `/uploads/...` 图片能通过 API 域名访问。

如果浏览器报 CORS 错误，检查后端容器环境变量 `APP_CORS_ALLOWED_ORIGINS` 是否包含 `https://admin.example.com`，并重启后端容器。

## 4. 小程序前端部署

### 4.1 修改小程序 API 地址

小程序请求基址在：

```text
miniprogram/app.js
```

上线前把本地地址改为生产 API 域名：

```js
App({
  globalData: {
    apiBaseUrl: 'https://api.example.com'
  }
})
```

注意：

- 小程序线上环境必须使用 HTTPS。
- API 域名必须在微信公众平台配置为 request 合法域名。
- 不要在小程序代码里写数据库密码、AppSecret 或后端 Token 密钥。

### 4.2 微信公众平台配置

登录微信公众平台：

```text
https://mp.weixin.qq.com
```

按以下步骤配置：

1. 进入「设置与开发」->「开发管理」->「开发设置」。
2. 记录小程序 `AppID`，填入后端 `.env` 的 `HEALTH_WECHAT_APP_ID`。
3. 生成或查看 `AppSecret`，填入后端 `.env` 的 `HEALTH_WECHAT_APP_SECRET`。
4. 在「服务器域名」中配置：
   - `request合法域名`：`https://api.example.com`
   - 如果图片、文件也走同一个 API 域名，`downloadFile合法域名` 也配置 `https://api.example.com`。
5. 保存后等待配置生效。

服务器域名要求：

- 必须是 HTTPS。
- 证书必须有效且域名匹配。
- 不能包含路径，只填域名。
- 不能使用 IP 地址、`localhost` 或内网域名。

### 4.3 微信手机号授权能力

当前小程序预约页使用手机号授权能力：前端通过手机号授权按钮拿到 `phoneCode`，再配合 `wx.login` 的 `loginCode` 调用后端 `/api/mp/auth/login`。

需要确认：

1. 小程序主体和类目满足微信手机号快速验证组件使用条件。
2. 微信公众平台中相关接口权限已开通，通常在「开发管理」或「接口权限」中查看手机号相关能力。
3. 小程序前端按钮使用 `open-type="getPhoneNumber"`，开发者工具和真机都能返回 `event.detail.code`。
4. 后端 `.env` 中 `HEALTH_WECHAT_APP_ID`、`HEALTH_WECHAT_APP_SECRET` 与当前小程序 AppID 完全一致。

如果手机号授权失败，优先检查：

- 后端日志中微信接口调用是否返回 AppID/AppSecret 错误。
- 微信开发者工具是否使用了正确 AppID 打开 `miniprogram`。
- 线上版本是否已经配置 request 合法域名。

### 4.4 微信开放平台配置

如果只发布单个小程序，微信公众平台配置已足够完成登录、手机号授权和发布。

如果后续需要在公众号、App、小程序之间打通同一用户身份，需要配置微信开放平台：

1. 登录微信开放平台：

   ```text
   https://open.weixin.qq.com
   ```

2. 创建或进入同主体开放平台账号。
3. 进入「管理中心」->「小程序」->「绑定小程序」。
4. 输入当前小程序 AppID，并按页面提示完成管理员扫码确认。
5. 绑定后，同一开放平台下的应用可获得同一用户的 `UnionID`。

当前后端登录链路主要使用小程序 `openId` 和手机号建用户；开放平台绑定不是当前版本上线的强制条件，但建议生产前完成，便于以后做多端账号合并。

### 4.5 微信开发者工具上传

安装并打开微信开发者工具：

```text
https://developers.weixin.qq.com/miniprogram/dev/devtools/download.html
```

操作步骤：

1. 选择「导入项目」。
2. 项目目录选择本仓库的 `miniprogram`。
3. AppID 选择生产小程序 AppID。
4. 确认 `miniprogram/project.config.json` 中 `appid` 与生产 AppID 一致。
5. 在「详情」中关闭「不校验合法域名」后预览，确保真机能请求 `https://api.example.com`。
6. 点击「上传」。
7. 填写版本号，例如 `1.0.0`。
8. 填写项目备注，例如 `健康预约 MVP 首次上线`。

### 4.6 提交审核与发布

在微信公众平台操作：

1. 进入「版本管理」。
2. 找到刚上传的开发版本。
3. 设置为体验版，先让内部人员扫码测试。
4. 测试通过后点击「提交审核」。
5. 按实际业务填写：
   - 服务类目。
   - 功能说明。
   - 测试账号或测试路径。
   - 隐私协议。
   - 用户信息收集说明，包含手机号用途。
6. 审核通过后点击「发布」。

审核前建议准备测试路径：

- 首页查看门店信息。
- 服务列表和服务详情。
- 预约页手机号授权。
- 创建预约。
- 订单列表和订单详情。

## 5. 联调验收清单

上线前按顺序检查：

1. 后端容器启动成功。
2. `https://api.example.com/api/store` 返回正常。
3. 管理后台能登录。
4. 管理后台服务项目、技师、排班、预约列表可正常加载。
5. 管理后台图片上传后能在页面显示。
6. 微信开发者工具关闭「不校验合法域名」后，小程序仍可加载首页。
7. 小程序预约页可以完成手机号授权。
8. 小程序可以创建预约。
9. 管理后台能看到小程序创建的预约。
10. 管理后台推进预约状态后，小程序订单详情能看到最新状态。

## 6. 常见问题

### 6.1 后端启动失败，提示数据库连接失败

检查：

```bash
docker compose logs -f health-backend
```

重点确认：

- `SPRING_DATASOURCE_URL` 主机名和端口容器内可访问。
- MySQL 账号有 `health` 库权限。
- MySQL 安全组或防火墙允许后端服务器访问。

### 6.2 Flyway 迁移失败

检查数据库是否已有旧表但没有 `flyway_schema_history`。生产环境不要直接删除表或清空 Flyway 记录，应先备份数据库，再按实际历史状态处理。

### 6.3 管理后台跨域失败

现象是浏览器控制台出现 CORS 错误。处理：

1. 在 `docker-compose.yml` 的后端环境变量中加入 `APP_CORS_ALLOWED_ORIGINS`。
2. `.env` 中设置后台域名。
3. 重启后端：

   ```bash
   docker compose up -d --build health-backend
   ```

### 6.4 小程序线上请求失败

检查：

- `miniprogram/app.js` 是否为 HTTPS API 域名。
- 微信公众平台 request 合法域名是否配置了同一个域名。
- 证书是否有效。
- 是否在体验版或线上版中关闭了「不校验合法域名」依赖。

### 6.5 手机号授权失败

检查：

- 小程序 AppID 与后端 `HEALTH_WECHAT_APP_ID` 一致。
- `HEALTH_WECHAT_APP_SECRET` 是当前小程序的最新密钥。
- 公众平台手机号能力可用。
- 后端 `/api/mp/auth/login` 日志没有微信接口错误。

## 7. 发布命令速查

后端：

```bash
cd /opt/health
docker compose up -d --build health-backend
docker compose logs -f health-backend
```

管理后台：

```bash
cd /opt/health/admin-web
npm ci
npm run build
rsync -av --delete dist/ /var/www/health-admin/
nginx -t
systemctl reload nginx
```

小程序静态检查：

```bash
cd /opt/health
find miniprogram -name '*.js' -exec node --check {} \;
node -e "const fs=require('fs'); const app=JSON.parse(fs.readFileSync('miniprogram/app.json','utf8')); for (const p of app.pages) { for (const ext of ['js','wxml','wxss','json']) { const f='miniprogram/'+p+'.'+ext; if (!fs.existsSync(f)) throw new Error('missing '+f); } JSON.parse(fs.readFileSync('miniprogram/'+p+'.json','utf8')); } JSON.parse(fs.readFileSync('miniprogram/sitemap.json','utf8')); console.log('mini program static check ok:', app.pages.length, 'pages');"
```
