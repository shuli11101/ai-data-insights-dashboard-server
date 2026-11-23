SELECT '--- conversations ---' AS section;
SELECT id, conversation_id, user_id, scene, title, update_time
FROM ai_conversation
WHERE user_id = 41
ORDER BY id DESC
LIMIT 5;

SELECT '--- messages of latest 3 ---' AS section;
SELECT conversation_pk, seq, role, LEFT(content, 24) AS preview
FROM ai_message
WHERE conversation_pk IN (SELECT id FROM ai_conversation WHERE user_id = 41 ORDER BY id DESC LIMIT 3)
ORDER BY conversation_pk, seq;

SELECT '--- duplicate seq check ---' AS section;
SELECT conversation_pk, seq, COUNT(*) AS cnt
FROM ai_message
GROUP BY conversation_pk, seq
HAVING cnt > 1;