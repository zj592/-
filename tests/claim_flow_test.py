"""认领审批闭环测试：同意后信息置完成 + 同信息其余申请自动驳回"""
import json
import urllib.request
import urllib.error
from datetime import datetime

BASE = "http://localhost:8080"
FAIL = []


def call(method, path, data=None, token=None, expect=200, show=True):
    url = BASE + path
    body = json.dumps(data).encode("utf-8") if data is not None else None
    req = urllib.request.Request(url, body, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            payload = json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        payload = {"code": e.code, "msg": "", "data": None}
    ok = payload.get("code") == expect
    if not ok:
        FAIL.append(f"{method} {path} 期望{expect} 实际{payload.get('code')}")
    if show:
        print(("  OK   " if ok else "  FAIL ") + f"{method} {path} -> {payload.get('code')} | {payload.get('msg')}")
    return payload.get("data"), payload


def login(u, p):
    data, _ = call("POST", "/api/auth/login", {"username": u, "password": p}, show=False)
    return data["token"]


student, admin = login("student", "123456"), login("admin", "admin123")

# 用一个临时注册的账号当"第二个申请人"，脚本重复执行也不会撞上历史申请
temp_username = "tmp" + datetime.now().strftime("%m%d%H%M%S")
temp_user, _ = call("POST", "/api/auth/register", {
    "username": temp_username, "password": "123456", "nickname": "测试同学"
}, show=False)
chen = temp_user["token"]
print(f"     临时申请人账号：{temp_username}")

print("== 1. 看发布者 student 收到的申请 ==")
received, _ = call("GET", "/api/claims/received", token=student)
print("     student 收到申请数:", received["total"])
for c in received["records"]:
    print(f"       - id={c['id']} 信息={c['itemTitle']} 申请人={c['claimantNickname']} 状态={c['statusText']}")

# 只挑「待处理」的申请：已处理的申请对应的信息可能已完成，再操作会（正确地）返回 400
pending = [c for c in received["records"] if c["status"] == "PENDING"]
if not pending:
    print("     没有待处理的申请，请先执行 mysql < db/init.sql 并重启服务重新灌入演示数据")
    raise SystemExit(1)
target = pending[0]
item_id = target["itemId"]
print(f"     选中的待处理申请 id={target['id']}，被申请的信息 id={item_id}")

print("== 2. 另一个用户 chen 也来申请同一物品 ==")
call("POST", "/api/claims", {
    "itemId": item_id,
    "description": "这件东西可能是我的，上面有一处不影响使用的小痕迹，可以拍照确认。",
    "contact": "tmp_user_wx"
}, token=chen)

print("== 3. 重复申请应被拦截 ==")
call("POST", "/api/claims", {"itemId": item_id, "description": "再申请一次试试看能不能提交"}, token=chen, expect=400)

print("== 4. 不能认领自己发布的信息 ==")
call("POST", "/api/claims", {"itemId": item_id, "description": "自己申请自己的东西应该被拒绝"}, token=student, expect=400)

print("== 5. student 同意 lin 的申请 ==")
call("POST", f"/api/claims/{target['id']}/handle", {"approved": True, "remark": "已确认，请联系我取回"}, token=student)

detail, _ = call("GET", "/api/items/" + str(item_id), token=student)
print("     信息状态 ->", detail["status"], detail["statusText"])

print("== 6. 同一信息其余待处理申请应被自动驳回 ==")
chen_claims, _ = call("GET", "/api/claims/mine", token=chen)
for c in chen_claims["records"]:
    print(f"       chen 的申请 -> {c['statusText']} | 备注: {c['auditRemark']}")

print("== 7. 已完成的信息不能再申请认领 ==")
call("POST", "/api/claims", {"itemId": item_id, "description": "东西已经完成了还想申请"}, token=admin, expect=400)

print("== 8. 非发布者不能处理别人的申请 ==")
other, _ = call("GET", "/api/claims/received", token=student, show=False)
print("     （当前无待处理申请，跳过）" if not other["records"] else "     有待处理申请")

print("\n=========== 结果 ===========")
print("失败项:", len(FAIL))
for f in FAIL:
    print("  ", f)
