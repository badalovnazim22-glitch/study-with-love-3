const MODEL = "gemini-3.8-flash";
const SYSTEM_INSTRUCTION = `Ты — доброжелательный ИИ-репетитор приложения Study with Love. Отвечай по-русски, если пользователь не просит другой язык. Объясняй математику и другие учебные темы понятно, по шагам, с небольшими примерами. Не просто выдавай ответ: помогай понять ход решения. Если условие задачи неполное, уточни недостающие данные. Не выдумывай факты и честно отмечай неопределённость. Не проси пароли, адреса, номера документов или другие личные данные. Будь уважительным, спокойным и поддерживающим, без давления и оценок личности.`;

function json(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      "Content-Type": "application/json; charset=utf-8",
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "POST, OPTIONS",
      "Access-Control-Allow-Headers": "Content-Type"
    }
  });
}

export default {
  async fetch(request, env) {
    if (request.method === "OPTIONS") return new Response(null, { status: 204, headers: { "Access-Control-Allow-Origin": "*", "Access-Control-Allow-Methods": "POST, OPTIONS", "Access-Control-Allow-Headers": "Content-Type" } });
    if (request.method !== "POST") return json({ error: "Используй POST." }, 405);
    if (!env.GEMINI_API_KEY) return json({ error: "Сервер ещё не настроен: отсутствует ключ модели." }, 503);

    let body;
    try { body = await request.json(); }
    catch { return json({ error: "Некорректный JSON-запрос." }, 400); }

    if (!Array.isArray(body.messages) || body.messages.length < 1 || body.messages.length > 12) {
      return json({ error: "Передай от 1 до 12 сообщений." }, 400);
    }

    const contents = [];
    let totalChars = 0;
    for (const item of body.messages) {
      if (!item || !["user", "model", "assistant"].includes(item.role) ||
          typeof item.content !== "string" || item.content.trim().length === 0 ||
          item.content.length > 2000) {
        return json({ error: "Одно из сообщений имеет неверный формат или слишком длинное." }, 400);
      }
      totalChars += item.content.length;
      if (totalChars > 12000) return json({ error: "Слишком длинная история чата. Начни новый диалог." }, 413);
      contents.push({
        role: item.role === "assistant" ? "model" : item.role,
        parts: [{ text: item.content }]
      });
    }

    try {
      const response = await fetch(
        "https://generativelanguage.googleapis.com/v1beta/models/" + MODEL + ":generateContent",
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "x-goog-api-key": env.GEMINI_API_KEY
          },
          body: JSON.stringify({
            system_instruction: { parts: [{ text: SYSTEM_INSTRUCTION }] },
            contents,
            generationConfig: { temperature: 0.55, maxOutputTokens: 1200 }
          })
        }
      );

      const result = await response.json();
      if (!response.ok) {
        const message = result?.error?.message || "Провайдер ИИ временно недоступен.";
        return json({ error: message.slice(0, 300) }, response.status === 429 ? 429 : 502);
      }
      const reply = (result?.candidates?.[0]?.content?.parts || [])
        .map(part => typeof part.text === "string" ? part.text : "")
        .join("")
        .trim();
      if (!reply) return json({ error: "Модель не вернула текстовый ответ." }, 502);
      return json({ reply });
    } catch {
      return json({ error: "Не удалось связаться с сервисом ИИ. Попробуй позже." }, 502);
    }
  }
};
