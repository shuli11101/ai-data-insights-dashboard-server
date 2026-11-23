"""临时脚本：验证 /ai/match 缓存/生成与 /user/match-report 的保存、详情、批量删除。"""
import base64
import hashlib
import hmac
import json
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

SECRET = b"7f3c9a1e5b8d2f6a4c0e9b7d3a1f8c5e2b6d4a9f7c3e1b8d5a2f6c9e4b7d1a3f"
BASE = "http://localhost:8081/api"
USER_ID = os.environ.get("AI_TEST_USER", "3")
JOB_ID = 24014
CONTENT = (
    "## 综合匹配分\n72\n\n"
    "## 技术栈\n"
    "- 已匹配技能：TypeScript\n"
    "- 缺失技能：SQL\n"
    "- 多余技能：无\n"
    "- 技能覆盖率：50\n\n"
    "## 城市\n- 是否同城：是\n- 是否支持远程：未提及\n\n"
    "## 薪资\n- 重叠百分比：60\n- 偏差百分比：-20\n\n"
    "## 建议\n- 补充 SQL 实战项目\n- 面试重点讲 TS 工程化\n"
)


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


def call(method: str, path: str, body: dict | None = None, stream: bool = False) -> None:
    url = BASE + path
    data = None
    headers = {"Authorization": "Bearer " + token()}
    if body is not None:
        data = json.dumps(body, ensure_ascii=False).encode("utf-8")
        headers["Content-Type"] = "application/json; charset=utf-8"
    request = urllib.request.Request(url, data=data, method=method, headers=headers)
    print(f"\n===== {method} {path} {json.dumps(body, ensure_ascii=False) if body else ''} =====")
    try:
        with urllib.request.urlopen(request, timeout=180) as response:
            print("status:", response.status, response.headers.get("Content-Type"))
            if not stream:
                print(response.read().decode("utf-8"))
                return
            shown = 0
            while True:
                chunk = response.readline()
                if not chunk:
                    break
                line = chunk.decode("utf-8").rstrip("\n")
                if not line:
                    continue
                # 流式只打印前 8 帧，避免刷屏
                if line.startswith("data:") and shown < 8:
                    print(line[:160])
                    shown += 1
                if line.strip() == "data:[DONE]":
                    print("data:[DONE]")
                    break
    except urllib.error.HTTPError as error:
        print("HTTP", error.code, error.read().decode("utf-8"))
    except Exception as error:  # noqa: BLE001
        print("ERROR", error)


if __name__ == "__main__":
    mode = sys.argv[1] if len(sys.argv) > 1 else "match"
    if mode == "match":
        call("POST", "/ai/match", {"jobId": JOB_ID, "force": False}, stream=True)
    elif mode == "match-force":
        call("POST", "/ai/match", {"jobId": JOB_ID, "force": True}, stream=True)
    elif mode == "save":
        call("POST", "/user/match-report", {"jobId": JOB_ID, "content": CONTENT})
    elif mode == "get":
        call("GET", f"/user/match-report?jobId={JOB_ID}")
    elif mode == "delete":
        call("DELETE", f"/user/match-report?jobId={JOB_ID}")
    elif mode == "delete-again":
        call("DELETE", f"/user/match-report?jobId={JOB_ID}")
    elif mode == "bad-job":
        call("POST", "/ai/match", {"jobId": 99999999, "force": True}, stream=True)
    elif mode == "raw":
        data = json.dumps({"jobId": JOB_ID, "force": False}).encode()
        request = urllib.request.Request(
            BASE + "/ai/match", data=data, method="POST",
            headers={"Content-Type": "application/json", "Authorization": "Bearer " + token()},
        )
        body = urllib.request.urlopen(request, timeout=60).read().decode("utf-8")
        print(repr(body))
        print("done_frames=", body.count("data:[DONE]"))