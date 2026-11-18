# Kinship Reasoning on CLUTRR: Direct LLM Answers vs Knowledge-Graph Paths

**Applied AI, Knowledge Management and Reasoning, Luleå University of Technology**

CLUTRR stories describe family members in short sentences and ask how two of them are related, for example
"How is Shantel related to Robert?" (mother-in-law). Answering needs several reasoning hops. The project
compares two ways of using a small local LLM (Qwen2.5-3B-Instruct, served through LM Studio) on 50 CLUTRR samples:

1. **Direct answer:** the LLM reads the story and picks one of 21 relation labels.
2. **Knowledge-graph pipeline:**
   - the LLM extracts relation triples from the story, e.g. `(Robert, has daughter, Michelle)`
   - the triples are scored against the ground-truth graph (precision, recall, F1)
   - a graph search (NetworkX) finds the shortest relation path between the two people
   - the LLM answers from that path only, using a fixed set of allowed inference rules

## Results (50 samples)

| Measure | Value |
|---|---|
| Direct answer accuracy | 34% |
| Path-based answer accuracy | 8% |
| Triple extraction precision / recall / F1 | 0.55 / 0.52 / 0.52 |

The knowledge-graph pipeline did worse than answering directly. The cause is the extraction step: the 3B model
misses about half of the true relations and adds wrong ones, such as reversed or gender-flipped relations.
That often leaves no path between the two people, and the model then has to answer "unknown". The structured
pipeline is only as good as the facts it extracts, so extraction quality is the bottleneck.

## Files

- `kinship_reasoning_clutrr.ipynb`: the full pipeline with per-sample output (story, extracted triples,
  extraction scores, path and both answers) and the summary
- `clutrr_sample_50.csv`: the 50 CLUTRR samples with their ground-truth relation graphs

## Run

Serve an OpenAI-compatible model locally (for example Qwen2.5-3B-Instruct in LM Studio), set `LLM_BASE_URL` if it
is not `http://localhost:1234/v1`, and run the notebook. Results are written to `clutrr_results.csv`.
