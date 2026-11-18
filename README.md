# Applied AI Projects

Selected projects from my MSc in Applied Artificial Intelligence at Luleå University of Technology.
Each folder has its own README with setup, method and results. My production work in industry (AI agents,
LLM/RAG applications and voice agents at BR Technologies) is proprietary and not included here.

| Project | Course | What it does | Methods |
|---|---|---|---|
| [Anchor-based object recognition](anchor-object-recognition/) | Computer Vision and Image Processing | Finds one specific object in RGB-D video from three anchor photos and rejects printed look-alike decoys. CPU only, no training. | DINOv2, SIFT, colour histograms, depth cues |
| [Car detection in snow](car-detection-in-snow/) | Advanced Deep Learning | Detects cars in snowy drone footage (Nordic Vehicle Dataset), comparing augmentation strategies and a light-to-heavy snow domain shift. | YOLOv9, snow augmentation, Optuna tuning |
| [Edge-ready ECG classification](edge-ecg-classification/) | Advanced Data Mining | Inter-patient heartbeat classification (MIT-BIH) with a 10k-parameter 1D CNN, quantised for the MAX78002 microcontroller, with a noise-robustness test. | 1D CNN, INT8/INT16 quantisation, QAT, CRISP-DM |
| [Speech commands under noise](speech-noise-robustness/) | Neural Networks and Learning Machines | Compares CNN, BiGRU and CRNN models at three sizes, trained clean and with noise, tested at 0%, 10% and 50% environmental noise. | CNN, BiGRU, CRNN, spectrograms |
| [Breast cancer histopathology](breast-cancer-histopathology/) | AI within the Healthcare System | Benign vs malignant classification on BreaKHis 400× under a patient-disjoint protocol, across three model tiers and five seeds. | PFTAS + SVM, EfficientNetV2-S, CBAM |
| [Credit card fraud detection](credit-card-fraud-detection/) | Data Science Programming | Detects fraud in highly imbalanced transactions, comparing unsupervised, supervised and hybrid models at fixed false-positive rates. | Autoencoder, VAE, One-Class SVM, XGBoost, stacking |
| [LLM kinship reasoning](llm-kinship-reasoning/) | Applied AI, Knowledge Management and Reasoning | Compares direct LLM answers with a knowledge-graph pipeline (triple extraction, graph path search, constrained answering) on multi-hop CLUTRR questions. | Local LLM (Qwen2.5-3B), knowledge graphs, NetworkX |

## NVIDIA Deep Learning Institute

[nvidia-dli-rag-agents](nvidia-dli-rag-agents/) holds my completed work for **Building RAG Agents with LLMs**, in four stages:
LLM endpoints and LangChain (LCEL), stateful chains and summarization, embeddings and FAISS vector stores, and a full
RAG pipeline with LLM-as-judge evaluation. I also hold NVIDIA DLI certificates in Generative AI with Diffusion
Models, Building LLM Applications with Prompt Engineering, and Disaster Risk Monitoring Using Satellite Imagery.

## Group work

Most of these were group projects; each README names the group.

## Data

Datasets are not included. Each project explains where to download them (Google Speech Commands, ESC-50,
MIT-BIH, BreaKHis, NVD, Kaggle credit card fraud, course recordings).
