#!/usr/bin/env python3
"""Compute measured metrics only from matching, human-annotated gold and run traces."""
import argparse
import json
import math
from pathlib import Path

TARGETS = {
    "memory_recall_at_5": (0.85, ">="),
    "error_memory_misuse_rate": (0.05, "<="),
    "followup_relevance_rate": (0.80, ">="),
    "score_agreement_within_1": (0.85, ">="),
}


def load_jsonl(path):
    items = {}
    for number, line in enumerate(Path(path).read_text(encoding="utf-8").splitlines(), 1):
        if not line.strip():
            continue
        item = json.loads(line)
        case_id = item.get("case_id")
        if not isinstance(case_id, str) or not case_id or case_id in items:
            raise ValueError(f"{path}:{number}: missing or duplicate case_id")
        items[case_id] = item
    return items


def wilson(successes, total):
    if not total:
        return None
    z = 1.96
    p = successes / total
    d = 1 + z * z / total
    c = (p + z * z / (2 * total)) / d
    h = z * math.sqrt(p * (1-p) / total + z*z/(4*total*total)) / d
    return [round(max(0, c-h), 4), round(min(1, c+h), 4)]


def ratio(numerator, denominator):
    return {"numerator": numerator, "denominator": denominator,
            "value": round(numerator/denominator, 4) if denominator else None,
            "wilson_95_ci": wilson(numerator, denominator)}


def evaluate(gold, predictions):
    if set(gold) != set(predictions):
        missing = sorted(set(gold) - set(predictions))
        extra = sorted(set(predictions) - set(gold))
        raise ValueError(f"case_id mismatch; missing predictions={missing}, extra predictions={extra}")
    recall_hit = recall_total = misuse_hit = misuse_total = 0
    relevant_hit = relevant_total = agreement_hit = agreement_total = 0
    for case_id, label in gold.items():
        pred = predictions[case_id]
        retrieved = pred.get("retrieved_memory_ids")
        if not isinstance(retrieved, list) or not all(isinstance(x,str) for x in retrieved):
            raise ValueError(f"{case_id}: retrieved_memory_ids must be a string list")
        retrieved = set(retrieved[:5])
        required = set(label.get("required_memory_ids", []))
        forbidden = set(label.get("forbidden_memory_ids", []))
        recall_hit += len(required & retrieved)
        recall_total += len(required)
        if forbidden:
            misuse_total += 1
            misuse_hit += bool(forbidden & retrieved)
        relevance = label.get("followup_relevant")
        if relevance is not None:
            if not isinstance(relevance, bool):
                raise ValueError(f"{case_id}: followup_relevant must be true/false/null")
            relevant_total += 1
            relevant_hit += relevance
        human = label.get("human_score")
        model = pred.get("model_score")
        if human is not None:
            if not isinstance(human,int) or not isinstance(model,int) or not 0 <= human <= 4 or not 0 <= model <= 4:
                raise ValueError(f"{case_id}: scores must be integers 0..4")
            agreement_total += 1
            agreement_hit += abs(human-model) <= 1
    metrics = {
        "memory_recall_at_5": ratio(recall_hit,recall_total),
        "error_memory_misuse_rate": ratio(misuse_hit,misuse_total),
        "followup_relevance_rate": ratio(relevant_hit,relevant_total),
        "score_agreement_within_1": ratio(agreement_hit,agreement_total),
    }
    enough = len(gold) >= 50 and all(m["denominator"] >= 20 for m in metrics.values())
    for name, metric in metrics.items():
        target, direction = TARGETS[name]
        metric["target"] = f"{direction}{target:.0%}"
        metric["target_met"] = None if not enough else (metric["value"] >= target if direction == ">=" else metric["value"] <= target)
    return {"case_count":len(gold),"status":"evaluated" if enough else "insufficient_sample",
            "note":"Targets are goals, not observed results. Requires >=50 cases and >=20 opportunities per metric.",
            "metrics":metrics}


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--gold", required=True)
    p.add_argument("--predictions", required=True)
    p.add_argument("--output")
    args = p.parse_args()
    result = evaluate(load_jsonl(args.gold),load_jsonl(args.predictions))
    rendered = json.dumps(result,ensure_ascii=False,indent=2)
    if args.output:
        Path(args.output).write_text(rendered+"\n",encoding="utf-8")
    print(rendered)


if __name__ == "__main__": main()
