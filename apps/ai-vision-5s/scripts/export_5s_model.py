"""Export a reproducible **toy** 5S ONNX model.

This script builds a *small but genuine* ONNX graph that satisfies the model
contract expected by :class:`app.infrastructure.onnx_backend.OnnxInferenceBackend`:

- **input**  ``input``  : float32 tensor, shape ``[1, 3, H, W]`` (NCHW), RGB,
  pixel values normalized to ``0..1``. ``H`` / ``W`` are dynamic (the backend
  resizes to ``VISION5S_ONNX_INPUT_SIZE``, default 224), so the graph uses
  ``GlobalAveragePool`` and is therefore size-agnostic.
- **output** ``scores`` : float32 tensor, shape ``[1, 5]``, per-pillar scores in
  ``0..1`` for (Seiri, Seiton, Seiso, Seiketsu, Shitsuke). The backend clamps to
  ``0..1`` and scales to the ``0..100`` domain range.

The graph performs a *real* computation (not a constant):

    x            : [1, 3, H, W]                 normalized RGB image
    pooled       : [1, 3, 1, 1]  = GlobalAveragePool(x)      # mean R,G,B
    flat         : [1, 3]        = Flatten(pooled)
    logits       : [1, 5]        = flat @ W + b              # W:[3,5], b:[5]
    scores       : [1, 5]        = Sigmoid(logits)           # 0..1

So the 5 pillar scores are a deterministic, image-dependent function of the
mean R/G/B of the photo. That is obviously **not** a quality model — it is a
*toy* used only to exercise the real ONNX inference path end-to-end (load a real
``.onnx``, run a real ``onnxruntime`` session) in CI, without shipping large
weights or requiring a GPU.

────────────────────────────────────────────────────────────────────────────
Replacing the toy with a real model (production pipeline)
────────────────────────────────────────────────────────────────────────────
The production backend is a **YOLOv8 fine-tuned on 5S labels** (CLAUDE.md §3.2,
§12.1). The training pipeline (dataset NOT shipped) is:

1. Collect & label workplace photos per pillar (Seiri/Seiton/Seiso/Seiketsu/
   Shitsuke): clutter, mislocated tools, dirt, missing markings, missing audit
   evidence. Bounding boxes for detections + a per-pillar score head.
2. Train with Ultralytics YOLOv8 (or a CV backbone + 5-logit regression head):
       yolo detect train data=5s.yaml model=yolov8n.pt imgsz=224 epochs=...
3. Export to ONNX, **keeping the contract above** (input NCHW float32 0..1,
   output 1x5 pillar scores 0..1 — add a small adapter head if the detector's
   native output differs):
       yolo export model=best.pt format=onnx imgsz=224 opset=17
4. Validate the exported graph against this contract (see ``tests`` and the
   runbook ``docs/runbooks/vision-5s.md`` §2), then ship the ``.onnx`` and set
   ``VISION5S_ONNX_MODEL_PATH`` in prod (mounted read-only volume).

Usage::

    python scripts/export_5s_model.py [--out models/vision5s-toy.onnx]

Requires the ``onnx`` package (``pip install onnx``). ``onnxruntime`` is only
needed to *run* the model, not to build it.
"""

from __future__ import annotations

import argparse
import hashlib
import os
import sys

# Deterministic toy weights — fixed, no randomness, so the committed .onnx is
# byte-stable and re-exportable identically. Shape [3, 5] (in_features=3 mean
# RGB channels -> 5 pillar logits). Hand-picked so a mid-gray image yields a
# spread of scores across pillars (not all identical), which makes the toy
# visibly "do something" in tests.
_WEIGHTS = [
    # seiri  seiton  seiso  seiket shitsu
    [2.0, -1.5, 0.5, 1.0, -0.5],   # R channel
    [-1.0, 2.0, 1.5, -0.5, 0.5],   # G channel
    [0.5, 0.5, -2.0, 1.5, 2.0],    # B channel
]
_BIAS = [0.1, -0.2, 0.3, -0.1, 0.0]

_INPUT_NAME = "input"
_OUTPUT_NAME = "scores"
_OPSET = 17
_DEFAULT_OUT = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "models",
    "vision5s-toy.onnx",
)


def build_model():
    """Build and return the toy 5S ONNX ModelProto."""
    import onnx  # local import: only needed to build, keeps deps optional
    from onnx import TensorProto, helper, numpy_helper

    import numpy as np

    w = np.asarray(_WEIGHTS, dtype=np.float32)  # [3, 5]
    b = np.asarray(_BIAS, dtype=np.float32)     # [5]

    w_init = numpy_helper.from_array(w, name="W")
    b_init = numpy_helper.from_array(b, name="B")

    # input: [1, 3, H, W] — H/W dynamic so any resize size works.
    inp = helper.make_tensor_value_info(
        _INPUT_NAME, TensorProto.FLOAT, [1, 3, "H", "W"]
    )
    out = helper.make_tensor_value_info(
        _OUTPUT_NAME, TensorProto.FLOAT, [1, 5]
    )

    nodes = [
        # [1,3,H,W] -> [1,3,1,1] mean over spatial dims
        helper.make_node("GlobalAveragePool", [_INPUT_NAME], ["pooled"]),
        # -> [1,3]
        helper.make_node("Flatten", ["pooled"], ["flat"], axis=1),
        # [1,3] @ [3,5] -> [1,5]
        helper.make_node("MatMul", ["flat", "W"], ["matmul"]),
        # + bias broadcast
        helper.make_node("Add", ["matmul", "B"], ["logits"]),
        # squash to 0..1
        helper.make_node("Sigmoid", ["logits"], [_OUTPUT_NAME]),
    ]

    graph = helper.make_graph(
        nodes,
        name="vision5s_toy",
        inputs=[inp],
        outputs=[out],
        initializer=[w_init, b_init],
    )
    model = helper.make_model(
        graph,
        producer_name="qualitos-export-5s-toy",
        opset_imports=[helper.make_opsetid("", _OPSET)],
    )
    # IR version pinned for byte-stability across onnx releases.
    model.ir_version = 9
    onnx.checker.check_model(model)
    return model


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Export the toy 5S ONNX model.")
    parser.add_argument(
        "--out", default=_DEFAULT_OUT,
        help=f"Output .onnx path (default: {_DEFAULT_OUT})",
    )
    args = parser.parse_args(argv)

    try:
        import onnx  # noqa: F401
    except Exception as exc:  # pragma: no cover - guidance only
        print(
            "ERROR: the 'onnx' package is required to export the model.\n"
            "       Install it with: pip install onnx\n"
            f"       ({exc})",
            file=sys.stderr,
        )
        return 2

    model = build_model()

    os.makedirs(os.path.dirname(os.path.abspath(args.out)), exist_ok=True)
    import onnx as _onnx
    _onnx.save_model(model, args.out)

    data = open(args.out, "rb").read()
    print(f"Wrote {args.out} ({len(data)} bytes)")
    print(f"  sha256: {hashlib.sha256(data).hexdigest()}")
    print(f"  input : {_INPUT_NAME} float32 [1,3,H,W] (NCHW, 0..1)")
    print(f"  output: {_OUTPUT_NAME} float32 [1,5] (per-pillar 0..1)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
