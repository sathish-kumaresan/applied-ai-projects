# Flood Detection from Satellite Imagery: DALI, TAO, TensorRT and Triton

My notebooks and notes from the NVIDIA Deep Learning Institute course **Disaster Risk Monitoring Using Satellite
Imagery** (certificate, March 2024). Course material © NVIDIA.

The course builds an end-to-end flood segmentation workflow on Sentinel-1 radar (SAR) imagery from the
Sen1Floods11 dataset:

| Notebook | Stage |
|---|---|
| `00_introduction.ipynb` | Environment and the GPU-accelerated toolchain |
| `01_sentinel1_data_and_dali_preprocessing.ipynb` | Sentinel-1 SAR data, tile selection by geographic boundaries, and GPU data loading and augmentation (crop, resize, flip, rotate) with NVIDIA DALI |
| `02_unet_training_with_tao.ipynb` | Transfer learning of a U-Net with a pre-trained ResNet-18 backbone using the TAO Toolkit, then evaluation and export |
| `03_tensorrt_triton_deployment.ipynb` | Optimising the model with TensorRT and serving it with Triton Inference Server |

`course_notes.md` holds my notes on each stage: SAR data, DALI pipelines, TAO spec files, the U-Net model,
model export and the Triton model repository.

The notebooks need NVIDIA's GPU course environment (TAO containers, Triton), so they are stored without outputs.
