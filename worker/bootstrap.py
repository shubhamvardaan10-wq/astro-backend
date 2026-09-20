import hashlib
import importlib.metadata
from pathlib import Path
import urllib.request

ROOT = Path(__file__).resolve().parent
ASSETS = {
    "sepl_18.se1": ("https://raw.githubusercontent.com/aloistr/swisseph/master/ephe/sepl_18.se1", "ca1393ceab3a44fbc895887cf789c68819ae6a1cbc9b22225872dbe4ccd99a66"),
    "semo_18.se1": ("https://raw.githubusercontent.com/aloistr/swisseph/master/ephe/semo_18.se1", "1ca07bd67c24374d77226180c20a4f9996cba013697894810518e7eb582ca4f7"),
}


def install():
    directory = ROOT / "ephe"
    directory.mkdir(exist_ok=True)
    for name, (url, checksum) in ASSETS.items():
        target = directory / name
        if target.exists():
            data = target.read_bytes()
        else:
            with urllib.request.urlopen(url, timeout=60) as response:
                data = response.read(4 * 1024 * 1024)
        if hashlib.sha256(data).hexdigest() != checksum:
            raise RuntimeError(f"Checksum mismatch for {name}; existing files were not overwritten")
        if not target.exists():
            with target.open("xb") as output:
                output.write(data)
        print(f"Verified {name}")
    distribution = importlib.metadata.distribution("PyJHora")
    license_file = next(p for p in distribution.files if str(p).endswith("dist-info/licenses/LICENSE"))
    license_text = distribution.locate_file(license_file).read_bytes()
    target = ROOT.parent / "LICENSE"
    if not target.exists():
        with target.open("xb") as output:
            output.write(license_text)
    elif target.read_bytes() != license_text:
        raise RuntimeError("Existing project LICENSE differs; it was not overwritten")


if __name__ == "__main__":
    install()
