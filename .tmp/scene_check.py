"""临时脚本：验证 drawer 场景下线后的参数校验行为（不触发 AI 调用）。"""
import json
import sys
import urllib.error
import urllib.request

sys.path.insert(0, ".tmp")
import ai_sse_check as m  # noqa: E402

m.USER_ID = "36"
TOKEN = m.token()
URL = "http://localhost:8081/api/ai/chat"


def probe(scene: str) -> str:
    body = json.dumps({"scene": scene, "input": "x"}).encode()
    req = urllib.request.Request(
        URL,
        data=body,
        method="POST",
        headers={"Content-Type": "application/json", "Authorization": "Bearer " + TOKEN},
    )
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            return f"{resp.status} (stream started)"
    except urllib.error.HTTPError as error:
        return f"{error.code} {error.read().decode()[:100]}"


for value in ("interpret", "drawer", "0"):
    print(f"scene={value!r} -> {probe(value)}")