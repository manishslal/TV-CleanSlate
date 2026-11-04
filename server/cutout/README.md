# Cut-out service

Lets Home draw a wallpaper's subject in front of the clock, as a phone's lock screen does with a
mountain or a tower. A TV sends the picture it is about to show; the service answers with the
subject's mask. Every TV in a home shows the same pictures each day, so one server cuts each out
once and keeps it.

- Model: ISNet, general use (DIS, Apache-2.0), run with onnxruntime on the CPU.
- Speed: about 2.5 to 6 seconds a picture on a 2018 MacBook Air (i5-8210Y) under Ubuntu, 2.3 on
  an M-series Mac.
- Memory: about 1 GB while it works, 400 MB at rest. Give it a 1.5 GB limit.
- Needs a CPU with AVX2 (onnxruntime's builds). A Celeron J1900 cannot run it.

## Run it

Beside other Docker Compose services:

```yaml
  cutout:
    build: ./cutout
    image: local/home-cutout
    ports: ["8890:8890"]
    volumes: [cutout:/cache]
    environment: [CUTOUT_THREADS=2]
    mem_limit: 1536m
    cpus: 2
    restart: unless-stopped
```

and `cutout: {}` under `volumes:`. Copy this folder next to the compose file, then
`docker compose up -d --build cutout`. Check it with `curl http://<server>:8890/health`.

The service runs as its own user (uid 1000). A cache volume made by an earlier build, which ran as
root, has to be handed over once before upgrading, or every new picture fails:

```bash
docker run --rm --user root --entrypoint chown -v <project>_cutout:/c local/home-cutout -R 1000 /c
```

## Point a TV at it

Android Home, over ADB:

```bash
adb shell am broadcast -a app.orchard.tv.CONFIG -n io.manish.home/app.orchard.tv.guard.ConfigReceiver --es depth_server http://<server>:8890
```

An empty `depth_server` turns it off; `--ez depth_effect false` keeps the server but drops the effect.
The address lives on the TV only, never in this repository.

## API

| Call | Answer |
|---|---|
| `GET /health` | `{"ok": true, "model": ..., "size": [960, 540]}` |
| `POST /cutout` with a JPEG or PNG body | The mask, an 8-bit PNG, 960×540 |
| `GET /cutout?url=<picture>` | The same for a picture from Bing, NASA, Wikimedia or Google Earth View; other hosts are refused |

The mask is the picture cropped to 16:9 from its middle, as Home shows it, white where the subject
is. `X-Subject` says how much of the frame it covers and `X-Solid` how firmly (0 to 1). Home uses
only cut-outs at 0.7 or firmer: the model also marks haze over a river or a sky.

Home draws the subject over the time only when it reaches the time and leaves it readable: between
3% and 35% of the time covered, and no digit more than half hidden. The date and weather stay in
front.
