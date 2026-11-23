"""临时测试脚本：用与 application.yml 相同的 JWT_SECRET 签发测试令牌，并压测 /ai/chat 的 SSE 帧。"""
import base64
import hashlib
import hmac
import json
import sys
import time
import urllib.request
import urllib.error

SECRET = b"7f3c9a1e5b8d2f6a4c0e9b7d3a1f8c5e2b6d4a9f7c3e1b8d5a2f6c9e4b7d1a3f"
BASE = "http://localhost:8081/api"
USER_ID = "41"


def b64(raw: bytes) -> bytes:
    return base64.urlsafe_b64encode(raw).rstrip(b"=")


def token() -> str:
    header = b64(json.dumps({"alg": "HS256", "typ": "JWT"}, separators=(",", ":")).encode())
    now = int(time.time())
    payload = b64(json.dumps(
        {"sub": USER_ID, "phone": "13800000000", "iat": now, "exp": now + 3600},
        separators=(",", ":"),
    ).encode())
    signature = b64(hmac.new(SECRET, header + b"." + payload, hashlib.sha256).digest())
    return (header + b"." + payload + b"." + signature).decode()


def post(path: str, body: dict, stream: bool = True) -> None:
    data = json.dumps(body, ensure_ascii=False).encode("utf-8")
    request = urllib.request.Request(
        BASE + path,
        data=data,
        method="POST",
        headers={
            "Content-Type": "application/json; charset=utf-8",
            "Authorization": "Bearer " + token(),
        },
    )
    print(f"\n===== POST {path} {json.dumps(body, ensure_ascii=False)[:120]} =====")
    try:
        with urllib.request.urlopen(request, timeout=180) as response:
            print("status:", response.status, response.headers.get("Content-Type"))
            if not stream:
                print(response.read().decode("utf-8"))
                return
            while True:
                chunk = response.readline()
                if not chunk:
                    break
                line = chunk.decode("utf-8").rstrip("\n")
                if line:
                    print(line)
    except urllib.error.HTTPError as error:
        print("HTTP", error.code, error.read().decode("utf-8"))
    except Exception as error:  # noqa: BLE001
        print("ERROR", error)


if __name__ == "__main__":
    mode = sys.argv[1] if len(sys.argv) > 1 else "single"
    if mode == "single":
        post("/ai/chat", {"scene": "jd", "input": "招聘数据分析师，要求 SQL 与 Python，薪资 20k-35k，工作地点深圳"})
    elif mode == "chat":
        post("/ai/chat", {"scene": "chat", "messages": [{"role": "user", "content": "你好，我想转行做数据分析"}]})
    elif mode == "bad-scene":
        post("/ai/chat", {"scene": "match", "input": "x"})
    elif mode == "empty":
        post("/ai/chat", {"scene": "jd"})
    elif mode == "bad-messages":
        post("/ai/chat", {"scene": "chat", "messages": [{"role": "user", "content": "a"}, {"role": "assistant", "content": "b"}]})