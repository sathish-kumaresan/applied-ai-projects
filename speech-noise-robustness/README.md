# Speech Command Classification under Noise: CNN vs BiGRU vs CRNN

**Neural Networks and Learning Machines, Luleå University of Technology** · group project (5 students); I built the CNN.

The project asks which neural architecture stays reliable as audio conditions get worse. Three models are
trained on Google Speech Commands (35 one-second spoken words, 16 kHz). They are then tested with ESC-50
environmental sounds mixed in at 0%, 10% and 50% of the speech RMS. The study repeats this at three model
sizes (about 100K, 500K and 2M parameters), each trained on clean audio and on audio with 50% noise.

## Models

| Model | Architecture |
|---|---|
| CNN | 3 conv blocks → global average pooling → fully connected layers |
| RNN | BatchNorm → bidirectional GRU → fully connected layers |
| CRNN (combined) | CNN front-end → LSTM back-end |

Input: log-mel spectrograms [1 × 64 × 101]. Training: AdamW, cosine learning rate with warm-up, label smoothing.

## Results: test accuracy at 0% / 10% / 50% noise

**Trained on clean audio**

| Size | CNN | RNN (BiGRU) | CRNN |
|---|---|---|---|
| ~100K | 95.1 / 93.0 / 84.5 | 94.1 / 90.4 / 77.4 | 94.9 / 92.1 / 82.6 |
| ~500K | 96.0 / 93.8 / 85.1 | 94.5 / 90.8 / 78.2 | 96.0 / 94.1 / 86.0 |
| ~2M | 98.4 / 94.6 / 82.2 | 98.9 / 94.2 / 82.7 | 99.1 / 96.4 / 85.5 |

**Trained with 50% noise**

| Size | CNN | RNN (BiGRU) | CRNN |
|---|---|---|---|
| ~100K | 91.8 / 92.4 / 88.9 | 91.7 / 91.9 / 87.5 | 90.8 / 90.8 / 87.2 |
| ~2M | 95.2 / 95.1 / 92.7 | 94.3 / 94.0 / 90.6 | 95.2 / 95.1 / 91.7 |

Findings:
- With clean training, the convolutional models degrade much less under noise than the recurrent-only model
  at small and medium sizes.
- Larger models gain mainly on clean audio. Their accuracy under heavy noise barely improves.
- Training with noise gives up a little clean accuracy but makes all models far more robust: the 2M CNN keeps
  92.7% at 50% noise.

Per-run details (parameter counts, most and least robust words) are in `results/`, and figures (accuracy
against noise, confusion matrices, training curves, per-class robustness) are in `figures/`.

## Code

- `speech_noise_robustness.ipynb`: the full study, from data loading to analysis
- `src/`: the same pipeline as modules: `data_pipeline.py` (loading, noise mixing, spectrogram cache),
  `cnn_model.py`, `rnn_model.py`, `combined_model.py`, `evaluate.py` and `utils.py`

## Run

Put Google Speech Commands and ESC-50 under `../data` (or change `DATA_DIR` in the data-pipeline cell), then run
the notebook. Model weights are not included.
