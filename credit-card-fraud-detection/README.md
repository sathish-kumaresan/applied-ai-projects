# Credit Card Fraud Detection: Anomaly Detection on Imbalanced Data

**Data Science Programming (D7054E), Luleå University of Technology** · group project

Fraud makes up about 0.17% of the transactions in the Kaggle Credit Card Fraud dataset, so accuracy is
meaningless and a model has to catch fraud while raising very few false alarms. The project compares
unsupervised anomaly detectors, supervised classifiers and hybrids that combine the two.

## Models

- **Unsupervised**, trained on legitimate transactions only: Autoencoder, Variational Autoencoder (VAE),
  One-Class SVM
- **Supervised baselines:** Logistic Regression, Random Forest, XGBoost
- **Hybrids:** anomaly scores stacked with the original features through a Random Forest meta-learner

## Results

Models are compared on AUPRC, ROC-AUC and recall at a fixed false-positive rate (FPR), the metrics that matter
when alerts have to be reviewed by people.

| Model | AUPRC | ROC-AUC | Recall @ 0.1% FPR | Recall @ 0.5% FPR |
|---|---|---|---|---|
| Autoencoder | 0.244 | 0.919 | 0.305 | 0.568 |
| VAE | 0.308 | 0.924 | 0.347 | 0.674 |
| One-Class SVM | 0.279 | 0.921 | 0.337 | 0.758 |
| Logistic Regression | 0.675 | 0.965 | 0.789 | 0.832 |
| Random Forest | 0.815 | 0.934 | 0.821 | 0.842 |
| XGBoost | 0.812 | 0.977 | 0.811 | 0.842 |
| Hybrid (unsupervised scores) | 0.817 | 0.940 | 0.821 | 0.842 |
| **Hybrid (stacking)** | **0.838** | 0.956 | **0.842** | **0.863** |

The stacking hybrid gives the best AUPRC and the highest recall at low false-positive rates. The purely
unsupervised models reach a high ROC-AUC but a low AUPRC, which shows why ROC-AUC alone is misleading on data
this imbalanced.

`figures/` holds the PR and ROC curves, confusion matrices, threshold analysis, the autoencoder latent space and
reconstruction-error distributions.

## Code

- `loader.py`: downloads the dataset with the Kaggle CLI and prepares the splits
- `models.py`: Autoencoder, VAE, One-Class SVM, supervised models and hybrids
- `visualizer.py`: all figures
- `main.py`: runs the full comparison

## Run

```bash
pip install torch scikit-learn xgboost pandas matplotlib seaborn kaggle
python main.py
```

The dataset ([Kaggle: mlg-ulb/creditcardfraud](https://www.kaggle.com/datasets/mlg-ulb/creditcardfraud)) is
downloaded automatically and needs a Kaggle API token.
