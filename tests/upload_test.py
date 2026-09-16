"""图片上传测试：上传 -> 静态映射回读 -> 非法后缀拦截 -> 体积与鉴权

注意：本项目业务错误统一返回 HTTP 200 + body 里的 code，
所以断言必须看 body["code"]，不能只看 HTTP 状态码。
"""
import json
import struct
import urllib.error
import urllib.request
import uuid
import zlib

BASE = "http://localhost:8080"
FAIL = []
CHECKS = []


def check(desc, condition, extra=""):
    CHECKS.append(condition)
    print(("  OK   " if condition else "  FAIL ") + desc + (" | " + str(extra) if extra else ""))


def post_json(path, data, token=None):
    req = urllib.request.Request(BASE + path, json.dumps(data).encode(), method="POST")
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    return json.loads(urllib.request.urlopen(req, timeout=10).read().decode())


def tiny_png():
    """构造一个合法的 1x1 PNG，避免依赖外部图片文件"""

    def chunk(tag, data):
        raw = tag + data
        return struct.pack(">I", len(data)) + raw + struct.pack(">I", zlib.crc32(raw) & 0xFFFFFFFF)

    ihdr = struct.pack(">IIBBBBB", 1, 1, 8, 2, 0, 0, 0)
    return (b"\x89PNG\r\n\x1a\n"
            + chunk(b"IHDR", ihdr)
            + chunk(b"IDAT", zlib.compress(b"\x00\xff\x00\x00"))
            + chunk(b"IEND", b""))


def upload(filename, content, content_type, token=None):
    boundary = "----" + uuid.uuid4().hex
    body = (("--" + boundary + "\r\n"
             + f'Content-Disposition: form-data; name="file"; filename="{filename}"\r\n'
             + f"Content-Type: {content_type}\r\n\r\n").encode()
            + content
            + ("\r\n--" + boundary + "--\r\n").encode())
    req = urllib.request.Request(BASE + "/api/files/upload", body, method="POST")
    req.add_header("Content-Type", "multipart/form-data; boundary=" + boundary)
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        resp = urllib.request.urlopen(req, timeout=20)
        return resp.status, json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read().decode() or "{}")


print("== 0. 准备登录态 ==")
token = post_json("/api/auth/login", {"username": "student", "password": "123456"})["data"]["token"]
check("拿到登录 token", bool(token))

print("== 1. 未登录上传应被拒绝 ==")
code, body = upload("t.png", tiny_png(), "image/png")
check("未登录上传被拦截", body.get("code") == 401, f"code={body.get('code')} msg={body.get('msg')}")

print("== 2. 正常上传 PNG ==")
png = tiny_png()
code, body = upload("t.png", png, "image/png", token=token)
ok = body.get("code") == 200
check("上传成功", ok, body.get("msg"))
url = body.get("data") or ""
check("返回可访问地址", "/uploads/" in url and url.endswith(".png"), url)

print("== 3. 回读图片（验证静态映射与数据卷写入）==")
if url:
    path = url.split("/uploads/")[1]
    got = urllib.request.urlopen(BASE + "/uploads/" + path, timeout=10)
    data = got.read()
    check("静态资源可访问", got.status == 200, f"HTTP {got.status}")
    check("回读字节与原文件一致", data == png, f"{len(data)} 字节")
    check("内容是合法 PNG", data[:8] == b"\x89PNG\r\n\x1a\n")
else:
    check("静态资源可访问", False, "上一步未拿到地址")

print("== 4. 非法后缀应被拒绝 ==")
for fn, ct, content in (("x.txt", "text/plain", b"hello"),
                        ("x.exe", "application/octet-stream", b"MZ"),
                        ("x", "application/octet-stream", b"noext")):
    code, body = upload(fn, content, ct, token=token)
    check(f"拒绝 {fn}", body.get("code") == 400, f"code={body.get('code')} msg={body.get('msg')}")

print("== 5. 空文件应被拒绝 ==")
code, body = upload("empty.png", b"", "image/png", token=token)
check("空文件被拒绝", body.get("code") == 400, f"code={body.get('code')} msg={body.get('msg')}")

print("\n=========== 结果 ===========")
print(f"断言通过 {sum(CHECKS)}/{len(CHECKS)}，失败 {len(CHECKS) - sum(CHECKS)} 项")
