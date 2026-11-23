const MODEL = "@cf/zai-org/glm-4.7-flash";

const SYSTEM_INSTRUCTION = `Ты — доброжелательный ИИ-репетитор приложения Study with Love. Отвечай по-русски, если пользователь не просит другой язык. Объясняй математику и другие учебные темы понятно, по шагам, с небольшими примерами. Не просто выдавай ответ: помогай понять ход решения. Если условие задачи неполное, уточни недостающие данные. Не выдумывай факты и честно отмечай неопределённость. Не проси пароли, адреса, номера документов или другие личные данные. Будь уважительным, спокойным и поддерживающим, без давления и оценок личности.`;

function json(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      "Content-Type": "application/json; charset=utf-8",
      "Cache-Control": "no-store",
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "POST, OPTIONS",
      "Access-Control-Allow-Headers": "Content-Type"
    }
  });
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    if (request.method === "OPTIONS") {
      return new Response(null, {
        status: 204,
        headers: {
          "Access-Control-Allow-Origin": "*",
          "Access-Control-Allow-Methods": "POST, OPTIONS",
          "Access-Control-Allow-Headers": "Content-Type"
        }
      });
    }

    if (url.pathname !== "/chat") return json({ error: "Адрес не найден." }, 404);
    if (request.method !== "POST") return json({ error: "Используй POST." }, 405);
    if (!env.AI || typeof env.AI.run !== "function") {
      return json({ error: "Не подключён Cloudflare Workers AI. Добавь AI binding с именем AI в настройках Worker." }, 503);
    }

    let body;
    try {
      const raw = await request.text();
      if (raw.length > 30000) return json({ error: "Запрос слишком большой." }, 413);
      body = JSON.parse(raw);
    } catch {
      return json({ error: "Некорректный JSON-запрос." }, 400);
    }

    if (!Array.isArray(body.messages) || body.messages.length < 1 || body.messages.length > 12) {
      return json({ error: "Передай от 1 до 12 сообщений." }, 400);
    }

    const messages = [{ role: "system", content: SYSTEM_INSTRUCTION }];
    let totalChars = 0;
    for (const item of body.messages) {
      if (!item || !["user", "model", "assistant"].includes(item.role) ||
          typeof item.content !== "string" || !item.content.trim() || item.content.length > 6000) {
        return json({ error: "Одно из сообщений имеет неверный формат или слишком длинное." }, 400);
      }
      totalChars += item.content.length;
      if (totalChars > 24000) return json({ error: "История слишком длинная. Начни новый диалог." }, 413);
      messages.push({
        role: item.role === "model" ? "assistant" : item.role,
        content: item.content
      });
    }

    try {
      let result;
      let lastError;
      // Four attempts with exponential backoff for temporary Workers AI overload.
      for (let attempt = 0; attempt < 4; attempt++) {
        try {
          result = await env.AI.run(MODEL, {
            messages,
            max_tokens: 1200,
            temperature: 0.6
          });
          break;
        } catch (error) {
          lastError = error;
          if (attempt === 3) throw error;
          const delay = 1200 * (2 ** attempt);
          console.warn(`Workers AI attempt ${attempt + 1} failed; retrying in ${delay}ms`);
          await new Promise(resolve => setTimeout(resolve, delay));
        }
      }

      const reply = result?.choices?.[0]?.message?.content ??
        result?.response ??
        result?.output_text ??
        "";
      const text = typeof reply === "string" ? reply.trim() : "";
      if (!text) {
        console.error("Workers AI returned no text");
        return json({ error: "ИИ не вернул текстовый ответ. Попробуй задать вопрос иначе." }, 502);
      }
      return json({ reply: text });
    } catch (error) {
      const message = String(error?.message || error || "unknown").slice(0, 400);
      console.error("Workers AI error", message);
      return json({
        error: "Cloudflare Workers AI временно перегружен или недоступен. Worker уже повторил запрос несколько раз. Подожди 10–20 секунд и попробуй снова.",
        providerMessage: message
      }, 503);
    }
  }
};
