const MODEL = "gemini-3.5-flash-lite";

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
    if (!env.GEMINI_API_KEY) return json({ error: "На сервере отсутствует секрет GEMINI_API_KEY." }, 503);

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

    const contents = [];
    let totalChars = 0;
    for (const item of body.messages) {
      if (!item || !["user", "model", "assistant"].includes(item.role) ||
          typeof item.content !== "string" || !item.content.trim() || item.content.length > 2000) {
        return json({ error: "Одно из сообщений имеет неверный формат или слишком длинное." }, 400);
      }
      totalChars += item.content.length;
      if (totalChars > 12000) return json({ error: "История слишком длинная. Начни новый диалог." }, 413);
      contents.push({
        role: item.role === "assistant" ? "model" : item.role,
        parts: [{ text: item.content }]
      });
    }

    try {
      const apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/" + MODEL + ":generateContent";
      const requestOptions = {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "x-goog-api-key": env.GEMINI_API_KEY
        },
        body: JSON.stringify({
          systemInstruction: { parts: [{ text: SYSTEM_INSTRUCTION }] },
          contents,
          generationConfig: { maxOutputTokens: 800 }
        })
      };

      // Gemini 503 означает временную перегрузку. Делаем до двух коротких повторов.
      let response;
      for (let attempt = 0; attempt < 3; attempt++) {
        response = await fetch(apiUrl, requestOptions);
        if (response.status !== 503 || attempt === 2) break;
        await new Promise(resolve => setTimeout(resolve, 1200 * (attempt + 1)));
      }

      const result = await response.json().catch(() => ({}));

      if (!response.ok) {
        const providerStatus = result?.error?.status || "UNKNOWN";
        const safeStatus = response.status;
        console.error("Gemini API error", { httpStatus: safeStatus, providerStatus });
        let message = "Сервис Gemini вернул ошибку HTTP " + safeStatus + " (" + providerStatus + ").";
        if (safeStatus === 400) message += " Проверь формат запроса.";
        else if (safeStatus === 401 || safeStatus === 403) message += " Проверь доступ ключа и проекта в Google AI Studio.";
        else if (safeStatus === 404) message += " Модель недоступна для этого проекта.";
        else if (safeStatus === 429) message += " Бесплатная квота или лимит запросов исчерпаны. Попробуй позже.";
        else if (safeStatus === 503) message += " Модель временно перегружена. Сервер уже сделал повторные попытки. Попробуй ещё раз через минуту.";
        const providerMessage = typeof result?.error?.message === "string"
          ? result.error.message.slice(0, 500)
          : "";
        return json({
          error: message,
          providerHttpStatus: safeStatus,
          providerStatus,
          providerMessage
        }, 502);
      }

      const reply = (result?.candidates?.[0]?.content?.parts || [])
        .map(part => typeof part.text === "string" ? part.text : "")
        .join("")
        .trim();

      if (!reply) {
        console.error("Gemini returned no text candidate");
        return json({ error: "Gemini не вернул текст. Попробуй задать вопрос иначе." }, 502);
      }

      return json({ reply });
    } catch (error) {
      console.error("Gemini network error", String(error?.name || "Error"));
      return json({ error: "Cloudflare не смог связаться с Gemini. Попробуй позже." }, 502);
    }
  }
};
