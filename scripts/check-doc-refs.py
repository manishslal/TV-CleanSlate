#!/usr/bin/env python3
"""Fail when a tracked Markdown file names a repo file in `backticks` that does not exist.

A span counts as a path when it has a slash or ends in a known file extension. It is looked up from the
repo root, then from the document's folder and each folder above it (android/docs/ARCHITECTURE.md can say
`app/src/...`). Skipped: placeholders (<...>, $VAR, {a,b}), globs, URLs, paths on a TV (/media, /var,
~/...), anything git ignores (generated files such as webos/index.html), and fenced code blocks. A
`:12` or `#heading` suffix is stripped.

Usage: scripts/check-doc-refs.py [file.md ...]   (default: every tracked .md)
"""
import fnmatch
import os
import re
import subprocess
import sys
from pathlib import Path

root = Path(__file__).resolve().parent.parent
EXT = {'sh', 'py', 'js', 'md', 'json', 'html', 'kt', 'kts', 'xml', 'conf', 'txt', 'toml', 'yml', 'yaml', 'env',
       'example', 'svg', 'png', 'jpg', 'gradle', 'properties', 'wav', 'ttf', 'webp', 'pem', 'swift', 'cfg', 'css'}


def git(*args, stdin=None):
    return subprocess.run(['git', '-C', str(root), *args], capture_output=True, text=True, input=stdin).stdout


def spans(text):
    """Backticked spans outside fenced code blocks."""
    text = re.sub(r'^(```|~~~).*?^\1', '', text, flags=re.S | re.M)
    return dict.fromkeys(s.strip() for s in re.findall(r'`([^`\n]+)`', text))


def as_path(span):
    """The repo path a span names, or None when it is not something to check."""
    if re.search(r'[\s<>*?{}$|()\[\]=;,"\'\\]', span) or '…' in span or '...' in span:
        return None
    if re.match(r'^(/|~|-|\w+:)', span) and not re.match(r'^[\w.-]+:\d+$', span):
        return None
    span = re.sub(r':\d+(-\d+)?$', '', span).split('#')[0]   # file.kt:12, file.md#heading
    if not span or ':' in span:
        return None
    name = span.rstrip('/').rsplit('/', 1)[-1]
    ext = name.rsplit('.', 1)[-1].lower() if '.' in name.strip('.') else ''
    if ext not in EXT and '/' not in span:
        return None
    if ext not in EXT and '.' in name.strip('.') and not span.endswith('/'):
        return None                                           # dotted ids such as an app's package name
    if ext not in EXT and not span.endswith('/') and span.split('/')[0] not in tops:
        return None                                           # a branch, a GitHub repo, a path on the TV
    if re.fullmatch(r'[\d./x-]+', span):
        return None                                           # versions, ratios, sizes
    return span


def exists(span, doc):
    bases, here = [root], doc.parent
    while here != root:
        bases.append(here)
        here = here.parent
    if any((b / span.rstrip('/')).exists() for b in bases):
        return True
    if '/' in span.strip('/') and span.split('/')[0] in tops:
        return False                      # `scripts/x.sh` or `webos/x`: spelled from the repo root, so it must be exact
    # Docs shorten paths (`ui/theme/Theme.kt`, `install.sh`): a tracked file or folder ending in the span counts.
    tail = '/' + span.strip('/')
    return any(('/' + f).endswith(tail) or (tail + '/') in '/' + f for f in tracked)


tracked = [f for f in git('ls-files').split('\n') if f]
tops = {f.split('/')[0] for f in tracked}


def rooted(doc, path):
    return os.path.normpath(os.path.relpath(doc.parent / path, root)) + ('/' if path.endswith('/') else '')


def allowed():
    """scripts/doc-refs.ignore: `doc glob: token` lines for names that are not files of this repo."""
    rules = []
    f = root / 'scripts/doc-refs.ignore'
    for line in f.read_text().split('\n') if f.exists() else []:
        line = line.split('  #')[0].strip()
        if line and not line.startswith('#'):
            glob, _, token = line.partition(':')
            rules.append((glob.strip(), token.strip() or '*'))
    return rules


def main():
    docs = [Path(a).resolve() for a in sys.argv[1:]] or [root / f for f in git('ls-files', '*.md').split('\n') if f]
    rules = allowed()
    missing = []
    for doc in docs:
        rel = str(doc.relative_to(root))
        for span in spans(doc.read_text(errors='replace')):
            path = as_path(span)
            if path and not exists(path, doc) and not any(fnmatch.fnmatch(rel, g) and (t == "*" or fnmatch.fnmatch(path, t)) for g, t in rules):
                missing.append((doc, path))
    if missing:
        wanted = {p for _, p in missing} | {rooted(d, p) for d, p in missing}
        # one call per path: git aborts the whole batch on a path outside the repo
        ignored = {w for w in wanted if not w.startswith('..') and git('check-ignore', '--no-index', w).strip()}
        missing = [(d, p) for d, p in missing if p not in ignored and rooted(d, p) not in ignored]
    for doc, path in missing:
        print(f'{doc.relative_to(root)}: `{path}` does not exist')
    print(f'{len(missing)} stale reference(s) in {len(docs)} document(s)')
    return 1 if missing else 0


if __name__ == '__main__':
    sys.exit(main())
