# Text-to-Image Diffusion from Scratch: U-Net, DDPM, Guidance and CLIP

My completed notebooks for the NVIDIA Deep Learning Institute course **Generative AI with Diffusion Models**
(certificate, April 2025). Course material © NVIDIA. The notebooks include my own training runs and generated
samples.

The five notebooks build a text-conditioned image generator step by step in PyTorch:

| Notebook | What it builds |
|---|---|
| `01_unet_denoising.ipynb` | A U-Net (down blocks, up blocks, skip connections) trained to remove noise from FashionMNIST images |
| `02_ddpm_diffusion.ipynb` | A DDPM: forward diffusion with a noise schedule, the closed-form jump to any timestep, a noise-prediction loss and reverse diffusion to generate new images |
| `03_architecture_optimizations.ipynb` | A stronger U-Net: group normalization, GELU, rearrange pooling, sinusoidal time embeddings and residual connections |
| `04_classifier_free_guidance.ipynb` | Class-conditional generation with classifier-free guidance, trained on FashionMNIST and then on a flowers dataset |
| `05_clip_text_conditioning.ipynb` | CLIP image and text encodings, similarity between them, and a CLIP-embedded dataset for text-to-image conditioning |

Each notebook keeps its outputs: training curves, denoising steps and generated images.

## Run

The notebooks were run in NVIDIA's GPU course environment. Locally they need PyTorch, torchvision, matplotlib and
OpenAI's `clip` package, plus a GPU for reasonable training times.
