#!/usr/bin/env python3
"""Validate the bundled Study with Love question bank before Android builds."""
import json
import sys
from collections import Counter, defaultdict
from pathlib import Path

BANK = Path("app/src/main/assets/question_bank.json")
EXPECTED_TOPICS = 41
EXPECTED_PER_TOPIC = 8
VALID_TYPES = {"choice", "input", "true_false", "find_error"}


def fail(errors):
    for error in errors:
        print(f"ERROR: {error}")
    raise SystemExit(1)


def main():
    try:
        bank = json.loads(BANK.read_text(encoding="utf-8"))
    except Exception as exc:
        fail([f"Cannot parse {BANK}: {exc}"])
    questions = bank.get("questions")
    if not isinstance(questions, list) or not questions:
        fail(["'questions' must be a non-empty JSON array"])

    errors = []
    per_topic = Counter()
    per_type = Counter()
    tf = Counter()
    seen = set()

    for index, question in enumerate(questions, start=1):
        where = f"question #{index}"
        if not isinstance(question, dict):
            errors.append(f"{where} is not an object")
            continue
        topic = question.get("topic")
        kind = question.get("type")
        prompt = question.get("prompt")
        answer = question.get("answer")
        if not isinstance(topic, str) or not topic.strip():
            errors.append(f"{where}: missing topic")
            continue
        if not isinstance(prompt, str) or not prompt.strip():
            errors.append(f"{where}: missing prompt")
            continue
        if not isinstance(answer, str) or not answer.strip():
            errors.append(f"{where}: missing answer")
            continue
        if kind not in VALID_TYPES:
            errors.append(f"{where} ({topic}): unsupported type {kind!r}")
        per_topic[topic] += 1
        per_type[kind] += 1
        key = (topic.casefold(), kind, prompt.strip().casefold())
        if key in seen:
            errors.append(f"{where} ({topic}): duplicate prompt")
        seen.add(key)

        options = question.get("options")
        if kind in {"choice", "find_error", "true_false"}:
            if not isinstance(options, list) or len(options) < 2:
                errors.append(f"{where} ({topic}): {kind} needs at least 2 options")
            else:
                normalized = [str(option).strip() for option in options]
                if len(set(normalized)) != len(normalized):
                    errors.append(f"{where} ({topic}): duplicate options")
                if answer.strip() not in normalized:
                    errors.append(f"{where} ({topic}): answer is not present in options")
        if kind == "true_false":
            if not {"Верно", "Неверно"}.issubset(set(options or [])):
                errors.append(f"{where} ({topic}): true/false options must contain both labels")
            tf[answer.strip()] += 1

    if len(per_topic) != EXPECTED_TOPICS:
        errors.append(f"expected {EXPECTED_TOPICS} topics, found {len(per_topic)}")
    for topic, count in sorted(per_topic.items()):
        if count != EXPECTED_PER_TOPIC:
            errors.append(f"{topic}: expected {EXPECTED_PER_TOPIC} questions, found {count}")
    if tf["Верно"] == 0 or tf["Неверно"] == 0:
        errors.append("true/false bank must contain both correct and incorrect statements")
    if sum(tf.values()) and min(tf["Верно"], tf["Неверно"]) / sum(tf.values()) < 0.25:
        errors.append(f"true/false answers are unbalanced: {dict(tf)}")

    if errors:
        fail(errors)
    print(
        f"Question bank OK: {len(questions)} questions, {len(per_topic)} topics; "
        f"types={dict(per_type)}; true/false={dict(tf)}"
    )


if __name__ == "__main__":
    main()
