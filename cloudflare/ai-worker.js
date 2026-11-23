const MODEL = "@cf/zai-org/glm-4.7-flash";

// Best-effort fallback for deployments without a Cloudflare Rate Limiting binding.
// Configure env.RATE_LIMITER for an edge-enforced limit shared by the Worker service.
const localRateBuckets = new Map();
const LOCAL_LIMIT = 12;
const LOCAL_WINDOW_MS = 60_000;

async function isRateLimited(request, env) {
  const ip = request.headers.get("CF-Connecting-IP") || "unknown";
  if (env.RATE_LIMITER && typeof env.RATE_LIMITER.limit === "function") {
    try {
      const result = await env.RATE_LIMITER.limit({ key: ip });
      return !result.success;
    } catch (error) {
      console.warn("Rate limiting binding failed; using isolate-local fallback", String(error?.message || error));
    }
  }
  const now = Date.now();
  let bucket = localRateBuckets.get(ip);
  if (!bucket || now - bucket.start >= LOCAL_WINDOW_MS) {
    bucket = { start: now, count: 0 };
    localRateBuckets.set(ip, bucket);
  }
  bucket.count++;
  if (localRateBuckets.size > 2000) {
    for (const [key, value] of localRateBuckets) {
      if (now - value.start >= LOCAL_WINDOW_MS) localRateBuckets.delete(key);
    }
  }
  return bucket.count > LOCAL_LIMIT;
}

const SYSTEM_INSTRUCTION = `Ты — доброжелательный ИИ-репетитор приложения Study with Love. Отвечай по-русски, если пользователь не просит другой язык. Объясняй математику и другие учебные темы понятно, по шагам, с небольшими примерами. Не просто выдавай ответ: помогай понять ход решения. Если условие задачи неполное, уточни недостающие данные. Не выдумывай факты и честно отмечай неопределённость. Не проси пароли, адреса, номера документов или другие личные данные. Будь уважительным, спокойным и поддерживающим, без давления и оценок личности.`;

function json(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      "Content-Type": "application/json; charset=utf-8",
      "Cache-Control": "no-store",
    }
  });
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    // This endpoint is used by the native Android app; no wildcard browser CORS is needed.\n    if (request.method === "OPTIONS") return new Response(null, { status: 405 });

    if (url.pathname !== "/chat") return json({ error: "Адрес не найден." }, 404);
    if (request.method !== "POST") return json({ error: "Используй POST." }, 405);
    if (await isRateLimited(request, env)) {\n      return json({ error: "Слишком много запросов. Подожди минуту и попробуй снова." }, 429);\n    }\n    if (!env.AI || typeof env.AI.run !== "function") {
      return json({ error: "Не подключён Cloudflare Workers AI. Добавь AI binding с именем AI в настройках Worker." }, 503);
    }

    let body;
    try {
      const raw = await request.text();
      if (raw.length > 40000) return json({ error: "Запрос слишком большой." }, 413);
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
          typeof item.content !== "string" || !item.content.trim() || item.content.length > 8000) {
        return json({ error: "Одно из сообщений имеет неверный формат или слишком длинное." }, 400);
      }
      totalChars += item.content.length;
      if (totalChars > 30000) return json({ error: "История слишком длинная. Начни новый диалог." }, 413);
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
            max_tokens: 1600,
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

      let reply = result?.choices?.[0]?.message?.content;
      if (typeof reply !== "string" || !reply.trim()) {
        reply = result?.response ?? result?.output_text ?? "";
      }
      if (Array.isArray(reply)) {
        reply = reply.map(part => typeof part === "string" ? part : (part?.text || "")).join("");
      }
      const text = typeof reply === "string" ? reply.trim() : "";
      if (!text) {
        console.error("Workers AI returned no visible answer", { finishReason: result?.choices?.[0]?.finish_reason || "unknown" });
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
