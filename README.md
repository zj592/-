# 校园失物招领平台

原生微信小程序 + Spring Boot 3 + MySQL 写的校园失物招领系统。
丢了东西发寻物启事，捡到东西发失物招领，信息经管理员审核后展示出来；
看到匹配的信息可以提交认领申请，由发布者确认后完成归还。

## 技术栈

- 小程序端：原生微信小程序（WXML / WXSS / JS），没引第三方 UI 库
- 后端：Spring Boot 3.3.0 / Java 17 / MyBatis-Plus 3.5.7
- 数据库：MySQL 8.0（utf8mb4）
- 鉴权：JWT + 拦截器 + 自定义 `@RequireRole` 注解，密码用 BCrypt 存
- 接口文档：Knife4j，启动后访问 `http://localhost:8080/doc.html`

## 目录结构

```
campus-lost-found/
├── backend/                        # Spring Boot 后端
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/campus/lostfound/
│       │   ├── common/             # Result、业务异常、全局异常处理、字典
│       │   ├── config/             # Web 配置、MyBatis-Plus 分页、演示数据初始化
│       │   ├── security/           # JWT、登录上下文、鉴权拦截器、@RequireRole
│       │   ├── entity/ mapper/     # User、Item、Claim、Notification 及其 Mapper
│       │   ├── dto/ vo/            # 入参出参模型
│       │   ├── service/            # 业务逻辑
│       │   └── controller/         # 接口层
│       └── resources/application.yml
├── db/init.sql                     # 建库建表脚本
├── docker-compose.yml              # 一键启动（应用 + MySQL）
└── miniprogram/                    # 小程序端
    ├── config.js                   # 后端地址等常量
    ├── utils/                      # 请求封装、工具函数
    └── pages/                      # 9 个页面
```

## 启动

### Docker（推荐，不用装 JDK 和 MySQL）

```bash
docker compose up -d --build
docker compose logs -f app
```

日志里出现 `Started LostFoundApplication` 就起来了。然后：

- 接口文档 <http://localhost:8080/doc.html>
- 数据库可以用客户端连 `127.0.0.1:3307`，库 `lost_found`，账号 `lf_app`，密码 `lf_pass_2026`

```bash
docker compose down               # 停掉，数据保留
docker compose down -v            # 停掉并清空数据，改过 init.sql 之后要用这个
docker compose up -d --build      # 改完代码重新构建
```

### 本地跑

需要 JDK 17、Maven 3.9+、MySQL 8.0。

```bash
mysql -uroot -p < db/init.sql
cd backend
mvn clean package -DskipTests
java -jar target/lost-found-backend-1.0.0.jar
```

要改数据库连接就改 `backend/src/main/resources/application.yml`。

### 小程序端

1. 微信开发者工具导入 `miniprogram` 目录，AppID 选测试号
2. 「详情 → 本地设置」里勾上「不校验合法域名」
3. 真机调试时把 `miniprogram/config.js` 里的 `BASE_URL` 换成电脑的局域网 IP

### 演示账号

| 账号 | 密码 | 说明 |
| --- | --- | --- |
| admin | admin123 | 管理员，能进信息审核后台 |
| student | 123456 | 有 3 条自己发布的信息、1 条别人提交的认领申请 |
| lin | 123456 | 提交过一条认领申请 |
| chen | 123456 | 有 1 条已通过、1 条被驳回的信息 |

`db/init.sql` 里只有表结构，账号和演示数据由后端启动时写入（密码要 BCrypt 加密，写死在 sql 里对不上）。

## 功能

小程序端 9 个页面：

| 页面 | 做什么 |
| --- | --- |
| 首页 | 关键词搜索、按类型和分类筛选、上拉加载更多、下拉刷新 |
| 发布 | 发寻物启事或失物招领，选分类、填描述、选时间地点、传最多 3 张图 |
| 详情 | 信息详情和图片预览、发布者信息、申请认领的弹窗 |
| 登录注册 | 账号密码登录，或用微信一键登录 |
| 个人中心 | 统计数字和各个功能入口 |
| 消息通知 | 站内信列表、未读标识、单条或全部标记已读 |
| 我的发布 | 按审核状态筛选，可以编辑、删除、标记完成 |
| 认领申请 | 我提交的可以撤销，我收到的可以同意或驳回 |
| 审核后台 | 数据概览和待审核列表，一键通过或驳回 |

后端这边几个要点：

- 注册登录：BCrypt 校验密码，登录返回 JWT（默认 7 天）。账号不存在和密码错误返回同一句提示，避免被人拿去枚举账号
- 微信登录：`wx.login` 拿到的 code 换 openid，第一次登录自动建号。没配 appid 的时候走 mock 模式，用小程序端存着的 deviceId 派生一个稳定的 openid，这样开发者工具里也能把整条链路跑通
- 鉴权：拦截器统一解析 token 写进 `UserContext`。列表和详情这类公开接口允许匿名访问，带了 token 也会识别身份
- 发布到审核：普通用户发出来是 `PENDING`，管理员通过了才变 `APPROVED` 并出现在首页。驳回必须填原因
- 编辑后重审：已经通过的信息被编辑过，要重新审
- 认领：不能认领自己发的信息，同一条信息也不能重复申请。发布者同意之后信息变成 `FINISHED`，同一条信息下其他人还在待处理的申请自动驳回
- 消息：审核结果、收到申请、认领结果、被自动驳回这几个节点都会写一条站内信。订阅消息放在事务提交之后再发，免得网络 IO 占着数据库连接
- 联系方式：没登录的人看到的联系方式是打码的（`138****1234`），登录之后才看得到完整的
- 其他：浏览量自增（自己看自己的不计数）、逻辑删除、分页上限 100 条

## 接口

27 个，前缀都是 `/api`，返回结构固定 `{ code, msg, data }`，`code` 为 200 表示成功。

| 模块 | 方法 | 路径 | 说明 | 需要登录 |
| --- | --- | --- | --- | --- |
| 认证 | POST | /api/auth/register | 注册，直接返回 token | 否 |
| 认证 | POST | /api/auth/login | 账号密码登录 | 否 |
| 认证 | POST | /api/auth/wx-login | 微信登录，首次自动建号 | 否 |
| 用户 | GET | /api/user/profile | 当前用户信息 | 是 |
| 用户 | PUT | /api/user/profile | 改资料 | 是 |
| 信息 | GET | /api/items | 列表，支持关键词、类型、分类、分页 | 否 |
| 信息 | GET | /api/items/meta | 分类和类型的字典 | 否 |
| 信息 | GET | /api/items/{id} | 详情，浏览量加一 | 否 |
| 信息 | POST | /api/items | 发布，进待审核 | 是 |
| 信息 | PUT | /api/items/{id} | 编辑，重新审核 | 是 |
| 信息 | DELETE | /api/items/{id} | 删除，逻辑删除 | 是 |
| 信息 | POST | /api/items/{id}/finish | 标记已完成 | 是 |
| 个人中心 | GET | /api/my/items | 我的发布 | 是 |
| 个人中心 | GET | /api/my/stats | 我的统计数字 | 是 |
| 认领 | POST | /api/claims | 提交认领申请 | 是 |
| 认领 | GET | /api/claims/mine | 我提交的申请 | 是 |
| 认领 | GET | /api/claims/received | 我收到的申请 | 是 |
| 认领 | POST | /api/claims/{id}/cancel | 撤销申请 | 是 |
| 认领 | POST | /api/claims/{id}/handle | 同意或驳回 | 是 |
| 后台 | GET | /api/admin/items | 审核列表 | 管理员 |
| 后台 | POST | /api/admin/items/{id}/audit | 审核通过或驳回 | 管理员 |
| 后台 | GET | /api/admin/claims | 全部认领申请 | 管理员 |
| 后台 | GET | /api/admin/stats | 数据概览 | 管理员 |
| 通知 | GET | /api/notifications | 我的消息 | 是 |
| 通知 | GET | /api/notifications/unread-count | 未读数 | 是 |
| 通知 | POST | /api/notifications/{id}/read | 单条标记已读 | 是 |
| 通知 | POST | /api/notifications/read-all | 全部标记已读 | 是 |
| 文件 | POST | /api/files/upload | 传图，返回访问地址 | 是 |

## 数据表

| 表 | 关键字段 |
| --- | --- |
| user | username 唯一、password 存 BCrypt 密文、openid 唯一（微信登录用）、role 分 STUDENT 和 ADMIN、status |
| item | type 分 LOST 和 FOUND、category、status 分 PENDING/APPROVED/REJECTED/FINISHED、publisher_id、deleted 逻辑删除 |
| claim | item_id、claimant_id、status 分 PENDING/APPROVED/REJECTED/CANCELED，唯一键 (item_id, claimant_id) 防重复申请 |
| notification | user_id、type 分 CLAIM_RECEIVED/CLAIM_RESULT/ITEM_AUDIT、related_id、is_read，联合索引 (user_id, is_read) 查未读数用 |

状态流转：

```
信息  PENDING ─通过→ APPROVED ─发布者同意认领或手动完成→ FINISHED
        └─驳回→ REJECTED（编辑后重新提交审核）

申请  PENDING ─同意→ APPROVED（同时信息置 FINISHED，其余申请自动 REJECTED）
        ├─驳回→ REJECTED
        └─申请人撤销→ CANCELED

通知  审核通过或驳回 → 通知发布者
      收到认领申请   → 通知发布者
      同意或驳回申请 → 通知申请人
      被自动驳回     → 通知其余申请人
```

## 微信登录与订阅消息

默认 `app.wx.mock: true`，没配 appid 也能跑：后端用小程序端存着的 `deviceId` 派生一个稳定的 openid，
同一台设备反复登录还是同一个账号。

要接真实微信登录改两处：

1. `miniprogram/project.config.json` 里的 appid 换成自己的
2. `application.yml` 填 `app.wx.appid` 和 `app.wx.secret`，`app.wx.mock` 改成 `false`

之后 code 就会真的去调 `sns/jscode2session` 换 openid。

订阅消息的发送代码是写好的（access_token 带缓存、模板字段映射、事务提交后发送），默认关着。要启用：

1. 微信公众平台申请模板，比如「认领结果通知」「审核结果通知」
2. `application.yml` 填 `app.subscribe.template.*` 的模板 id，`app.subscribe.enabled` 改成 `true`
3. `miniprogram/config.js` 填 `SUBSCRIBE_TEMPLATE_IDS`

没配的时候消息只落站内信，后端打一条 debug 日志，不影响其他功能。

## 测试

`tests/` 下四个脚本打的是真实接口，跑之前先把后端起起来。

```bash
python tests/api_smoke_test.py     # 19 项：公开接口、鉴权、发布到审核、统计
python tests/claim_flow_test.py    # 认领闭环：重复申请、认领自己的、同意后置完成、其余自动驳回
python tests/wx_notify_test.py     # 14 项：微信登录、四类通知、未读数和已读
python tests/upload_test.py        # 11 项：上传回读、非法后缀、未登录、空文件
```

有个地方容易踩：业务错误统一返回 HTTP 200，错误码在 body 的 `code` 里
（`400` 参数问题、`401` 没登录、`403` 没权限、`404` 找不到）。
所以断言要看 `body["code"]`，光看 HTTP 状态码什么都测不出来。

CI（GitHub Actions）会在每次 push 和 PR 时自动跑这四个脚本，配置在 `.github/workflows/ci.yml`。

## Docker 配置说明

| 文件 | 作用 |
| --- | --- |
| docker-compose.yml | 编排 MySQL 和应用两个服务 |
| backend/Dockerfile | 两阶段构建，先 Maven 编译再拷进 JRE 镜像 |
| backend/.dockerignore | 排除 target 和 uploads，减小构建上下文 |
| backend/maven-settings.xml | 镜像内构建走阿里云仓库，拉依赖快一点 |

配置里有几处是按实际情况定的：

1. MySQL 映射到宿主 3307，因为 3306 一般已经被本机的 MySQL 占了。要换端口加环境变量：`MYSQL_PORT=3308 docker compose up -d`
2. 应用连库用的是 `lf_app` 账号，不是 root。官方 MySQL 镜像的 root 只允许 localhost 登录，从应用容器连过去会被拒
3. `depends_on` 配了 `condition: service_healthy`，等 MySQL 的 ping 通了再起应用，不然应用会抢在数据库前启动然后挂掉
4. 健康检查用 bash 的 `/dev/tcp` 探端口，JRE 镜像里没有 curl 和 wget
5. Dockerfile 里先拷 pom 再拷 src，改 Java 代码时 Maven 依赖那层能命中缓存
6. 上传目录挂到具名卷，容器重建图片不丢

## 已知限制

- 图片存本地磁盘的 `./uploads`，上线要换成对象存储
- 微信登录默认 mock 模式，不配 appid 不会真的调微信接口
- 消息通知没做离线补偿，订阅消息本身也有一次性授权限制
- 浏览量直接写库自增，没上 Redis
- 后台只有管理员一个角色，没做完整的权限表
