"""Plate colors, shared by the web builds."""


def readable(plate, ink):
    """As on Android: a plate too light for a white logo deepens until it reaches 4.5:1."""
    if not plate or (ink or '').upper() != '#FFFFFF':
        return plate
    rgb = [int(plate[i:i + 2], 16) / 255 for i in (1, 3, 5)]
    light = sum(w * (c / 12.92 if c <= .03928 else ((c + .055) / 1.055) ** 2.4) for w, c in zip((.2126, .7152, .0722), rgb))
    most = 1.05 / 4.5 - .05
    if light <= most:
        return plate
    keep = (most / light) ** (1 / 2.2)
    return '#' + ''.join(f'{round(c * keep * 255):02X}' for c in rgb)
