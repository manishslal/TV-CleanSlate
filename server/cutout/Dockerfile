FROM python:3.12-slim
RUN pip install --no-cache-dir onnxruntime==1.20.1 pillow==11.0.0 numpy==2.1.3
# ISNet, general use (DIS), as rembg publishes it; Apache-2.0 (xuebinqin/DIS), about 170 MB.
ADD https://github.com/danielgatis/rembg/releases/download/v0.0.0/isnet-general-use.onnx /models/isnet-general-use.onnx
COPY app.py /app/app.py
EXPOSE 8890
CMD ["python", "-u", "/app/app.py"]
