# Breast Cancer Detection from Histopathology Images

**Artificial Intelligence within the Healthcare System, Luleå University of Technology**

Patch-level binary classification (benign vs malignant) of H&E-stained breast
tissue microscopy images at 400× magnification, evaluated under a patient-aware
protocol on the BreaKHis 400× subset.

## Models

Three tiers are compared under identical preprocessing, splitting, and evaluation.
Each is driven by a YAML config in `configs/` and run over five seeds.

- **Tier 1** — PFTAS handcrafted texture features + RBF-kernel SVM. The `C` and
  `gamma` hyperparameters are selected by patient-grouped cross-validation. CPU.
- **Tier 2** — EfficientNetV2-S, ImageNet-pretrained, fine-tuned end-to-end. GPU.
- **Tier 3** — EfficientNetV2-S + CBAM (channel + spatial attention). The backbone
  is identical to Tier 2; a single CBAM module applied to the final feature map
  is the only architectural difference, isolating the effect of attention.

## Evaluation protocols

Each model is evaluated under two protocols:

- **honest** — a patient-disjoint train/test split; no patient appears on both
  sides. Tier 2 and Tier 3 additionally carve a patient-grouped validation set
  from the training split for early stopping.
- **provided** — the dataset's original train/test folders, which share patients
  across the split. Reported alongside the honest protocol to quantify the
  optimism introduced by patient leakage.

Sensitivity, specificity, precision, F1, AUROC, balanced accuracy, and the
confusion matrix are reported at both image and patient level, as mean ± std
over five seeds.

## Running

```
python train.py --tier 1   # PFTAS + RBF SVM
python train.py --tier 2   # EfficientNetV2-S
python train.py --tier 3   # EfficientNetV2-S + CBAM
```

Per-seed predictions, metrics, and checkpoints are written to
`results/runs/tier{N}/`; aggregated summaries to `results/reports/`.
`python make_figures.py` builds the figures and the results table. `aih_colab_notebook.ipynb` runs everything on Colab.

## Results

Five-seed mean ± std, patient-disjoint ("honest") protocol:

| Model | Level | AUROC | Sensitivity | Specificity | F1 |
|---|---|---|---|---|---|
| PFTAS + RBF SVM | Image | 0.720 ± 0.054 | 0.932 ± 0.015 | 0.238 ± 0.114 | 0.752 ± 0.024 |
| EfficientNetV2-S | Image | 0.893 ± 0.020 | 0.939 ± 0.042 | 0.722 ± 0.046 | 0.877 ± 0.013 |
| EfficientNetV2-S + CBAM | Image | 0.884 ± 0.021 | 0.897 ± 0.039 | 0.748 ± 0.034 | 0.863 ± 0.018 |
| PFTAS + RBF SVM | Patient | 0.812 ± 0.100 | 0.945 ± 0.045 | 0.233 ± 0.133 | 0.801 ± 0.043 |
| EfficientNetV2-S | Patient | 0.939 ± 0.021 | 0.945 ± 0.045 | 0.800 ± 0.067 | 0.920 ± 0.033 |
| EfficientNetV2-S + CBAM | Patient | 0.909 ± 0.014 | 0.891 ± 0.036 | 0.833 ± 0.000 | 0.899 ± 0.021 |

With the dataset's provided split, where patients appear on both sides, EfficientNetV2-S reaches an image-level
AUROC of 0.993. Patient leakage inflates results substantially, which is why the patient-disjoint protocol is the
one to trust. Adding CBAM did not improve on the plain EfficientNetV2-S.

The full table (both protocols) is in `figures/table_master_results.tex`. The figures in `figures/` show the
leakage gap, patient-level confusion matrices, probability histograms, error rate per tumour subtype and a
qualitative grid.

## Data

BreaKHis (Spanhol et al., 2016), 400× subset only, under `data/raw/400X/`.
