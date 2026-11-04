FROM python:3.12-slim
RUN pip install --no-cache-dir onnxruntime==1.20.1 pillow==11.0.0 numpy==2.1.3
# ISNet, general use (DIS), as rembg publishes it; Apache-2.0 (xuebinqin/DIS), about 170 MB. Checked
# against the file this service was tested with.
ADD --checksum=sha256:60920e99c45464f2ba57bee2ad08c919a52bbf852739e96947fbb4358c0d964a https://github.com/danielgatis/rembg/releases/download/v0.0.0/isnet-general-use.onnx /models/isnet-general-use.onnx
COPY app.py /app/app.py
# Not as root: its own user, owning only the cut-out cache.
RUN useradd --uid 1000 --no-create-home cutout && mkdir -p /cache && chown cutout /cache && chmod 444 /models/isnet-general-use.onnx
USER cutout
EXPOSE 8890
CMD ["python", "-u", "/app/app.py"]
