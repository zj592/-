"""接口冒烟测试：覆盖公开接口、登录、鉴权、发布、审核、认领闭环"""
import json
import urllib.request
import urllib.error

BASE = "http://localhost:8080"
PASS, FAIL = [], []


def call(method, path, data=None, token=None, expect=200):
    url = BASE + path
    body = json.dumps(data).encode("utf-8") if data is not None else None
    req = urllib.request.Request(url, data=body, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            payload = json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        payload = {"code": e.code, "msg": e.read().decode("utf-8", "ignore"), "data": None}
    ok = payload.get("code") == expect
    (PASS if ok else FAIL).append(f"{method} {path} -> code={payload.get('code')} msg={payload.get('msg')}")
    print(("  OK   " if ok else "  FAIL ") + f"{method} {path} -> code={payload.get('code')} | {payload.get('msg')}")
    return payload.get("data")


print("== 1. 公开接口（无需登录）==")
items = call("GET", "/api/items?page=1&size=5")
print("     列表条数:", len(items["records"]), "总数:", items["total"])
print("     首条:", items["records"][0]["title"], "| 联系方式是否打码:",
      items["records"][0].get("contact") is None)
call("GET", "/api/items/meta")
call("GET", "/api/items/" + str(items["records"][0]["id"]))

print("== 2. 登录与鉴权 ==")
admin = call("POST", "/api/auth/login", {"username": "admin", "password": "admin123"})
student = call("POST", "/api/auth/login", {"username": "student", "password": "123456"})
call("POST", "/api/auth/login", {"username": "admin", "password": "wrong-pwd"}, expect=400)
call("GET", "/api/user/profile", token=student["token"])
print("== 3. 普通用户访问后台应被拒绝(403) ==")
call("GET", "/api/admin/stats", token=student["token"], expect=403)
print("== 4. 未带 token 访问受保护接口应 401 ==")
call("POST", "/api/items", data={}, expect=401)

print("== 5. 发布 -> 待审核 -> 管理员通过 ==")
new_item = call("POST", "/api/items", {
    "title": "（接口测试）丢失一把黑色雨伞",
    "type": "LOST",
    "category": "device",
    "description": "在图书馆一楼大厅丢的，手柄上有一圈红绳，麻烦捡到的同学联系我。",
    "place": "图书馆一楼大厅",
    "contact": "13800001234",
    "lostTime": "2026-09-15 09:00:00"
}, token=student["token"])
print("     新信息状态:", new_item["status"])
pending = call("GET", "/api/admin/items?status=PENDING", token=admin["token"])
print("     待审核数量:", pending["total"])
call("POST", f"/api/admin/items/{new_item['id']}/audit", {"approved": True}, token=admin["token"])
after = call("GET", "/api/items/" + str(new_item["id"]))
print("     审核后状态:", after["status"], "| 联系方式(已登录):", after["contact"])

print("== 6. 认领闭环 ==")
claim = call("POST", "/api/claims", {
    "itemId": items["records"][0]["id"],
    "description": "这把伞是我丢的，手柄上有红色绳子，可以描述伞套特征。",
    "contact": "student_wx"
}, token=student["token"])
print("     申请状态:", claim["status"] if claim else claim)
mine = call("GET", "/api/claims/mine", token=student["token"])
print("     我提交的申请数:", mine["total"])

print("== 7. 发布者审批：lin 处理别人对自己信息的申请 ==")
lin = call("POST", "/api/auth/login", {"username": "lin", "password": "123456"})
received = call("GET", "/api/claims/received", token=lin["token"])
print("     lin 收到的申请数:", received["total"])
if received["total"] > 0:
    cid = received["records"][0]["id"]
    call("POST", f"/api/claims/{cid}/handle", {"approved": True, "remark": "已确认，请联系我取回"}, token=lin["token"])
    detail = call("GET", "/api/items/" + str(received["records"][0]["itemId"]))
    print("     同意后信息状态:", detail["status"])

print("== 8. 个人中心统计 ==")
print("     student:", json.dumps(call("GET", "/api/my/stats", token=student["token"]), ensure_ascii=False))
print("     后台概览:", json.dumps(call("GET", "/api/admin/stats", token=admin["token"]), ensure_ascii=False))

print("\n================ 结果 ================")
print(f"通过 {len(PASS)} 项，失败 {len(FAIL)} 项")
for f in FAIL:
    print("  FAIL:", f)
