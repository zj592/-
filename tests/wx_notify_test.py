"""微信登录 + 消息通知 联调测试
覆盖：mock openid 账号稳定性、审核通知、认领通知、自动驳回通知、未读数与已读
"""
import json
import urllib.request
import urllib.error

BASE = "http://localhost:8080"
FAIL = []
CHECKS = []


def call(method, path, data=None, token=None, expect=200, quiet=False):
    body = json.dumps(data).encode("utf-8") if data is not None else None
    req = urllib.request.Request(BASE + path, body, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            payload = json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        payload = {"code": e.code, "msg": "", "data": None}
    if payload.get("code") != expect:
        FAIL.append(f"{method} {path} 期望{expect} 实际{payload.get('code')} {payload.get('msg')}")
    if not quiet:
        print(("  OK   " if payload.get("code") == expect else "  FAIL ")
              + f"{method} {path} -> {payload.get('code')} | {payload.get('msg')}")
    return payload.get("data")


def check(desc, condition, extra=""):
    CHECKS.append(condition)
    print(("  OK   " if condition else "  FAIL ") + desc + (" | " + str(extra) if extra else ""))


def login(u, p):
    return call("POST", "/api/auth/login", {"username": u, "password": p}, quiet=True)["token"]


def notices(token, size=30):
    return call("GET", f"/api/notifications?page=1&size={size}", token=token, quiet=True)["records"]


def unread(token):
    return call("GET", "/api/notifications/unread-count", token=token, quiet=True)["count"]


print("== 1. 微信登录（mock 模式）==")
first = call("POST", "/api/auth/wx-login", {"code": "code-abc-1", "deviceId": "device-A"}, quiet=True)
again = call("POST", "/api/auth/wx-login", {"code": "code-abc-2", "deviceId": "device-A"}, quiet=True)
other = call("POST", "/api/auth/wx-login", {"code": "code-xyz", "deviceId": "device-B"}, quiet=True)
check("同一设备两次登录拿到同一 userId", first["userId"] == again["userId"], f"{first['userId']} vs {again['userId']}")
check("不同设备是不同账号", first["userId"] != other["userId"])
check("自动生成的账号名以 wx_ 开头", str(first["username"]).startswith("wx_"), first["username"])
check("返回了 token 与角色", bool(first["token"]) and first["role"] == "STUDENT")
call("POST", "/api/auth/wx-login", {"deviceId": "device-C"}, expect=400, quiet=True)
print("     （缺少 code 的请求已按 400 拦截）")

print("== 2. 未登录访问消息接口应 401 ==")
call("GET", "/api/notifications", expect=401, quiet=True)
check("未登录访问 /api/notifications 返回 401", True)

print("== 3. 发布 -> 审核通过 -> 发布者收到审核通知 ==")
student, lin, chen, admin = login("student", "123456"), login("lin", "123456"), login("chen", "123456"), login("admin", "admin123")
item = call("POST", "/api/items", {
    "title": "（联调）丢失一个蓝色保温杯",
    "type": "LOST", "category": "device",
    "description": "在体育馆篮球场旁边丢的，杯身有一张白色贴纸，麻烦捡到的同学联系我。",
    "place": "体育馆篮球场", "contact": "13800002222", "lostTime": "2026-09-15 08:00:00"
}, token=student, quiet=True)
call("POST", f"/api/admin/items/{item['id']}/audit", {"approved": True}, token=admin, quiet=True)

list_of_student = notices(student)
audit_notice = [n for n in list_of_student if n["type"] == "ITEM_AUDIT" and n["relatedId"] == item["id"]]
check("发布者收到审核通过通知", len(audit_notice) == 1, audit_notice[0]["title"] if audit_notice else "无")

print("== 4. 提交认领 -> 发布者收到「收到申请」通知 ==")
claim_lin = call("POST", "/api/claims", {"itemId": item["id"], "description": "杯子是我的，贴纸上有我写的名字，可以拍照确认。", "contact": "lin_wx"}, token=lin, quiet=True)
claim_chen = call("POST", "/api/claims", {"itemId": item["id"], "description": "我也觉得这个杯子像我的，杯底有一个小凹痕。", "contact": "chen_qq"}, token=chen, quiet=True)

recv = [n for n in notices(student) if n["type"] == "CLAIM_RECEIVED" and n["relatedId"] == claim_lin["id"]]
check("发布者收到认领申请通知", len(recv) == 1, recv[0]["content"] if recv else "无")

print("== 5. 同意认领 -> 申请人收到结果、其他申请人收到自动驳回 ==")
call("POST", f"/api/claims/{claim_lin['id']}/handle", {"approved": True, "remark": "已确认，请联系我取回"}, token=student, quiet=True)

lin_result = [n for n in notices(lin) if n["type"] == "CLAIM_RESULT" and n["relatedId"] == claim_lin["id"]]
chen_result = [n for n in notices(chen) if n["type"] == "CLAIM_RESULT" and n["relatedId"] == claim_chen["id"]]
check("被同意的人收到「已通过」通知", len(lin_result) == 1 and "已通过" in lin_result[0]["title"],
      lin_result[0]["title"] if lin_result else "无")
check("被自动驳回的人收到「未通过」通知", len(chen_result) == 1 and "未通过" in chen_result[0]["title"],
      chen_result[0]["title"] if chen_result else "无")

detail = call("GET", "/api/items/" + str(item["id"]), token=student, quiet=True)
check("信息状态已置为 FINISHED", detail["status"] == "FINISHED", detail["statusText"])

print("== 6. 未读数与已读 ==")
before = unread(lin)
check("lin 有未读消息", before >= 1, before)
first_id = notices(lin)[0]["id"]
call("POST", f"/api/notifications/{first_id}/read", token=lin, quiet=True)
check("单条已读后未读数 -1", unread(lin) == before - 1, unread(lin))
call("POST", "/api/notifications/read-all", token=lin, quiet=True)
check("全部已读后未读数为 0", unread(lin) == 0, unread(lin))
call("POST", f"/api/notifications/{first_id}/read", token=chen, expect=404, quiet=True)
check("不能标记别人的消息为已读（返回 404）", True)

print("\n=========== 结果 ===========")
print(f"断言通过 {sum(CHECKS)}/{len(CHECKS)}，接口异常 {len(FAIL)} 项")
for f in FAIL:
    print("  ", f)
