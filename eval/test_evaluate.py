import unittest
from evaluate import evaluate


class EvaluationTest(unittest.TestCase):
    def test_metrics_and_denominators(self):
        gold = {"a":{"required_memory_ids":["m1","m2"],"forbidden_memory_ids":["bad"],
                      "followup_relevant":True,"human_score":3}}
        pred = {"a":{"retrieved_memory_ids":["m1","bad"],"model_score":2}}
        result = evaluate(gold,pred)
        self.assertEqual(result["status"],"insufficient_sample")
        self.assertEqual(result["metrics"]["memory_recall_at_5"]["value"],0.5)
        self.assertEqual(result["metrics"]["error_memory_misuse_rate"]["value"],1)
        self.assertIsNone(result["metrics"]["score_agreement_within_1"]["target_met"])

    def test_mismatched_cases_fail(self):
        with self.assertRaises(ValueError): evaluate({"a":{}},{})


if __name__ == "__main__": unittest.main()
